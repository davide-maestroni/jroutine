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
 * {@link Leap} decorator class.
 * <p/>
 * Created by davide on 6/8/14.
 *
 * @param <SOURCE> The river source data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 */
public class LeapDecorator<SOURCE, IN, OUT> implements Leap<SOURCE, IN, OUT> {

    private final Leap<SOURCE, IN, OUT> mLeap;

    /**
     * Default constructor.
     *
     * @param wrapped The decorated instance.
     */
    public LeapDecorator(final Leap<SOURCE, IN, OUT> wrapped) {

        if (wrapped == null) {

            throw new IllegalArgumentException("wrapped leap cannot be null");
        }

        mLeap = wrapped;
    }

    @Override
    public int hashCode() {

        return mLeap.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {

            return true;
        }

        //noinspection SimplifiableIfStatement
        if (!(obj instanceof LeapDecorator)) {

            return false;
        }

        return mLeap.equals(((LeapDecorator) obj).mLeap);
    }

    @Override
    public void onDischarge(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber) {

        mLeap.onDischarge(upRiver, downRiver, fallNumber);
    }

    @Override
    public void onPush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final IN drop) {

        mLeap.onPush(upRiver, downRiver, fallNumber, drop);
    }

    @Override
    public void onUnhandled(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        mLeap.onUnhandled(upRiver, downRiver, fallNumber, throwable);
    }
}