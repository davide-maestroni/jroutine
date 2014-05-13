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
package com.bmd.wtf.xtr.flood;

import com.bmd.wtf.dam.Dam;

/**
 * A levee is a special {@link com.bmd.wtf.dam.Dam} guarded by a controller.
 * <p/>
 * Created by davide on 5/9/14.
 *
 * @param <IN>         The input data type.
 * @param <OUT>        The output data type.
 * @param <CONTROLLER> The controller tye.
 */
public interface Levee<IN, OUT, CONTROLLER extends FloodObserver<IN, OUT>> extends Dam<IN, OUT> {

    /**
     * Controls the levee through its controller.
     *
     * @return The controller.
     */
    public CONTROLLER controller();
}