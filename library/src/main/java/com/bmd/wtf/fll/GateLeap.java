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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Leap decorator used to protect a common leap with a gate.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <SOURCE> The source data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 */
class GateLeap<SOURCE, IN, OUT> extends LeapDecorator<SOURCE, IN, OUT> {

    final Condition condition;

    final Leap<SOURCE, IN, OUT> leap;

    final ReentrantLock lock;

    public GateLeap(final Leap<SOURCE, IN, OUT> wrapped) {

        super(wrapped);

        leap = wrapped;
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Override
    public void onDischarge(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber) {

        final ReentrantLock glassLock = lock;

        glassLock.lock();

        try {

            super.onDischarge(upRiver, downRiver, fallNumber);

        } finally {

            condition.signalAll();

            glassLock.unlock();
        }
    }

    @Override
    public void onPush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final IN drop) {

        final ReentrantLock glassLock = lock;

        glassLock.lock();

        try {

            super.onPush(upRiver, downRiver, fallNumber, drop);

        } finally {

            condition.signalAll();

            glassLock.unlock();
        }
    }

    @Override
    public void onUnhandled(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        final ReentrantLock glassLock = lock;

        glassLock.lock();

        try {

            super.onUnhandled(upRiver, downRiver, fallNumber, throwable);

        } finally {

            condition.signalAll();

            glassLock.unlock();
        }
    }
}