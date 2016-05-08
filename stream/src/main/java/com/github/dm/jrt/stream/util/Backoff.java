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

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;

/**
 * Created by davide-maestroni on 05/07/2016.
 */
public abstract class Backoff implements BiFunction<Integer, RoutineException, UnitDuration> {

    @NotNull
    public static ConstantBackoff times(final int count) {

        return new ConstantBackoff(count, UnitDuration.zero());
    }

    public static class ConstantBackoff extends Backoff {

        private final int mCount;

        private final UnitDuration mDelay;

        private ConstantBackoff(final int count, @NotNull final UnitDuration delay) {

            mCount = ConstantConditions.notNegative("retry count", count);
            mDelay = ConstantConditions.notNull("backoff delay", delay);
        }

        public UnitDuration apply(final Integer count, final RoutineException error) {

            if (count <= mCount) {
                return mDelay;
            }

            return null;
        }

        @NotNull
        public Backoff constantDelay(final long value, @NotNull final TimeUnit unit) {

            return new ConstantBackoff(mCount, fromUnit(value, unit));
        }

        @NotNull
        public Backoff constantDelay(@NotNull final UnitDuration delay) {

            return new ConstantBackoff(mCount, delay);
        }

        @NotNull
        public Backoff exponentialDelay(final long value, @NotNull final TimeUnit unit) {

            return new ExponentialBackoff(mCount, fromUnit(value, unit));
        }

        @NotNull
        public Backoff exponentialDelay(@NotNull final UnitDuration delay) {

            return new ExponentialBackoff(mCount, delay);
        }

        @NotNull
        public Backoff linearDelay(final long value, @NotNull final TimeUnit unit) {

            return new LinearBackoff(mCount, fromUnit(value, unit));
        }

        @NotNull
        public Backoff linearDelay(@NotNull final UnitDuration delay) {

            return new LinearBackoff(mCount, delay);
        }
    }

    private static class ExponentialBackoff extends Backoff {

        private final int mCount;

        private final UnitDuration mDelay;

        private ExponentialBackoff(final int count, @NotNull final UnitDuration delay) {

            mCount = ConstantConditions.notNegative("retry count", count);
            mDelay = ConstantConditions.notNull("backoff delay", delay);
        }

        public UnitDuration apply(final Integer count, final RoutineException error) {

            if (count <= mCount) {
                final UnitDuration delay = mDelay;
                return fromUnit(delay.value << (count - 1), delay.unit);
            }

            return null;
        }
    }

    private static class LinearBackoff extends Backoff {

        private final int mCount;

        private final UnitDuration mDelay;

        private LinearBackoff(final int count, @NotNull final UnitDuration delay) {

            mCount = ConstantConditions.notNegative("retry count", count);
            mDelay = ConstantConditions.notNull("backoff delay", delay);
        }

        public UnitDuration apply(final Integer count, final RoutineException error) {

            if (count <= mCount) {
                final UnitDuration delay = mDelay;
                return fromUnit(delay.value * count, delay.unit);
            }

            return null;
        }
    }
}
