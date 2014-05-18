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
package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.crr.Current;

/**
 * Utility class for {@link CurrentFactory} instances.
 * <p/>
 * Created by davide on 3/5/14.
 */
public class CurrentFactories {

    /**
     * Avoid direct instantiation.
     */
    protected CurrentFactories() {

    }

    /**
     * Creates a new factory that always return the specified current.
     *
     * @param current The singleton current instance.
     * @return The factory.
     */
    public static CurrentFactory singletonCurrentFactory(final Current current) {

        return new CurrentFactory() {

            @Override
            public Current createForStream(final int streamNumber) {

                return current;
            }
        };
    }
}