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

/**
 * Gate decorator class.
 * <p/>
 * Created by davide on 6/8/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class GateDecorator<IN, OUT> implements Gate<IN, OUT> {

    private final Gate<IN, OUT> mGate;

    /**
     * Default constructor.
     *
     * @param wrapped the decorated instance.
     */
    public GateDecorator(final Gate<IN, OUT> wrapped) {

        if (wrapped == null) {

            throw new IllegalArgumentException("wrapped gate cannot be null");
        }

        mGate = wrapped;
    }

    @Override
    public int hashCode() {

        return mGate.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {

            return true;
        }

        //noinspection SimplifiableIfStatement
        if (!(obj instanceof GateDecorator)) {

            return false;
        }

        return mGate.equals(((GateDecorator) obj).mGate);
    }

    @Override
    public void onFlush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber) {

        mGate.onFlush(upRiver, downRiver, fallNumber);
    }

    @Override
    public void onPush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber,
            final IN drop) {

        mGate.onPush(upRiver, downRiver, fallNumber, drop);
    }

    @Override
    public void onUnhandled(final River<IN> upRiver, final River<OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        mGate.onUnhandled(upRiver, downRiver, fallNumber, throwable);
    }
}