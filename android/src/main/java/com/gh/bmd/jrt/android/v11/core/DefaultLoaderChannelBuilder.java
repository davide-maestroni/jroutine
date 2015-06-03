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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.builder.LoaderChannelBuilder;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.builder.LoaderRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of an loader channel builder.
 * <p/>
 * Created by davide-maestroni on 1/14/15.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
class DefaultLoaderChannelBuilder
        implements LoaderChannelBuilder, LoaderConfiguration.Configurable<LoaderChannelBuilder>,
        InvocationConfiguration.Configurable<LoaderChannelBuilder> {

    private final WeakReference<Object> mContext;

    private final int mLoaderId;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity the context activity.
     * @param loaderId the loader ID.
     */
    DefaultLoaderChannelBuilder(@Nonnull final Activity activity, final int loaderId) {

        this((Object) activity, loaderId);
    }

    /**
     * Constructor.
     *
     * @param fragment the context fragment.
     * @param loaderId the loader ID.
     */
    DefaultLoaderChannelBuilder(@Nonnull final Fragment fragment, final int loaderId) {

        this((Object) fragment, loaderId);
    }

    /**
     * Constructor.
     *
     * @param context  the context instance.
     * @param loaderId the loader ID.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultLoaderChannelBuilder(@Nonnull final Object context, final int loaderId) {

        if (context == null) {

            throw new NullPointerException("the channel context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mLoaderId = loaderId;
    }

    @Nonnull
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel() {

        final Object context = mContext.get();
        final ContextInvocationFactory<OUTPUT, OUTPUT> factory =
                MissingLoaderInvocation.factoryOf(mLoaderId);

        if (context == null) {

            return JRoutine.on(factory).callSync();
        }

        final LoaderRoutineBuilder<OUTPUT, OUTPUT> builder;

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            builder = JRoutine.onActivity(activity, factory);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            builder = JRoutine.onFragment(fragment, factory);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
        final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
        final ClashResolutionType resolutionType =
                loaderConfiguration.getClashResolutionTypeOr(null);

        if (resolutionType != null) {

            final Logger logger = invocationConfiguration.newLogger(this);
            logger.wrn("the specified clash resolution type will be ignored: %s", resolutionType);
        }

        return builder.withInvocation()
                      .with(invocationConfiguration)
                      .set()
                      .withLoader()
                      .withId(mLoaderId)
                      .with(loaderConfiguration)
                      .withClashResolution(ClashResolutionType.MERGE)
                      .withInputClashResolution(ClashResolutionType.MERGE)
                      .set()
                      .callAsync();
    }

    public void purge(@Nullable final Object input) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<Object> inputList = Collections.singletonList(input);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mLoaderId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object... inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<Object> inputList =
                    (inputs == null) ? Collections.emptyList() : Arrays.asList(inputs);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mLoaderId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Iterable<?> inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

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
                   .run(new PurgeInputsExecution(context, mLoaderId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    public void purge() {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            Runners.mainRunner()
                   .run(new PurgeExecution(context, mLoaderId), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends LoaderChannelBuilder> withInvocation() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderChannelBuilder> withLoader() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderChannelBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderChannelBuilder setConfiguration(@Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderChannelBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    /**
     * Execution implementation purging the loader with a specific ID.
     */
    private static class PurgeExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context reference.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@Nonnull final WeakReference<Object> context, final int loaderId) {

            mContext = context;
            mLoaderId = loaderId;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mLoaderId);
            }
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID and inputs.
     */
    private static class PurgeInputsExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final List<Object> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context reference.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final WeakReference<Object> context,
                final int loaderId, @Nonnull final List<Object> inputs) {

            mContext = context;
            mLoaderId = loaderId;
            mInputs = inputs;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mLoaderId, mInputs);
            }
        }
    }
}
