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
package com.bmd.wtf.bdr;

import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.flw.Flow;

/**
 * This utility class is used to start the waterfall by creating the first
 * {@link com.bmd.wtf.bdr.Stream}.
 * <p/>
 * Created by davide on 2/25/14.
 */
public class DataWaterfall {

    /**
     * Avoid direct instantiation.
     */
    private DataWaterfall() {

    }

    /**
     * Creates a new waterfall flowing into the specified flow and through the specified dam and
     * returns the newly created stream.
     *
     * @param inputFlow The input flow instance.
     * @param dam       The dam to flow through.
     * @param <IN>      The input data type.
     * @param <OUT>     The output data type.
     * @return The new stream.
     */
    public static <IN, OUT> Stream<IN, IN, OUT> flowingThrough(final Flow inputFlow,
            final Dam<IN, OUT> dam) {

        if ((inputFlow == null) || (dam == null)) {

            throw new IllegalArgumentException();
        }

        final DataSpring<IN> spring = new DataSpring<IN>();
        final DataPool<IN, OUT> pool = new DataPool<IN, OUT>(inputFlow, dam);
        final Stream<IN, Void, IN> stream = new Stream<IN, Void, IN>(spring, null, pool);

        spring.setOutStream(stream);
        pool.inputStreams.add(stream);

        return new Stream<IN, IN, OUT>(spring, pool, null);
    }
}