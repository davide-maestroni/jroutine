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
package com.bmd.wtf.lps;

import com.bmd.wtf.flw.River;

/**
 * Created by davide on 6/7/14.
 */
public interface Leap<SOURCE, IN, OUT> {

    public void onFlush(River<SOURCE, IN> upRiver, River<SOURCE, OUT> downRiver, int fallNumber);

    public void onPush(River<SOURCE, IN> upRiver, River<SOURCE, OUT> downRiver, int fallNumber,
            IN drop);

    public void onUnhandled(River<SOURCE, IN> upRiver, River<SOURCE, OUT> downRiver, int fallNumber,
            Throwable throwable);
}