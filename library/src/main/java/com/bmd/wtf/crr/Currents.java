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
package com.bmd.wtf.crr;

/**
 * Utility class for {@link Current} instances.
 * <p/>
 * Created by davide on 3/4/14.
 */
public class Currents {

    private static volatile StraightCurrent sStraight;

    /**
     * Avoid direct instantiation.
     */
    protected Currents() {

    }

    /**
     * Returns the default {@link StraightCurrent} instance.
     *
     * @return The instance.
     */
    public static Current straightCurrent() {

        if (sStraight == null) {

            sStraight = new StraightCurrent();
        }

        return sStraight;
    }

    /**
     * Returns a new {@link ThreadPoolCurrent} instance.
     *
     * @param poolSize The maximum size of the thread pool.
     * @return The new instance.
     */
    public static Current threadPoolCurrent(final int poolSize) {

        return new ThreadPoolCurrent(poolSize);
    }
}