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

import java.lang.ref.WeakReference;

/**
 * Implementation of a leap decorator which retains a weak reference of the wrapped instance.
 * <p/>
 * Created by davide on 6/8/14.
 */
class WeakLeap<SOURCE, IN, OUT> implements Leap<SOURCE, IN, OUT> {

    private final boolean mFreeWhenVanished;

    private final WeakReference<Leap<SOURCE, IN, OUT>> mLeap;

    /**
     * Default constructor.
     *
     * @param wrapped The wrapped leap.
     */
    public WeakLeap(final Leap<SOURCE, IN, OUT> wrapped) {

        this(wrapped, true);
    }

    /**
     * Parametrized constructor.
     *
     * @param wrapped          The wrapped leap.
     * @param freeWhenVanished Whether this instance must behave like a free leap after the
     *                         wrapped instance is garbage collected.
     */
    public WeakLeap(final Leap<SOURCE, IN, OUT> wrapped, final boolean freeWhenVanished) {

        if (wrapped == null) {

            throw new IllegalArgumentException("wrapped leap cannot be null");
        }

        mLeap = new WeakReference<Leap<SOURCE, IN, OUT>>(wrapped);
        mFreeWhenVanished = freeWhenVanished;
    }

    @Override
    public int hashCode() {

        int result = (mFreeWhenVanished ? 1 : 0);

        final Leap<SOURCE, IN, OUT> leap = mLeap.get();

        if (leap != null) {

            result = 31 * result + leap.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {

            return true;
        }

        if (!(obj instanceof WeakLeap)) {

            return false;
        }

        final WeakLeap weakLeap = (WeakLeap) obj;

        //noinspection SimplifiableIfStatement
        if (mFreeWhenVanished != weakLeap.mFreeWhenVanished) {

            return false;
        }

        final Leap leap = mLeap.get();

        return (leap == null) ? (weakLeap.mLeap.get() == null) : leap.equals(weakLeap.mLeap.get());
    }

    @Override
    public void onDischarge(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber) {

        final Leap<SOURCE, IN, OUT> leap = mLeap.get();

        if (leap != null) {

            leap.onDischarge(upRiver, downRiver, fallNumber);

        } else if (mFreeWhenVanished) {

            downRiver.discharge();
        }
    }

    @Override
    public void onPush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final IN drop) {

        final Leap<SOURCE, IN, OUT> leap = mLeap.get();

        if (leap != null) {

            leap.onPush(upRiver, downRiver, fallNumber, drop);
        }
    }

    @Override
    public void onUnhandled(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        final Leap<SOURCE, IN, OUT> leap = mLeap.get();

        if (leap != null) {

            leap.onUnhandled(upRiver, downRiver, fallNumber, throwable);

        } else if (mFreeWhenVanished) {

            downRiver.forward(throwable);
        }
    }
}