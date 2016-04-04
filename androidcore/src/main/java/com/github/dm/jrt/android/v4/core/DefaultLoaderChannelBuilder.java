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

import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.runner.AndroidRunners.mainRunner;
import static com.github.dm.jrt.android.v4.core.LoaderInvocation.purgeLoader;

/**
 * Default implementation of a loader channel builder.
 * <p>
 * Created by davide-maestroni on 01/14/2015.
 */
class DefaultLoaderChannelBuilder
        implements LoaderChannelBuilder, LoaderConfiguration.Configurable<LoaderChannelBuilder>,
        ChannelConfiguration.Configurable<LoaderChannelBuilder> {

    private final LoaderContextCompat mContext;

    private ChannelConfiguration mChannelConfiguration =
            ChannelConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the context instance.
     */
    DefaultLoaderChannelBuilder(@NotNull final LoaderContextCompat context) {

        mContext = ConstantConditions.notNull("loader context", context);
    }

    @NotNull
    public <OUT> OutputChannel<OUT> buildChannel() {

        final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
        final int loaderId = loaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO);
        if (loaderId == LoaderConfiguration.AUTO) {
            throw new IllegalArgumentException("the loader ID must not be generated");
        }

        final LoaderContextCompat context = mContext;
        final Object component = context.getComponent();
        if (component == null) {
            final IOChannel<OUT> ioChannel = JRoutineCore.io().buildChannel();
            ioChannel.abort(new MissingLoaderException(loaderId));
            return ioChannel.close();
        }

        final MissingLoaderInvocationFactory<OUT> factory =
                new MissingLoaderInvocationFactory<OUT>(loaderId);
        final DefaultLoaderRoutineBuilder<Void, OUT> builder =
                new DefaultLoaderRoutineBuilder<Void, OUT>(context, factory);
        final InvocationConfiguration invocationConfiguration =
                mChannelConfiguration.toOutputChannelConfiguration();
        final Logger logger = invocationConfiguration.newLogger(this);
        final ClashResolutionType resolutionType =
                loaderConfiguration.getClashResolutionTypeOr(null);
        if (resolutionType != null) {
            logger.wrn("the specified clash resolution type will be ignored: %s", resolutionType);
        }

        final ClashResolutionType inputResolutionType =
                loaderConfiguration.getInputClashResolutionTypeOr(null);
        if (inputResolutionType != null) {
            logger.wrn("the specified input clash resolution type will be ignored: %s",
                    inputResolutionType);
        }

        final TimeDuration resultStaleTime = loaderConfiguration.getResultStaleTimeOr(null);
        if (resultStaleTime != null) {
            logger.wrn("the specified results stale time will be ignored: %s", resultStaleTime);
        }

        return builder.invocationConfiguration()
                      .with(invocationConfiguration)
                      .setConfiguration()
                      .loaderConfiguration()
                      .with(loaderConfiguration)
                      .withClashResolution(ClashResolutionType.JOIN)
                      .withInputClashResolution(ClashResolutionType.JOIN)
                      .withResultStaleTime(TimeDuration.INFINITY)
                      .setConfiguration()
                      .asyncCall();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderChannelBuilder> loaderConfiguration() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    public void purge() {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            mainRunner().run(new PurgeExecution(context,
                            mLoaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO)), 0,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object input) {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            final List<Object> inputList = Collections.singletonList(input);
            mainRunner().run(new PurgeInputsExecution(context,
                    mLoaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO), inputList), 0,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object... inputs) {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            final List<Object> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = new ArrayList<Object>(inputs.length);
                Collections.addAll(inputList, inputs);
            }

            mainRunner().run(new PurgeInputsExecution(context,
                    mLoaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO), inputList), 0,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Iterable<?> inputs) {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            final List<Object> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = new ArrayList<Object>();
                for (final Object input : inputs) {
                    inputList.add(input);
                }
            }

            mainRunner().run(new PurgeInputsExecution(context,
                    mLoaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO), inputList), 0,
                    TimeUnit.MILLISECONDS);
        }
    }

    @NotNull
    public ChannelConfiguration.Builder<? extends LoaderChannelBuilder> channelConfiguration() {

        final ChannelConfiguration config = mChannelConfiguration;
        return new ChannelConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    @NotNull
    public LoaderChannelBuilder setConfiguration(@NotNull final LoaderConfiguration configuration) {

        mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }

    @NotNull
    public LoaderChannelBuilder setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        mChannelConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        return this;
    }

    /**
     * Execution implementation purging the loader with a specific ID.
     */
    private static class PurgeExecution implements Execution {

        private final LoaderContextCompat mContext;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@NotNull final LoaderContextCompat context, final int loaderId) {

            mContext = context;
            mLoaderId = loaderId;
        }

        public void run() {

            purgeLoader(mContext, mLoaderId);
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID and inputs.
     */
    private static class PurgeInputsExecution implements Execution {

        private final LoaderContextCompat mContext;

        private final List<Object> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@NotNull final LoaderContextCompat context, final int loaderId,
                @NotNull final List<Object> inputs) {

            mContext = context;
            mLoaderId = loaderId;
            mInputs = inputs;
        }

        public void run() {

            purgeLoader(mContext, mLoaderId, mInputs);
        }
    }
}
