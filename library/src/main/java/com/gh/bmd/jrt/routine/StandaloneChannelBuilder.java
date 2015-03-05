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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.channel.StandaloneChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of standalone channel objects.
 * <p/>
 * Created by davide on 10/25/14.
 */
public class StandaloneChannelBuilder implements RoutineBuilder {

    private RoutineConfiguration mConfiguration;

    /**
     * Avoid direct instantiation.
     */
    StandaloneChannelBuilder() {

    }

    /**
     * Builds and returns the standalone channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    public <T> StandaloneChannel<T> buildChannel() {

        return new DefaultStandaloneChannel<T>(RoutineConfiguration.notNull(mConfiguration));
    }

    /**
     * Note that only options related to the output channel, the asynchronous runner and the logs
     * will be employed.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    public StandaloneChannelBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }
}
