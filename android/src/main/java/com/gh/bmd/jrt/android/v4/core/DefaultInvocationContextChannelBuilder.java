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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.builder.InvocationContextChannelBuilder;
import com.gh.bmd.jrt.android.builder.InvocationContextRoutineBuilder;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.runner.Execution;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.withId;

/**
 * Default implementation of an Android channel builder.
 * <p/>
 * Created by davide on 1/14/15.
 */
class DefaultInvocationContextChannelBuilder implements InvocationContextChannelBuilder {

    private final WeakReference<Object> mContext;

    private final int mInvocationId;

    private CacheStrategyType mCacheStrategyType;

    private RoutineConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param activity     the context activity.
     * @param invocationId the invocation ID.
     * @throws java.lang.NullPointerException if the activity is null.
     */
    DefaultInvocationContextChannelBuilder(@Nonnull final FragmentActivity activity,
            final int invocationId) {

        this((Object) activity, invocationId);
    }

    /**
     * Constructor.
     *
     * @param fragment     the context fragment.
     * @param invocationId the invocation ID.
     * @throws java.lang.NullPointerException if the fragment is null.
     */
    DefaultInvocationContextChannelBuilder(@Nonnull final Fragment fragment,
            final int invocationId) {

        this((Object) fragment, invocationId);
    }

    /**
     * Constructor.
     *
     * @param context      the context instance.
     * @param invocationId the invocation ID.
     * @throws java.lang.NullPointerException if the context is null.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultInvocationContextChannelBuilder(@Nonnull final Object context,
            final int invocationId) {

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

        final InvocationContextRoutineBuilder<OUTPUT, OUTPUT> builder;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            builder = JRoutine.onActivity(activity, new MissingToken<OUTPUT>());

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            builder = JRoutine.onFragment(fragment, new MissingToken<OUTPUT>());

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        return builder.withConfig(mConfiguration)
                      .withInvocations(withId(mInvocationId).onClash(ClashResolutionType.KEEP_THAT)
                                                            .onComplete(mCacheStrategyType))
                      .callAsync();
    }

    @Nonnull
    public InvocationContextChannelBuilder onComplete(
            @Nullable final CacheStrategyType strategyType) {

        mCacheStrategyType = strategyType;
        return this;
    }

    public void purge() {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            Runners.mainRunner()
                   .run(new PurgeExecution(context, mInvocationId), 0, TimeUnit.MILLISECONDS);
        }
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

    @Nonnull
    public InvocationContextChannelBuilder withConfig(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public InvocationContextChannelBuilder withConfig(@Nonnull final Builder builder) {

        return withConfig(builder.buildConfiguration());
    }

    /**
     * Missing loader invocation token.
     *
     * @param <DATA> the data type.
     */
    private static class MissingToken<DATA>
            extends ClassToken<MissingLoaderInvocation<DATA, DATA>> {

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
