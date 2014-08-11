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

import com.bmd.wtf.flw.Fall;
import com.bmd.wtf.flw.Stream;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link Current}
 * <p/>
 * Created by davide on 6/26/14.
 */
public class CurrentTest extends TestCase {

    public void testFall() throws InterruptedException {

        final TestFall fall = new TestFall();
        final Current current = Currents.pool(1);

        fall.reset();
        current.push(fall, "test");
        fall.waitCall();
        assertThat(fall.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        fall.reset();
        current.pushAfter(fall, 1000, TimeUnit.MILLISECONDS, "delay1");
        fall.waitCall();
        assertThat(fall.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(fall.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        fall.reset();
        current.pushAfter(fall, 1, TimeUnit.SECONDS, "delay2");
        fall.waitCall();
        assertThat(fall.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(fall.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        fall.reset();
        current.pushAfter(fall, 1, TimeUnit.SECONDS, Arrays.asList("delay1", "delay2", "delay3"));
        fall.waitCall();
        fall.waitCall();
        fall.waitCall();
        assertThat(fall.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(fall.getDrop()).isEqualTo("delay3");

        fall.reset();
        current.push(fall, "test");
        fall.waitCall();
        assertThat(fall.getDrop()).isEqualTo("test");
        assertThat(fall.isFlushed()).isFalse();
        assertThat(fall.getThrowable()).isNull();

        fall.reset();
        current.flush(fall, null);
        fall.waitCall();
        assertThat(fall.getDrop()).isEqualTo("test");
        assertThat(fall.isFlushed()).isTrue();
        assertThat(fall.getThrowable()).isNull();

        fall.setFlush(false);

        fall.reset();
        current.forward(fall, new IllegalArgumentException());
        fall.waitCall();
        assertThat(fall.getDrop()).isEqualTo("test");
        assertThat(fall.isFlushed()).isFalse();
        assertThat(fall.getThrowable()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    public void testStraight() {

        final TestFall fall = new TestFall();
        final Current current = Currents.straight();

        current.push(fall, "test");
        assertThat(fall.getDrop()).isEqualTo("test");

        long now = System.currentTimeMillis();
        current.pushAfter(fall, 1000, TimeUnit.MILLISECONDS, "delay1");
        assertThat(fall.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(fall.getDrop()).isEqualTo("delay1");

        now = System.currentTimeMillis();
        current.pushAfter(fall, 1, TimeUnit.SECONDS, "delay2");
        assertThat(fall.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(fall.getDrop()).isEqualTo("delay2");

        now = System.currentTimeMillis();
        current.pushAfter(fall, 1, TimeUnit.SECONDS, Arrays.asList("delay1", "delay2", "delay3"));
        assertThat(fall.getTime()).isGreaterThanOrEqualTo(now + 1000);
        assertThat(fall.getDrop()).isEqualTo("delay3");

        current.push(fall, "test");
        assertThat(fall.getDrop()).isEqualTo("test");
        assertThat(fall.isFlushed()).isFalse();
        assertThat(fall.getThrowable()).isNull();

        current.flush(fall, null);
        assertThat(fall.getDrop()).isEqualTo("test");
        assertThat(fall.isFlushed()).isTrue();
        assertThat(fall.getThrowable()).isNull();

        fall.setFlush(false);

        current.forward(fall, new IllegalArgumentException());
        assertThat(fall.getDrop()).isEqualTo("test");
        assertThat(fall.isFlushed()).isFalse();
        assertThat(fall.getThrowable()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    private static class TestFall implements Fall<String> {

        private final Semaphore mSemaphore = new Semaphore(0);

        private String mDrop;

        private boolean mFlush;

        private Object mThrowable;

        private long mTime;

        @Override
        public void flush(final Stream<String> origin) {

            mFlush = true;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void forward(final Throwable throwable) {

            mThrowable = throwable;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        @Override
        public void push(final String drop) {

            mDrop = drop;
            mTime = System.currentTimeMillis();
            mSemaphore.release();
        }

        public String getDrop() {

            return mDrop;
        }

        public Object getThrowable() {

            return mThrowable;
        }

        public long getTime() {

            return mTime;
        }

        public boolean isFlushed() {

            return mFlush;
        }

        public void reset() {

            mSemaphore.drainPermits();
        }

        public void setFlush(final boolean flush) {

            mFlush = flush;
        }

        public void waitCall() throws InterruptedException {

            mSemaphore.acquire();
        }
    }
}