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
package com.bmd.wtf.xtr.qdc;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.dam.ClosedDam;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Spring;

import java.util.ArrayList;
import java.util.List;

/**
 * An aqueduct object is used to create a connection between two parts of the
 * {@link com.bmd.wtf.Waterfall}, where the data flow within a different asynchronous mechanism.
 * <br/>It can be also employed to balance the data flow by discharging them into different
 * streams.
 * <p/>
 * Flowing data are filtered in a thread safe way by the use of {@link Archway}s.
 * <p/>
 * For consistency, like happens with {@link com.bmd.wtf.dam.Dam}s, also archway instances are
 * allowed just one time.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <SOURCE> The spring data type.
 * @param <IN>     The input data type of the upstream pool.
 * @param <OUT>    The transported data type, that is the output data type of the upstream pool.
 */
public class Aqueduct<SOURCE, IN, OUT> {

    private final Stream<SOURCE, IN, OUT> mInputStream;

    private final int mLevels;

    /**
     * Private constructor to avoid direct instantiation.
     *
     * @param stream      The aqueduct input stream.
     * @param levelNumber The number of levels.
     */
    private Aqueduct(final Stream<SOURCE, IN, OUT> stream, final int levelNumber) {

        if (stream == null) {

            throw new IllegalArgumentException("the input stream cannot be null");
        }

        if (levelNumber < 1) {

            throw new IllegalArgumentException("the number of levels cannot less than 1");
        }

        mInputStream = stream;
        mLevels = levelNumber;
    }

    /**
     * Creates an aqueduct bound to the specified stream.
     *
     * @param stream   The data source stream.
     * @param <SOURCE> The spring data type.
     * @param <IN>     The input data type of the upstream pool.
     * @param <OUT>    The transported data type, that is the output data type of the upstream pool.
     * @return The new aqueduct.
     */
    public static <SOURCE, IN, OUT> Aqueduct<SOURCE, IN, OUT> binding(
            final Stream<SOURCE, IN, OUT> stream) {

        return new Aqueduct<SOURCE, IN, OUT>(stream, 1);
    }

    /**
     * Crosses the gap by making data flowing through the specified archway.
     *
     * @param arch   The archway.
     * @param <NOUT> The output data type.
     * @return The new aqueduct.
     */
    public <NOUT> Aqueduct<SOURCE, NOUT, NOUT> thenCrossingThrough(final Archway<OUT, NOUT> arch) {

        final List<Stream<SOURCE, NOUT, NOUT>> streams = thenFallingThrough(arch);

        return new Aqueduct<SOURCE, NOUT, NOUT>(streams.get(0).thenMerging(streams), 1);
    }

    /**
     * Makes this aqueduct flow through the specified archway and returns the resulting streams.
     *
     * @param arch   The archway.
     * @param <NOUT> The output data type.
     * @return The list of streams.
     */
    public <NOUT> List<Stream<SOURCE, NOUT, NOUT>> thenFallingThrough(
            final Archway<OUT, NOUT> arch) {

        if (arch == null) {

            throw new IllegalArgumentException("the archway cannot be null");
        }

        final int levelNumber = mLevels;

        final ArrayList<Spring<NOUT>> springs = new ArrayList<Spring<NOUT>>(levelNumber);

        final Stream<SOURCE, OUT, NOUT> stream =
                mInputStream.thenFallingThrough(new ArchwayDam<OUT, NOUT>(arch, springs));

        final ArrayList<Stream<SOURCE, NOUT, NOUT>> outStreams =
                new ArrayList<Stream<SOURCE, NOUT, NOUT>>(levelNumber);

        for (int i = 0; i < levelNumber; i++) {

            final Stream<NOUT, NOUT, NOUT> outStream = Waterfall.fallingFrom(new OpenDam<NOUT>());

            springs.add(outStream.backToSource());

            outStreams.add(stream.thenFallingThrough(new ClosedDam<NOUT, NOUT>())
                                 .thenFeeding(outStream));
        }

        return outStreams;
    }

    /**
     * Separates this aqueduct into the specified number of levels.
     *
     * @param levelNumber The number of levels.
     * @return The new aqueduct.
     */
    public Aqueduct<SOURCE, IN, OUT> thenSeparatingIn(final int levelNumber) {

        return new Aqueduct<SOURCE, IN, OUT>(mInputStream, levelNumber);
    }
}