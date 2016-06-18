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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;

/**
 * Default implementation of a stream channel.
 * <p>
 * Created by davide-maestroni on 12/23/2015.
 *
 * @param <OUT> the output data type.
 */
class DefaultStreamChannel<IN, OUT> extends AbstractStreamChannel<IN, OUT> {

    /**
     * Constructor.
     *
     * @param channel the wrapped channel.
     */
    DefaultStreamChannel(@NotNull final Channel<?, IN> channel) {
        super(new DefaultStreamConfiguration(InvocationConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC), channel);
    }

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     * @param sourceChannel       the source channel.
     * @param bindingFunction     if null the stream will act as a wrapper of the source output
     *                            channel.
     */
    private DefaultStreamChannel(@NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, IN> sourceChannel,
            @Nullable final Function<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
        super(streamConfiguration, sourceChannel, bindingFunction);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> StreamChannel<BEFORE, AFTER> newChannel(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, BEFORE> sourceChannel,
            @NotNull final Function<Channel<?, BEFORE>, Channel<?, AFTER>> bindingFunction) {
        return new DefaultStreamChannel<BEFORE, AFTER>(streamConfiguration, sourceChannel,
                bindingFunction);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode) {
        return new DefaultStreamConfiguration(streamConfiguration, configuration, invocationMode);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode) {
        return new DefaultStreamConfiguration(streamConfiguration,
                InvocationConfiguration.defaultConfiguration(), invocationMode);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return JRoutineCore.on(factory)
                           .invocationConfiguration()
                           .with(streamConfiguration.asInvocationConfiguration())
                           .apply()
                           .buildRoutine();
    }

    /**
     * Default implementation of a stream configuration.
     */
    private static class DefaultStreamConfiguration implements StreamConfiguration {

        private final InvocationConfiguration mConfiguration;

        private final InvocationMode mInvocationMode;

        private final InvocationConfiguration mStreamConfiguration;

        private volatile ChannelConfiguration mChannelConfiguration;

        private volatile InvocationConfiguration mInvocationConfiguration;

        /**
         * Constructor.
         *
         * @param streamConfiguration  the stream invocation configuration.
         * @param currentConfiguration the current invocation configuration.
         * @param invocationMode       the invocation mode.
         */
        private DefaultStreamConfiguration(
                @NotNull final InvocationConfiguration streamConfiguration,
                @NotNull final InvocationConfiguration currentConfiguration,
                @NotNull final InvocationMode invocationMode) {
            mStreamConfiguration = ConstantConditions.notNull("stream invocation configuration",
                    streamConfiguration);
            mConfiguration = ConstantConditions.notNull("current invocation configuration",
                    currentConfiguration);
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        @NotNull
        public ChannelConfiguration asChannelConfiguration() {
            if (mChannelConfiguration == null) {
                mChannelConfiguration =
                        builderFromOutputChannel(asInvocationConfiguration()).apply();
            }

            return mChannelConfiguration;
        }

        @NotNull
        public InvocationConfiguration asInvocationConfiguration() {
            if (mInvocationConfiguration == null) {
                mInvocationConfiguration =
                        mStreamConfiguration.builderFrom().with(mConfiguration).apply();
            }

            return mInvocationConfiguration;
        }

        @NotNull
        public InvocationConfiguration getCurrentConfiguration() {
            return mConfiguration;
        }

        @NotNull
        public InvocationMode getInvocationMode() {
            return mInvocationMode;
        }

        @NotNull
        public InvocationConfiguration getStreamConfiguration() {
            return mStreamConfiguration;
        }
    }
}
