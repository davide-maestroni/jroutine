/*
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
package com.gh.bmd.jrt.android.v4.core;

import com.gh.bmd.jrt.channel.TransportChannel;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a loader invocation result.
 * <p/>
 * Created by davide-maestroni on 1/4/15.
 *
 * @param <OUTPUT> the output data type.
 */
interface InvocationResult<OUTPUT> {

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
    Throwable getAbortException();

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
    boolean passTo(@Nonnull Collection<TransportChannel<OUTPUT>> newChannels,
            @Nonnull Collection<TransportChannel<OUTPUT>> oldChannels,
            @Nonnull Collection<TransportChannel<OUTPUT>> abortedChannels);
}
