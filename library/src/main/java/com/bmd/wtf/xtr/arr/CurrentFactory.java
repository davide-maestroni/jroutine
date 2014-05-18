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
 * A {@link com.bmd.wtf.crr.Current} factory used to provide instances to an array of streams.
 * <p/>
 * Created by davide on 3/3/14.
 */
public interface CurrentFactory {

    /**
     * Creates the current associated to the specified stream number.
     *
     * @param streamNumber The number of the stream.
     * @return The associated current.
     */
    public Current createForStream(int streamNumber);
}