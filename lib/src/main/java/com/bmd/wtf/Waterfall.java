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
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.flw.Flow;
import com.bmd.wtf.flw.Flows;

/**
 * Here is where everything starts.
 * <p/>
 * Created by davide on 2/25/14.
 */
public class Waterfall {

    private final Flow mFlow;

    /**
     * Private constructor to avoid direct instantiation.
     *
     * @param flow The starting flow.
     */
    private Waterfall(final Flow flow) {

        if (flow == null) {

            throw new IllegalArgumentException();
        }

        mFlow = flow;
    }

    /**
     * Creates a new waterfall running through a straight flow.
     *
     * @return The new waterfall instance.
     */
    public static Waterfall falling() {

        return new Waterfall(Flows.straightFlow());
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
     * Creates a new waterfall running into the specified flow.
     *
     * @param flow The flow instance.
     * @return The new waterfall instance.
     */
    public static Waterfall flowingInto(final Flow flow) {

        return new Waterfall(flow);
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

        return DataWaterfall.flowingThrough(mFlow, dam);
    }
}