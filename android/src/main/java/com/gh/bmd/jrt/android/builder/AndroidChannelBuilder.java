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
package com.gh.bmd.jrt.android.builder;

import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.CacheStrategy;
import com.gh.bmd.jrt.builder.ConfigurableBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of output channels bound to routine invocations.<br/>
 * In order to be successfully bound, the specific routine invocation must have a user defined ID
 * and still running (or cached) at the time of the channel creation.
 * <p/>
 * Created by davide on 1/14/15.
 *
 * @see com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder
 */
public interface AndroidChannelBuilder extends ConfigurableBuilder {

    /**
     * Builds and returns an output channel bound to the routine invocation.
     *
     * @return the newly created output channel.
     */
    @Nonnull
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel();

    /**
     * Tells the builder how to cache the invocation result after its completion. A null value means
     * that it is up to the framework to chose a default strategy.
     *
     * @param cacheStrategy the cache strategy.
     * @return this builder.
     */
    @Nonnull
    public AndroidChannelBuilder onComplete(@Nullable CacheStrategy cacheStrategy);

    /**
     * Makes the builder destroy the cached invocation instance.
     */
    public void purge();

    /**
     * Note that only the options related to logs will be employed.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    public AndroidChannelBuilder withConfiguration(@Nullable RoutineConfiguration configuration);
}
