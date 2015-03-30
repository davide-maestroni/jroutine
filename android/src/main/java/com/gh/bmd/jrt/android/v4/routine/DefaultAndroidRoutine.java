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
package com.gh.bmd.jrt.android.v4.routine;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.CacheStrategy;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.gh.bmd.jrt.android.invocation.AndroidInvocation;
import com.gh.bmd.jrt.android.routine.AndroidRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.AbstractRoutine;
import com.gh.bmd.jrt.runner.Execution;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Routine implementation delegating to Android loaders the asynchronous processing.
 * <p/>
 * Created by davide on 1/10/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultAndroidRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT>
        implements AndroidRoutine<INPUT, OUTPUT> {

    private final CacheStrategy mCacheStrategy;

    private final ClashResolution mClashResolution;

    private final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final int mInvocationId;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     * @param context       the context reference.
     * @param invocationId  the invocation ID.
     * @param resolution    the clash resolution type.
     * @param cacheStrategy the result cache type.
     * @param constructor   the invocation constructor.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     * @throws java.lang.NullPointerException     if any of the specified non-null parameter is
     *                                            null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultAndroidRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final WeakReference<Object> context, final int invocationId,
            @Nullable final ClashResolution resolution, @Nullable final CacheStrategy cacheStrategy,
            @Nonnull final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor) {

        super(configuration);

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (constructor == null) {

            throw new NullPointerException("the invocation constructor must not be null");
        }

        mContext = context;
        mInvocationId = invocationId;
        mClashResolution = (resolution == null) ? ClashResolution.ABORT_THAT_INPUT : resolution;
        mCacheStrategy = (cacheStrategy == null) ? CacheStrategy.CLEAR : cacheStrategy;
        mConstructor = constructor;
        mOrderType = configuration.getOutputOrderOr(null);
    }

    @Override
    public void purge() {

        super.purge();

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final Class<? extends AndroidInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            Runners.mainRunner()
                   .run(new PurgeExecution(context, mInvocationId, invocationClass), 0,
                        TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> convertInvocation(final boolean async,
            @Nonnull final Invocation<INPUT, OUTPUT> invocation) {

        try {

            invocation.onDestroy();

        } catch (final InvocationInterruptedException e) {

            throw e;

        } catch (final Throwable t) {

            getLogger().wrn(t, "ignoring exception while destroying invocation instance");
        }

        return newInvocation(async);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> newInvocation(final boolean async) {

        final Logger logger = getLogger();

        if (async) {

            return new LoaderInvocation<INPUT, OUTPUT>(mContext, mInvocationId, mClashResolution,
                                                       mCacheStrategy, mConstructor, mOrderType,
                                                       logger);
        }

        final Object context = mContext.get();

        if (context == null) {

            throw new IllegalStateException("the routine context has been destroyed");
        }

        final Context appContext;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            appContext = activity.getApplicationContext();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            appContext = fragment.getActivity().getApplicationContext();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        try {

            final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor =
                    mConstructor;
            logger.dbg("creating a new instance of class: %s", constructor.getDeclaringClass());
            final AndroidInvocation<INPUT, OUTPUT> invocation = constructor.newInstance();
            invocation.onContext(appContext);
            return invocation;

        } catch (final InvocationTargetException e) {

            logger.err(e, "error creating the invocation instance");
            throw new InvocationException(e.getCause());

        } catch (final RoutineException e) {

            logger.err(e, "error creating the invocation instance");
            throw e;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance");
            throw new InvocationException(t);
        }
    }

    public void purge(@Nullable final INPUT input) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final Class<? extends AndroidInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            final List<INPUT> inputList = Collections.singletonList(input);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution<INPUT>(context, mInvocationId, invocationClass,
                                                        inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final INPUT... inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final Class<? extends AndroidInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            final List<INPUT> inputList =
                    (inputs == null) ? Collections.<INPUT>emptyList() : Arrays.asList(inputs);
            Runners.mainRunner()
                   .run(new PurgeInputsExecution<INPUT>(context, mInvocationId, invocationClass,
                                                        inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Iterable<? extends INPUT> inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<INPUT> inputList;

            if (inputs == null) {

                inputList = Collections.emptyList();

            } else {

                inputList = new ArrayList<INPUT>();

                for (final INPUT input : inputs) {

                    inputList.add(input);
                }
            }

            final Class<? extends AndroidInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            Runners.mainRunner()
                   .run(new PurgeInputsExecution<INPUT>(context, mInvocationId, invocationClass,
                                                        inputList), 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation class.
     */
    private static class PurgeExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final Class<?> mInvocationClass;

        private final int mInvocationId;

        /**
         * Constructor.
         *
         * @param context         the context reference.
         * @param invocationId    the invocation ID.
         * @param invocationClass the invocation class.
         */
        private PurgeExecution(@Nonnull final WeakReference<Object> context, final int invocationId,
                @Nonnull final Class<?> invocationClass) {

            mContext = context;
            mInvocationId = invocationId;
            mInvocationClass = invocationClass;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoaders(context, mInvocationId, mInvocationClass);
            }
        }
    }

    /**
     * Execution implementation purging the loader with a specific invocation class and inputs.
     *
     * @param <INPUT> the input data type.
     */
    private static class PurgeInputsExecution<INPUT> implements Execution {

        private final WeakReference<Object> mContext;

        private final List<INPUT> mInputs;

        private final Class<?> mInvocationClass;

        private final int mInvocationId;

        /**
         * Constructor.
         *
         * @param context         the context reference.
         * @param invocationId    the invocation ID.
         * @param invocationClass the invocation class.
         * @param inputs          the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final WeakReference<Object> context,
                final int invocationId, @Nonnull final Class<?> invocationClass,
                @Nonnull final List<INPUT> inputs) {

            mContext = context;
            mInvocationId = invocationId;
            mInvocationClass = invocationClass;
            mInputs = inputs;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mInvocationId, mInvocationClass, mInputs);
            }
        }
    }
}
