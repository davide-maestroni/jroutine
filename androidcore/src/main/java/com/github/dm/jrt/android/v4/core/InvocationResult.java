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

package com.github.dm.jrt.android.v4.core;

import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Interface defining a loader invocation result.
 * <p>
 * Created by davide-maestroni on 01/04/2015.
 *
 * @param <OUT> the output data type.
 */
interface InvocationResult<OUT> {

    /**
     * Aborts the loader invocation.
     */
    void abort();

    /**
     * Returns the abort exception.
     *
     * @return the exception.
     */
    @Nullable
    RoutineException getAbortException();

    /**
     * Returns the timestamp of the latest delivered result or {@link Long#MAX_VALUE} if no result
     * has been produced yet.
     *
     * @return the timestamp.
     */
    long getResultTimestamp();

    /**
     * Checks if this result represents an error.
     *
     * @return whether the result is an error.
     */
    boolean isError();

    /**
     * Passes the cached results to the specified channels.
     *
     * @param newChannels     new channels freshly created.
     * @param oldChannels     old channels already fed with previous results.
     * @param abortedChannels list to be filled with the channels, from the other lists, that are
     *                        aborted while passing the results.
     * @return whether the invocation is complete.
     */
    boolean passTo(@NotNull Collection<IOChannel<OUT>> newChannels,
            @NotNull Collection<IOChannel<OUT>> oldChannels,
            @NotNull Collection<IOChannel<OUT>> abortedChannels);
}
