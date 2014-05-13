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

import com.bmd.wtf.flw.Flow;

/**
 * Utility class for {@link FlowFactory} instances.
 * <p/>
 * Created by davide on 3/5/14.
 */
public class FlowFactories {

    /**
     * Avoid direct instantiation.
     */
    protected FlowFactories() {

    }

    /**
     * Creates a new factory that always return the specified flow.
     *
     * @param flow The singleton flow instance.
     * @return The factory.
     */
    public static FlowFactory singletonFlowFactory(final Flow flow) {

        return new FlowFactory() {

            @Override
            public Flow createForStream(final int streamNumber) {

                return flow;
            }
        };
    }
}