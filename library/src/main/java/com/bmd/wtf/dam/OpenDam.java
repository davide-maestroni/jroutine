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
 * Implementation of an open {@link Dam}, that lets all the data and objects flow downstream
 * untouched.
 * <p/>
 * Created by davide on 2/27/14.
 *
 * @param <DATA> The data type.
 */
public class OpenDam<DATA> extends AbstractDam<DATA, DATA> {

    @Override
    public void onDischarge(final Floodgate<DATA, DATA> gate, final DATA drop) {

        gate.discharge(drop);
    }
}