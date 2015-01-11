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

import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ResultCache;
import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.AbstractRoutine;
import com.bmd.jrt.runner.Runner;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nonnull;

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
     * @param configuration the routine configuration.
     * @param syncRunner    the runner used for synchronous invocation.
     * @param context       the context reference.
     * @param loaderId      the loader ID.
     * @param resolution    the clash resolution type.
     * @param cacheType     the result cache type.
     * @param constructor   the invocation constructor.
     * @throws NullPointerException     if any of the specified nonnull parameter is null.
     * @throws IllegalArgumentException if at least one of the parameter is invalid.
     */
    @SuppressWarnings("ConstantConditions")
    AndroidRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final Runner syncRunner, @Nonnull final WeakReference<Object> context,
            final int loaderId, @Nonnull final ClashResolution resolution,
            @Nonnull final ResultCache cacheType,
            @Nonnull final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> constructor) {

        super(configuration, syncRunner);

        if (context == null) {

            throw new NullPointerException("the context must not be null");
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
        mDataOrder = configuration.getOutputOrder(DataOrder.DEFAULT);
        mLogger = Logger.create(configuration.getLog(null), configuration.getLogLevel(null), this);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> convertInvocation(final boolean async,
            @Nonnull final Invocation<INPUT, OUTPUT> invocation) {

        try {

            invocation.onDestroy();

        } catch (final RoutineInterruptedException e) {

            throw e.interrupt();

        } catch (final Throwable t) {

            mLogger.wrn(t, "ignoring exception while destroying invocation instance");
        }

        return createInvocation(async);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> createInvocation(final boolean async) {

        final Logger logger = mLogger;

        if (async) {

            return new LoaderInvocation<INPUT, OUTPUT>(mContext, mLoaderId, mClashResolution,
                                                       mCacheType, mConstructor, mDataOrder,
                                                       logger);
        }

        final Object context = mContext.get();
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
            throw new RoutineException(e.getCause());

        } catch (final RoutineInterruptedException e) {

            logger.err(e, "error creating the invocation instance");
            throw e.interrupt();

        } catch (final RoutineException e) {

            logger.err(e, "error creating the invocation instance");
            throw e;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance");
            throw new RoutineException(t);
        }
    }
}
