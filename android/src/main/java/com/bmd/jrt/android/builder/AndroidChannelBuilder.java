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
package com.bmd.jrt.android.builder;

import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ResultCache;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of output channel linked to routine invocations.<br/>
 * In order to be successfully linked, the specific routine invocation must have a user defined
 * ID and still running (or cached) at the time of the channel creation.
 * <p/>
 * Created by davide on 1/14/15.
 *
 * @see AndroidRoutineBuilder
 */
public interface AndroidChannelBuilder {

    /**
     * Builds and returns the an output channel linked to the routine invocation.
     *
     * @return the newly created output channel.
     */
    @Nonnull
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel();

    /**
     * Sets the log level.
     *
     * @param level the log level.
     * @return this builder.
     * @throws NullPointerException if the log level is null.
     */
    @Nonnull
    public AndroidChannelBuilder logLevel(@Nonnull LogLevel level);

    /**
     * Sets the log instance. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param log the log instance.
     * @return this builder.
     */
    @Nonnull
    public AndroidChannelBuilder loggedWith(@Nullable Log log);

    /**
     * Tells the builder how to cache the invocation result after its completion.
     *
     * @param cacheType the cache type.
     * @return this builder.
     * @throws NullPointerException if the specified cache type is null.
     */
    @Nonnull
    public AndroidChannelBuilder onComplete(@Nonnull ResultCache cacheType);
}
