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
package com.bmd.wtf.spr;

/**
 * A generator of gate instances.
 * <p/>
 * Created by davide on 8/20/14.
 *
 * @param <DATA> the spring data type.
 */
public interface SpringGenerator<DATA> {

    /**
     * Creates and returns the spring flowing from the specified fall.
     *
     * @param fallNumber the number identifying the fall.
     * @return the spring.
     */
    public Spring<DATA> create(int fallNumber);
}