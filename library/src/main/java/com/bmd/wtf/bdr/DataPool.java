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
package com.bmd.wtf.bdr;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.src.Pool;

import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Instances of this class implement {@link com.bmd.wtf.src.Pool}s by managing internally
 * stored {@link com.bmd.wtf.dam.Dam}s. Each instance has a single input
 * {@link com.bmd.wtf.crr.Current}, shared by all the input {@link Stream}s which feed it with data
 * and objects.
 * <p/>
 * This class ensures that the internal Dam is always accessed in a thread safe way, so that the
 * implementer does not have to worry about concurrency issues.
 * <p/>
 * Created by davide on 3/2/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class DataPool<IN, OUT> implements Pool<IN> {

    private static final WeakHashMap<Dam<?, ?>, Void> sDams = new WeakHashMap<Dam<?, ?>, Void>();

    final Current inputCurrent;

    final CopyOnWriteArraySet<Stream<?, ?, IN>> inputStreams =
            new CopyOnWriteArraySet<Stream<?, ?, IN>>();

    final CopyOnWriteArraySet<Stream<?, IN, OUT>> outputStreams =
            new CopyOnWriteArraySet<Stream<?, IN, OUT>>();

    private final Condition mCondition;

    private final Dam<IN, OUT> mDam;

    private final DataFloodgate<IN, OUT> mGate;

    private final ReentrantLock mLock = new ReentrantLock();

    private int mIdleCountdown;

    public DataPool(final Current inputCurrent, final Dam<IN, OUT> dam) {

        if (inputCurrent == null) {

            throw new IllegalArgumentException("the input current cannot be null");
        }

        if (dam == null) {

            throw new IllegalArgumentException("the output dam cannot be null");
        }

        if (sDams.containsKey(dam)) {

            throw new DuplicateDamException("the waterfall already contains the dam: " + dam);
        }

        sDams.put(dam, null);

        this.inputCurrent = inputCurrent;
        mDam = dam;
        mCondition = mLock.newCondition();
        mGate = new DataFloodgate<IN, OUT>(this);
    }

    @Override
    public void discharge(final IN drop) {

        final DataFloodgate<IN, OUT> gate = mGate;

        try {

            gate.open();

            Object debris;

            try {

                debris = mDam.onDischarge(gate, drop);

            } catch (final Throwable t) {

                debris = t;
            }

            if (debris != null) {

                gate.pull(debris).push(debris);
            }

        } finally {

            gate.close();

            decrementIdleCountdown();
        }
    }

    @Override
    public void flush() {

        final DataFloodgate<IN, OUT> gate = mGate;

        try {

            gate.open();

            Object debris;

            try {

                debris = mDam.onFlush(gate);

            } catch (final Throwable t) {

                debris = t;
            }

            if (debris != null) {

                gate.pull(debris).push(debris);
            }

        } finally {

            gate.close();

            decrementIdleCountdown();
        }
    }

    @Override
    public void pull(final Object debris) {

        final DataFloodgate<IN, OUT> gate = mGate;

        try {

            gate.open();

            Object next = null;

            try {

                next = mDam.onPullDebris(gate, debris);

            } catch (final Throwable t) {

                gate.pull(t).push(t);
            }

            if (next != null) {

                gate.pull(next);
            }

        } finally {

            gate.close();

            decrementIdleCountdown();
        }
    }

    @Override
    public void push(final Object debris) {

        final DataFloodgate<IN, OUT> gate = mGate;

        try {

            gate.open();

            Object next = null;

            try {

                next = mDam.onPushDebris(gate, debris);

            } catch (final Throwable t) {

                gate.pull(t).push(t);
            }

            if (next != null) {

                gate.push(next);
            }

        } finally {

            gate.close();

            decrementIdleCountdown();
        }
    }

    void incrementIdleCountdown(final int count) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mIdleCountdown += count;

        } finally {

            lock.unlock();
        }
    }

    void waitIdle(final long timeout, TimeUnit timeUnit, final RuntimeException exception) {

        boolean isTimeout = false;

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            if (mIdleCountdown <= 0) {

                return;
            }

            long currentTimeout = timeUnit.toMillis(timeout);

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    mCondition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if (mIdleCountdown > 0) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    mCondition.await();
                }

            } while (mIdleCountdown > 0);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            if (!isTimeout) {

                mIdleCountdown = 0;
            }

            lock.unlock();
        }

        if (isTimeout && (exception != null)) {

            throw exception;
        }
    }

    void waitIdle() {

        waitIdle(-1, TimeUnit.MILLISECONDS, null);
    }

    private void decrementIdleCountdown() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            if (--mIdleCountdown <= 0) {

                mCondition.signalAll();
            }

        } finally {

            lock.unlock();
        }
    }
}