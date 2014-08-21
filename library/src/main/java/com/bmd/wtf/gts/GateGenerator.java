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

/**
 * A generator of gate instances.
 * <p/>
 * Created by davide on 6/8/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface GateGenerator<IN, OUT> {

    /**
     * Creates and returns the gate forming the specified fall.
     *
     * @param fallNumber the number identifying the fall.
     * @return the gate.
     */
    public Gate<IN, OUT> create(int fallNumber);
}