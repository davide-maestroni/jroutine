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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.android.v4.core.LoaderInvocation.clearLoader;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFromOutput;

/**
 * Default implementation of a loader channel builder.
 * <p>
 * Created by davide-maestroni on 01/14/2015.
 */
class DefaultLoaderChannelBuilder
        implements LoaderChannelBuilder, LoaderConfiguration.Configurable<LoaderChannelBuilder> {

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
    @Override
    public LoaderChannelBuilder apply(@NotNull final LoaderConfiguration configuration) {
        mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }

    @NotNull
    @Override
    public LoaderChannelBuilder apply(@NotNull final ChannelConfiguration configuration) {
        mChannelConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        return this;
    }

    @NotNull
    @Override
    public LoaderChannelBuilder patch(@NotNull final ChannelConfiguration configuration) {
        ConstantConditions.notNull("channel configuration", configuration);
        mChannelConfiguration =
                mChannelConfiguration.builderFrom().with(configuration).buildConfiguration();
        return this;
    }

    @NotNull
    @Override
    public <OUT> Channel<?, OUT> buildChannel() {
        final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
        final int loaderId = loaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO);
        if (loaderId == LoaderConfiguration.AUTO) {
            throw new IllegalArgumentException("the loader ID must not be generated");
        }

        final LoaderContextCompat context = mContext;
        final Object component = context.getComponent();
        if (component == null) {
            final Channel<OUT, OUT> channel = JRoutineCore.io().buildChannel();
            channel.abort(new MissingLoaderException(loaderId));
            return channel.close();
        }

        final MissingLoaderInvocationFactory<OUT> factory =
                new MissingLoaderInvocationFactory<OUT>(loaderId);
        final DefaultLoaderRoutineBuilder<Void, OUT> builder =
                new DefaultLoaderRoutineBuilder<Void, OUT>(context, factory);
        final InvocationConfiguration invocationConfiguration =
                builderFromOutput(mChannelConfiguration).configured();
        return builder.invocationConfiguration()
                      .with(invocationConfiguration)
                      .configured()
                      .loaderConfiguration()
                      .withClashResolution(ClashResolutionType.JOIN)
                      .withMatchResolution(ClashResolutionType.JOIN)
                      .with(loaderConfiguration)
                      .configured()
                      .asyncCall()
                      .close();
    }

    @Override
    public void clear() {
        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            clearLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO));
        }
    }

    @Override
    public void clear(@Nullable final Object input) {
        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            clearLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
                    Collections.singletonList(input));
        }
    }

    @Override
    public void clear(@Nullable final Object... inputs) {
        final LoaderContextCompat context = mContext;
        if (context.getComponent() != null) {
            final List<Object> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = Arrays.asList(inputs);
            }

            clearLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
                    inputList);
        }
    }

    @Override
    public void clear(@Nullable final Iterable<?> inputs) {
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

            clearLoader(context, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
                    inputList);
        }
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderChannelBuilder> loaderConfiguration() {
        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }
}
