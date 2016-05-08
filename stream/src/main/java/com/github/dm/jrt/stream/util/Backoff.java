/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.stream.util;

import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiFunction;

import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Backoff utility class.
 * <br>
 * This class is useful to build a backoff policy to be used with the
 * {@link com.github.dm.jrt.stream.StreamChannel#retry(BiFunction) StreamChannel#retry(BiFunction)}
 * method.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 */
public abstract class Backoff implements BiFunction<Integer, RoutineException, Long> {

    /**
     * Returns a backoff policy retrying the specified number of time with a 0 delay.
     *
     * @param count the maximum number of retries.
     * @return the backoff policy instance.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    public static ConstantBackoff times(final int count) {

        return new ConstantBackoff(count, 0);
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
    public Backoff cappedTo(final long value, @NotNull final TimeUnit unit) {

        return new CappedBackoff(this, unit.toMillis(value));
    }

    /**
     * Caps this backoff policy to the specified maximum delay.
     *
     * @param delay the maximum delay.
     * @return the capped backoff policy.
     * @throws java.lang.IllegalArgumentException if the delay is negative.
     */
    @NotNull
    public Backoff cappedTo(@NotNull final UnitDuration delay) {

        return new CappedBackoff(this, delay.toMillis());
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
    public Backoff withJitter(final float percentage) {

        return new JitterBackoff(this, percentage);
    }

    /**
     * Constant backoff policy.
     */
    public static class ConstantBackoff extends DelayBackoff {

        private final int mCount;

        /**
         * Constructor.
         *
         * @param count       the maximum number of retries.
         * @param delayMillis the delay in milliseconds.
         */
        private ConstantBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
            mCount = count;
        }

        // TODO: 08/05/16 javadoc
        @NotNull
        public Backoff constantDelay(final long value, @NotNull final TimeUnit unit) {

            return new ConstantBackoff(mCount, unit.toMillis(value));
        }

        @NotNull
        public Backoff constantDelay(@NotNull final UnitDuration delay) {

            return new ConstantBackoff(mCount, delay.toMillis());
        }

        @NotNull
        public Backoff exponentialDelay(final long value, @NotNull final TimeUnit unit) {

            return new ExponentialBackoff(mCount, unit.toMillis(value));
        }

        @NotNull
        public Backoff exponentialDelay(@NotNull final UnitDuration delay) {

            return new ExponentialBackoff(mCount, delay.toMillis());
        }

        @NotNull
        public Backoff jitterDelay(final long value, @NotNull final TimeUnit unit) {

            return new DecorrelatedJitterBackoff(mCount, unit.toMillis(value));
        }

        @NotNull
        public Backoff jitterDelay(@NotNull final UnitDuration delay) {

            return new DecorrelatedJitterBackoff(mCount, delay.toMillis());
        }

        @NotNull
        public Backoff linearDelay(final long value, @NotNull final TimeUnit unit) {

            return new LinearBackoff(mCount, unit.toMillis(value));
        }

        @NotNull
        public Backoff linearDelay(@NotNull final UnitDuration delay) {

            return new LinearBackoff(mCount, delay.toMillis());
        }

        @Override
        protected long nexDelay(@NotNull final Integer count, final long delayMillis) {

            return delayMillis;
        }
    }

    /**
     * Capped delay backoff policy.
     */
    private static class CappedBackoff extends Backoff {

        private final Backoff mBackoff;

        private final long mDelay;

        /**
         * Constructor.
         *
         * @param wrapped     the wrapped backoff instance.
         * @param delayMillis the maximum delay in milliseconds.
         */
        private CappedBackoff(@NotNull final Backoff wrapped, final long delayMillis) {

            mBackoff = wrapped;
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        public Long apply(final Integer count, final RoutineException error) {

            final Long delay = mBackoff.apply(count, error);
            if (delay != null) {
                return Math.min(delay, mDelay);
            }

            return null;
        }
    }

    /**
     * De-correlated jitter backoff.
     */
    private static class DecorrelatedJitterBackoff extends DelayBackoff {

        private final Random mRandom = new Random();

        private long mLast;

        /**
         * Constructor.
         *
         * @param count       the maximum number of retries.
         * @param delayMillis the delay in milliseconds.
         */
        private DecorrelatedJitterBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
            mLast = delayMillis;
        }

        @Override
        protected long nexDelay(@NotNull final Integer count, final long delayMillis) {

            mLast = delayMillis + Math.round(mLast * 3 * mRandom.nextDouble());
            return mLast;
        }
    }

    /**
     * Delay based backoff policy.
     */
    private static abstract class DelayBackoff extends Backoff {

        private final int mCount;

        private final long mDelay;

        /**
         * Constructor.
         *
         * @param count       the maximum number of retries.
         * @param delayMillis the delay in milliseconds.
         */
        private DelayBackoff(final int count, final long delayMillis) {

            mCount = ConstantConditions.notNegative("retry count", count);
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        /**
         * Gets the next delay in milliseconds.
         *
         * @param count       the retry count.
         * @param delayMillis the base delay.
         * @return the next delay.
         */
        protected abstract long nexDelay(@NotNull Integer count, long delayMillis);

        public final Long apply(final Integer count, final RoutineException error) {

            if (count <= mCount) {
                return nexDelay(count, mDelay);
            }

            return null;
        }
    }

    /**
     * Exponentially increasing backoff policy.
     */
    private static class ExponentialBackoff extends DelayBackoff {

        /**
         * Constructor.
         *
         * @param count       the maximum number of retries.
         * @param delayMillis the delay in milliseconds.
         */
        private ExponentialBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
        }

        @Override
        protected long nexDelay(@NotNull final Integer count, final long delayMillis) {

            return delayMillis << (count - 1);
        }
    }

    /**
     * Backoff policy with jitter addition.
     */
    private static class JitterBackoff extends Backoff {

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

            if ((percentage < 0) || (percentage > 1)) {
                throw new IllegalArgumentException(
                        "the jitter percentage must be between 0 and 1, but is: " + percentage);
            }

            mBackoff = wrapped;
            mPercent = percentage;
        }

        public Long apply(final Integer count, final RoutineException error) {

            final Long delay = mBackoff.apply(count, error);
            if (delay != null) {
                final float percent = mPercent;
                final long value = delay;
                return Math.round(
                        (value * percent * mRandom.nextDouble()) + (value * (1 - percent)));
            }

            return null;
        }
    }

    /**
     * Linearly increasing backoff policy.
     */
    private static class LinearBackoff extends DelayBackoff {

        /**
         * Constructor.
         *
         * @param count       the maximum number of retries.
         * @param delayMillis the delay in milliseconds.
         */
        private LinearBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
        }

        @Override
        protected long nexDelay(@NotNull final Integer count, final long delayMillis) {

            return delayMillis * count;
        }
    }
}
