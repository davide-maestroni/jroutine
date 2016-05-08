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
 * Created by davide-maestroni on 05/07/2016.
 */
public abstract class Backoff implements BiFunction<Integer, RoutineException, Long> {

    @NotNull
    public static ConstantBackoff times(final int count) {

        return new ConstantBackoff(count, 0);
    }

    @NotNull
    public Backoff cappedTo(final long value, @NotNull final TimeUnit unit) {

        return new CappedBackoff(this, unit.toMillis(value));
    }

    @NotNull
    public Backoff cappedTo(@NotNull final UnitDuration delay) {

        return new CappedBackoff(this, delay.toMillis());
    }

    @NotNull
    public Backoff withJitter(final float percent) {

        return new JitterBackoff(this, percent);
    }

    public static class ConstantBackoff extends DelayBackoff {

        private final int mCount;

        private ConstantBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
            mCount = count;
        }

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
        protected long nexDelay(final Integer count, final long delayMillis) {

            return delayMillis;
        }
    }

    private static class DecorrelatedJitterBackoff extends DelayBackoff {

        private final Random mRandom = new Random();

        private long mLast;

        private DecorrelatedJitterBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
            mLast = delayMillis;
        }

        @Override
        protected long nexDelay(final Integer count, final long delayMillis) {

            mLast = delayMillis + Math.round(mLast * 3 * mRandom.nextDouble());
            return mLast;
        }
    }

    private static abstract class DelayBackoff extends Backoff {

        private final int mCount;

        private final long mDelay;

        private DelayBackoff(final int count, final long delayMillis) {

            mCount = ConstantConditions.notNegative("retry count", count);
            mDelay = ConstantConditions.notNegative("backoff delay", delayMillis);
        }

        public final Long apply(final Integer count, final RoutineException error) {

            if (count <= mCount) {
                return nexDelay(count, mDelay);
            }

            return null;
        }

        protected abstract long nexDelay(Integer count, long delayMillis);
    }

    private static class ExponentialBackoff extends DelayBackoff {

        private ExponentialBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
        }

        @Override
        protected long nexDelay(final Integer count, final long delayMillis) {

            return delayMillis << (count - 1);
        }
    }

    private static class LinearBackoff extends DelayBackoff {

        private LinearBackoff(final int count, final long delayMillis) {

            super(count, delayMillis);
        }

        @Override
        protected long nexDelay(final Integer count, final long delayMillis) {

            return delayMillis * count;
        }
    }

    private class CappedBackoff extends Backoff {

        private final Backoff mBackoff;

        private final long mDelay;

        public CappedBackoff(@NotNull final Backoff wrapped, final long delayMillis) {

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

    private class JitterBackoff extends Backoff {

        private final Backoff mBackoff;

        private final float mPercent;

        private final Random mRandom = new Random();

        public JitterBackoff(@NotNull final Backoff wrapped, final float percent) {

            if ((percent < 0) || (percent > 1)) {
                throw new IllegalArgumentException(
                        "the jitter percent must be between 0 and 1, but is: " + percent);
            }

            mBackoff = wrapped;
            mPercent = percent;
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
}
