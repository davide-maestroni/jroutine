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
package com.gh.bmd.jrt.android.v4.core;

import com.gh.bmd.jrt.android.builder.LoaderChannelBuilder;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.builder.LoaderRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.MissingInvocationException;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.ChannelConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.TemplateExecution;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a loader channel builder.
 * <p/>
 * Created by davide-maestroni on 1/14/15.
 */
class DefaultLoaderChannelBuilder
        implements LoaderChannelBuilder, LoaderConfiguration.Configurable<LoaderChannelBuilder>,
        ChannelConfiguration.Configurable<LoaderChannelBuilder> {

    private final LoaderContext mContext;

    private ChannelConfiguration mChannelConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context the context instance.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderChannelBuilder(@Nonnull final LoaderContext context) {

        if (context == null) {

            throw new NullPointerException("the channel context must not be null");
        }

        mContext = context;
    }

    @Nonnull
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel() {

        final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
        final int loaderId = loaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO);

        if (loaderId == LoaderConfiguration.AUTO) {

            throw new IllegalArgumentException("the loader ID must not be generated");
        }

        final LoaderContext context = mContext;
        final Object component = context.getComponent();

        if (component == null) {

            final TransportChannel<OUTPUT> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.abort(new MissingInvocationException(loaderId));
            return transportChannel.close();
        }

        final LoaderRoutineBuilder<Void, OUTPUT> builder =
                JRoutine.on(context, new MissingLoaderInvocation<OUTPUT>(loaderId));
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

        return builder.invocations()
                      .with(invocationConfiguration)
                      .set()
                      .loaders()
                      .with(loaderConfiguration)
                      .withClashResolution(ClashResolutionType.JOIN)
                      .withInputClashResolution(ClashResolutionType.JOIN)
                      .withResultStaleTime(TimeDuration.INFINITY)
                      .set()
                      .asyncCall();
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderChannelBuilder> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    public void purge(@Nullable final Iterable<?> inputs) {

        final LoaderContext context = mContext;

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

            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mLoaderConfiguration.getLoaderIdOr(
                           LoaderConfiguration.AUTO), inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge() {

        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            Runners.mainRunner()
                   .run(new PurgeExecution(context, mLoaderConfiguration.getLoaderIdOr(
                           LoaderConfiguration.AUTO)), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object input) {

        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            final List<Object> inputList = Collections.singletonList(input);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mLoaderConfiguration.getLoaderIdOr(
                           LoaderConfiguration.AUTO), inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object... inputs) {

        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            final List<Object> inputList;

            if (inputs == null) {

                inputList = Collections.emptyList();

            } else {

                inputList = new ArrayList<Object>(inputs.length);
                Collections.addAll(inputList, inputs);
            }

            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mLoaderConfiguration.getLoaderIdOr(
                           LoaderConfiguration.AUTO), inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    public ChannelConfiguration.Builder<? extends LoaderChannelBuilder> channels() {

        final ChannelConfiguration config = mChannelConfiguration;
        return new ChannelConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderChannelBuilder setConfiguration(@Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderChannelBuilder setConfiguration(
            @Nonnull final ChannelConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the channel configuration must not be null");
        }

        mChannelConfiguration = configuration;
        return this;
    }

    /**
     * Execution implementation purging the loader with a specific ID.
     */
    private static class PurgeExecution extends TemplateExecution {

        private final LoaderContext mContext;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@Nonnull final LoaderContext context, final int loaderId) {

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

        private final LoaderContext mContext;

        private final List<Object> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final LoaderContext context, final int loaderId,
                @Nonnull final List<Object> inputs) {

            mContext = context;
            mLoaderId = loaderId;
            mInputs = inputs;
        }

        public void run() {

            LoaderInvocation.purgeLoader(mContext, mLoaderId, mInputs);
        }
    }
}
