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
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.SparseArray;

import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration;
import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.builder.InputClashException;
import com.gh.bmd.jrt.android.builder.InvocationClashException;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneInput;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.invocation.SingleCallInvocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;

/**
 * Invocation implementation employing loaders to perform background operations.
 * <p/>
 * Created by davide on 12/11/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
class LoaderInvocation<INPUT, OUTPUT> extends SingleCallInvocation<INPUT, OUTPUT> {

    private static final WeakIdentityHashMap<Object,
            SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>
            sCallbackMap =
            new WeakIdentityHashMap<Object,
                    SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>();

    private final Object[] mArgs;

    private final CacheStrategyType mCacheStrategyType;

    private final ClashResolutionType mClashResolutionType;

    private final Constructor<? extends ContextInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final int mLoaderId;

    private final Logger mLogger;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param context        the context reference.
     * @param loaderId       the loader ID.
     * @param resolutionType the clash resolution type.
     * @param strategyType   the result cache strategy type.
     * @param constructor    the invocation constructor.
     * @param args           the invocation constructor arguments.
     * @param order          the input data order.
     * @param logger         the logger instance.
     * @throws java.lang.NullPointerException if any of the specified non-null parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    LoaderInvocation(@Nonnull final WeakReference<Object> context, final int loaderId,
            @Nullable final ClashResolutionType resolutionType,
            @Nullable final CacheStrategyType strategyType,
            @Nonnull final Constructor<? extends ContextInvocation<INPUT, OUTPUT>> constructor,
            @Nonnull final Object[] args, @Nullable final OrderType order,
            @Nonnull final Logger logger) {

        if (context == null) {

            throw new NullPointerException("the context reference must not be null");
        }

        if (constructor == null) {

            throw new NullPointerException("the invocation constructor must not be null");
        }

        if (args == null) {

            throw new NullPointerException(
                    "the invocation constructor array of arguments must not be null");
        }

        mContext = context;
        mLoaderId = loaderId;
        mClashResolutionType =
                (resolutionType == null) ? ClashResolutionType.ABORT_THAT_INPUT : resolutionType;
        mCacheStrategyType = (strategyType == null) ? CacheStrategyType.CLEAR : strategyType;
        mConstructor = constructor;
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

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            loaderManager = activity.getLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.removeAt(i);
                continue;
            }

            final RoutineLoader<?, ?> loader = callbacks.mLoader;

            if ((loaderId == callbackArray.keyAt(i)) && (loader.getInvocationCount() == 0)) {

                loaderManager.destroyLoader(loaderId);
                callbackArray.removeAt(i);
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
     * @param context         the context.
     * @param loaderId        the loader ID.
     * @param invocationClass the invocation class.
     * @param invocationArgs  the invocation constructor arguments.
     * @param inputs          the invocation inputs.
     */
    @SuppressWarnings("unchecked")
    static void purgeLoader(@Nonnull final Object context, final int loaderId,
            @Nonnull final Class<?> invocationClass, @Nonnull final Object[] invocationArgs,
            @Nonnull final List<?> inputs) {

        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                sCallbackMap.get(context);

        if (callbackArray == null) {

            return;
        }

        final LoaderManager loaderManager;

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            loaderManager = activity.getLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.removeAt(i);
                continue;
            }

            final RoutineLoader<Object, Object> loader =
                    (RoutineLoader<Object, Object>) callbacks.mLoader;

            if ((loader.getInvocationType() == invocationClass) && Arrays.equals(
                    loader.getInvocationArgs(), invocationArgs) && (loader.getInvocationCount()
                    == 0)) {

                final int id = callbackArray.keyAt(i);

                if (((loaderId == ContextInvocationConfiguration.AUTO) || (loaderId == id))
                        && loader.areSameInputs(inputs)) {

                    loaderManager.destroyLoader(id);
                    callbackArray.removeAt(i);
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

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            loaderManager = activity.getLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.removeAt(i);
                continue;
            }

            final RoutineLoader<Object, Object> loader =
                    (RoutineLoader<Object, Object>) callbacks.mLoader;

            if ((loader.getInvocationCount() == 0) && (loaderId == callbackArray.keyAt(i)) && loader
                    .areSameInputs(inputs)) {

                loaderManager.destroyLoader(loaderId);
                callbackArray.removeAt(i);
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
     * @param context         the context.
     * @param loaderId        the loader ID.
     * @param invocationClass the invocation class.
     * @param invocationArgs  the invocation constructor arguments.
     */
    static void purgeLoaders(@Nonnull final Object context, final int loaderId,
            @Nonnull final Class<?> invocationClass, @Nonnull final Object[] invocationArgs) {

        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                sCallbackMap.get(context);

        if (callbackArray == null) {

            return;
        }

        final LoaderManager loaderManager;

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            loaderManager = activity.getLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        int i = 0;

        while (i < callbackArray.size()) {

            final RoutineLoaderCallbacks<?> callbacks = callbackArray.valueAt(i).get();

            if (callbacks == null) {

                callbackArray.removeAt(i);
                continue;
            }

            final RoutineLoader<?, ?> loader = callbacks.mLoader;

            if ((loader.getInvocationType() == invocationClass) && Arrays.equals(
                    loader.getInvocationArgs(), invocationArgs) && (loader.getInvocationCount()
                    == 0)) {

                final int id = callbackArray.keyAt(i);

                if ((loaderId == ContextInvocationConfiguration.AUTO) || (loaderId == id)) {

                    loaderManager.destroyLoader(id);
                    callbackArray.removeAt(i);
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
        }

        return object.hashCode();
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

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            loaderContext = activity.getApplicationContext();
            loaderManager = activity.getLoaderManager();
            logger.dbg("running invocation bound to activity: %s", activity);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderContext = fragment.getActivity().getApplicationContext();
            loaderManager = fragment.getLoaderManager();
            logger.dbg("running invocation bound to fragment: %s", fragment);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        int loaderId = mLoaderId;

        if (loaderId == ContextInvocationConfiguration.AUTO) {

            loaderId = mConstructor.getDeclaringClass().hashCode();

            for (final Object arg : mArgs) {

                loaderId = 31 * loaderId + recursiveHashCode(arg);
            }

            loaderId = 31 * loaderId + inputs.hashCode();
            logger.dbg("generating invocation ID: %d", loaderId);
        }

        final Loader<InvocationResult<OUTPUT>> loader = loaderManager.getLoader(loaderId);
        final boolean isClash = isClash(loader, loaderId, inputs);
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

        if ((callbacks == null) || (loader == null) || isClash) {

            final RoutineLoader<INPUT, OUTPUT> routineLoader;

            if (!isClash && (loader != null) && (loader.getClass() == RoutineLoader.class)) {

                routineLoader = (RoutineLoader<INPUT, OUTPUT>) loader;

            } else {

                routineLoader = null;
            }

            final RoutineLoaderCallbacks<OUTPUT> newCallbacks =
                    createCallbacks(loaderContext, loaderManager, routineLoader, inputs, loaderId);

            if (callbacks != null) {

                logger.dbg("resetting existing callbacks [%d]", loaderId);
                callbacks.reset();
            }

            callbackArray.put(loaderId, new WeakReference<RoutineLoaderCallbacks<?>>(newCallbacks));
            callbacks = newCallbacks;
        }

        logger.dbg("setting result cache type [%d]: %s", loaderId, mCacheStrategyType);
        callbacks.setCacheStrategy(mCacheStrategyType);

        final OutputChannel<OUTPUT> outputChannel = callbacks.newChannel();

        if (isClash) {

            logger.dbg("restarting loader [%d]", loaderId);
            loaderManager.restartLoader(loaderId, Bundle.EMPTY, callbacks);

        } else {

            logger.dbg("initializing loader [%d]", loaderId);
            loaderManager.initLoader(loaderId, Bundle.EMPTY, callbacks);
        }

        result.pass(outputChannel);
    }

    private RoutineLoaderCallbacks<OUTPUT> createCallbacks(@Nonnull final Context loaderContext,
            @Nonnull final LoaderManager loaderManager,
            @Nullable final RoutineLoader<INPUT, OUTPUT> loader,
            @Nonnull final List<? extends INPUT> inputs, final int loaderId) {

        final Logger logger = mLogger;
        final Object[] args = mArgs;
        final Constructor<? extends ContextInvocation<INPUT, OUTPUT>> constructor = mConstructor;
        final ContextInvocation<INPUT, OUTPUT> invocation;

        try {

            logger.dbg("creating a new instance of class [%d]: %s", loaderId,
                       constructor.getDeclaringClass());
            invocation = constructor.newInstance(args);
            invocation.onContext(loaderContext.getApplicationContext());

        } catch (final RoutineException e) {

            logger.err(e, "error creating the invocation instance [%d]", loaderId);
            throw e;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance [%d]", loaderId);
            throw new InvocationException(t);
        }

        final RoutineLoader<INPUT, OUTPUT> callbacksLoader = (loader != null) ? loader
                : new RoutineLoader<INPUT, OUTPUT>(loaderContext, invocation, args, inputs,
                                                   mOrderType, logger);
        return new RoutineLoaderCallbacks<OUTPUT>(loaderManager, callbacksLoader, logger);
    }

    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    private boolean isClash(@Nullable final Loader<InvocationResult<OUTPUT>> loader,
            final int loaderId, @Nonnull final List<? extends INPUT> inputs) {

        if (loader == null) {

            return false;
        }

        final Logger logger = mLogger;

        if (loader.getClass() != RoutineLoader.class) {

            logger.err("clashing invocation ID [%d]: %s", loaderId,
                       loader.getClass().getCanonicalName());
            throw new InvocationClashException(loaderId);
        }

        final RoutineLoader<INPUT, OUTPUT> routineLoader = (RoutineLoader<INPUT, OUTPUT>) loader;
        final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass =
                mConstructor.getDeclaringClass();

        if ((new ClassToken<MissingLoaderInvocation<INPUT, OUTPUT>>() {}.getRawClass()
                != invocationClass) && ((routineLoader.getInvocationType() != invocationClass)
                || !Arrays.equals(routineLoader.getInvocationArgs(), mArgs))) {

            logger.wrn("clashing invocation ID [%d]: %s", loaderId,
                       routineLoader.getInvocationType().getCanonicalName());
            throw new InvocationClashException(loaderId);
        }

        final ClashResolutionType resolution = mClashResolutionType;

        if (resolution == ClashResolutionType.ABORT_THAT) {

            logger.dbg("restarting existing invocation [%d]", loaderId);
            return true;

        } else if (resolution == ClashResolutionType.ABORT_THIS) {

            logger.dbg("aborting invocation [%d]", loaderId);
            throw new InputClashException(loaderId);

        } else if ((resolution == ClashResolutionType.KEEP_THAT) || routineLoader.areSameInputs(
                inputs)) {

            logger.dbg("keeping existing invocation [%d]", loaderId);
            return false;

        } else if (resolution == ClashResolutionType.ABORT_THAT_INPUT) {

            logger.dbg("restarting existing invocation [%d]", loaderId);
            return true;

        } else if (resolution == ClashResolutionType.ABORT_THIS_INPUT) {

            logger.dbg("aborting invocation [%d]", loaderId);
            throw new InputClashException(loaderId);
        }

        return true;
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

        private final ArrayList<StandaloneInput<OUTPUT>> mChannels =
                new ArrayList<StandaloneInput<OUTPUT>>();

        private final RoutineLoader<?, OUTPUT> mLoader;

        private final LoaderManager mLoaderManager;

        private final Logger mLogger;

        private final ArrayList<StandaloneInput<OUTPUT>> mNewChannels =
                new ArrayList<StandaloneInput<OUTPUT>>();

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
         * @return the new output channel.
         */
        @Nonnull
        public OutputChannel<OUTPUT> newChannel() {

            final Logger logger = mLogger;
            logger.dbg("creating new result channel");
            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;
            final ArrayList<StandaloneInput<OUTPUT>> channels = mNewChannels;
            final RoutineConfiguration configuration = builder().withOutputSize(Integer.MAX_VALUE)
                                                                .withOutputTimeout(
                                                                        TimeDuration.ZERO)
                                                                .withLog(logger.getLog())
                                                                .withLogLevel(logger.getLogLevel())
                                                                .buildConfiguration();
            final StandaloneChannel<OUTPUT> channel =
                    JRoutine.standalone().configure(configuration).buildChannel();
            channels.add(channel.input());
            internalLoader.setInvocationCount(
                    Math.max(channels.size(), internalLoader.getInvocationCount()));
            return channel.output();
        }

        public Loader<InvocationResult<OUTPUT>> onCreateLoader(final int id, final Bundle args) {

            mLogger.dbg("creating Android loader: %d", id);
            return mLoader;
        }

        public void onLoadFinished(final Loader<InvocationResult<OUTPUT>> loader,
                final InvocationResult<OUTPUT> data) {

            final Logger logger = mLogger;
            final ArrayList<StandaloneInput<OUTPUT>> channels = mChannels;
            final ArrayList<StandaloneInput<OUTPUT>> newChannels = mNewChannels;
            logger.dbg("dispatching invocation result: %s", data);

            if (data.passTo(newChannels, channels)) {

                final ArrayList<StandaloneInput<OUTPUT>> channelsToClose =
                        new ArrayList<StandaloneInput<OUTPUT>>(channels);
                channelsToClose.addAll(newChannels);
                mResultCount += channels.size() + newChannels.size();
                channels.clear();
                newChannels.clear();
                final RoutineLoader<?, OUTPUT> internalLoader = mLoader;

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

                    for (final StandaloneInput<OUTPUT> channel : channelsToClose) {

                        channel.abort(exception);
                    }

                } else {

                    for (final StandaloneInput<OUTPUT> channel : channelsToClose) {

                        channel.close();
                    }
                }

            } else {

                channels.addAll(newChannels);
                newChannels.clear();
            }
        }

        public void onLoaderReset(final Loader<InvocationResult<OUTPUT>> loader) {

            mLogger.dbg("resetting Android loader: %d", mLoader.getId());
            reset();
        }

        private void reset() {

            mLogger.dbg("aborting result channels");
            final ArrayList<StandaloneInput<OUTPUT>> channels = mChannels;
            final ArrayList<StandaloneInput<OUTPUT>> newChannels = mNewChannels;
            final InvocationClashException reason = new InvocationClashException(mLoader.getId());

            for (final InputChannel<OUTPUT> channel : channels) {

                channel.abort(reason);
            }

            channels.clear();

            for (final InputChannel<OUTPUT> newChannel : newChannels) {

                newChannel.abort(reason);
            }

            newChannels.clear();
        }

        private void setCacheStrategy(@Nonnull final CacheStrategyType strategyType) {

            mLogger.dbg("setting cache type: %s", strategyType);
            mCacheStrategyType = strategyType;
        }
    }
}
