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
package com.bmd.jrt.android.v4.routine;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.SparseArray;

import com.bmd.jrt.android.invocator.InputClashException;
import com.bmd.jrt.android.invocator.RoutineClashException;
import com.bmd.jrt.android.invocator.RoutineInvocator;
import com.bmd.jrt.android.invocator.RoutineInvocator.ClashResolution;
import com.bmd.jrt.android.invocator.RoutineInvocator.ResultCache;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.IOChannel.IOChannelInput;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.invocation.SimpleInvocation;

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
class LoaderInvocation<INPUT, OUTPUT> extends SimpleInvocation<INPUT, OUTPUT> {

    private static final CacheHashMap<Object, SparseArray<RoutineLoaderCallbacks<?>>> sCallbackMap =
            new CacheHashMap<Object, SparseArray<RoutineLoaderCallbacks<?>>>();

    private final ResultCache mCacheType;

    private final ClashResolution mClashResolution;

    private final Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final int mLoaderId;

    /**
     * Constructor.
     *
     * @param context     the context reference.
     * @param loaderId    the loader ID.
     * @param resolution  the clash resolution type.
     * @param cacheType   the result cache type.
     * @param constructor the invocation constructor.
     * @throws NullPointerException if one of the specified parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    LoaderInvocation(@Nonnull final WeakReference<Object> context, final int loaderId,
            @Nonnull final ClashResolution resolution, @Nonnull final ResultCache cacheType,
            @Nonnull final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor) {

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

        mContext = context;
        mLoaderId = loaderId;
        mClashResolution = resolution;
        mCacheType = cacheType;
        mConstructor = constructor;
    }

    /**
     * Enables routine invocation for the specified activity.<br/>
     * This method must be called in the activity <code>onCreate()</code> method.
     *
     * @param activity the activity instance.
     * @throws NullPointerException if the specified activity is null.
     */
    @SuppressWarnings("ConstantConditions")
    static void enable(@Nonnull final FragmentActivity activity) {

        if (activity == null) {

            throw new NullPointerException("the activity instance must not be null");
        }

        if (!sCallbackMap.containsKey(activity)) {

            sCallbackMap.put(activity, new SparseArray<RoutineLoaderCallbacks<?>>());
        }

        activity.getSupportLoaderManager();
    }

    /**
     * Enables routine invocation for the specified fragment.<br/>
     * This method must be called in the fragment <code>onCreate()</code> method.
     *
     * @param fragment the fragment instance.
     * @throws NullPointerException if the specified fragment is null.
     */
    @SuppressWarnings("ConstantConditions")
    static void enable(@Nonnull final Fragment fragment) {

        if (fragment == null) {

            throw new NullPointerException("the fragment instance must not be null");
        }

        if (!sCallbackMap.containsKey(fragment)) {

            sCallbackMap.put(fragment, new SparseArray<RoutineLoaderCallbacks<?>>());
        }

        fragment.getLoaderManager();
    }

    /**
     * Checks if the specified activity is enabled for routine invocation.
     *
     * @param activity the activity instance.
     * @return whether the activity is enabled.
     */
    static boolean isEnabled(@Nonnull final FragmentActivity activity) {

        return sCallbackMap.containsKey(activity);
    }

    /**
     * Checks if the specified fragment is enabled for routine invocation.
     *
     * @param fragment the fragment instance.
     * @return whether the fragment is enabled.
     */
    static boolean isEnabled(@Nonnull final Fragment fragment) {

        return sCallbackMap.containsKey(fragment);
    }

