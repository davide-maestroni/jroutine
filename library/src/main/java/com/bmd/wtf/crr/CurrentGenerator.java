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
 * A generator of current instances.
 * <p/>
 * Created by davide on 6/10/14.
 */
public interface CurrentGenerator {

    /**
     * Returns the current for the specified fall.
     * <p/>
     * Note that the same instance can be returned for different falls.
     *
     * @param fallNumber the number identifying the fall.
     * @return the current.
     */
    public Current create(int fallNumber);
}
