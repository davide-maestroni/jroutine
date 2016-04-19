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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public LoaderChannelBuilder applyConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }

    @NotNull
    public LoaderChannelBuilder applyConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        mChannelConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        return this;
    }

    @NotNull
    public <OUT> OutputChannel<OUT> buildChannel() {

        final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
        final int loaderId = loaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO);
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
                loaderConfiguration.getClashResolutionTypeOrElse(null);
        if (resolutionType != null) {
            logger.wrn("the specified clash resolution type will be ignored: %s", resolutionType);
        }

        final ClashResolutionType inputResolutionType =
                loaderConfiguration.getInputClashResolutionTypeOrElse(null);
        if (inputResolutionType != null) {
            logger.wrn("the specified input clash resolution type will be ignored: %s",
                    inputResolutionType);
        }

        final TimeDuration resultStaleTime = loaderConfiguration.getResultStaleTimeOrElse(null);
        if (resultStaleTime != null) {
            logger.wrn("the specified results stale time will be ignored: %s", resultStaleTime);
        }

        return builder.invocationConfiguration()
                      .with(invocationConfiguration)
                      .apply()
                      .loaderConfiguration()
                      .with(loaderConfiguration)
                      .withClashResolution(ClashResolutionType.JOIN)
                      .withInputClashResolution(ClashResolutionType.JOIN)
                      .withResultStaleTime(TimeDuration.INFINITY)
                      .apply()
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
            purgeLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO));
        }
    }

    public void purge(@Nullable final Object input) {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            purgeLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
                    Collections.singletonList(input));
        }
    }

    public void purge(@Nullable final Object... inputs) {

        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            final List<Object> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = Arrays.asList(inputs);
            }

            purgeLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
                    inputList);
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

            purgeLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
                    inputList);
        }
    }

    @NotNull
    public ChannelConfiguration.Builder<? extends LoaderChannelBuilder> channelConfiguration() {

        final ChannelConfiguration config = mChannelConfiguration;
        return new ChannelConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }
}