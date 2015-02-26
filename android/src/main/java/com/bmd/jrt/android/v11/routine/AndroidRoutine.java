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
import android.content.Context;
import android.os.Build.VERSION_CODES;

import com.bmd.jrt.android.builder.AndroidRoutineBuilder.CacheStrategy;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfiguration.OrderBy;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.common.InvocationInterruptedException;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.AbstractRoutine;
import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
@TargetApi(VERSION_CODES.HONEYCOMB)
class AndroidRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Runner mAsyncRunner;

    private final CacheStrategy mCacheStrategy;

    private final ClashResolution mClashResolution;

    private final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private final int mLoaderId;

    private final OrderBy mOrderBy;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     * @param context       the context reference.
     * @param loaderId      the loader ID.
     * @param resolution    the clash resolution type.
     * @param cacheStrategy the result cache type.
     * @param constructor   the invocation constructor.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     * @throws java.lang.NullPointerException     if any of the specified non-null parameter is
     *                                            null.
     */
    @SuppressWarnings("ConstantConditions")
    AndroidRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final WeakReference<Object> context, final int loaderId,
            @Nullable final ClashResolution resolution, @Nullable final CacheStrategy cacheStrategy,
            @Nonnull final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor) {

        super(configuration);

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (constructor == null) {

            throw new NullPointerException("the invocation constructor must not be null");
        }

        mContext = context;
        mLoaderId = loaderId;
        mClashResolution = (resolution == null) ? ClashResolution.ABORT_THAT_INPUT : resolution;
        mCacheStrategy = (cacheStrategy == null) ? CacheStrategy.CLEAR : cacheStrategy;
        mConstructor = constructor;
        mOrderBy = configuration.getOutputOrderOr(null);
        mAsyncRunner = configuration.getRunnerOr(null);
    }

    @Override
    public void purge() {

        super.purge();

        final Object context = mContext.get();

        if (context != null) {

            mAsyncRunner.run(new PurgeExecution(context, mConstructor.getDeclaringClass()), 0,
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

            throw e.interrupt();

        } catch (final Throwable t) {

            getLogger().wrn(t, "ignoring exception while destroying invocation instance");
        }

        return createInvocation(async);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> createInvocation(final boolean async) {

        final Logger logger = getLogger();

        if (async) {

            return new LoaderInvocation<INPUT, OUTPUT>(mContext, mLoaderId, mClashResolution,
                                                       mCacheStrategy, mConstructor, mOrderBy,
                                                       logger);
        }

        final Object context = mContext.get();

        if (context == null) {

            throw new IllegalStateException("routine context has been destroyed");
        }

        final Context appContext;

        if (context instanceof Activity) {

            final Activity activity = (Activity) context;
            appContext = activity.getApplicationContext();

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            appContext = fragment.getActivity().getApplicationContext();

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        try {

            final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor =
                    mConstructor;
            logger.dbg("creating a new instance of class: %s", constructor.getDeclaringClass());
            final AndroidInvocation<INPUT, OUTPUT> invocation = constructor.newInstance();
            invocation.onContext(appContext);
            return invocation;

        } catch (final InvocationTargetException e) {

            logger.err(e, "error creating the invocation instance");
            throw new InvocationException(e.getCause());

        } catch (final InvocationInterruptedException e) {

            logger.err(e, "error creating the invocation instance");
            throw e.interrupt();

        } catch (final RoutineException e) {

            logger.err(e, "error creating the invocation instance");
            throw e;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance");
            throw new InvocationException(t);
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation class.
     */
    private static class PurgeExecution implements Execution {

        private final Object mContext;

        private final Class<?> mInvocationClass;

        /**
         * Constructor.
         *
         * @param context         the context.
         * @param invocationClass the invocation class.
         */
        private PurgeExecution(@Nonnull final Object context,
                @Nonnull final Class<?> invocationClass) {

            mContext = context;
            mInvocationClass = invocationClass;
        }

        @Override
        public void run() {

            LoaderInvocation.purgeLoaders(mContext, mInvocationClass);
        }
    }
}
