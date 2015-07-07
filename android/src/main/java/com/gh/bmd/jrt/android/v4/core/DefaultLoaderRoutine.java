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

import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.routine.LoaderRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.AbstractRoutine;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.TemplateExecution;

import java.lang.ref.WeakReference;
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
 * Created by davide-maestroni on 1/10/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultLoaderRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT>
        implements LoaderRoutine<INPUT, OUTPUT> {

    private final LoaderConfiguration mConfiguration;

    private final WeakReference<Object> mContext;

    private final ContextInvocationFactory<INPUT, OUTPUT> mFactory;

    private final int mLoaderId;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param context                 the context reference.
     * @param factory                 the invocation factory.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the loader configuration.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderRoutine(@Nonnull final WeakReference<Object> context,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final LoaderConfiguration loaderConfiguration) {

        super(invocationConfiguration);

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (factory == null) {

            throw new NullPointerException("the context invocation factory must not be null");
        }

        mContext = context;
        mFactory = factory;
        mConfiguration = loaderConfiguration;
        mLoaderId = loaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO);
        mOrderType = invocationConfiguration.getOutputOrderTypeOr(null);
        getLogger().dbg("building context routine with configuration: %s", loaderConfiguration);
    }

    @Override
    public void purge() {

        super.purge();
        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            Runners.mainRunner()
                   .run(new PurgeExecution(context, mFactory, mLoaderId), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> convertInvocation(
            @Nonnull final Invocation<INPUT, OUTPUT> invocation,
            @Nonnull final InvocationType type) {

        try {

            invocation.onDestroy();

        } catch (final InvocationInterruptedException e) {

            throw e;

        } catch (final Throwable ignored) {

            getLogger().wrn(ignored, "ignoring exception while destroying invocation instance");
        }

        return newInvocation(type);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final InvocationType type) {

        final Logger logger = getLogger();

        if (type == InvocationType.ASYNC) {

            return new LoaderInvocation<INPUT, OUTPUT>(mContext, mFactory, mConfiguration,
                                                       mOrderType, logger);
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
                    "invalid context type: " + context.getClass().getName());
        }

        try {

            final ContextInvocationFactory<INPUT, OUTPUT> factory = mFactory;
            logger.dbg("creating a new invocation instance");
            final ContextInvocation<INPUT, OUTPUT> invocation = factory.newInvocation();
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

            final List<INPUT> inputList = Collections.singletonList(input);
            final PurgeInputsExecution<INPUT> execution =
                    new PurgeInputsExecution<INPUT>(context, mFactory, mLoaderId, inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final INPUT... inputs) {

        final WeakReference<Object> context = mContext;

        if (context.get() != null) {

            final List<INPUT> inputList =
                    (inputs == null) ? Collections.<INPUT>emptyList() : Arrays.asList(inputs);
            final PurgeInputsExecution<INPUT> execution =
                    new PurgeInputsExecution<INPUT>(context, mFactory, mLoaderId, inputList);
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

            final PurgeInputsExecution<INPUT> execution =
                    new PurgeInputsExecution<INPUT>(context, mFactory, mLoaderId, inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation factory.
     */
    private static class PurgeExecution extends TemplateExecution {

        private final WeakReference<Object> mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context reference.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@Nonnull final WeakReference<Object> context,
                @Nonnull final ContextInvocationFactory<?, ?> factory, final int loaderId) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
        }

        @Override
        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoaders(context, mLoaderId, mFactory);
            }
        }
    }

    /**
     * Execution implementation purging the loader with a specific invocation factory and inputs.
     *
     * @param <INPUT> the input data type.
     */
    private static class PurgeInputsExecution<INPUT> extends TemplateExecution {

        private final WeakReference<Object> mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        private final List<INPUT> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context reference.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final WeakReference<Object> context,
                @Nonnull final ContextInvocationFactory<?, ?> factory, final int loaderId,
                @Nonnull final List<INPUT> inputs) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
            mInputs = inputs;
        }

        @Override
        public void run() {

            final Object context = mContext.get();

            if (context != null) {

                LoaderInvocation.purgeLoader(context, mLoaderId, mFactory, mInputs);
            }
        }
    }
}
