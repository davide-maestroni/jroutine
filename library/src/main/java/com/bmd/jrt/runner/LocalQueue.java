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
package com.bmd.jrt.runner;

import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/18/14.
 */
class LocalQueue {

    private static final int INITIAL_CAPACITY = 10;

    private static final ThreadLocal<LocalQueue> sQueue = new ThreadLocal<LocalQueue>() {

        @Override
        protected LocalQueue initialValue() {

            return new LocalQueue();
        }
    };

    private TimeDuration[] mDelays;

    private int mFirst;

    private InvocationInstruction[] mInstructions;

    private long[] mInvocationTimeNs;

    private boolean mIsRunning;

    private int mLast;

    private InstructionType[] mTypes;

    /**
     * Default constructor.
     */
    private LocalQueue() {

        mInvocationTimeNs = new long[INITIAL_CAPACITY];
        mInstructions = new InvocationInstruction[INITIAL_CAPACITY];
        mDelays = new TimeDuration[INITIAL_CAPACITY];
        mTypes = new InstructionType[INITIAL_CAPACITY];
    }

    public static void run(final InvocationInstruction invocationInstruction, final long delay,
            final TimeUnit timeUnit) {

        sQueue.get().addRun(invocationInstruction, delay, timeUnit);
    }

    public static void runAbort(final InvocationInstruction invocationInstruction) {

        sQueue.get().addAbort(invocationInstruction);
    }

    private static void resizeArray(final InvocationInstruction[] src,
            final InvocationInstruction[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final TimeDuration[] src, final TimeDuration[] dst,
            final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final InstructionType[] src, final InstructionType[] dst,
            final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final long[] src, final long[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private void add(final InvocationInstruction invocationInstruction, TimeDuration delay,
            InstructionType type) {

        if (mInstructions.length == 0) {

            mInvocationTimeNs = new long[1];
            mInstructions = new InvocationInstruction[1];
            mDelays = new TimeDuration[1];
            mTypes = new InstructionType[1];
        }

        final int i = mLast;

        mInvocationTimeNs[i] = System.nanoTime();
        mInstructions[i] = invocationInstruction;
        mDelays[i] = delay;
        mTypes[i] = type;

        final int newLast;

        if ((i >= (mInstructions.length - 1)) || (i == Integer.MAX_VALUE)) {

            newLast = 0;

        } else {

            newLast = i + 1;
        }

        if (mFirst == newLast) {

            ensureCapacity(mInstructions.length + 1);
        }

        mLast = newLast;
    }

    private void addAbort(final InvocationInstruction invocationInstruction) {

        add(invocationInstruction, TimeDuration.ZERO, InstructionType.ABORT);

        if (!mIsRunning) {

            run();
        }
    }

    private void addRun(final InvocationInstruction invocationInstruction, final long delay,
            final TimeUnit timeUnit) {

        add(invocationInstruction, TimeDuration.fromUnit(delay, timeUnit), InstructionType.RUN);

        if (!mIsRunning) {

            run();
        }
    }

    private void ensureCapacity(final int capacity) {

        final int size = mInstructions.length;

        if (capacity <= size) {

            return;
        }

        int newSize = size;

        while (newSize < capacity) {

            newSize = newSize << 1;

            if (newSize < size) {

                throw new OutOfMemoryError();
            }
        }

        final int first = mFirst;

        final int last = mLast;

        final long[] newInvocationTimeNs = new long[newSize];
        resizeArray(mInvocationTimeNs, newInvocationTimeNs, first);

        final InvocationInstruction[] newInvocationInstructions =
                new InvocationInstruction[newSize];
        resizeArray(mInstructions, newInvocationInstructions, first);

        final TimeDuration[] newDelays = new TimeDuration[newSize];
        resizeArray(mDelays, newDelays, first);

        final InstructionType[] newTypes = new InstructionType[newSize];
        resizeArray(mTypes, newTypes, first);

        mInvocationTimeNs = newInvocationTimeNs;
        mInstructions = newInvocationInstructions;
        mDelays = newDelays;
        mTypes = newTypes;

        final int shift = newSize - size;

        mFirst = first + shift;
        mLast = (last < first) ? last : last + shift;
    }

    private void run() {

        mIsRunning = true;

        try {

            while (mFirst != mLast) {

                final int i = mFirst;
                final int last = mLast;
                final long[] invocationTimeNs = mInvocationTimeNs;
                final InvocationInstruction[] invocationInstructions = mInstructions;
                final TimeDuration[] delays = mDelays;
                final InstructionType[] types = mTypes;

                long timeNs = invocationTimeNs[i];
                InvocationInstruction invocationInstruction = invocationInstructions[i];
                TimeDuration delay = delays[i];
                InstructionType type = types[i];

                final long currentTimeNs = System.nanoTime();
                long delayNs = timeNs - currentTimeNs + delay.toNanos();

                if (delayNs > 0) {

                    final int length = invocationInstructions.length;
                    long minDelay = delayNs;
                    int s = i;

                    int j = i + 1;

                    if (j >= length) {

                        j = 0;
                    }

                    while (j != last) {

                        final long nextDelayNs =
                                invocationTimeNs[j] - currentTimeNs + delays[j].toNanos();

                        if (nextDelayNs <= 0) {

                            s = j;

                            break;
                        }

                        if (nextDelayNs < minDelay) {

                            minDelay = nextDelayNs;
                            s = j;
                        }

                        if (++j >= length) {

                            j = 0;
                        }
                    }

                    if (s != i) {

                        timeNs = invocationTimeNs[s];
                        invocationInstruction = invocationInstructions[s];
                        delay = delays[s];
                        type = types[s];

                        invocationTimeNs[s] = invocationTimeNs[i];
                        invocationInstructions[s] = invocationInstructions[i];
                        delays[s] = delays[i];
                        types[s] = types[i];
                    }

                    delayNs = timeNs - System.nanoTime() + delay.toNanos();
                }

                if (delayNs > 0) {

                    try {

                        TimeDuration.nanos(delayNs).sleepAtLeast();

                    } catch (final InterruptedException e) {

                        RoutineInterruptedException.interrupt(e);
                    }
                }

                try {

                    // This call could be re-entrant
                    if (type == InstructionType.RUN) {

                        invocationInstruction.run();

                    } else {

                        invocationInstruction.abort();
                    }

                } catch (final RoutineInterruptedException e) {

                    throw e;

                } catch (final Throwable ignored) {

                }

                // Note that the field values may have changed here
                final int n = mFirst;

                mInstructions[n] = null;
                mDelays[n] = null;

                final int newFirst = mFirst + 1;

                if (newFirst >= mInstructions.length) {

                    mFirst = 0;

                } else {

                    mFirst = newFirst;
                }
            }

        } finally {

            mIsRunning = false;
        }
    }

    private enum InstructionType {

        RUN,
        ABORT
    }
}