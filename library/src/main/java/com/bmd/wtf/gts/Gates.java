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
package com.bmd.wtf.gts;

import com.bmd.wtf.gts.WeakGate.WhenVanished;

/**
 * Utility class for creating gate instances.
 * <p/>
 * Created by davide on 6/8/14.
 */
public class Gates {

    /**
     * Avoid direct instantiation.
     */
    protected Gates() {

    }

    /**
     * Creates and returns a gate retaining the specified instance through a weak reference.
     *
     * @param gate  the wrapped gate instance.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the wrapping gate.
     */
    public static <IN, OUT> Gate<IN, OUT> weak(final Gate<IN, OUT> gate) {

        return new WeakGate<IN, OUT>(gate);
    }

    /**
     * Creates and returns a gate retaining the specified instance through a weak reference.
     *
     * @param gate         the wrapped gate instance.
     * @param whenVanished whether the wrapping gate must behave like an open one or not after the
     *                     retained instance has vanished.
     * @param <IN>         the input data type.
     * @param <OUT>        the output data type.
     * @return the wrapping gate.
     */
    public static <IN, OUT> Gate<IN, OUT> weak(final Gate<IN, OUT> gate,
            final WhenVanished whenVanished) {

        return new WeakGate<IN, OUT>(gate, whenVanished);
    }
}