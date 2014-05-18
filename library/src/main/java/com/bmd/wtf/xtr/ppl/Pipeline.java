/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.xtr.ppl;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.bdr.DuplicateDamException;
import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Spring;

import java.util.WeakHashMap;

/**
 * A pipeline object is used to create a connection between two parts of the
 * {@link com.bmd.wtf.Waterfall}, where the data flow within a different asynchronous mechanism.
 * <p/>
 * Flowing data are filtered in a thread safe way by the use of {@link Duct}s.
 * <p/>
 * For consistency, like happens with {@link com.bmd.wtf.dam.Dam}s, also Duct instances are
 * allowed just one time.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <SOURCE> The spring data type.
 * @param <IN>     The input data type of the upstream pool.
 * @param <OUT>    The transported data type, that is the output data type of the upstream pool.
 */
public class Pipeline<SOURCE, IN, OUT> {

    private static final WeakHashMap<Duct<?, ?>, Void> sDucts = new WeakHashMap<Duct<?, ?>, Void>();

    private final Stream<SOURCE, IN, OUT> mInputStream;

    /**
     * Private constructor to avoid direct instantiation.
     *
     * @param inputStream The pipeline input stream.
     */
    private Pipeline(final Stream<SOURCE, IN, OUT> inputStream) {

        if (inputStream == null) {

            throw new IllegalArgumentException("the input stream cannot be null");
        }

        mInputStream = inputStream;
    }

    /**
     * Creates a pipeline bound to the specified stream.
     *
     * @param stream   The data source stream.
     * @param <SOURCE> The spring data type.
     * @param <IN>     The input data type of the upstream pool.
     * @param <NOUT>   The transported data type, that is the output data type of the upstream pool.
     * @return The new pipeline.
     */
    public static <SOURCE, IN, NOUT> Pipeline<SOURCE, IN, NOUT> binding(
            final Stream<SOURCE, IN, NOUT> stream) {

        return new Pipeline<SOURCE, IN, NOUT>(stream);
    }

    /**
     * Connects the specified duct to this pipeline by making data flowing through it.
     *
     * @param duct   The duct to connect.
     * @param <NOUT> The output data type.
     * @return The new pipeline.
     */
    public <NOUT> Pipeline<SOURCE, NOUT, NOUT> thenConnecting(final Duct<OUT, NOUT> duct) {

        return new Pipeline<SOURCE, NOUT, NOUT>(thenFlowingThrough(duct));
    }

    /**
     * Makes this pipeline flow through the specified duct and returns the resulting stream.
     *
     * @param duct   The duct.
     * @param <NOUT> The output data type.
     * @return The new pipeline.
     */
    public <NOUT> Stream<SOURCE, NOUT, NOUT> thenFlowingThrough(final Duct<OUT, NOUT> duct) {

        if (duct == null) {

            throw new IllegalArgumentException("the output duct cannot be null");
        }

        if (sDucts.containsKey(duct)) {

            throw new DuplicateDamException("the waterfall already contains the duct: " + duct);
        }

        sDucts.put(duct, null);

        final Stream<NOUT, NOUT, NOUT> stream = Waterfall.fallingFrom(new OpenDam<NOUT>());

        final Spring<NOUT> spring = stream.backToSource();

        return mInputStream.thenFlowingThrough(new DuctDam<OUT, NOUT>(duct, spring))
                           .thenFeeding(stream);
    }
}