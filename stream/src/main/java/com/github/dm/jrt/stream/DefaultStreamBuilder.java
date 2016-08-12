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
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of a stream routine builder.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStreamBuilder<IN, OUT> extends AbstractStreamBuilder<IN, OUT> {

    /**
     * Constructor.
     */
    DefaultStreamBuilder() {
        this(new DefaultStreamConfiguration(InvocationConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC));
    }

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     */
    DefaultStreamBuilder(@NotNull final StreamConfiguration streamConfiguration) {
        super(streamConfiguration);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationConfiguration currentConfiguration,
            @NotNull final InvocationMode invocationMode) {
        return new DefaultStreamConfiguration(streamConfiguration, currentConfiguration,
                invocationMode);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final InvocationFactory<? super BEFORE, ? extends AFTER> factory) {
        return newRoutine(streamConfiguration, JRoutineCore.with(factory));
    }

    @NotNull
    @Override
    protected StreamConfiguration resetConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode) {
        return new DefaultStreamConfiguration(streamConfiguration,
                InvocationConfiguration.defaultConfiguration(), invocationMode);
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
                        asInvocationConfiguration().outputConfigurationBuilder().configured();
            }

            return mChannelConfiguration;
        }

        @NotNull
        public InvocationConfiguration asInvocationConfiguration() {
            if (mInvocationConfiguration == null) {
                mInvocationConfiguration =
                        mStreamConfiguration.builderFrom().with(mConfiguration).configured();
            }

            return mInvocationConfiguration;
        }

        @NotNull
        public InvocationConfiguration getCurrentInvocationConfiguration() {
            return mConfiguration;
        }

        @NotNull
        public InvocationMode getInvocationMode() {
            return mInvocationMode;
        }

        @NotNull
        public InvocationConfiguration getStreamInvocationConfiguration() {
            return mStreamConfiguration;
        }
    }
}
