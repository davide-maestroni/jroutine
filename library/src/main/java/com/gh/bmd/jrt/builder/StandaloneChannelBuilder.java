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

import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Configurable;
import com.gh.bmd.jrt.channel.StandaloneChannel;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of standalone channel objects.
 * <p/>
 * Created by davide on 3/7/15.
 */
public interface StandaloneChannelBuilder extends Configurable<StandaloneChannelBuilder> {

    /**
     * Builds and returns the standalone channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    <T> StandaloneChannel<T> buildChannel();

    /**
     * Note that only options related to the output channel, the asynchronous runner and the logs
     * will be employed.
     *
     * @return the configuration builder.
     */
    @Nonnull
    Builder<StandaloneChannelBuilder> configure();
}
