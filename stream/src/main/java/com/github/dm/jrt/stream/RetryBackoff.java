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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Backoff retry policy implementation.
 * <p>
 * Created by davide-maestroni on 05/09/2016.
 */
class RetryBackoff implements BiFunction<Integer, RoutineException, Long> {

    private final Backoff mBackoff;

    private final int mCount;

    /**
     * Constructor.
     *
     * @param count   the retry count.
     * @param backoff the backoff policy
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    RetryBackoff(final int count, @NotNull final Backoff backoff) {

        mCount = ConstantConditions.positive("max retries", count);
        mBackoff = ConstantConditions.notNull("backoff policy", backoff);
    }

    public Long apply(final Integer count, final RoutineException error) {

        if (count <= mCount) {
            return mBackoff.getDelay(count);
        }

        return null;
    }
}
