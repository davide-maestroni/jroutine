/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * A builder of backoff instances.
 * <br>
 * This class is useful to build a backoff policy, returning a delay in milliseconds to apply when
 * a counter exceeds a specified limit.
 * <p>
 * Created by davide-maestroni on 05/10/2016.
 */
public class BackoffBuilder {

    private static final BaseBackoff sNoDelay = new BaseBackoff(null) {

        public long getDelay(final int count) {
            return NO_DELAY;
        }
    };

    private final int mCount;

    /**
     * Constructor.
     *
     * @param count the offset count.
     */
    private BackoffBuilder(final int count) {
        mCount = ConstantConditions.notNegative("offset count", count);
    }

    /**
     * Returns a builder of backoff instances applying a delay when the passed count exceeds the
     * specified value.
     *
     * @param count the count value.
     * @return the builder instance.
     */
    @NotNull
    public static BackoffBuilder afterCount(final int count) {
        return new BackoffBuilder(count);
    }

    /**
     * Returns the no delay backoff instance.
     * <br>
     * The backoff will always return {@code NO_DELAY}.
     *
     * @return the backoff instance.
     */
    @NotNull
    public static BaseBackoff noDelay() {
        return sNoDelay;
    }

    /**
     * Returns a constant backoff.
     * <br>
     * The backoff will always return the specified delay.
     *
     * @param value the delay value.
     * @param unit  the delay unit.
     * @return the backoff instance.
     * @throws java.lang.IllegalArgumentException if the delay is negative.
     */
    @NotNull
    public BaseBackoff constantDelay(final long value, @NotNull final TimeUnit unit) {
        return new ConstantBackoff(mCount, unit.toMillis(value));
    }

    /**
     * Returns a constant backoff.
     * <br>
     * The backoff will always return the specified delay.
     *
     * @param delay the delay.
     * @return the backoff instance.
     */
    @NotNull
    public BaseBackoff constantDelay(@NotNull final UnitDuration delay) {
        return new ConstantBackoff(mCount, delay.toMillis());
    }

    /**
     * Returns an exponentially increasing backoff.
     * <br>
     * The backoff will return a delay computed as: {@code delay * 2^(count - 1)}.
     *
     * @param value the delay value.
     * @param unit  the delay unit.
     * @return the backoff instance.
     * @throws java.lang.IllegalArgumentException if the delay is negative.
     */
    @NotNull
    public BaseBackoff exponentialDelay(final long value, @NotNull final TimeUnit unit) {
        return new ExponentialBackoff(mCount, unit.toMillis(value));
    }

    /**
     * Returns an exponentially increasing backoff.
     * <br>
     * The backoff will return a delay computed as: {@code delay * 2^(count - 1)}.
     *
     * @param delay the delay.
     * @return the backoff instance.
     */
    @NotNull
    public BaseBackoff exponentialDelay(@NotNull final UnitDuration delay) {
        return new ExponentialBackoff(mCount, delay.toMillis());
    }

    /**
     * Returns a de-correlated jitter backoff.
     * <br>
     * The backoff will return a delay computed by taking in consideration the previous jitter
     * delay.
     * <p>
     * Note that this particular implementation tries to scale the maximum jitter on the count
     * value.
     *
     * @param value the delay value.
     * @param unit  the delay unit.
     * @return the backoff instance.
     * @throws java.lang.IllegalArgumentException if the delay is negative.
     */
    @NotNull
    public BaseBackoff jitterDelay(final long value, @NotNull final TimeUnit unit) {
        return new DecorrelatedJitterBackoff(mCount, unit.toMillis(value));
    }

    /**
     * Returns a de-correlated jitter backoff.
     * <br>
     * The backoff will return a delay computed by taking in consideration the previous jitter
     * delay.
     * <p>
     * Note that this particular implementation tries to scale the maximum jitter on the count
     * value.
     *
     * @param delay the delay.
     * @return the backoff instance.
     */
    @NotNull
    public BaseBackoff jitterDelay(@NotNull final UnitDuration delay) {
        return new DecorrelatedJitterBackoff(mCount, delay.toMillis());
    }