    @Override
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
                        justification = "class comparison with == is done")
    @SuppressWarnings("unchecked")
    public void onCall(@Nonnull final List<? extends INPUT> inputs,
            @Nonnull final ResultChannel<OUTPUT> result) {

        final Object context = mContext.get();

        if (context == null) {

            return;
        }

        final Context loaderContext;
        final LoaderManager loaderManager;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            loaderContext = activity.getApplicationContext();
            loaderManager = activity.getSupportLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderContext = fragment.getActivity().getApplicationContext();
            loaderManager = fragment.getLoaderManager();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        if ((loaderContext == null) || (loaderManager == null)) {

            return;
        }

        final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor = mConstructor;
        int loaderId = mLoaderId;

        if (loaderId == RoutineInvocator.GENERATED_ID) {

            loaderId = 31 * constructor.getDeclaringClass().hashCode() + inputs.hashCode();
        }

        boolean needRestart = true;
        final Loader<InvocationResult<OUTPUT>> loader = loaderManager.getLoader(loaderId);

        if (loader != null) {

            if (loader.getClass() != RoutineLoader.class) {

                throw new IllegalStateException("invalid loader with ID=" + loaderId + ": " + loader
                        .getClass()
                        .getCanonicalName());
            }

            final RoutineLoader<INPUT, OUTPUT> routineLoader =
                    (RoutineLoader<INPUT, OUTPUT>) loader;

            if (!routineLoader.isSameInvocationType(mConstructor.getDeclaringClass())) {

                throw new RoutineClashException(loaderId);
            }

            final ClashResolution resolution = mClashResolution;

            if (resolution != ClashResolution.RESET) {

                if ((resolution == ClashResolution.KEEP) || routineLoader.areSameInputs(inputs)) {

                    needRestart = false;

                } else if (resolution == ClashResolution.ABORT) {

                    throw new InputClashException(loaderId);
                }
            }
        }

        final CacheHashMap<Object, SparseArray<RoutineLoaderCallbacks<?>>> callbackMap =
                sCallbackMap;
        final SparseArray<RoutineLoaderCallbacks<?>> callbackArray = callbackMap.get(context);

        RoutineLoaderCallbacks<OUTPUT> callbacks =
                (RoutineLoaderCallbacks<OUTPUT>) callbackArray.get(loaderId);

        if ((callbacks == null) || needRestart) {

            final Invocation<INPUT, OUTPUT> invocation;

            try {

                invocation = mConstructor.newInstance();

            } catch (final InvocationTargetException e) {

                throw new RoutineException(e.getCause());

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new RoutineException(t);
            }

            if (callbacks != null) {

                callbacks.reset();
            }

            final RoutineLoader<INPUT, OUTPUT> routineLoader =
                    new RoutineLoader<INPUT, OUTPUT>(loaderContext, invocation, inputs);
            callbacks = new RoutineLoaderCallbacks<OUTPUT>(loaderManager, routineLoader);
            callbackArray.put(loaderId, callbacks);
            needRestart = true;
        }

        callbacks.setCacheType(mCacheType);

        final OutputChannel<OUTPUT> outputChannel = callbacks.newChannel();

        if (needRestart) {

            loaderManager.restartLoader(loaderId, Bundle.EMPTY, callbacks);

        } else {

            loaderManager.initLoader(loaderId, Bundle.EMPTY, callbacks);
        }

        result.pass(outputChannel);
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

        private final ArrayList<IOChannel<OUTPUT>> mChannels = new ArrayList<IOChannel<OUTPUT>>();

        private final RoutineLoader<?, OUTPUT> mLoader;

        private final LoaderManager mLoaderManager;

        private ResultCache mCacheType;

        private int mResultCount;

        /**
         * Constructor.
         *
         * @param loaderManager the loader manager.
         * @param loader        the loader instance.
         */
        private RoutineLoaderCallbacks(@Nonnull final LoaderManager loaderManager,
                @Nonnull final RoutineLoader<?, OUTPUT> loader) {

            mLoaderManager = loaderManager;
            mLoader = loader;
        }

        /**
         * Creates and returns a new output channel.<br/>
         * The channel will be used to deliver the loader results.
         *
         * @return the new output channel.
         */
        @Nonnull
        public OutputChannel<OUTPUT> newChannel() {

            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;
            final IOChannel<OUTPUT> channel = JRoutine.io().buildChannel();
            final ArrayList<IOChannel<OUTPUT>> channels = mChannels;
            channels.add(channel);
            internalLoader.setInvocationCount(
                    Math.max(channels.size(), internalLoader.getInvocationCount()));
            return channel.output();
        }

        @Override
        public Loader<InvocationResult<OUTPUT>> onCreateLoader(final int id, final Bundle args) {

            return mLoader;
        }

        @Override
        public void onLoadFinished(final Loader<InvocationResult<OUTPUT>> loader,
                final InvocationResult<OUTPUT> result) {

            final ArrayList<IOChannel<OUTPUT>> channels = mChannels;

            for (final IOChannel<OUTPUT> channel : channels) {

                final IOChannelInput<OUTPUT> input = channel.input();
                result.passTo(input);
                input.close();
            }

            mResultCount += channels.size();
            channels.clear();

            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;

            if (mResultCount >= internalLoader.getInvocationCount()) {

                mResultCount = 0;

                final ResultCache cacheType = mCacheType;

                if ((cacheType == ResultCache.CLEAR) || (result.isError() ? (cacheType
                        == ResultCache.CLEAR_IF_ERROR)
                        : (cacheType == ResultCache.CLEAR_IF_RESULT))) {

                    mLoaderManager.destroyLoader(internalLoader.getId());
                }
            }
        }

        @Override
        public void onLoaderReset(final Loader<InvocationResult<OUTPUT>> loader) {

            reset();
        }

        private void reset() {

            final ArrayList<IOChannel<OUTPUT>> channels = mChannels;

            for (final IOChannel<OUTPUT> channel : channels) {

                channel.input().abort();
            }

            channels.clear();
        }

        private void setCacheType(@Nonnull final ResultCache cacheType) {

            mCacheType = cacheType;
        }
    }
}
