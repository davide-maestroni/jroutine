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

import com.bmd.wtf.src.Pool;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link Current}
 * <p/>
 * Created by davide on 4/10/14.
 */
public class CurrentTest extends TestCase {

    public void testDecorator() {

        final TestPool pool = new TestPool();
        final Current wrapped = Currents.straightCurrent();
        final MyDecorator current = new MyDecorator(wrapped);

        assertThat(current.getWrapped()).isEqualTo(wrapped);

        current.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        current.dischargeAfter(pool, 1000, TimeUnit.MILLISECONDS, "delay1");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        current.dischargeAfter(pool, 1, TimeUnit.SECONDS, "delay2");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        current.dischargeAfter(pool, 1, TimeUnit.SECONDS,
                               Arrays.asList("delay1", "delay2", "delay3"));
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay3");

        current.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getDebris()).isNull();

        current.flush(pool);
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isTrue();
        assertThat(pool.getDebris()).isNull();

        pool.setFlush(false);

        current.drop(pool, new IllegalArgumentException());
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getDebris()).isExactlyInstanceOf(IllegalArgumentException.class);

        now = System.currentTimeMillis();
        current.dropAfter(pool, 1, TimeUnit.SECONDS, "delay");
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDebris()).isEqualTo("delay");
    }

    public void testPool() throws InterruptedException {

        final TestPool pool = new TestPool();
        final Current current = Currents.threadPoolCurrent(1);

        pool.reset();
        current.discharge(pool, "test");
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        pool.reset();
        current.dischargeAfter(pool, 1000, TimeUnit.MILLISECONDS, "delay1");
        pool.waitCall();
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        pool.reset();
        current.dischargeAfter(pool, 1, TimeUnit.SECONDS, "delay2");
        pool.waitCall();
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        pool.reset();
        current.dischargeAfter(pool, 1, TimeUnit.SECONDS,
                               Arrays.asList("delay1", "delay2", "delay3"));
        pool.waitCall();
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay3");

        pool.reset();
        current.discharge(pool, "test");
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getDebris()).isNull();

        pool.reset();
        current.flush(pool);
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isTrue();
        assertThat(pool.getDebris()).isNull();

        pool.setFlush(false);

        pool.reset();
        current.drop(pool, new IllegalArgumentException());
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getDebris()).isExactlyInstanceOf(IllegalArgumentException.class);

        pool.reset();
        now = System.currentTimeMillis();
        current.dropAfter(pool, 1, TimeUnit.SECONDS, "delay");
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDebris()).isEqualTo("delay");
    }

    public void testStraight() {

        final TestPool pool = new TestPool();
        final Current current = Currents.straightCurrent();

        current.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        current.dischargeAfter(pool, 1000, TimeUnit.MILLISECONDS, "delay1");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        current.dischargeAfter(pool, 1, TimeUnit.SECONDS, "delay2");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        current.dischargeAfter(pool, 1, TimeUnit.SECONDS,
                               Arrays.asList("delay1", "delay2", "delay3"));
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay3");

        current.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getDebris()).isNull();

        current.flush(pool);
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isTrue();
        assertThat(pool.getDebris()).isNull();

        pool.setFlush(false);

        current.drop(pool, new IllegalArgumentException());
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getDebris()).isExactlyInstanceOf(IllegalArgumentException.class);

        now = System.currentTimeMillis();
        current.dropAfter(pool, 1, TimeUnit.SECONDS, "delay");
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDebris()).isEqualTo("delay");
    }

    private static class MyDecorator extends CurrentDecorator {

        private MyDecorator(final Current wrapped) {

            super(wrapped);
        }

        public Current getWrapped() {

            return wrapped();
        }
    }

    private static class TestPool implements Pool<String> {

        private final Semaphore mSemaphore = new Semaphore(0);

        private Object mDebris;

        private String mDrop;

        private boolean mFlush;

        private long mTime;

        @Override
        public void discharge(final String drop) {

            mDrop = drop;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void drop(final Object debris) {

            mDebris = debris;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void flush() {

            mFlush = true;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        public Object getDebris() {

            return mDebris;
        }

        public String getDrop() {

            return mDrop;
        }

        public long getTime() {

            return mTime;
        }

        public boolean isFlush() {

            return mFlush;
        }

        public void setFlush(final boolean flush) {

            mFlush = flush;
        }

        public void reset() {

            mSemaphore.drainPermits();
        }

        public void waitCall() throws InterruptedException {

            mSemaphore.acquire();
        }
    }
}