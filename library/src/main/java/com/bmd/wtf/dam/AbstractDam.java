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
package com.bmd.wtf.dam;

import com.bmd.wtf.src.Floodgate;

/**
 * Base abstract implementation of a {@link Dam}. It behaves like an {@link OpenDam}.
 * <p/>
 * Created by davide on 2/27/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public abstract class AbstractDam<IN, OUT> implements Dam<IN, OUT> {

    @Override
    public void onDrop(final Floodgate<IN, OUT> gate, final Object debris) {

        gate.drop(debris);
    }

    @Override
    public void onFlush(final Floodgate<IN, OUT> gate) {

        gate.flush();
    }
}