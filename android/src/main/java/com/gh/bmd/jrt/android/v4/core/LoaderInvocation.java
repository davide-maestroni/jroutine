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
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.SparseArray;

import com.gh.bmd.jrt.android.builder.InputClashException;
import com.gh.bmd.jrt.android.builder.InvocationClashException;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportInput;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.invocation.ProcedureInvocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Invocation implementation employing loaders to perform background operations.
 * <p/>
 * Created by davide-maestroni on 12/11/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class LoaderInvocation<INPUT, OUTPUT> extends ProcedureInvocation<INPUT, OUTPUT> {

    private static final WeakIdentityHashMap<Object,
            SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>
            sCallbackMap =
            new WeakIdentityHashMap<Object,
                    SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>();

    private final Object[] mArgs;

    private final CacheStrategyType mCacheStrategyType;

    private final ClashResolutionType mClashResolutionType;

    private final WeakReference<Object> mContext;

    private final ContextInvocationFactory<INPUT, OUTPUT> mFactory;

    private final ClashResolutionType mInputClashResolutionType;

    private final int mLoaderId;

    private final Logger mLogger;

    private final Looper mLooper;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param context       the context reference.
     * @param factory       the invocation factory.
     * @param args          the invocation factory arguments.
     * @param configuration the loader configuration.
     * @param order         the input data order.
     * @param logger        the logger instance.
     */
    @SuppressWarnings("ConstantConditions")
    LoaderInvocation(@Nonnull final WeakReference<Object> context,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory,
            @Nonnull final Object[] args, @Nonnull final LoaderConfiguration configuration,
            @Nullable final OrderType order, @Nonnull final Logger logger) {

        if (context == null) {

            throw new NullPointerException("the context reference must not be null");
        }

        if (factory == null) {

            throw new NullPointerException("the context invocation factory must not be null");
        }

        if (args == null) {

            throw new NullPointerException("the array of arguments must not be null");
        }

        mContext = context;
        mFactory = factory;
        mLooper = configuration.getResultLooperOr(null);
        mLoaderId = configuration.getLoaderIdOr(LoaderConfiguration.AUTO);
        mClashResolutionType =
                configuration.getClashResolutionTypeOr(ClashResolutionType.ABORT_THAT);
        mInputClashResolutionType =
                configuration.getInputClashResolutionTypeOr(ClashResolutionType.MERGE);
        mCacheStrategyType = configuration.getCacheStrategyTypeOr(CacheStrategyType.CLEAR);
        mArgs = args;
        mOrderType = order;
        mLogger = logger.subContextLogger(this);
    }

    /**
     * Destroys the loader with the specified ID.
     *
     * @param context  the context.
     * @param loaderId the loader ID.
     */
    static void purgeLoader(@Nonnull final Object context, final int loaderId) {

        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                sCallbackMap.get(context);

        if (callbackArray == null) {

            return;
        }

        final LoaderManager loaderManager;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderManager = activity.getSupportLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.remove(callbackArray.keyAt(i));
                continue;
            }

            final RoutineLoader<?, ?> loader = callbacks.mLoader;

            if ((loaderId == callbackArray.keyAt(i)) && (loader.getInvocationCount() == 0)) {

                loaderManager.destroyLoader(loaderId);
                callbackArray.remove(loaderId);
                continue;
            }

            ++i;
        }

        if (callbackArray.size() == 0) {

            sCallbackMap.remove(context);
        }
    }

    /**
     * Destroys all loaders with the specified invocation class and the specified inputs.
     *
     * @param context        the context.
     * @param loaderId       the loader ID.
     * @param invocationType the invocation type.
     * @param invocationArgs the invocation factory arguments.
     * @param inputs         the invocation inputs.
     */
    @SuppressWarnings("unchecked")
    static void purgeLoader(@Nonnull final Object context, final int loaderId,
            @Nonnull final String invocationType, @Nonnull final Object[] invocationArgs,
            @Nonnull final List<?> inputs) {

        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                sCallbackMap.get(context);

        if (callbackArray == null) {

            return;
        }

        final LoaderManager loaderManager;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderManager = activity.getSupportLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.remove(callbackArray.keyAt(i));
                continue;
            }

            final RoutineLoader<Object, Object> loader =
                    (RoutineLoader<Object, Object>) callbacks.mLoader;

            if (loader.getInvocationType().equals(invocationType) && Arrays.equals(
                    loader.getInvocationArgs(), invocationArgs) && (loader.getInvocationCount()
                    == 0)) {

                final int id = callbackArray.keyAt(i);

                if (((loaderId == LoaderConfiguration.AUTO) || (loaderId == id))
                        && loader.areSameInputs(inputs)) {

                    loaderManager.destroyLoader(id);
                    callbackArray.remove(id);
                    continue;
                }
            }

            ++i;
        }

        if (callbackArray.size() == 0) {

            sCallbackMap.remove(context);
        }
    }

    /**
     * Destroys the loader with the specified ID and the specified inputs.
     *
     * @param context  the context.
     * @param loaderId the loader ID.
     * @param inputs   the invocation inputs.
     */
    @SuppressWarnings("unchecked")
    static void purgeLoader(@Nonnull final Object context, final int loaderId,
            @Nonnull final List<?> inputs) {

        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                sCallbackMap.get(context);

        if (callbackArray == null) {

            return;
        }

        final LoaderManager loaderManager;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderManager = activity.getSupportLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.remove(callbackArray.keyAt(i));
                continue;
            }

            final RoutineLoader<Object, Object> loader =
                    (RoutineLoader<Object, Object>) callbacks.mLoader;

            if ((loader.getInvocationCount() == 0) && (loaderId == callbackArray.keyAt(i)) && loader
                    .areSameInputs(inputs)) {

                loaderManager.destroyLoader(loaderId);
                callbackArray.remove(loaderId);
                continue;
            }

            ++i;
        }

        if (callbackArray.size() == 0) {

            sCallbackMap.remove(context);
        }
    }

    /**
     * Destroys all loaders with the specified invocation class.
     *
     * @param context        the context.
     * @param loaderId       the loader ID.
     * @param invocationType the invocation type.
     * @param invocationArgs the invocation factory arguments.
     */
    static void purgeLoaders(@Nonnull final Object context, final int loaderId,
            @Nonnull final String invocationType, @Nonnull final Object[] invocationArgs) {

        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                sCallbackMap.get(context);

        if (callbackArray == null) {

            return;
        }

        final LoaderManager loaderManager;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderManager = activity.getSupportLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.remove(callbackArray.keyAt(i));
                continue;
            }

            final RoutineLoader<?, ?> loader = callbacks.mLoader;

            if (loader.getInvocationType().equals(invocationType) && Arrays.equals(
                    loader.getInvocationArgs(), invocationArgs) && (loader.getInvocationCount()
                    == 0)) {

                final int id = callbackArray.keyAt(i);

                if ((loaderId == LoaderConfiguration.AUTO) || (loaderId == id)) {

                    loaderManager.destroyLoader(id);
                    callbackArray.remove(id);
                    continue;
                }
            }

            ++i;
        }

        if (callbackArray.size() == 0) {

            sCallbackMap.remove(context);
        }
    }

    private static int recursiveHashCode(@Nullable final Object object) {

        if (object == null) {

            return 0;
        }

        if (object.getClass().isArray()) {

            int hashCode = 0;
            final int length = Array.getLength(object);

            for (int i = 0; i < length; i++) {

                hashCode = 31 * hashCode + recursiveHashCode(Array.get(object, i));
            }

            return hashCode;

        } else if (object instanceof Collection) {

            int hashCode = 0;

            for (final Object o : ((Collection<?>) object)) {

                hashCode = 31 * hashCode + recursiveHashCode(o);
            }

            return hashCode;

        } else if (object instanceof Map) {

            int hashCode = 0;

            for (final Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {

                hashCode = 31 * hashCode + recursiveHashCode(entry.getKey());
                hashCode = 31 * hashCode + recursiveHashCode(entry.getValue());
            }

            return hashCode;
        }

        return object.hashCode();
    }

    @Override
    public void onAbort(@Nullable final Throwable reason) {

        super.onAbort(reason);
        final Logger logger = mLogger;
        final Object context = mContext.get();

        if (context == null) {

            logger.dbg("avoiding aborting invocation since context is null");
            return;
        }

        final Context loaderContext;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderContext = activity.getApplicationContext();
            logger.dbg("aborting invocation bound to activity: %s", activity);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderContext = fragment.getActivity().getApplicationContext();
            logger.dbg("aborting invocation bound to fragment: %s", fragment);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        final ContextInvocation<INPUT, OUTPUT> invocation =
                createInvocation(loaderContext, mLoaderId);

        try {

            invocation.onInitialize();
            invocation.onAbort(reason);
            invocation.onTerminate();

        } catch (final InvocationInterruptedException e) {

            throw e;

        } catch (final Throwable ignored) {

        }

        try {

            invocation.onDestroy();

        } catch (final InvocationInterruptedException e) {

            throw e;

        } catch (final Throwable ignored) {

        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
            justification = "class comparison with == is done")
    public void onCall(@Nonnull final List<? extends INPUT> inputs,
            @Nonnull final ResultChannel<OUTPUT> result) {

        final Logger logger = mLogger;
        final Object context = mContext.get();

        if (context == null) {

            logger.dbg("avoiding running invocation since context is null");
            return;
        }

        final Context loaderContext;
        final LoaderManager loaderManager;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderContext = activity.getApplicationContext();
            loaderManager = activity.getSupportLoaderManager();
            logger.dbg("running invocation bound to activity: %s", activity);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderContext = fragment.getActivity().getApplicationContext();
            loaderManager = fragment.getLoaderManager();
            logger.dbg("running invocation bound to fragment: %s", fragment);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getName());
        }

        int loaderId = mLoaderId;

        if (loaderId == LoaderConfiguration.AUTO) {

            loaderId = mFactory.getInvocationType().hashCode();

            for (final Object arg : mArgs) {

                loaderId = 31 * loaderId + recursiveHashCode(arg);
            }

            loaderId = 31 * loaderId + inputs.hashCode();
            logger.dbg("generating loader ID: %d", loaderId);
        }

        final Loader<InvocationResult<OUTPUT>> loader = loaderManager.getLoader(loaderId);
        final ClashType clashType = getClashType(loader, loaderId, inputs);
        final WeakIdentityHashMap<Object, SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbackMap;
        SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(context);

        if (callbackArray == null) {

            callbackArray = new SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>();
            callbackMap.put(context, callbackArray);
        }

        final WeakReference<RoutineLoaderCallbacks<?>> callbackReference =
                callbackArray.get(loaderId);
        RoutineLoaderCallbacks<OUTPUT> callbacks = (callbackReference != null)
                ? (RoutineLoaderCallbacks<OUTPUT>) callbackReference.get() : null;

        if (clashType == ClashType.ABORT_BOTH) {

            final InvocationClashException clashException = new InvocationClashException(loaderId);

            if (callbacks != null) {

                logger.dbg("resetting existing callbacks [%d]", loaderId);
                callbacks.reset(clashException);
            }

            throw clashException;
        }

        if ((callbacks == null) || (loader == null) || (clashType != ClashType.NONE)) {

            final RoutineLoader<INPUT, OUTPUT> routineLoader;

            if ((clashType == ClashType.NONE) && (loader != null) && (loader.getClass()
                    == RoutineLoader.class)) {

                routineLoader = (RoutineLoader<INPUT, OUTPUT>) loader;

            } else {

                routineLoader = null;
            }

            final RoutineLoaderCallbacks<OUTPUT> newCallbacks =
                    createCallbacks(loaderContext, loaderManager, routineLoader, inputs, loaderId);

            if (callbacks != null) {

                logger.dbg("resetting existing callbacks [%d]", loaderId);
                callbacks.reset(new InvocationClashException(loaderId));
            }

            callbackArray.put(loaderId, new WeakReference<RoutineLoaderCallbacks<?>>(newCallbacks));
            callbacks = newCallbacks;
        }

        logger.dbg("setting result cache type [%d]: %s", loaderId, mCacheStrategyType);
        callbacks.setCacheStrategy(mCacheStrategyType);
        result.pass(callbacks.newChannel(mLooper));

        if (clashType == ClashType.ABORT_THAT) {

            logger.dbg("restarting loader [%d]", loaderId);
            loaderManager.restartLoader(loaderId, Bundle.EMPTY, callbacks);

        } else {

            logger.dbg("initializing loader [%d]", loaderId);
            loaderManager.initLoader(loaderId, Bundle.EMPTY, callbacks);
        }
    }

    @Nonnull
    private RoutineLoaderCallbacks<OUTPUT> createCallbacks(@Nonnull final Context loaderContext,
            @Nonnull final LoaderManager loaderManager,
            @Nullable final RoutineLoader<INPUT, OUTPUT> loader,
            @Nonnull final List<? extends INPUT> inputs, final int loaderId) {

        final Logger logger = mLogger;
        final RoutineLoader<INPUT, OUTPUT> callbacksLoader = (loader != null) ? loader
                : new RoutineLoader<INPUT, OUTPUT>(loaderContext,
                                                   createInvocation(loaderContext, loaderId),
                                                   mFactory.getInvocationType(), mArgs, inputs,
                                                   mOrderType, logger);
        return new RoutineLoaderCallbacks<OUTPUT>(loaderManager, callbacksLoader, logger);
    }

    @Nonnull
    private ContextInvocation<INPUT, OUTPUT> createInvocation(@Nonnull final Context loaderContext,
            final int loaderId) {

        final Logger logger = mLogger;
        final Object[] args = mArgs;
        final ContextInvocationFactory<INPUT, OUTPUT> factory = mFactory;
        final ContextInvocation<INPUT, OUTPUT> invocation;

        try {

            logger.dbg("creating a new invocation instance of type [%d]: %s", loaderId,
                       factory.getInvocationType());
            invocation = factory.newInvocation(args);
            invocation.onContext(loaderContext.getApplicationContext());

        } catch (final RoutineException e) {

            logger.err(e, "error creating the invocation instance [%d]", loaderId);
            throw e;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance [%d]", loaderId);
            throw new InvocationException(t);
        }

        return invocation;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    private ClashType getClashType(@Nullable final Loader<InvocationResult<OUTPUT>> loader,
            final int loaderId, @Nonnull final List<? extends INPUT> inputs) {

        if (loader == null) {

            return ClashType.NONE;
        }

        final Logger logger = mLogger;

        if (loader.getClass() != RoutineLoader.class) {

            logger.err("clashing loader ID [%d]: %s", loaderId, loader.getClass().getName());
            throw new InvocationClashException(loaderId);
        }

        final RoutineLoader<INPUT, OUTPUT> routineLoader = (RoutineLoader<INPUT, OUTPUT>) loader;
        final String invocationType = mFactory.getInvocationType();

        if (!MissingLoaderInvocation.TYPE.equals(invocationType) && (
                !routineLoader.getInvocationType().equals(invocationType) || !Arrays.equals(
                        routineLoader.getInvocationArgs(), mArgs))) {

            logger.wrn("clashing loader ID [%d]: %s", loaderId, routineLoader.getInvocationType());
            throw new InvocationClashException(loaderId);
        }

        final ClashResolutionType resolution =
                routineLoader.areSameInputs(inputs) ? mInputClashResolutionType
                        : mClashResolutionType;

        if (resolution == ClashResolutionType.MERGE) {

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
            throw new InputClashException(loaderId);
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
     * Loader callbacks implementation.<br/>
     * The callbacks object will make sure that the loader results are passed to the returned output
     * channels.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class RoutineLoaderCallbacks<OUTPUT>
            implements LoaderCallbacks<InvocationResult<OUTPUT>> {

        private final ArrayList<TransportInput<OUTPUT>> mAbortedChannels =
                new ArrayList<TransportInput<OUTPUT>>();

        private final ArrayList<TransportInput<OUTPUT>> mChannels =
                new ArrayList<TransportInput<OUTPUT>>();

        private final RoutineLoader<?, OUTPUT> mLoader;

        private final LoaderManager mLoaderManager;

        private final Logger mLogger;

        private final ArrayList<TransportInput<OUTPUT>> mNewChannels =
                new ArrayList<TransportInput<OUTPUT>>();

        private CacheStrategyType mCacheStrategyType;

        private int mResultCount;

        /**
         * Constructor.
         *
         * @param loaderManager the loader manager.
         * @param loader        the loader instance.
         * @param logger        the logger instance.
         */
        private RoutineLoaderCallbacks(@Nonnull final LoaderManager loaderManager,
                @Nonnull final RoutineLoader<?, OUTPUT> loader, @Nonnull final Logger logger) {

            mLoaderManager = loaderManager;
            mLoader = loader;
            mLogger = logger.subContextLogger(this);
        }

        /**
         * Creates and returns a new output channel.<br/>
         * The channel will be used to deliver the loader results.
         *
         * @param looper the looper instance.
         * @return the new output channel.
         */
        @Nonnull
        public OutputChannel<OUTPUT> newChannel(@Nullable final Looper looper) {

            final Logger logger = mLogger;
            logger.dbg("creating new result channel");
            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;
            final ArrayList<TransportInput<OUTPUT>> channels = mNewChannels;
            final TransportChannel<OUTPUT> channel = JRoutine.transport()
                                                             .withInvocation()
                                                             .withOutputMaxSize(Integer.MAX_VALUE)
                                                             .withOutputTimeout(TimeDuration.ZERO)
                                                             .withLog(logger.getLog())
                                                             .withLogLevel(logger.getLogLevel())
                                                             .set()
                                                             .buildChannel();
            channels.add(channel.input());
            internalLoader.setInvocationCount(Math.max(channels.size() + mAbortedChannels.size(),
                                                       internalLoader.getInvocationCount()));

            if ((looper != null) && (looper != Looper.getMainLooper())) {

                return JRoutine.on(PassingInvocation.<OUTPUT>factoryOf())
                               .withInvocation()
                               .withAsyncRunner(Runners.looperRunner(looper))
                               .withInputMaxSize(Integer.MAX_VALUE)
                               .withInputTimeout(TimeDuration.ZERO)
                               .withOutputMaxSize(Integer.MAX_VALUE)
                               .withOutputTimeout(TimeDuration.ZERO)
                               .withLog(logger.getLog())
                               .withLogLevel(logger.getLogLevel())
                               .set()
                               .callAsync(channel.output());
            }

            return channel.output();
        }

        public Loader<InvocationResult<OUTPUT>> onCreateLoader(final int id, final Bundle args) {

            mLogger.dbg("creating Android loader: %d", id);
            return mLoader;
        }

        public void onLoadFinished(final Loader<InvocationResult<OUTPUT>> loader,
                final InvocationResult<OUTPUT> data) {

            final Logger logger = mLogger;
            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;
            final ArrayList<TransportInput<OUTPUT>> channels = mChannels;
            final ArrayList<TransportInput<OUTPUT>> newChannels = mNewChannels;
            final ArrayList<TransportInput<OUTPUT>> abortedChannels = mAbortedChannels;
            logger.dbg("dispatching invocation result: %s", data);

            if (data.passTo(newChannels, channels, abortedChannels)) {

                final ArrayList<TransportInput<OUTPUT>> channelsToClose =
                        new ArrayList<TransportInput<OUTPUT>>(channels);
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

                    final Throwable exception = data.getAbortException();

                    for (final TransportInput<OUTPUT> channel : channelsToClose) {

                        channel.abort(exception);
                    }

                } else {

                    for (final TransportInput<OUTPUT> channel : channelsToClose) {

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

        public void onLoaderReset(final Loader<InvocationResult<OUTPUT>> loader) {

            mLogger.dbg("resetting Android loader: %d", mLoader.getId());
            reset(new InvocationClashException(mLoader.getId()));
        }

        private void reset(@Nullable final Throwable reason) {

            mLogger.dbg("aborting result channels");
            mResultCount = 0;
            final ArrayList<TransportInput<OUTPUT>> channels = mChannels;
            final ArrayList<TransportInput<OUTPUT>> newChannels = mNewChannels;

            for (final InputChannel<OUTPUT> channel : channels) {

                channel.abort(reason);
            }

            channels.clear();

            for (final InputChannel<OUTPUT> newChannel : newChannels) {

                newChannel.abort(reason);
            }

            newChannels.clear();
            mAbortedChannels.clear();
        }

        private void setCacheStrategy(@Nonnull final CacheStrategyType strategyType) {

            mLogger.dbg("setting cache type: %s", strategyType);
            mCacheStrategyType = strategyType;
        }
    }
}
