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
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapDecorator;

/**
 * Created by davide on 6/14/14.
 */
public class SegmentedLeap<SOURCE, IN, OUT> extends LeapDecorator<SOURCE, IN, OUT> {

    private final Object mMutex = new Object();

    public SegmentedLeap(final Leap<SOURCE, IN, OUT> wrapped) {

        super(wrapped);
    }

    @Override
    public void onFlush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber) {

        synchronized (mMutex) {

            super.onFlush(upRiver, downRiver, fallNumber);
        }
    }

    @Override
    public void onPush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final IN drop) {

        synchronized (mMutex) {

            super.onPush(upRiver, downRiver, fallNumber, drop);
        }
    }

    @Override
    public void onUnhandled(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        synchronized (mMutex) {

            super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
        }
    }
}