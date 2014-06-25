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
package com.bmd.wtf.crr;

import com.bmd.wtf.fll.DelayInterruptedException;
import com.bmd.wtf.flw.Fall;

import java.util.concurrent.TimeUnit;

/**
 * Synchronous implementation of a {@link com.bmd.wtf.crr.Current}.
 * <p/>
 * Since the calls are synchronous, delayed operations makes the calling thread to sleep for the
 * required time.
 * <p/>
 * Created by davide on 6/7/14.
 */
public class StraightCurrent implements Current {

    /**
     * Avoid instantiation outside the package.
     */
    StraightCurrent() {

    }

    @Override
    public void discharge(final Fall<?> fall) {

        fall.discharge();
    }

    @Override
    public void forward(final Fall<?> fall, final Throwable throwable) {

        fall.forward(throwable);
    }

    @Override
    public <DATA> void push(final Fall<DATA> fall, final DATA drop) {

        fall.push(drop);
    }

    @Override
    public <DATA> void pushAfter(final Fall<DATA> fall, final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        try {

            long timeToWait = timeUnit.toMillis(delay);

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + timeToWait;

            do {

                Thread.sleep(timeToWait);

                timeToWait = endTime - System.currentTimeMillis();

            } while (timeToWait > 0);

            fall.push(drop);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }
    }

    @Override
    public <DATA> void pushAfter(final Fall<DATA> fall, final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        try {

            long timeToWait = timeUnit.toMillis(delay);

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + timeToWait;

            do {

                Thread.sleep(timeToWait);

                timeToWait = endTime - System.currentTimeMillis();

            } while (timeToWait > 0);

            for (final DATA drop : drops) {

                fall.push(drop);
            }

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }
    }
}