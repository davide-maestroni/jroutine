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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.builder.InvocationConfiguration.Builder;
import com.gh.bmd.jrt.channel.TransportChannel;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of transport channel objects.
 * <p/>
 * Created by davide-maestroni on 3/7/15.
 */
public interface TransportChannelBuilder extends ConfigurableBuilder<TransportChannelBuilder> {

    /**
     * Builds and returns the transport channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    <T> TransportChannel<T> buildChannel();

    /**
     * Note that only options related to the output channel, the asynchronous runner and the logs
     * will be employed.
     *
     * @return the invocation configuration builder.
     */
    @Nonnull
    Builder<? extends TransportChannelBuilder> invocations();
}