    /**
     * Returns an linearly increasing backoff.
     * <br>
     * The backoff will return a delay computed as: {@code delay * count}.
     *
     * @param value the delay value.
     * @param unit  the delay unit.
     * @return the backoff instance.
     * @throws java.lang.IllegalArgumentException if the delay is negative.
     */
    @NotNull
    public BaseBackoff linearDelay(final long value, @NotNull final TimeUnit unit) {
        return new LinearBackoff(mCount, unit.toMillis(value));
    }

    /**
     * Returns an linearly increasing backoff.
     * <br>
     * The backoff will return a delay computed as: {@code delay * count}.
     *
     * @param delay the delay.
     * @return the backoff instance.
     */
    @NotNull
    public BaseBackoff linearDelay(@NotNull final UnitDuration delay) {
        return new LinearBackoff(mCount, delay.toMillis());
    }

    /**
     * Base backoff policy implementation.
     */
    public static abstract class BaseBackoff extends DeepEqualObject implements Backoff {

        /**
         * Constructor.
         *
         * @param args the constructor arguments.
         */
        private BaseBackoff(@Nullable final Object[] args) {
            super(args);
        }

        /**
         * Sums the specified backoff to this one.
         * <br>
         * For each input count, if at least one of the returned delays is different than
         * {@code NO_DELAY}, its value is returned. If both are different, the sum of the two values
         * is returned.
         *
         * @param backoff the backoff to add.
         * @return the summed backoff policy.
         */
        @NotNull
        public BaseBackoff add(@NotNull final Backoff backoff) {
            return new AddedBackoff(this, backoff);
        }

        /**
         * Caps this backoff policy to the specified maximum delay.
         *
         * @param delay the maximum delay.
         * @return the capped backoff policy.
         * @throws java.lang.IllegalArgumentException if the delay is negative.
         */
        @NotNull
        public BaseBackoff cappedTo(@NotNull final UnitDuration delay) {
            return new CappedBackoff(this, delay.toMillis());
        }

        /**
         * Caps this backoff policy to the specified maximum delay.
         *
         * @param value the delay value.
         * @param unit  the delay unit.
         * @return the capped backoff policy.
         * @throws java.lang.IllegalArgumentException if the delay is negative.
         */
        @NotNull
        public BaseBackoff cappedTo(final long value, @NotNull final TimeUnit unit) {
            return new CappedBackoff(this, unit.toMillis(value));
        }

        /**
         * Adds jitter to this backoff policy.
         *
         * @param percentage a floating number between 0 and 1 indicating the percentage of delay to
         *                   randomize.
         * @return the backoff policy with jitter.
         * @throws java.lang.IllegalArgumentException if the percentage is outside the [0, 1] range.
         */
        @NotNull
        public BaseBackoff withJitter(final float percentage) {
            return new JitterBackoff(this, percentage);
        }
    }

    /**
     * Constant backoff policy.
     */
    public static class ConstantBackoff extends BaseBackoff {

        private final long mDelay;

        private final int mOffset;

        /**
         * Constructor.
         *
         * @param offset      the offset count;
         * @param delayMillis the delay in milliseconds.
         * @throws java.lang.IllegalArgumentException if the delay is negative.
         */
        private ConstantBackoff(final int offset, final long delayMillis) {
            super(asArgs(offset, delayMillis));
            mOffset = offset;
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        public long getDelay(final int count) {
            if (count <= mOffset) {
                return NO_DELAY;
            }

            return mDelay;
        }
    }

    /**
     * Sum backoff policy.
     */
    private static class AddedBackoff extends BaseBackoff {

        private final Backoff mBackoff;

        private final Backoff mOther;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped backoff instance.
         * @param other   the other backoff to sum.
         */
        private AddedBackoff(@NotNull final Backoff wrapped, @NotNull final Backoff other) {
            super(asArgs(wrapped, other));
            mBackoff = wrapped;
            mOther = ConstantConditions.notNull("backoff instance", other);
        }

