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

package com.github.dm.jrt.android.v11.core;

import android.content.Context;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.invocation.ContextInvocation;
import com.github.dm.jrt.android.invocation.FunctionContextInvocation;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.android.runner.AndroidRunners;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.builder.InvocationConfiguration;
import com.github.dm.jrt.core.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.runner.TemplateExecution;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Routine implementation delegating to Android loaders the asynchronous processing.
 * <p/>
 * Created by davide-maestroni on 01/10/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
        implements LoaderRoutine<IN, OUT> {

    private final LoaderConfiguration mConfiguration;

    private final LoaderContext mContext;

    private final FunctionContextInvocationFactory<IN, OUT> mFactory;

    private final int mLoaderId;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param context                 the routine context.
     * @param factory                 the invocation factory.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the loader configuration.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderRoutine(@NotNull final LoaderContext context,
            @NotNull final FunctionContextInvocationFactory<IN, OUT> factory,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration) {

        super(invocationConfiguration);
        if (context == null) {
            throw new NullPointerException("the routine context must not be null");
        }

        if (factory == null) {
            throw new NullPointerException("the context invocation factory must not be null");
        }

        final int routineId = loaderConfiguration.getRoutineIdOr(LoaderConfiguration.AUTO);
        mContext = context;
        mFactory = (routineId == LoaderConfiguration.AUTO) ? factory
                : new FactoryWrapper<IN, OUT>(factory, routineId);
        mConfiguration = loaderConfiguration;
        mLoaderId = loaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO);
        mOrderType = invocationConfiguration.getOutputOrderTypeOr(null);
        getLogger().dbg("building context routine with configuration: %s", loaderConfiguration);
    }

    @Override
    public void purge() {

        super.purge();
        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            AndroidRunners.mainRunner()
                          .run(new PurgeExecution(context, mFactory, mLoaderId), 0,
                               TimeUnit.MILLISECONDS);
        }
    }

    @NotNull
    @Override
    protected Invocation<IN, OUT> convertInvocation(@NotNull final Invocation<IN, OUT> invocation,
            @NotNull final InvocationType type) throws Exception {

        try {
            invocation.onDestroy();

        } catch (final Throwable t) {
            InvocationInterruptedException.throwIfInterrupt(t);
            getLogger().wrn(t, "ignoring exception while destroying invocation instance");
        }

        return newInvocation(type);
    }

    @NotNull
    @Override
    protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) throws
            Exception {

        final Logger logger = getLogger();
        if (type == InvocationType.ASYNC) {
            return new LoaderInvocation<IN, OUT>(mContext, mFactory, mConfiguration, mOrderType,
                                                 logger);
        }

        final Context loaderContext = mContext.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the routine context has been destroyed");
        }

        final FunctionContextInvocationFactory<IN, OUT> factory = mFactory;
        logger.dbg("creating a new invocation instance");
        final ContextInvocation<IN, OUT> invocation = factory.newInvocation();
        invocation.onContext(loaderContext.getApplicationContext());
        return invocation;
    }

    public void purge(@Nullable final IN input) {

        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            final List<IN> inputList = Collections.singletonList(input);
            final PurgeInputsExecution<IN> execution =
                    new PurgeInputsExecution<IN>(context, mFactory, mLoaderId, inputList);
            AndroidRunners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final IN... inputs) {

        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            final List<IN> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = new ArrayList<IN>(inputs.length);
                Collections.addAll(inputList, inputs);
            }

            final PurgeInputsExecution<IN> execution =
                    new PurgeInputsExecution<IN>(context, mFactory, mLoaderId, inputList);
            AndroidRunners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Iterable<? extends IN> inputs) {

        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            final List<IN> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = new ArrayList<IN>();
                for (final IN input : inputs) {
                    inputList.add(input);
                }
            }

            final PurgeInputsExecution<IN> execution =
                    new PurgeInputsExecution<IN>(context, mFactory, mLoaderId, inputList);
            AndroidRunners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Wrapper of function context invocation factory overriding {@code equals()} and
     * {@code hashCode()}.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FactoryWrapper<IN, OUT> extends FunctionContextInvocationFactory<IN, OUT> {

        private final FunctionContextInvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory   the wrapped factory.
         * @param routineId the routine ID.
         */
        protected FactoryWrapper(@NotNull final FunctionContextInvocationFactory<IN, OUT> factory,
                final int routineId) {

            super(asArgs(routineId));
            mFactory = factory;
        }

        @NotNull
        @Override
        public FunctionContextInvocation<IN, OUT> newInvocation() throws Exception {

            return mFactory.newInvocation();
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation factory.
     */
    private static class PurgeExecution extends TemplateExecution {

        private final LoaderContext mContext;

        private final FunctionContextInvocationFactory<?, ?> mFactory;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@NotNull final LoaderContext context,
                @NotNull final FunctionContextInvocationFactory<?, ?> factory, final int loaderId) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
        }

        public void run() {

            LoaderInvocation.purgeLoaders(mContext, mLoaderId, mFactory);
        }
    }

    /**
     * Execution implementation purging the loader with a specific invocation factory and inputs.
     *
     * @param <IN> the input data type.
     */
    private static class PurgeInputsExecution<IN> extends TemplateExecution {

        private final LoaderContext mContext;

        private final FunctionContextInvocationFactory<?, ?> mFactory;

        private final List<IN> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@NotNull final LoaderContext context,
                @NotNull final FunctionContextInvocationFactory<?, ?> factory, final int loaderId,
                @NotNull final List<IN> inputs) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
            mInputs = inputs;
        }

        public void run() {

            LoaderInvocation.purgeLoader(mContext, mLoaderId, mFactory, mInputs);
        }
    }
}
