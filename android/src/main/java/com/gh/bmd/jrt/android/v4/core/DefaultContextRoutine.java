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

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.builder.ContextRoutineBuilder.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.ContextRoutineBuilder.ClashResolutionType;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.routine.ContextRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.core.AbstractRoutine;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
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
class DefaultContextRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT>
        implements ContextRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final CacheStrategyType mCacheStrategyType;

    private final ClashResolutionType mClashResolutionType;

    private final Constructor<? extends ContextInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final int mInvocationId;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param configuration     the routine configuration.
     * @param context           the context reference.
     * @param invocationId      the invocation ID.
     * @param resolutionType    the clash resolution type.
     * @param cacheStrategyType the result cache strategy type.
     * @param constructor       the invocation constructor.
     * @param args              the invocation constructor arguments.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     * @throws java.lang.NullPointerException     if any of the specified non-null parameter is
     *                                            null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultContextRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final WeakReference<Object> context, final int invocationId,
            @Nullable final ClashResolutionType resolutionType,
            @Nullable final CacheStrategyType cacheStrategyType,
            @Nonnull final Constructor<? extends ContextInvocation<INPUT, OUTPUT>> constructor,
            @Nonnull final Object[] args) {

        super(configuration);

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (constructor == null) {

            throw new NullPointerException("the invocation constructor must not be null");
        }

        if (args == null) {

            throw new NullPointerException(
                    "the invocation constructor array of arguments must not be null");
        }

        mContext = context;
        mInvocationId = invocationId;
        mClashResolutionType =
                (resolutionType == null) ? ClashResolutionType.ABORT_THAT_INPUT : resolutionType;
        mCacheStrategyType =
                (cacheStrategyType == null) ? CacheStrategyType.CLEAR : cacheStrategyType;
        mConstructor = constructor;
        mArgs = args;
        mOrderType = configuration.getOutputOrderOr(null);
    }

    @Override
    public void purge() {

        super.purge();
        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            Runners.mainRunner()
                   .run(new PurgeExecution(context, mInvocationId, invocationClass, mArgs), 0,
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

        } catch (final Throwable ignored) {

            getLogger().wrn(ignored, "ignoring exception while destroying invocation instance");
        }

        return newInvocation(async);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> newInvocation(final boolean async) {

        final Logger logger = getLogger();

        if (async) {

            return new LoaderInvocation<INPUT, OUTPUT>(mContext, mInvocationId,
                                                       mClashResolutionType, mCacheStrategyType,
                                                       mConstructor, mArgs, mOrderType, logger);
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

            final Constructor<? extends ContextInvocation<INPUT, OUTPUT>> constructor =
                    mConstructor;
            logger.dbg("creating a new instance of class: %s", constructor.getDeclaringClass());
            final ContextInvocation<INPUT, OUTPUT> invocation = constructor.newInstance(mArgs);
            invocation.onContext(appContext);
            return invocation;

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

            final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            final List<INPUT> inputList = Collections.singletonList(input);
            final PurgeInputsExecution<INPUT> execution =
                    new PurgeInputsExecution<INPUT>(context, mInvocationId, invocationClass, mArgs,
                                                    inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final INPUT... inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            final List<INPUT> inputList =
                    (inputs == null) ? Collections.<INPUT>emptyList() : Arrays.asList(inputs);
            final PurgeInputsExecution<INPUT> execution =
                    new PurgeInputsExecution<INPUT>(context, mInvocationId, invocationClass, mArgs,
                                                    inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
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

            final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();
            final PurgeInputsExecution<INPUT> execution =
                    new PurgeInputsExecution<INPUT>(context, mInvocationId, invocationClass, mArgs,
                                                    inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation class.
     */
    private static class PurgeExecution implements Execution {

        private final WeakReference<Object> mContext;

        private final Object[] mInvocationArgs;

        private final Class<?> mInvocationClass;

        private final int mInvocationId;

        /**
         * Constructor.
         *
         * @param context         the context reference.
         * @param invocationId    the invocation ID.
         * @param invocationClass the invocation class.
         * @param invocationArgs  the invocation constructor arguments.
         */
        private PurgeExecution(@Nonnull final WeakReference<Object> context, final int invocationId,
                @Nonnull final Class<?> invocationClass, @Nonnull final Object[] invocationArgs) {

            mContext = context;
            mInvocationId = invocationId;
            mInvocationClass = invocationClass;
            mInvocationArgs = invocationArgs;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoaders(context, mInvocationId, mInvocationClass,
                                              mInvocationArgs);
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

        private final Object[] mInvocationArgs;

        private final Class<?> mInvocationClass;

        private final int mInvocationId;

        /**
         * Constructor.
         *
         * @param context         the context reference.
         * @param invocationId    the invocation ID.
         * @param invocationClass the invocation class.
         * @param invocationArgs  the invocation constructor arguments.
         * @param inputs          the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final WeakReference<Object> context,
                final int invocationId, @Nonnull final Class<?> invocationClass,
                @Nonnull final Object[] invocationArgs, @Nonnull final List<INPUT> inputs) {

            mContext = context;
            mInvocationId = invocationId;
            mInvocationClass = invocationClass;
            mInvocationArgs = invocationArgs;
            mInputs = inputs;
        }

        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mInvocationId, mInvocationClass,
                                             mInvocationArgs, mInputs);
            }
        }
    }
}
