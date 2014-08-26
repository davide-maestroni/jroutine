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
import com.bmd.wtf.gts.OpenGate;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Gate implementation which forward an exception in case a specific timeout elapses before at
 * least one data drop is pushed through it.
 * <p/>
 * Created by davide on 8/26/14.
 *
 * @param <DATA> the data type.
 */
class TimeoutGate<DATA> extends OpenGate<DATA> {

    private static final Timer sTimer = new Timer();

    private final long mDelay;

    private final RuntimeException mException;

    private final Waterfall<?, ?, ?> mWaterfall;

    private TimerTask mTask;

    /**
     * Constructor.
     *
     * @param waterfall this gate the waterfall.
     * @param delay     the delay in <code>timeUnit</code> time units.
     * @param timeUnit  the delay time unit.
     * @param exception the exception to be thrown.
     */
    public TimeoutGate(final Waterfall<?, ?, ?> waterfall, final long delay,
            final TimeUnit timeUnit, final RuntimeException exception) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        mWaterfall = waterfall;
        mDelay = timeUnit.toMillis(delay);
        mException = exception;

        schedule();
    }

    @Override
    public void onFlush(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber) {

        cancel();

        super.onFlush(upRiver, downRiver, fallNumber);
    }

    @Override
    public void onPush(final River<DATA> upRiver, final River<DATA> downRiver, final int fallNumber,
            final DATA drop) {

        schedule();

        super.onPush(upRiver, downRiver, fallNumber, drop);
    }

    @Override
    public void onUnhandled(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        cancel();

        super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
    }

    private void cancel() {

        if (mTask != null) {

            mTask.cancel();
            mTask = null;
        }
    }

    private void schedule() {

        cancel();

        mTask = new TimerTask() {

            @Override
            public void run() {

                mWaterfall.forward(mException);
            }
        };

        sTimer.schedule(mTask, mDelay);
    }
}