        public long getDelay(final int count) {
            final long delay1 = mBackoff.getDelay(count);
            final long delay2 = mOther.getDelay(count);
            if (delay1 < 0) {
                if (delay2 < 0) {
                    return NO_DELAY;
                }

                return delay2;

            } else if (delay2 < 0) {
                return delay1;
            }

            return delay1 + delay2;
        }
    }

    /**
     * Capped delay backoff policy.
     */
    private static class CappedBackoff extends BaseBackoff {

        private final Backoff mBackoff;

        private final long mDelay;

        /**
         * Constructor.
         *
         * @param wrapped     the wrapped backoff instance.
         * @param delayMillis the maximum delay in milliseconds.
         */
        private CappedBackoff(@NotNull final Backoff wrapped, final long delayMillis) {
            super(asArgs(wrapped, delayMillis));
            mBackoff = wrapped;
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        public long getDelay(final int count) {
            return Math.min(mBackoff.getDelay(count), mDelay);
        }
    }

    /**
     * De-correlated jitter backoff.
     */
    private static class DecorrelatedJitterBackoff extends BaseBackoff {

        private final long mDelay;

        private final int mOffset;

        private final Random mRandom = new Random();

        private long mLast;

        /**
         * Constructor.
         *
         * @param offset      the offset count;
         * @param delayMillis the delay in milliseconds.
         */
        private DecorrelatedJitterBackoff(final int offset, final long delayMillis) {
            super(asArgs(offset, delayMillis));
            mOffset = offset;
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
            mLast = delayMillis;
        }

        public long getDelay(final int count) {
            final int excess = count - mOffset;
            if (excess <= 0) {
                return NO_DELAY;
            }

            final long delay = mDelay;
            final double last = Math.IEEEremainder(mLast, delay) + (delay * (1 << (excess - 1)));
            mLast = delay + Math.round(((last * 3) - delay) * mRandom.nextDouble());
            return mLast;
        }
    }

    /**
     * Exponentially increasing backoff policy.
     */
    private static class ExponentialBackoff extends BaseBackoff {

        private final long mDelay;

        private final int mOffset;

        /**
         * Constructor.
         *
         * @param offset      the offset count;
         * @param delayMillis the delay in milliseconds.
         */
        private ExponentialBackoff(final int offset, final long delayMillis) {
            super(asArgs(offset, delayMillis));
            mOffset = offset;
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        public long getDelay(final int count) {
            final int excess = count - mOffset;
            if (excess <= 0) {
                return NO_DELAY;
            }

            return mDelay << (excess - 1);
        }
    }

    /**
     * Backoff policy with jitter addition.
     */
    private static class JitterBackoff extends BaseBackoff {

        private final Backoff mBackoff;

        private final float mPercent;

        private final Random mRandom = new Random();

        /**
         * Constructor.
         *
         * @param wrapped    the wrapped backoff instance.
         * @param percentage the percentage of delay to randomize.
         * @throws java.lang.IllegalArgumentException if the percentage is outside the [0, 1] range.
         */
        private JitterBackoff(@NotNull final Backoff wrapped, final float percentage) {
            super(asArgs(wrapped, percentage));
            if ((percentage < 0) || (percentage > 1)) {
                throw new IllegalArgumentException(
                        "the jitter percentage must be between 0 and 1, but is: " + percentage);
            }

            mBackoff = wrapped;
            mPercent = percentage;
        }

        public long getDelay(final int count) {
            final long delay = mBackoff.getDelay(count);
            if (delay == NO_DELAY) {
                return NO_DELAY;
            }

            final float percent = mPercent;
            return Math.round((delay * percent * mRandom.nextDouble()) + (delay * (1 - percent)));
        }
    }

    /**
     * Linearly increasing backoff policy.
     */
    private static class LinearBackoff extends BaseBackoff {

        private final long mDelay;

        private final int mOffset;

        /**
         * Constructor.
         *
         * @param offset      the offset count;
         * @param delayMillis the delay in milliseconds.
         */
        private LinearBackoff(final int offset, final long delayMillis) {
            super(asArgs(offset, delayMillis));
            mOffset = offset;
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        public long getDelay(final int count) {
            final int excess = count - mOffset;
            if (excess <= 0) {
                return NO_DELAY;
            }

            return mDelay * excess;
        }
    }
}
