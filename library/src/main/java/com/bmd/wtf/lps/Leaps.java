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
     * @param leap     The wrapped leap instance.
     * @param <SOURCE> The river source data type.
     * @param <IN>     The input data type.
     * @param <OUT>    The output data type.
     * @return The wrapping leap.
     */
    public static <SOURCE, IN, OUT> Leap<SOURCE, IN, OUT> weak(final Leap<SOURCE, IN, OUT> leap) {

        return new WeakLeap<SOURCE, IN, OUT>(leap);
    }

    /**
     * Creates and returns a leap retaining the specified instance through a weak reference.
     *
     * @param leap             The wrapped leap instance.
     * @param freeWhenVanished Whether the wrapping leap must behave like a free one after the
     *                         retained instance has vanished.
     * @param <SOURCE>         The river source data type.
     * @param <IN>             The input data type.
     * @param <OUT>            The output data type.
     * @return The wrapping leap.
     */
    public static <SOURCE, IN, OUT> Leap<SOURCE, IN, OUT> weak(final Leap<SOURCE, IN, OUT> leap,
            final boolean freeWhenVanished) {

        return new WeakLeap<SOURCE, IN, OUT>(leap, freeWhenVanished);
    }
}