/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.v4.routine;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.builder.AndroidChannelBuilder;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.CacheStrategy;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
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

/**
 * Default implementation of an Android channel builder.
 * <p/>
 * Created by davide on 1/14/15.
 */
class DefaultAndroidChannelBuilder implements AndroidChannelBuilder {

    private final WeakReference<Object> mContext;

    private final int mId;

    private CacheStrategy mCacheStrategy;

    private RoutineConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param activity the context activity.
     * @param id       the invocation ID.
     * @throws java.lang.NullPointerException if the activity is null.
     */
    DefaultAndroidChannelBuilder(@Nonnull final FragmentActivity activity, final int id) {

        this((Object) activity, id);
    }

    /**
     * Constructor.
     *
     * @param fragment the context fragment.
     * @param id       the invocation ID.
     * @throws java.lang.NullPointerException if the fragment is null.
     */
    DefaultAndroidChannelBuilder(@Nonnull final Fragment fragment, final int id) {

        this((Object) fragment, id);
    }

    /**
     * Constructor.
     *
     * @param context the context instance.
     * @param id      the invocation ID.
     * @throws java.lang.NullPointerException if the context is null.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultAndroidChannelBuilder(@Nonnull final Object context, final int id) {

        if (context == null) {

            throw new NullPointerException("the channel context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mId = id;
    }

    @Nonnull
    @Override
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel() {

        final Object context = mContext.get();

        if (context == null) {

            return JRoutine.on(MissingLoaderInvocation.<OUTPUT, OUTPUT>factoryOf())
                           .callSync();
        }

        final AndroidRoutineBuilder<OUTPUT, OUTPUT> builder;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            builder = JRoutine.onActivity(activity, new MissingToken<OUTPUT>()).withId(mId);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            builder = JRoutine.onFragment(fragment, new MissingToken<OUTPUT>()).withId(mId);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        return builder.withConfiguration(mConfiguration)
                      .onClash(ClashResolution.KEEP_THAT)
                      .onComplete(mCacheStrategy)
                      .callAsync();
    }

    @Nonnull
    @Override
    public AndroidChannelBuilder onComplete(@Nullable final CacheStrategy cacheStrategy) {

        mCacheStrategy = cacheStrategy;
        return this;
    }

    @Override
    public void purge() {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            Runners.mainRunner().run(new PurgeExecution(context, mId), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void purge(@Nullable final Object input) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mId, Collections.singletonList(input)), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void purge(@Nullable final Object... inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<Object> inputList =
                    (inputs == null) ? Collections.emptyList() : Arrays.asList(inputs);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution(context, mId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void purge(@Nullable final Iterable<Object> inputs) {

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
                   .run(new PurgeInputsExecution(context, mId, inputList), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    @Override
    public AndroidChannelBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
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

        private final int mId;

        /**
         * Constructor.
         *
         * @param context the context reference.
         * @param id      the invocation ID.
         */
        private PurgeExecution(@Nonnull final WeakReference<Object> context, final int id) {

            mContext = context;
            mId = id;
        }

        @Override
        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mId);
            }
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID and inputs.
     */
    private static class PurgeInputsExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final int mId;

        private final List<Object> mInputs;

        /**
         * Constructor.
         *
         * @param context the context reference.
         * @param id      the invocation ID.
         * @param inputs  the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final WeakReference<Object> context, final int id,
                @Nonnull final List<Object> inputs) {

            mContext = context;
            mId = id;
            mInputs = inputs;
        }

        @Override
        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mId, mInputs);
            }
        }
    }
}
