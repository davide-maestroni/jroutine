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
 * Base abstract implementation of a leap. By default it behaves like a free leap.
 * <p/>
 * Created by davide on 6/9/14.
 *
 * @param <SOURCE> The river source data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 * @see FreeLeap
 */
public abstract class AbstractLeap<SOURCE, IN, OUT> implements Leap<SOURCE, IN, OUT> {

    @Override
    public void onDischarge(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber) {

        downRiver.discharge();
    }

    @Override
    public void onUnhandled(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        downRiver.forward(throwable);
    }
}