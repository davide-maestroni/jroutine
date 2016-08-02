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

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.core.ConverterRoutine;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.android.v11.core.LoaderInvocation.clearLoaders;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Routine implementation delegating to Android loaders the asynchronous processing.
 * <p>
 * Created by davide-maestroni on 01/10/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderRoutine<IN, OUT> extends ConverterRoutine<IN, OUT>
        implements LoaderRoutine<IN, OUT> {

    private final LoaderConfiguration mConfiguration;

    private final LoaderContext mContext;

    private final ContextInvocationFactory<IN, OUT> mFactory;

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
    DefaultLoaderRoutine(@NotNull final LoaderContext context,
            @NotNull final ContextInvocationFactory<IN, OUT> factory,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration) {
        super(invocationConfiguration);
        final int factoryId = loaderConfiguration.getFactoryIdOrElse(LoaderConfiguration.AUTO);
        mContext = ConstantConditions.notNull("loader context", context);
        ConstantConditions.notNull("context invocation factory", factory);
        mFactory = (factoryId == LoaderConfiguration.AUTO) ? factory
                : new FactoryWrapper<IN, OUT>(factory, factoryId);
        mConfiguration = loaderConfiguration;
        mLoaderId = loaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO);
        mOrderType = invocationConfiguration.getOutputOrderTypeOrElse(null);
        getLogger().dbg("building context routine with configuration: %s", loaderConfiguration);
    }

    @Override
    public void clear() {
        super.clear();
        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            clearLoaders(context, mLoaderId, mFactory);
        }
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

        final ContextInvocationFactory<IN, OUT> factory = mFactory;
        logger.dbg("creating a new invocation instance");
        final ContextInvocation<IN, OUT> invocation = factory.newInvocation();
        invocation.onContext(loaderContext.getApplicationContext());
        return invocation;
    }

    @Override
    public void clear(@Nullable final IN input) {
        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            clearLoaders(context, mLoaderId, mFactory, Collections.singletonList(input));
        }
    }

    public void clear(@Nullable final IN... inputs) {
        final LoaderContext context = mContext;
        if (context.getComponent() != null) {
            final List<IN> inputList;
            if (inputs == null) {
                inputList = Collections.emptyList();

            } else {
                inputList = Arrays.asList(inputs);
            }

            clearLoaders(context, mLoaderId, mFactory, inputList);
        }
    }

    @Override
    public void clear(@Nullable final Iterable<? extends IN> inputs) {
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

            clearLoaders(context, mLoaderId, mFactory, inputList);
        }
    }

    /**
     * Wrapper of call context invocation factories overriding {@code equals()} and
     * {@code hashCode()}.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FactoryWrapper<IN, OUT> extends ContextInvocationFactory<IN, OUT> {

        private final ContextInvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory   the wrapped factory.
         * @param factoryId the factory ID.
         */
        protected FactoryWrapper(@NotNull final ContextInvocationFactory<IN, OUT> factory,
                final int factoryId) {
            super(asArgs(factoryId));
            mFactory = factory;
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() throws Exception {
            return mFactory.newInvocation();
        }
    }
}
