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

import com.bmd.wtf.flw.River;

import java.lang.ref.WeakReference;

/**
 * Implementation of a gate decorator which retains a weak reference of the wrapped instance.
 * <p/>
 * Created by davide on 6/8/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class WeakGate<IN, OUT> implements Gate<IN, OUT> {

    private final WeakReference<Gate<IN, OUT>> mGate;

    private final WhenVanished mWhenVanished;

    /**
     * Default constructor.
     *
     * @param wrapped the wrapped gate.
     */
    public WeakGate(final Gate<IN, OUT> wrapped) {

        this(wrapped, WhenVanished.OPEN);
    }

    /**
     * Parametrized constructor.
     *
     * @param wrapped      the wrapped gate.
     * @param whenVanished whether this instance must behave like an open gate or not after the
     *                     wrapped instance is garbage collected.
     */
    public WeakGate(final Gate<IN, OUT> wrapped, final WhenVanished whenVanished) {

        if (wrapped == null) {

            throw new IllegalArgumentException("wrapped gate cannot be null");
        }

        mGate = new WeakReference<Gate<IN, OUT>>(wrapped);
        mWhenVanished = whenVanished;
    }

    @Override
    public int hashCode() {

        int result = mWhenVanished.hashCode();

        final Gate<IN, OUT> gate = mGate.get();

        if (gate != null) {

            result = 31 * result + gate.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {

            return true;
        }

        if (!(obj instanceof WeakGate)) {

            return false;
        }

        final WeakGate weakGate = (WeakGate) obj;

        //noinspection SimplifiableIfStatement
        if (mWhenVanished != weakGate.mWhenVanished) {

            return false;
        }

        final Gate gate = mGate.get();

        return (gate == null) ? (weakGate.mGate.get() == null) : gate.equals(weakGate.mGate.get());
    }

    @Override
    public void onFlush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber) {

        final Gate<IN, OUT> gate = mGate.get();

        if (gate != null) {

            gate.onFlush(upRiver, downRiver, fallNumber);

        } else if (mWhenVanished == WhenVanished.OPEN) {

            downRiver.flush();
        }
    }

    @Override
    public void onPush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber,
            final IN drop) {

        final Gate<IN, OUT> gate = mGate.get();

        if (gate != null) {

            gate.onPush(upRiver, downRiver, fallNumber, drop);
        }
    }

    @Override
    public void onUnhandled(final River<IN> upRiver, final River<OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        final Gate<IN, OUT> gate = mGate.get();

        if (gate != null) {

            gate.onUnhandled(upRiver, downRiver, fallNumber, throwable);

        } else if (mWhenVanished == WhenVanished.OPEN) {

            downRiver.forward(throwable);
        }
    }

    /**
     * How the gate must behaved when the wrapped instance is garbage collected.
     */
    public enum WhenVanished {

        CLOSE,
        OPEN
    }
}