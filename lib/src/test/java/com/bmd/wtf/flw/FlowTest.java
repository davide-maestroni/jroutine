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
package com.bmd.wtf.flw;

import com.bmd.wtf.src.Pool;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.flw.Flow}
 * <p/>
 * Created by davide on 4/10/14.
 */
public class FlowTest extends TestCase {

    public void testDecorator() {

        final TestPool pool = new TestPool();
        final Flow wrapped = Flows.straightFlow();
        final MyDecorator flow = new MyDecorator(wrapped);

        assertThat(flow.getWrapped()).isEqualTo(wrapped);

        flow.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        flow.dischargeAfter(pool, 1000, TimeUnit.MILLISECONDS, "delay1");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        flow.dischargeAfter(pool, 1, TimeUnit.SECONDS, "delay2");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        flow.dischargeAfter(pool, 1, TimeUnit.SECONDS, Arrays.asList("delay1", "delay2", "delay3"));
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay3");

        flow.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isNull();
        assertThat(pool.getPull()).isNull();

        flow.flush(pool);
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isTrue();
        assertThat(pool.getPush()).isNull();
        assertThat(pool.getPull()).isNull();

        pool.setFlush(false);

        flow.push(pool, new IllegalArgumentException());
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(pool.getPull()).isNull();

        flow.pull(pool, new IllegalStateException());
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(pool.getPull()).isExactlyInstanceOf(IllegalStateException.class);
    }

    public void testPool() throws InterruptedException {

        final TestPool pool = new TestPool();
        final Flow flow = Flows.threadPoolFlow(1);

        pool.reset();
        flow.discharge(pool, "test");
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        pool.reset();
        flow.dischargeAfter(pool, 1000, TimeUnit.MILLISECONDS, "delay1");
        pool.waitCall();
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        pool.reset();
        flow.dischargeAfter(pool, 1, TimeUnit.SECONDS, "delay2");
        pool.waitCall();
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        pool.reset();
        flow.dischargeAfter(pool, 1, TimeUnit.SECONDS, Arrays.asList("delay1", "delay2", "delay3"));
        pool.waitCall();
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay3");

        pool.reset();
        flow.discharge(pool, "test");
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isNull();
        assertThat(pool.getPull()).isNull();

        pool.reset();
        flow.flush(pool);
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isTrue();
        assertThat(pool.getPush()).isNull();
        assertThat(pool.getPull()).isNull();

        pool.setFlush(false);

        pool.reset();
        flow.push(pool, new IllegalArgumentException());
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(pool.getPull()).isNull();

        pool.reset();
        flow.pull(pool, new IllegalStateException());
        pool.waitCall();
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(pool.getPull()).isExactlyInstanceOf(IllegalStateException.class);
    }

    public void testStraight() {

        final TestPool pool = new TestPool();
        final Flow flow = Flows.straightFlow();

        flow.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        flow.dischargeAfter(pool, 1000, TimeUnit.MILLISECONDS, "delay1");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        flow.dischargeAfter(pool, 1, TimeUnit.SECONDS, "delay2");
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        flow.dischargeAfter(pool, 1, TimeUnit.SECONDS, Arrays.asList("delay1", "delay2", "delay3"));
        assertThat(pool.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(pool.getDrop()).isEqualTo("delay3");

        flow.discharge(pool, "test");
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isNull();
        assertThat(pool.getPull()).isNull();

        flow.flush(pool);
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isTrue();
        assertThat(pool.getPush()).isNull();
        assertThat(pool.getPull()).isNull();

        pool.setFlush(false);

        flow.push(pool, new IllegalArgumentException());
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(pool.getPull()).isNull();

        flow.pull(pool, new IllegalStateException());
        assertThat(pool.getDrop()).isEqualTo("test");
        assertThat(pool.isFlush()).isFalse();
        assertThat(pool.getPush()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(pool.getPull()).isExactlyInstanceOf(IllegalStateException.class);
    }

    private static class MyDecorator extends FlowDecorator {

        private MyDecorator(final Flow wrapped) {

            super(wrapped);
        }

        public Flow getWrapped() {

            return wrapped();
        }
    }

    private static class TestPool implements Pool<String> {

        private final Semaphore mSemaphore = new Semaphore(0);

        private String mDrop;

        private boolean mFlush;

        private Object mPull;

        private Object mPush;

        private long mTime;

        @Override
        public void discharge(final String drop) {

            mDrop = drop;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void flush() {

            mFlush = true;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void pull(final Object debris) {

            mPull = debris;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void push(final Object debris) {

            mPush = debris;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        public String getDrop() {

            return mDrop;
        }

        public void setDrop(final String drop) {

            mDrop = drop;
        }

        public Object getPull() {

            return mPull;
        }

        public void setPull(final Object debris) {

            mPull = debris;
        }

        public Object getPush() {

            return mPush;
        }

        public void setPush(final Object debris) {

            mPush = debris;
        }

        public long getTime() {

            return mTime;
        }

        public void setTime(final long time) {

            mTime = time;
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