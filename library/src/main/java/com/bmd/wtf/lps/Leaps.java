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
package com.bmd.wtf.lps;

import com.bmd.wtf.lps.WeakLeap.WhenVanished;

/**
 * Utility class for creating leap instances.
 * <p/>
 * Created by davide on 6/8/14.
 */
public class Leaps {

    /**
     * Avoid direct instantiation.
     */
    protected Leaps() {

    }

    /**
     * Creates and returns a leap retaining the specified instance through a weak reference.
     *
     * @param leap  the wrapped leap instance.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the wrapping leap.
     */
    public static <IN, OUT> Leap<IN, OUT> weak(final Leap<IN, OUT> leap) {

        return new WeakLeap<IN, OUT>(leap);
    }

    /**
     * Creates and returns a leap retaining the specified instance through a weak reference.
     *
     * @param leap         the wrapped leap instance.
     * @param whenVanished whether the wrapping leap must behave like a free one or not after the
     *                     retained instance has vanished.
     * @param <IN>         the input data type.
     * @param <OUT>        the output data type.
     * @return the wrapping leap.
     */
    public static <IN, OUT> Leap<IN, OUT> weak(final Leap<IN, OUT> leap,
            final WhenVanished whenVanished) {

        return new WeakLeap<IN, OUT>(leap, whenVanished);
    }
}