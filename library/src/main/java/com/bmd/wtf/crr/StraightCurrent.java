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

import com.bmd.wtf.bdr.DelayInterruptedException;
import com.bmd.wtf.src.Pool;

import java.util.concurrent.TimeUnit;

/**
 * Synchronous implementation of a {@link Current}.
 * <p/>
 * Since the calls are synchronous, delayed operations makes the calling thread to sleep for the
 * required time.
 * <p/>
 * Created by davide on 2/27/14.
 */
public class StraightCurrent implements Current {

    /**
     * Avoid instantiation outside the package.
     */
    StraightCurrent() {

    }

    @Override
    public <DATA> void discharge(final Pool<DATA> pool, final DATA drop) {

        pool.discharge(drop);
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        try {

            long timeToWait = timeUnit.toMillis(delay);

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + timeToWait;

            do {

                Thread.sleep(timeToWait);

                timeToWait = endTime - System.currentTimeMillis();

            } while (timeToWait > 0);

            pool.discharge(drop);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay, final TimeUnit timeUnit,
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

                pool.discharge(drop);
            }

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }
    }

    @Override
    public void drop(final Pool<?> pool, final Object debris) {

        pool.drop(debris);
    }

    @Override
    public void dropAfter(final Pool<?> pool, final long delay, final TimeUnit timeUnit, final Object debris) {

        try {

            long timeToWait = timeUnit.toMillis(delay);

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + timeToWait;

            do {

                Thread.sleep(timeToWait);

                timeToWait = endTime - System.currentTimeMillis();

            } while (timeToWait > 0);

            pool.drop(debris);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }
    }

    @Override
    public void flush(final Pool<?> pool) {

        pool.flush();
    }
}