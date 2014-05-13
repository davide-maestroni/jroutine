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
package com.bmd.wtf.flw;

/**
 * Utility class for {@link Flow} instances.
 * <p/>
 * Created by davide on 3/4/14.
 */
public class Flows {

    private static volatile StraightFlow sStraight;

    /**
     * Avoid direct instantiation.
     */
    protected Flows() {

    }

    /**
     * Returns the default {@link StraightFlow} instance.
     *
     * @return The instance.
     */
    public static Flow straightFlow() {

        if (sStraight == null) {

            sStraight = new StraightFlow();
        }

        return sStraight;
    }

    /**
     * Returns a new {@link ThreadPoolFlow} instance.
     *
     * @param poolSize The maximum size of the thread pool.
     * @return The new instance.
     */
    public static Flow threadPoolFlow(final int poolSize) {

        return new ThreadPoolFlow(poolSize);
    }
}