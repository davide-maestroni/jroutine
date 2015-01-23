/**
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
package com.bmd.jrt.android.v11.routine;

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

import com.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ResultCache;
import com.bmd.jrt.android.builder.InputClashException;
import com.bmd.jrt.android.builder.InvocationClashException;
import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.channel.Tunnel;
import com.bmd.jrt.channel.Tunnel.TunnelInput;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.common.InvocationInterruptedException;
import com.bmd.jrt.invocation.SimpleInvocation;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Invocation implementation employing loaders to perform background operations.
 * <p/>
 * Created by davide on 12/11/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
class LoaderInvocation<INPUT, OUTPUT> extends SimpleInvocation<INPUT, OUTPUT> {

    private static final CacheHashMap<Object, SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>
            sCallbackMap =
            new CacheHashMap<Object, SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>();

    private final ResultCache mCacheType;

    private final ClashResolution mClashResolution;

    private final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final DataOrder mDataOrder;

    private final int mLoaderId;

    private final Logger mLogger;

    /**
     * Constructor.
     *
     * @param context     the context reference.
     * @param loaderId    the loader ID.
     * @param resolution  the clash resolution type.
     * @param cacheType   the result cache type.
     * @param constructor the invocation constructor.
     * @param order       the input data order.
     * @param logger      the logger instance.
     * @throws NullPointerException if any of the specified parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    LoaderInvocation(@Nonnull final WeakReference<Object> context, final int loaderId,
            @Nonnull final ClashResolution resolution, @Nonnull final ResultCache cacheType,
            @Nonnull final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor,
            @Nonnull final DataOrder order, @Nonnull final Logger logger) {

        if (context == null) {

            throw new NullPointerException("the context reference must not be null");
        }

        if (resolution == null) {

            throw new NullPointerException("the clash resolution type must not be null");
        }

        if (cacheType == null) {

            throw new NullPointerException("the result cache type must not be null");
        }

        if (constructor == null) {

            throw new NullPointerException("the invocation constructor must not be null");
        }

        if (order == null) {

            throw new NullPointerException("the data order must not be null");
        }

        mContext = context;
        mLoaderId = loaderId;
        mClashResolution = resolution;
        mCacheType = cacheType;
        mConstructor = constructor;
        mDataOrder = order;
        mLogger = logger.subContextLogger(this);
    }

    /**
     * Enables routine invocation for the specified activity.<br/>
     * This method must be called in the activity <code>onCreate()</code> method.
     *
     * @param activity the activity instance.
     * @throws NullPointerException if the specified activity is null.
     */
    @SuppressWarnings("ConstantConditions")
    static void initActivity(@Nonnull final Activity activity) {

        if (activity == null) {

            throw new NullPointerException("the activity instance must not be null");
        }

        synchronized (sCallbackMap) {

            if (!sCallbackMap.containsKey(activity)) {

                sCallbackMap.put(activity,
                                 new SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>());
            }

            activity.getLoaderManager();
        }
    }

    /**
     * Enables routine invocation for the specified fragment.<br/>
     * This method must be called in the fragment <code>onCreate()</code> method.
     *
     * @param fragment the fragment instance.
     * @throws NullPointerException if the specified fragment is null.
     */
    @SuppressWarnings("ConstantConditions")
    static void initFragment(@Nonnull final Fragment fragment) {

        if (fragment == null) {

            throw new NullPointerException("the fragment instance must not be null");
        }

        synchronized (sCallbackMap) {

            if (!sCallbackMap.containsKey(fragment)) {

                sCallbackMap.put(fragment,
                                 new SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>());
            }

            fragment.getLoaderManager();
        }
    }

    /**
     * Checks if the specified activity is enabled for routine invocation.
     *
     * @param activity the activity instance.
     * @return whether the activity is enabled.
     * @throws NullPointerException if the specified activity is null.
     */
    @SuppressWarnings("ConstantConditions")
    static boolean isEnabled(@Nonnull final Activity activity) {

        if (activity == null) {

            throw new NullPointerException("the activity instance must not be null");
        }

        synchronized (sCallbackMap) {

            return sCallbackMap.containsKey(activity);
        }
    }

    /**
     * Checks if the specified fragment is enabled for routine invocation.
     *
     * @param fragment the fragment instance.
     * @return whether the fragment is enabled.
     * @throws NullPointerException if the specified fragment is null.
     */
    @SuppressWarnings("ConstantConditions")
    static boolean isEnabled(@Nonnull final Fragment fragment) {

        if (fragment == null) {

            throw new NullPointerException("the fragment instance must not be null");
        }

        synchronized (sCallbackMap) {

            return sCallbackMap.containsKey(fragment);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
            logger.dbg("running invocation linked to activity: %s", activity);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderContext = fragment.getActivity().getApplicationContext();
            loaderManager = fragment.getLoaderManager();
            logger.dbg("running invocation linked to fragment: %s", fragment);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        int loaderId = mLoaderId;

        if (loaderId == AndroidRoutineBuilder.GENERATED_ID) {

            loaderId = 31 * mConstructor.getDeclaringClass().hashCode() + inputs.hashCode();
            logger.dbg("generating invocation ID: %d", loaderId);
        }

        boolean needsRestart = isClash(loaderManager, loaderId, inputs);
        final CacheHashMap<Object, SparseArray<WeakReference<RoutineLoaderCallbacks<?>>>>
                callbackMap = sCallbackMap;
        final SparseArray<WeakReference<RoutineLoaderCallbacks<?>>> callbackArray =
                callbackMap.get(context);

        final WeakReference<RoutineLoaderCallbacks<?>> callbackReference =
                callbackArray.get(loaderId);
        RoutineLoaderCallbacks<OUTPUT> callbacks = (callbackReference != null)
                ? (RoutineLoaderCallbacks<OUTPUT>) callbackReference.get() : null;

        if ((callbacks == null) || needsRestart) {

            final RoutineLoaderCallbacks<OUTPUT> newCallbacks =
                    createCallbacks(loaderContext, loaderManager, inputs, loaderId);

            if (callbacks != null) {

                logger.dbg("resetting existing callbacks [%d]", loaderId);
                callbacks.reset();
            }

            callbackArray.put(loaderId, new WeakReference<RoutineLoaderCallbacks<?>>(newCallbacks));
            callbacks = newCallbacks;
            needsRestart = true;
        }

        logger.dbg("setting result cache type [%d]: %s", loaderId, mCacheType);
        callbacks.setCacheType(mCacheType);

        final OutputChannel<OUTPUT> outputChannel = callbacks.newChannel();

        if (needsRestart) {

            logger.dbg("restarting loader [%d]", loaderId);
            loaderManager.restartLoader(loaderId, Bundle.EMPTY, callbacks);

        } else {

            logger.dbg("initializing loader [%d]", loaderId);
            loaderManager.initLoader(loaderId, Bundle.EMPTY, callbacks);
        }

        result.pass(outputChannel);
    }

    private RoutineLoaderCallbacks<OUTPUT> createCallbacks(@Nonnull final Context loaderContext,
            @Nonnull final LoaderManager loaderManager, @Nonnull final List<? extends INPUT> inputs,
            final int loaderId) {

        final Logger logger = mLogger;
        final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor = mConstructor;
        final AndroidInvocation<INPUT, OUTPUT> invocation;

        try {

            logger.dbg("creating a new instance of class [%d]: %s", loaderId,
                       constructor.getDeclaringClass());
            invocation = constructor.newInstance();
            invocation.onContext(loaderContext.getApplicationContext());

        } catch (final InvocationTargetException e) {

            logger.err(e, "error creating the invocation instance [%d]", loaderId);
            throw new InvocationException(e.getCause());

        } catch (final InvocationInterruptedException e) {

            logger.err(e, "error creating the invocation instance");
            throw e.interrupt();

        } catch (final InvocationException e) {

            logger.err(e, "error creating the invocation instance [%d]", loaderId);
            throw e;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance [%d]", loaderId);
            throw new InvocationException(t);
        }

        final RoutineLoader<INPUT, OUTPUT> routineLoader =
                new RoutineLoader<INPUT, OUTPUT>(loaderContext, invocation, inputs, mDataOrder,
                                                 logger);
        return new RoutineLoaderCallbacks<OUTPUT>(loaderManager, routineLoader, logger);
    }

    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
                        justification = "class comparison with == is done")
    private boolean isClash(@Nonnull final LoaderManager loaderManager, final int loaderId,
            @Nonnull final List<? extends INPUT> inputs) {

        final Logger logger = mLogger;
        final Loader<InvocationResult<OUTPUT>> loader = loaderManager.getLoader(loaderId);

        if (loader != null) {

            if (loader.getClass() != RoutineLoader.class) {

                logger.err("clashing invocation ID [%d]: %s", loaderId,
                           loader.getClass().getCanonicalName());
                throw new InvocationClashException(loaderId);
            }

            final RoutineLoader<INPUT, OUTPUT> routineLoader =
                    (RoutineLoader<INPUT, OUTPUT>) loader;
            final Class<? extends AndroidInvocation<INPUT, OUTPUT>> invocationClass =
                    mConstructor.getDeclaringClass();

            if ((new ClassToken<MissingLoaderInvocation<INPUT, OUTPUT>>() {}.getRawClass()
                    != invocationClass) && (routineLoader.getInvocationType() != invocationClass)) {

                logger.wrn("clashing invocation ID [%d]: %s", loaderId,
                           routineLoader.getInvocationType().getCanonicalName());
                throw new InvocationClashException(loaderId);
            }

            final ClashResolution resolution = mClashResolution;

            if (resolution == ClashResolution.RESTART) {

                logger.dbg("restarting existing invocation [%d]", loaderId);
                return true;

            } else if (resolution == ClashResolution.ABORT) {

                logger.dbg("aborting invocation invocation [%d]", loaderId);
                throw new InputClashException(loaderId);

            } else if ((resolution == ClashResolution.KEEP) || routineLoader.areSameInputs(
                    inputs)) {

                logger.dbg("keeping existing invocation [%d]", loaderId);
                return false;

            } else if (resolution == ClashResolution.RESTART_ON_INPUT) {

                logger.dbg("restarting existing invocation [%d]", loaderId);
                return true;

            } else if (resolution == ClashResolution.ABORT_ON_INPUT) {

                logger.dbg("aborting invocation invocation [%d]", loaderId);
                throw new InputClashException(loaderId);
            }
        }

        return true;
    }

    /**
     * Loader callbacks implementation.<br/>
     * The callbacks object will make sure that the loader results are passed to the output channels
     * returned.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class RoutineLoaderCallbacks<OUTPUT>
            implements LoaderCallbacks<InvocationResult<OUTPUT>> {

        private final ArrayList<TunnelInput<OUTPUT>> mChannels =
                new ArrayList<TunnelInput<OUTPUT>>();

        private final RoutineLoader<?, OUTPUT> mLoader;

        private final LoaderManager mLoaderManager;

        private final Logger mLogger;

        private final ArrayList<TunnelInput<OUTPUT>> mNewChannels =
                new ArrayList<TunnelInput<OUTPUT>>();

        private ResultCache mCacheType;

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
            final ArrayList<TunnelInput<OUTPUT>> channels = mNewChannels;
            final Tunnel<OUTPUT> tunnel = JRoutine.io()
                                                  .maxSize(Integer.MAX_VALUE)
                                                  .bufferTimeout(TimeDuration.ZERO)
                                                  .loggedWith(logger.getLog())
                                                  .logLevel(logger.getLogLevel())
                                                  .buildTunnel();
            channels.add(tunnel.input());
            internalLoader.setInvocationCount(
                    Math.max(channels.size(), internalLoader.getInvocationCount()));
            return tunnel.output();
        }

        @Override
        public Loader<InvocationResult<OUTPUT>> onCreateLoader(final int id, final Bundle args) {

            mLogger.dbg("creating Android loader: %d", id);
            return mLoader;
        }

        @Override
        public void onLoadFinished(final Loader<InvocationResult<OUTPUT>> loader,
                final InvocationResult<OUTPUT> data) {

            final Logger logger = mLogger;
            final ArrayList<TunnelInput<OUTPUT>> channels = mChannels;
            final ArrayList<TunnelInput<OUTPUT>> newChannels = mNewChannels;

            logger.dbg("dispatching invocation result: %s", data);

            if (data.passTo(newChannels, channels)) {

                final ArrayList<TunnelInput<OUTPUT>> channelsToClose =
                        new ArrayList<TunnelInput<OUTPUT>>(channels);
                channelsToClose.addAll(newChannels);

                mResultCount += channels.size() + newChannels.size();
                channels.clear();
                newChannels.clear();

                final RoutineLoader<?, OUTPUT> internalLoader = mLoader;

                if (mResultCount >= internalLoader.getInvocationCount()) {

                    mResultCount = 0;
                    internalLoader.setInvocationCount(0);

                    final ResultCache cacheType = mCacheType;

                    if ((cacheType == ResultCache.CLEAR) || (data.isError() ? (cacheType
                            == ResultCache.RETAIN_RESULT)
                            : (cacheType == ResultCache.RETAIN_ERROR))) {

                        final int id = internalLoader.getId();
                        logger.dbg("destroying Android loader: %d", id);
                        mLoaderManager.destroyLoader(id);
                    }
                }

                if (data.isError()) {

                    final Throwable exception = data.getAbortException();

                    for (final TunnelInput<OUTPUT> channel : channelsToClose) {

                        channel.abort(exception);
                    }

                } else {

                    for (final TunnelInput<OUTPUT> channel : channelsToClose) {

                        channel.close();
                    }
                }

            } else {

                channels.addAll(newChannels);
                newChannels.clear();
            }
        }

        @Override
        public void onLoaderReset(final Loader<InvocationResult<OUTPUT>> loader) {

            mLogger.dbg("resetting Android loader: %d", mLoader.getId());
            reset();
        }

        private void reset() {

            mLogger.dbg("aborting result channels");
            final ArrayList<TunnelInput<OUTPUT>> channels = mChannels;
            final ArrayList<TunnelInput<OUTPUT>> newChannels = mNewChannels;
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

        private void setCacheType(@Nonnull final ResultCache cacheType) {

            mLogger.dbg("setting cache type: %s", cacheType);
            mCacheType = cacheType;
        }
    }
}
