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
import com.bmd.wtf.gts.Gate;
import com.bmd.wtf.gts.GateDecorator;

/**
 * Gate decorator used to protect a gate when the same instance handles different streams.
 * <p/>
 * Created by davide on 6/14/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BarrageGate<IN, OUT> extends GateDecorator<IN, OUT> {

    private final Object mMutex = new Object();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped gate.
     */
    public BarrageGate(final Gate<IN, OUT> wrapped) {

        super(wrapped);
    }

    @Override
    public void onFlush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber) {

        synchronized (mMutex) {

            super.onFlush(upRiver, downRiver, fallNumber);
        }
    }

    @Override
    public void onPush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber,
            final IN drop) {

        synchronized (mMutex) {

            super.onPush(upRiver, downRiver, fallNumber, drop);
        }
    }

    @Override
    public void onException(final River<IN> upRiver, final River<OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        synchronized (mMutex) {

            super.onException(upRiver, downRiver, fallNumber, throwable);
        }
    }
}