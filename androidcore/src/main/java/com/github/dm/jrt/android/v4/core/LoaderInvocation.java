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

package com.github.dm.jrt.android.v4.core;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.InvocationClashException;
import com.github.dm.jrt.android.core.invocation.InvocationTypeException;
import com.github.dm.jrt.android.core.invocation.StaleResultException;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.fromFactory;
import static com.github.dm.jrt.android.core.runner.AndroidRunners.mainRunner;
import static com.github.dm.jrt.core.util.UnitDuration.infinity;

/**
 * Invocation implementation employing loaders to perform background operations.
 * <p>
 * Created by davide-maestroni on 12/11/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class LoaderInvocation<IN, OUT> extends CallInvocation<IN, OUT> {

    private static final WeakIdentityHashMap<Object,
            SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>
            sCallbacks =
            new WeakIdentityHashMap<Object,
                    SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>();

    private static final Runner sMainRunner = Runners.zeroDelayRunner(mainRunner());

    private final CacheStrategyType mCacheStrategyType;

    private final ClashResolutionType mClashResolutionType;

    private final LoaderContextCompat mContext;

    private final ContextInvocationFactory<IN, OUT> mFactory;

    private final ClashResolutionType mInputClashResolutionType;

    private final int mLoaderId;

    private final Logger mLogger;

    private final OrderType mOrderType;

    private final long mResultStaleTimeMillis;

    /**
     * Constructor.
     *
     * @param context       the context instance.
     * @param factory       the invocation factory.
     * @param configuration the loader configuration.
     * @param order         the input data order.
     * @param logger        the logger instance.
     */
    LoaderInvocation(@NotNull final LoaderContextCompat context,
            @NotNull final ContextInvocationFactory<IN, OUT> factory,
            @NotNull final LoaderConfiguration configuration, @Nullable final OrderType order,
            @NotNull final Logger logger) {

        mContext = ConstantConditions.notNull("loader context", context);
        mFactory = ConstantConditions.notNull("context invocation factory", factory);
        mLoaderId = configuration.getLoaderIdOrElse(LoaderConfiguration.AUTO);
        mClashResolutionType =
                configuration.getClashResolutionTypeOrElse(ClashResolutionType.ABORT_THAT);
        mInputClashResolutionType =
                configuration.getInputClashResolutionTypeOrElse(ClashResolutionType.JOIN);
        mCacheStrategyType = configuration.getCacheStrategyTypeOrElse(CacheStrategyType.CLEAR);
        mResultStaleTimeMillis = configuration.getResultStaleTimeOrElse(infinity()).toMillis();
        mOrderType = order;
        mLogger = logger.subContextLogger(this);
    }

    /**
     * Destroys the loader with the specified ID.
     *
     * @param context  the context instance.
     * @param loaderId the loader ID.
     */
    static void purgeLoader(@NotNull final LoaderContextCompat context, final int loaderId) {

        sMainRunner.run(new PurgeExecution(context, loaderId), 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Destroys the loader with the specified ID and the specified inputs.
     *
     * @param context  the context instance.
     * @param loaderId the loader ID.
     * @param inputs   the invocation inputs.
     */
    static void purgeLoader(@NotNull final LoaderContextCompat context, final int loaderId,
            @NotNull final List<?> inputs) {

        sMainRunner.run(new PurgeInputsExecution(context, loaderId, inputs), 0,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Destroys all the loaders with the specified invocation factory.
     *
     * @param context  the context instance.
     * @param loaderId the loader ID.
     * @param factory  the invocation factory.
     */
    static void purgeLoaders(@NotNull final LoaderContextCompat context, final int loaderId,
            @NotNull final ContextInvocationFactory<?, ?> factory) {

        sMainRunner.run(new PurgeFactoryExecution(context, factory, loaderId), 0,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Destroys all the loaders with the specified invocation factory and inputs.
     *
     * @param context  the context instance.
     * @param loaderId the loader ID.
     * @param factory  the invocation factory.
     * @param inputs   the invocation inputs.
     */
    static void purgeLoaders(@NotNull final LoaderContextCompat context, final int loaderId,
            @NotNull final ContextInvocationFactory<?, ?> factory, @NotNull final List<?> inputs) {

        sMainRunner.run(new PurgeFactoryInputsExecution(context, factory, loaderId, inputs), 0,
                TimeUnit.MILLISECONDS);
    }

    private static void purgeLoaderInternal(@NotNull final LoaderContextCompat context,
            final int loaderId) {

        final Object component = context.getComponent();
        final WeakIdentityHashMap<Object,
                SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbacks;
        final SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(component);
        if (callbackArray == null) {
            return;
        }

        final LoaderManager loaderManager = context.getLoaderManager();
        if (loaderManager == null) {
            return;
        }

        int i = 0;
        while (i < callbackArray.size()) {
            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();
            if (callbacks == null) {
                callbackArray.removeAt(i);
                continue;
            }

            final InvocationLoader<?, ?> loader = callbacks.mLoader;
            if ((loaderId == callbackArray.keyAt(i)) && (loader.getInvocationCount() == 0)) {
                loaderManager.destroyLoader(loaderId);
                callbackArray.removeAt(i);
                continue;
            }

            ++i;
        }

        if (callbackArray.size() == 0) {
            callbackMap.remove(component);
        }
    }

    private static void purgeLoaderInternal(@NotNull final LoaderContextCompat context,
            final int loaderId, @NotNull final List<?> inputs) {

        final Object component = context.getComponent();
        final WeakIdentityHashMap<Object,
                SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbacks;
        final SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(component);
        if (callbackArray == null) {
            return;
        }

        final LoaderManager loaderManager = context.getLoaderManager();
        if (loaderManager == null) {
            return;
        }

        int i = 0;
        while (i < callbackArray.size()) {
            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();
            if (callbacks == null) {
                callbackArray.removeAt(i);
                continue;
            }

            @SuppressWarnings("unchecked") final InvocationLoader<Object, Object> loader =
                    (InvocationLoader<Object, Object>) callbacks.mLoader;
            if ((loader.getInvocationCount() == 0) && (loaderId == callbackArray.keyAt(i)) && loader
                    .areSameInputs(inputs)) {
                loaderManager.destroyLoader(loaderId);
                callbackArray.removeAt(i);
                continue;
            }

            ++i;
        }

        if (callbackArray.size() == 0) {
            callbackMap.remove(component);
        }
    }

    private static void purgeLoadersInternal(@NotNull final LoaderContextCompat context,
            final int loaderId, @NotNull final ContextInvocationFactory<?, ?> factory) {

        final Object component = context.getComponent();
        final WeakIdentityHashMap<Object,
                SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbacks;
        final SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(component);
        if (callbackArray == null) {
            return;
        }

        final LoaderManager loaderManager = context.getLoaderManager();
        if (loaderManager == null) {
            return;
        }

        int i = 0;
        while (i < callbackArray.size()) {
            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();
            if (callbacks == null) {
                callbackArray.removeAt(i);
                continue;
            }

            final InvocationLoader<?, ?> loader = callbacks.mLoader;
            if (loader.getInvocationFactory().equals(factory) && (loader.getInvocationCount()
                    == 0)) {
                final int id = callbackArray.keyAt(i);
                if ((loaderId == LoaderConfiguration.AUTO) || (loaderId == id)) {
                    loaderManager.destroyLoader(id);
                    callbackArray.removeAt(i);
                    continue;
                }
            }

            ++i;
        }

        if (callbackArray.size() == 0) {
            callbackMap.remove(component);
        }
    }

    private static void purgeLoadersInternal(@NotNull final LoaderContextCompat context,
            final int loaderId, @NotNull final ContextInvocationFactory<?, ?> factory,
            @NotNull final List<?> inputs) {

        final Object component = context.getComponent();
        final WeakIdentityHashMap<Object,
                SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbacks;
        final SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(component);
        if (callbackArray == null) {
            return;
        }

        final LoaderManager loaderManager = context.getLoaderManager();
        if (loaderManager == null) {
            return;
        }

        int i = 0;
        while (i < callbackArray.size()) {
            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();
            if (callbacks == null) {
                callbackArray.removeAt(i);
                continue;
            }

            @SuppressWarnings("unchecked") final InvocationLoader<Object, Object> loader =
                    (InvocationLoader<Object, Object>) callbacks.mLoader;
            if (loader.getInvocationFactory().equals(factory) && (loader.getInvocationCount()
                    == 0)) {
                final int id = callbackArray.keyAt(i);
                if (((loaderId == LoaderConfiguration.AUTO) || (loaderId == id))
                        && loader.areSameInputs(inputs)) {

                    loaderManager.destroyLoader(id);
                    callbackArray.removeAt(i);
                    continue;
                }
            }

            ++i;
        }

        if (callbackArray.size() == 0) {
            callbackMap.remove(component);
        }
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {

        super.onAbort(reason);
        final Context loaderContext = mContext.getLoaderContext();
        if (loaderContext == null) {
            mLogger.dbg("avoiding aborting invocation since context is null");
            return;
        }

        final LoaderContextInvocationFactory<IN, OUT> factory =
                new LoaderContextInvocationFactory<IN, OUT>(this, mLoaderId);
        final Routine<IN, OUT> routine =
                JRoutineCore.on(fromFactory(loaderContext.getApplicationContext(), factory))
                            .buildRoutine();
        routine.syncInvoke().abort(reason);
        routine.purge();
    }

    @Override
    protected void onCall(@NotNull final List<? extends IN> inputs,
            @NotNull final ResultChannel<OUT> result) throws Exception {

        final LoaderContextCompat context = mContext;
        final Object component = context.getComponent();
        final Context loaderContext = context.getLoaderContext();
        final LoaderManager loaderManager = context.getLoaderManager();
        if ((component == null) || (loaderContext == null) || (loaderManager == null)) {
            throw new IllegalArgumentException("the routine context has been destroyed");
        }

        final Logger logger = mLogger;
        int loaderId = mLoaderId;
        if (loaderId == LoaderConfiguration.AUTO) {
            loaderId = 31 * mFactory.hashCode() + inputs.hashCode();
            logger.dbg("generating loader ID: %d", loaderId);
        }

        final Loader<InvocationResult<OUT>> loader = loaderManager.getLoader(loaderId);
        final ClashType clashType = getClashType(loader, loaderId, inputs);
        final WeakIdentityHashMap<Object,
                SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbacks;
        SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(component);
        if (callbackArray == null) {
            callbackArray = new SparseArrayCompat<WeakReference<RoutineLoaderCallbacks<?>>>();
            callbackMap.put(component, callbackArray);
        }

        final WeakReference<RoutineLoaderCallbacks<?>> callbackReference =
                callbackArray.get(loaderId);
        @SuppressWarnings("unchecked") RoutineLoaderCallbacks<OUT> callbacks =
                (callbackReference != null) ? (RoutineLoaderCallbacks<OUT>) callbackReference.get()
                        : null;
        if (clashType == ClashType.ABORT_BOTH) {
            final InvocationClashException clashException = new InvocationClashException(loaderId);
            if (callbacks != null) {
                logger.dbg("resetting existing callbacks [%d]", loaderId);
                callbacks.reset(clashException);
            }

            throw clashException;
        }

        final boolean isRoutineLoader =
                (loader != null) && (loader.getClass() == InvocationLoader.class);
        final boolean isStaleResult =
                isRoutineLoader && ((InvocationLoader<?, OUT>) loader).isStaleResult(
                        mResultStaleTimeMillis);
        if ((callbacks == null) || (loader == null) || (clashType == ClashType.ABORT_THAT)
                || isStaleResult) {
            final InvocationLoader<IN, OUT> invocationLoader;
            if ((clashType == ClashType.NONE) && isRoutineLoader && !isStaleResult) {
                invocationLoader = (InvocationLoader<IN, OUT>) loader;

            } else {
                invocationLoader = null;
            }

            final RoutineLoaderCallbacks<OUT> newCallbacks =
                    createCallbacks(loaderContext, loaderManager, invocationLoader, inputs,
                            loaderId);
            if (callbacks != null) {
                logger.dbg("resetting existing callbacks [%d]", loaderId);
                callbacks.reset(((clashType == ClashType.ABORT_THAT) || !isStaleResult)
                        ? new InvocationClashException(loaderId)
                        : new StaleResultException(loaderId));
            }

            callbackArray.put(loaderId, new WeakReference<RoutineLoaderCallbacks<?>>(newCallbacks));
            callbacks = newCallbacks;
        }

        final CacheStrategyType strategyType = mCacheStrategyType;
        logger.dbg("setting result cache type [%d]: %s", loaderId, strategyType);
        callbacks.setCacheStrategy(strategyType);
        result.pass(callbacks.newChannel());
        if ((clashType == ClashType.ABORT_THAT) || isStaleResult) {
            logger.dbg("restarting loader [%d]", loaderId);
            loaderManager.restartLoader(loaderId, Bundle.EMPTY, callbacks);

        } else {
            logger.dbg("initializing loader [%d]", loaderId);
            loaderManager.initLoader(loaderId, Bundle.EMPTY, callbacks);
        }
    }

    @NotNull
    private RoutineLoaderCallbacks<OUT> createCallbacks(@NotNull final Context loaderContext,
            @NotNull final LoaderManager loaderManager,
            @Nullable final InvocationLoader<IN, OUT> loader,
            @NotNull final List<? extends IN> inputs, final int loaderId) throws Exception {

        final Logger logger = mLogger;
        final InvocationLoader<IN, OUT> callbacksLoader = (loader != null) ? loader
                : new InvocationLoader<IN, OUT>(loaderContext, createInvocation(loaderId), mFactory,
                        inputs, mOrderType, logger);
        return new RoutineLoaderCallbacks<OUT>(loaderManager, callbacksLoader, logger);
    }

    @NotNull
    private ContextInvocation<IN, OUT> createInvocation(final int loaderId) throws Exception {

        final Logger logger = mLogger;
        final ContextInvocationFactory<IN, OUT> factory = mFactory;
        final ContextInvocation<IN, OUT> invocation;
        try {
            logger.dbg("creating a new invocation instance [%d]", loaderId);
            invocation = factory.newInvocation();

        } catch (final Exception e) {
            logger.err(e, "error creating the invocation instance [%d]", loaderId);
            throw e;
        }

        return invocation;
    }

    @NotNull
    private ClashType getClashType(@Nullable final Loader<InvocationResult<OUT>> loader,
            final int loaderId, @NotNull final List<? extends IN> inputs) {

        if (loader == null) {
            return ClashType.NONE;
        }

        final Logger logger = mLogger;
        if (loader.getClass() != InvocationLoader.class) {
            logger.err("clashing loader ID [%d]: %s", loaderId, loader.getClass().getName());
            throw new InvocationTypeException(loaderId);
        }

        final ContextInvocationFactory<IN, OUT> factory = mFactory;
        @SuppressWarnings("unchecked") final InvocationLoader<IN, OUT> invocationLoader =
                (InvocationLoader<IN, OUT>) loader;
        if (!(factory instanceof MissingLoaderInvocationFactory)
                && !invocationLoader.getInvocationFactory().equals(factory)) {
            logger.wrn("clashing loader ID [%d]: %s", loaderId,
                    invocationLoader.getInvocationFactory());
            throw new InvocationTypeException(loaderId);
        }

        final ClashResolutionType resolution =
                invocationLoader.areSameInputs(inputs) ? mInputClashResolutionType
                        : mClashResolutionType;
        if (resolution == ClashResolutionType.JOIN) {
            logger.dbg("keeping existing invocation [%d]", loaderId);
            return ClashType.NONE;

        } else if (resolution == ClashResolutionType.ABORT) {
            logger.dbg("restarting existing invocation [%d]", loaderId);
            return ClashType.ABORT_BOTH;

        } else if (resolution == ClashResolutionType.ABORT_THAT) {
            logger.dbg("restarting existing invocation [%d]", loaderId);
            return ClashType.ABORT_THAT;

        } else if (resolution == ClashResolutionType.ABORT_THIS) {
            logger.dbg("aborting invocation [%d]", loaderId);
            throw new InvocationClashException(loaderId);
        }

        return ClashType.ABORT_BOTH;
    }

    /**
     * Clash type enumeration.
     */
    private enum ClashType {

        NONE,       // no clash detected
        ABORT_THAT, // need to abort the running loader
        ABORT_BOTH  // need to abort both the invocation and the running loader
    }

    /**
     * Context invocation factory implementation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class LoaderContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

        private final LoaderInvocation<IN, OUT> mInvocation;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param invocation the loader invocation instance.
         * @param loaderId   the loader ID.
         */
        private LoaderContextInvocationFactory(@NotNull final LoaderInvocation<IN, OUT> invocation,
                final int loaderId) {

            super(null);
            mInvocation = invocation;
            mLoaderId = loaderId;
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() throws Exception {

            return mInvocation.createInvocation(mLoaderId);
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID.
     */
    private static class PurgeExecution implements Execution {

        private final LoaderContextCompat mContext;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@NotNull final LoaderContextCompat context, final int loaderId) {

            mContext = context;
            mLoaderId = loaderId;
        }

        public void run() {

            purgeLoaderInternal(mContext, mLoaderId);
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation factory.
     */
    private static class PurgeFactoryExecution implements Execution {

        private final LoaderContextCompat mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         */
        private PurgeFactoryExecution(@NotNull final LoaderContextCompat context,
                @NotNull final ContextInvocationFactory<?, ?> factory, final int loaderId) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
        }

        public void run() {

            purgeLoadersInternal(mContext, mLoaderId, mFactory);
        }
    }

    /**
     * Execution implementation purging the loader with a specific invocation factory and inputs.
     */
    private static class PurgeFactoryInputsExecution implements Execution {

        private final LoaderContextCompat mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        private final List<?> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeFactoryInputsExecution(@NotNull final LoaderContextCompat context,
                @NotNull final ContextInvocationFactory<?, ?> factory, final int loaderId,
                @NotNull final List<?> inputs) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
            mInputs = new ArrayList<Object>(inputs);
        }

        public void run() {

            purgeLoadersInternal(mContext, mLoaderId, mFactory, mInputs);
        }
    }

    /**
     * Execution implementation purging the loader with a specific ID and inputs.
     */
    private static class PurgeInputsExecution implements Execution {

        private final LoaderContextCompat mContext;

        private final List<?> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@NotNull final LoaderContextCompat context, final int loaderId,
                @NotNull final List<?> inputs) {

            mContext = context;
            mLoaderId = loaderId;
            mInputs = new ArrayList<Object>(inputs);
        }

        public void run() {

            purgeLoaderInternal(mContext, mLoaderId, mInputs);
        }
    }

    /**
     * Loader callbacks implementation.
     * <br>
     * The callbacks object will make sure that the loader results are passed to the returned output
     * channels.
     *
     * @param <OUT> the output data type.
     */
    private static class RoutineLoaderCallbacks<OUT>
            implements LoaderCallbacks<InvocationResult<OUT>> {

        private final ArrayList<IOChannel<OUT>> mAbortedChannels = new ArrayList<IOChannel<OUT>>();

        private final ArrayList<IOChannel<OUT>> mChannels = new ArrayList<IOChannel<OUT>>();

        private final InvocationLoader<?, OUT> mLoader;

        private final LoaderManager mLoaderManager;

        private final Logger mLogger;

        private final ArrayList<IOChannel<OUT>> mNewChannels = new ArrayList<IOChannel<OUT>>();

        private CacheStrategyType mCacheStrategyType;

        private int mResultCount;

        /**
         * Constructor.
         *
         * @param loaderManager the loader manager.
         * @param loader        the loader instance.
         * @param logger        the logger instance.
         */
        private RoutineLoaderCallbacks(@NotNull final LoaderManager loaderManager,
                @NotNull final InvocationLoader<?, OUT> loader, @NotNull final Logger logger) {

            mLoaderManager = loaderManager;
            mLoader = loader;
            mLogger = logger.subContextLogger(this);
        }

        public Loader<InvocationResult<OUT>> onCreateLoader(final int id, final Bundle args) {

            mLogger.dbg("creating Android loader: %d", id);
            return mLoader;
        }

        public void onLoadFinished(final Loader<InvocationResult<OUT>> loader,
                final InvocationResult<OUT> data) {

            final Logger logger = mLogger;
            final InvocationLoader<?, OUT> internalLoader = mLoader;
            final ArrayList<IOChannel<OUT>> channels = mChannels;
            final ArrayList<IOChannel<OUT>> newChannels = mNewChannels;
            final ArrayList<IOChannel<OUT>> abortedChannels = mAbortedChannels;
            logger.dbg("dispatching invocation result: %s", data);
            if (data.passTo(newChannels, channels, abortedChannels)) {
                final ArrayList<IOChannel<OUT>> channelsToClose =
                        new ArrayList<IOChannel<OUT>>(channels);
                channelsToClose.addAll(newChannels);
                mResultCount += channels.size() + newChannels.size();
                channels.clear();
                newChannels.clear();
                abortedChannels.clear();
                if (mResultCount >= internalLoader.getInvocationCount()) {
                    mResultCount = 0;
                    internalLoader.setInvocationCount(0);
                    final CacheStrategyType strategyType = mCacheStrategyType;
                    if ((strategyType == CacheStrategyType.CLEAR) || (data.isError() ? (strategyType
                            == CacheStrategyType.CACHE_IF_SUCCESS)
                            : (strategyType == CacheStrategyType.CACHE_IF_ERROR))) {
                        final int id = internalLoader.getId();
                        logger.dbg("destroying Android loader: %d", id);
                        mLoaderManager.destroyLoader(id);
                    }
                }

                if (data.isError()) {
                    final RoutineException exception = data.getAbortException();
                    for (final IOChannel<OUT> channel : channelsToClose) {
                        channel.abort(exception);
                    }

                } else {
                    for (final IOChannel<OUT> channel : channelsToClose) {
                        channel.close();
                    }
                }

            } else {
                mResultCount += abortedChannels.size();
                channels.addAll(newChannels);
                channels.removeAll(abortedChannels);
                newChannels.clear();
                if (channels.isEmpty() && (mResultCount >= internalLoader.getInvocationCount())) {
                    data.abort();
                }
            }
        }

        public void onLoaderReset(final Loader<InvocationResult<OUT>> loader) {

            mLogger.dbg("resetting Android loader: %d", mLoader.getId());
            reset(new InvocationClashException(mLoader.getId()));
        }

        @NotNull
        private OutputChannel<OUT> newChannel() {

            final Logger logger = mLogger;
            logger.dbg("creating new result channel");
            final InvocationLoader<?, OUT> internalLoader = mLoader;
            final ArrayList<IOChannel<OUT>> channels = mNewChannels;
            final IOChannel<OUT> channel = JRoutineCore.io()
                                                       .channelConfiguration()
                                                       .withLog(logger.getLog())
                                                       .withLogLevel(logger.getLogLevel())
                                                       .apply()
                                                       .buildChannel();
            channels.add(channel);
            internalLoader.setInvocationCount(Math.max(channels.size() + mAbortedChannels.size(),
                    internalLoader.getInvocationCount()));
            return channel;
        }

        private void reset(@Nullable final Throwable reason) {

            mLogger.dbg("aborting result channels");
            mResultCount = 0;
            final ArrayList<IOChannel<OUT>> channels = mChannels;
            final ArrayList<IOChannel<OUT>> newChannels = mNewChannels;
            for (final IOChannel<OUT> channel : channels) {
                channel.abort(reason);
            }

            channels.clear();
            for (final IOChannel<OUT> newChannel : newChannels) {
                newChannel.abort(reason);
            }

            newChannels.clear();
            mAbortedChannels.clear();
        }

        private void setCacheStrategy(@NotNull final CacheStrategyType strategyType) {

            mLogger.dbg("setting cache type: %s", strategyType);
            mCacheStrategyType = strategyType;
        }
    }
}
