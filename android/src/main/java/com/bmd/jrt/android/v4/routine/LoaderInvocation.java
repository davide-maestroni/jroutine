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

import com.bmd.jrt.android.invocator.RoutineInvocator;
import com.bmd.jrt.android.invocator.RoutineInvocator.ClashResolution;
import com.bmd.jrt.android.invocator.RoutineInvocator.InvocationCachePolicy;
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

    private final InvocationCachePolicy mCachePolicy;

    private final Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final int mLoaderId;

    private final ClashResolution mResolution;

    /**
     * Constructor.
     *
     * @param context     the context reference.
     * @param loaderId    the loader ID.
     * @param resolution  the clash resolution type.
     * @param cachePolicy the cache policy type.
     * @param constructor the invocation constructor.
     * @throws NullPointerException if one of the specified parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    LoaderInvocation(@Nonnull final WeakReference<Object> context, final int loaderId,
            @Nonnull final ClashResolution resolution,
            @Nonnull final InvocationCachePolicy cachePolicy,
            @Nonnull final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor) {

        if (context == null) {

            throw new NullPointerException("the context reference must not be null");
        }

        if (resolution == null) {

            throw new NullPointerException("the clash resolution type must not be null");
        }

        if (cachePolicy == null) {

            throw new NullPointerException("the cache policy type must not be null");
        }

        if (constructor == null) {

            throw new NullPointerException("the invocation constructor must not be null");
        }

        mContext = context;
        mLoaderId = loaderId;
        mResolution = resolution;
        mCachePolicy = cachePolicy;
        mConstructor = constructor;
    }

    @Override
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
            loaderContext = activity;
            loaderManager = activity.getSupportLoaderManager();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            loaderContext = fragment.getActivity();
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

            loaderId = constructor.getDeclaringClass().hashCode() + inputs.hashCode();
        }

        final Loader<InvocationResult<OUTPUT>> loader = loaderManager.getLoader(loaderId);
        RoutineLoader<INPUT, OUTPUT> routineLoader = null;
        boolean isRestart = false;

        if (loader != null) {

            if (loader.getClass() != RoutineLoader.class) {

                throw new IllegalStateException("invalid loader with ID=" + loaderId + ": " + loader
                        .getClass()
                        .getCanonicalName());
            }

            final ClashResolution resolution = mResolution;

            if (resolution == ClashResolution.RESET) {

                loaderManager.destroyLoader(loaderId);

            } else {

                routineLoader = (RoutineLoader<INPUT, OUTPUT>) loader;
                isRestart = (resolution != ClashResolution.KEEP) && !routineLoader.areSameInputs(
                        inputs);
            }
        }

        final CacheHashMap<Object, SparseArray<RoutineLoaderCallbacks<?>>> callbackMap =
                sCallbackMap;
        SparseArray<RoutineLoaderCallbacks<?>> callbackArray = callbackMap.get(context);

        if (callbackArray == null) {

            callbackArray = new SparseArray<RoutineLoaderCallbacks<?>>();
            callbackMap.put(context, callbackArray);
        }

        RoutineLoaderCallbacks<?> callbacks = callbackArray.get(loaderId);

        if (callbacks == null) {

            if (routineLoader == null) {

                final Invocation<INPUT, OUTPUT> invocation;

                try {

                    invocation = constructor.newInstance();

                } catch (final InvocationTargetException e) {

                    throw new RoutineException(e.getCause());

                } catch (final RoutineException e) {

                    throw e;

                } catch (final Throwable t) {

                    throw new RoutineException(t);
                }

                routineLoader = new RoutineLoader<INPUT, OUTPUT>(loaderContext, invocation, inputs);
            }

            callbacks =
                    new RoutineLoaderCallbacks<OUTPUT>(loaderManager, routineLoader, mCachePolicy);
            callbackArray.put(loaderId, callbacks);
        }

        if (isRestart) {

            loaderManager.restartLoader(loaderId, Bundle.EMPTY, callbacks);

        } else {

            loaderManager.initLoader(loaderId, Bundle.EMPTY, callbacks);
        }

        result.pass((OutputChannel<OUTPUT>) callbacks.newChannel());
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

        private final InvocationCachePolicy mCachePolicy;

        private final ArrayList<IOChannel<OUTPUT>> mChannels = new ArrayList<IOChannel<OUTPUT>>();

        private final RoutineLoader<?, OUTPUT> mLoader;

        private final LoaderManager mLoaderManager;

        private InvocationResult<OUTPUT> mResult;

        private int mResultCount;

        /**
         * Constructor.
         *
         * @param loaderManager the loader manager.
         * @param loader        the loader instance.
         * @param cachePolicy   the cache policy type.
         */
        private RoutineLoaderCallbacks(@Nonnull final LoaderManager loaderManager,
                @Nonnull final RoutineLoader<?, OUTPUT> loader,
                @Nonnull final InvocationCachePolicy cachePolicy) {

            mLoaderManager = loaderManager;
            mLoader = loader;
            mCachePolicy = cachePolicy;
        }

        /**
         * Creates and returns a new output channel.<br/>
         * The channel will be used to deliver the loader results.
         *
         * @return the new output channel.
         */
        @Nonnull
        public OutputChannel<OUTPUT> newChannel() {

            final InvocationResult<OUTPUT> result = mResult;
            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;
            final IOChannel<OUTPUT> channel = JRoutine.io().buildChannel();

            if (result == null) {

                final ArrayList<IOChannel<OUTPUT>> channels = mChannels;
                channels.add(channel);
                internalLoader.setInvocationCount(
                        Math.max(channels.size(), internalLoader.getInvocationCount()));

                return channel.output();
            }

            final IOChannelInput<OUTPUT> input = channel.input();
            result.passTo(input);
            input.close();

            ++mResultCount;

            checkComplete();

            return channel.output();
        }

        @Override
        public Loader<InvocationResult<OUTPUT>> onCreateLoader(final int id, final Bundle args) {

            return mLoader;
        }

        @Override
        public void onLoadFinished(final Loader<InvocationResult<OUTPUT>> loader,
                final InvocationResult<OUTPUT> result) {

            mResult = result;

            final ArrayList<IOChannel<OUTPUT>> channels = mChannels;

            for (final IOChannel<OUTPUT> channel : channels) {

                final IOChannelInput<OUTPUT> input = channel.input();
                result.passTo(input);
                input.close();
            }

            mResultCount += channels.size();
            channels.clear();

            checkComplete();
        }

        @Override
        public void onLoaderReset(final Loader<InvocationResult<OUTPUT>> loader) {

            mResult = null;
        }

        private void checkComplete() {

            final RoutineLoader<?, OUTPUT> internalLoader = mLoader;

            if (mResultCount >= internalLoader.getInvocationCount()) {

                final InvocationCachePolicy cachePolicy = mCachePolicy;

                if ((cachePolicy == InvocationCachePolicy.CLEAR) || (mResult.isError() ? (
                        cachePolicy == InvocationCachePolicy.CLEAR_IF_ERROR)
                        : (cachePolicy == InvocationCachePolicy.CLEAR_IF_RESULT))) {

                    mLoaderManager.destroyLoader(internalLoader.getId());
                }
            }
        }
    }
}
