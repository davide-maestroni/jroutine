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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gate decorator used to protect a common gate with a dam.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DamGate<IN, OUT> extends GateDecorator<IN, OUT> {

    final Condition condition;

    final Gate<IN, OUT> gate;

    final ReentrantLock lock;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped gate.
     */
    public DamGate(final Gate<IN, OUT> wrapped) {

        super(wrapped);

        gate = wrapped;
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Override
    public void onFlush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber) {

        final ReentrantLock lock = this.lock;
        lock.lock();

        try {

            super.onFlush(upRiver, downRiver, fallNumber);

        } finally {

            condition.signalAll();

            lock.unlock();
        }
    }

    @Override
    public void onPush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber,
            final IN drop) {

        final ReentrantLock lock = this.lock;
        lock.lock();

        try {

            super.onPush(upRiver, downRiver, fallNumber, drop);

        } finally {

            condition.signalAll();

            lock.unlock();
        }
    }

    @Override
    public void onUnhandled(final River<IN> upRiver, final River<OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        final ReentrantLock lock = this.lock;
        lock.lock();

        try {

            super.onUnhandled(upRiver, downRiver, fallNumber, throwable);

        } finally {

            condition.signalAll();

            lock.unlock();
        }
    }
}