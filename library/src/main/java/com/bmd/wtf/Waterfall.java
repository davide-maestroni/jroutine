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
package com.bmd.wtf;

import com.bmd.wtf.bdr.DataWaterfall;
import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.dam.Dam;

/**
 * Here is where everything starts.
 * <p/>
 * Created by davide on 2/25/14.
 */
public class Waterfall {

    private final Current mCurrent;

    /**
     * Private constructor to avoid direct instantiation.
     *
     * @param current The starting current.
     */
    private Waterfall(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException();
        }

        mCurrent = current;
    }

    /**
     * Creates a new waterfall running through a straight current.
     *
     * @return The new waterfall instance.
     */
    public static Waterfall falling() {

        return new Waterfall(Currents.straightCurrent());
    }

    /**
     * Creates a new waterfall falling from the specified dam.
     *
     * @return The new waterfall instance.
     */
    public static <IN, OUT> Stream<IN, IN, OUT> fallingFrom(final Dam<IN, OUT> dam) {

        return falling().thenFlowingThrough(dam);
    }

    /**
     * Creates a new waterfall running into the specified current.
     *
     * @param current The current instance.
     * @return The new waterfall instance.
     */
    public static Waterfall flowingInto(final Current current) {

        return new Waterfall(current);
    }

    /**
     * Makes the waterfall flow through the specified dam and returns the newly created stream.
     *
     * @param dam   The dam to flow through.
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     * @return The new stream.
     */
    public <IN, OUT> Stream<IN, IN, OUT> thenFlowingThrough(final Dam<IN, OUT> dam) {

        return DataWaterfall.flowingThrough(mCurrent, dam);
    }
}