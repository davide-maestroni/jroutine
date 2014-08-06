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
 * Implementation of a free leap which lets all the data and objects flow downstream unmodified.
 * <p/>
 * Created by davide on 6/8/14.
 *
 * @param <DATA> The data type.
 */
public class FreeLeap<DATA> implements Leap<DATA, DATA> {

    @Override
    public void onFlush(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber) {

        downRiver.flush();
    }

    @Override
    public void onPush(final River<DATA> upRiver, final River<DATA> downRiver, final int fallNumber,
            final DATA drop) {

        downRiver.push(drop);
    }

    @Override
    public void onUnhandled(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        downRiver.forward(throwable);
    }
}