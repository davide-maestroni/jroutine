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

import com.github.dm.jrt.android.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.invocation.MissingInvocationException;
import com.github.dm.jrt.android.runner.AndroidRunners;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelConfiguration;
import com.github.dm.jrt.core.builder.InvocationConfiguration;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.TemplateExecution;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of a loader channel builder.
 * <p/>
 * Created by davide-maestroni on 01/14/2015.
 */
class DefaultLoaderChannelBuilder
        implements LoaderChannelBuilder, LoaderConfiguration.Configurable<LoaderChannelBuilder>,
        ChannelConfiguration.Configurable<LoaderChannelBuilder> {

    private final LoaderContextCompat mContext;

    private ChannelConfiguration mChannelConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context the context instance.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderChannelBuilder(@NotNull final LoaderContextCompat context) {

        if (context == null) {
            throw new NullPointerException("the channel context must not be null");
        }

        mContext = context;
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
            ioChannel.abort(new MissingInvocationException(loaderId));
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

        return builder.withInvocations()
                      .with(invocationConfiguration)
                      .getConfigured()
                      .withLoaders()
                      .with(loaderConfiguration)
                      .withClashResolution(ClashResolutionType.JOIN)
                      .withInputClashResolution(ClashResolutionType.JOIN)
                      .withResultStaleTime(TimeDuration.INFINITY)
                      .getConfigured()
                      .asyncCall();
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

            AndroidRunners.mainRunner()
                          .run(new PurgeInputsExecution(context, mLoaderConfiguration.getLoaderIdOr(
                                  LoaderConfiguration.AUTO), inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge() {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            AndroidRunners.mainRunner()
                          .run(new PurgeExecution(context, mLoaderConfiguration.getLoaderIdOr(
                                  LoaderConfiguration.AUTO)), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object input) {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            final List<Object> inputList = Collections.singletonList(input);
            AndroidRunners.mainRunner()
                          .run(new PurgeInputsExecution(context, mLoaderConfiguration.getLoaderIdOr(
                                  LoaderConfiguration.AUTO), inputList), 0, TimeUnit.MILLISECONDS);
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

            AndroidRunners.mainRunner()
                          .run(new PurgeInputsExecution(context, mLoaderConfiguration.getLoaderIdOr(
                                  LoaderConfiguration.AUTO), inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderChannelBuilder> withLoaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderChannelBuilder setConfiguration(@NotNull final LoaderConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderChannelBuilder setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the channel configuration must not be null");
        }

        mChannelConfiguration = configuration;
        return this;
    }

    @NotNull
    public ChannelConfiguration.Builder<? extends LoaderChannelBuilder> withChannels() {

        final ChannelConfiguration config = mChannelConfiguration;
        return new ChannelConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    /**
     * Execution implementation purging the loader with a specific ID.
     */
    private static class PurgeExecution extends TemplateExecution {

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

            LoaderInvocation.purgeLoader(mContext, mLoaderId);
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID and inputs.
     */
    private static class PurgeInputsExecution extends TemplateExecution {

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

            LoaderInvocation.purgeLoader(mContext, mLoaderId, mInputs);
        }
    }
}
