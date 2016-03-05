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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.builder.ChannelConfiguration;
import com.github.dm.jrt.core.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.builder.IOChannelBuilder;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.runner.Runners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Class implementing a builder of I/O channel objects.
 * <p/>
 * Created by davide-maestroni on 10/25/2014.
 */
class DefaultIOChannelBuilder implements IOChannelBuilder, Configurable<IOChannelBuilder> {

    private static final Runner sSyncRunner = Runners.syncRunner();

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Avoid direct instantiation.
     */
    DefaultIOChannelBuilder() {

    }

    @NotNull
    public <DATA> IOChannel<DATA> buildChannel() {

        final ChannelConfiguration configuration = mConfiguration;
        final IORunner runner = new IORunner(configuration.getRunnerOr(Runners.sharedRunner()));
        return new DefaultIOChannel<DATA>(
                configuration.builderFrom().withRunner(runner).getConfigured());
    }

    @NotNull
    public <DATA> IOChannel<DATA> of(@Nullable final DATA input) {

        return this.<DATA>buildChannel().pass(input).close();
    }

    @NotNull
    public <DATA> IOChannel<DATA> of(@Nullable final DATA... inputs) {

        return this.<DATA>buildChannel().pass(inputs).close();
    }

    @NotNull
    public <DATA> IOChannel<DATA> of(@Nullable final Iterable<DATA> inputs) {

        return this.<DATA>buildChannel().pass(inputs).close();
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public IOChannelBuilder setConfiguration(@NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }

    @NotNull
    public Builder<? extends IOChannelBuilder> withChannels() {

        return new Builder<IOChannelBuilder>(this, mConfiguration);
    }

    /**
     * Runner decorator which run executions synchronously if delay is 0.
     */
    private static class IORunner extends RunnerDecorator {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped instance.
         */
        public IORunner(@NotNull final Runner wrapped) {

            super(wrapped);
        }

        @Override
        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

            if (delay == 0) {
                sSyncRunner.run(execution, delay, timeUnit);

            } else {
                super.run(execution, delay, timeUnit);
            }
        }
    }
}
