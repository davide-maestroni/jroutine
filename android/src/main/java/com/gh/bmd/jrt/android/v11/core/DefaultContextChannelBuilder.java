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

import com.gh.bmd.jrt.android.builder.ContextChannelBuilder;
import com.gh.bmd.jrt.android.builder.ContextRoutineBuilder;
import com.gh.bmd.jrt.android.builder.InvocationConfiguration;
import com.gh.bmd.jrt.android.builder.InvocationConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
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
 * Default implementation of an Android channel builder.
 * <p/>
 * Created by davide on 1/14/15.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
class DefaultContextChannelBuilder implements ContextChannelBuilder,
        InvocationConfiguration.Configurable<ContextChannelBuilder>,
        RoutineConfiguration.Configurable<ContextChannelBuilder> {

    private final WeakReference<Object> mContext;

    private final int mInvocationId;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity     the context activity.
     * @param invocationId the invocation ID.
     */
    DefaultContextChannelBuilder(@Nonnull final Activity activity, final int invocationId) {

        this((Object) activity, invocationId);
    }

    /**
     * Constructor.
     *
     * @param fragment     the context fragment.
     * @param invocationId the invocation ID.
     */
    DefaultContextChannelBuilder(@Nonnull final Fragment fragment, final int invocationId) {

        this((Object) fragment, invocationId);
    }

    /**
     * Constructor.
     *
     * @param context      the context instance.
     * @param invocationId the invocation ID.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultContextChannelBuilder(@Nonnull final Object context, final int invocationId) {

        if (context == null) {

            throw new NullPointerException("the channel context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mInvocationId = invocationId;
    }

    @Nonnull
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel() {

        final Object context = mContext.get();

        if (context == null) {

            return JRoutine.on(MissingLoaderInvocation.<OUTPUT, OUTPUT>factoryOf()).callSync();
        }

        final ContextRoutineBuilder<OUTPUT, OUTPUT> builder;

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            builder = JRoutine.onActivity(activity,
                                          MissingLoaderInvocation.<OUTPUT, OUTPUT>factoryOf());

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            builder = JRoutine.onFragment(fragment,
                                          MissingLoaderInvocation.<OUTPUT, OUTPUT>factoryOf());

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        final RoutineConfiguration routineConfiguration = mRoutineConfiguration;
        final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
        final ClashResolutionType resolutionType =
                invocationConfiguration.getClashResolutionTypeOr(null);

        if (resolutionType != null) {

            final Logger logger = routineConfiguration.newLogger(this);
            logger.wrn("the specified clash resolution type will be ignored: %s", resolutionType);
        }

        return builder.withRoutine()
                      .with(routineConfiguration)
                      .set()
                      .withInvocation()
                      .withId(mInvocationId)
                      .with(invocationConfiguration)
                      .withClashResolution(ClashResolutionType.KEEP_THAT)
                      .set()
                      .callAsync();
    }

    public void purge(@Nullable final Object input) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<Object> inputList = Collections.singletonList(input);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mInvocationId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Object... inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<Object> inputList =
                    (inputs == null) ? Collections.emptyList() : Arrays.asList(inputs);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mInvocationId, inputList), 0,
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
                   .run(new PurgeInputsExecution(context, mInvocationId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    public void purge() {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            Runners.mainRunner()
                   .run(new PurgeExecution(context, mInvocationId), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ContextChannelBuilder> withInvocation() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ContextChannelBuilder>(this, config);
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ContextChannelBuilder> withRoutine() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<ContextChannelBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextChannelBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextChannelBuilder setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    /**
     * Execution implementation purging the loader with a specific ID.
     */
    private static class PurgeExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final int mInvocationId;

        /**
         * Constructor.
         *
         * @param context      the context reference.
         * @param invocationId the invocation ID.
         */
        private PurgeExecution(@Nonnull final WeakReference<Object> context,
                final int invocationId) {

            mContext = context;
            mInvocationId = invocationId;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mInvocationId);
            }
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID and inputs.
     */
    private static class PurgeInputsExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final List<Object> mInputs;

        private final int mInvocationId;

        /**
         * Constructor.
         *
         * @param context      the context reference.
         * @param invocationId the invocation ID.
         * @param inputs       the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final WeakReference<Object> context,
                final int invocationId, @Nonnull final List<Object> inputs) {

            mContext = context;
            mInvocationId = invocationId;
            mInputs = inputs;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mInvocationId, mInputs);
            }
        }
    }
}
