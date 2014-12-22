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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.bmd.jrt.android.runner.Runners;
import com.bmd.jrt.builder.DefaultConfigurationBuilder;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.Reflection;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.AbstractRoutine;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.runner.Runner;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of an Android routine builder.
 * <p/>
 * Created by davide on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultAndroidRoutineBuilder<INPUT, OUTPUT> implements AndroidRoutineBuilder<INPUT, OUTPUT> {

    private final RoutineConfigurationBuilder mBuilder;

    private final Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private ResultCache mCacheType = ResultCache.DEFAULT;

    private ClashResolution mClashResolution = ClashResolution.DEFAULT;

    private int mLoaderId = AndroidRoutineBuilder.GENERATED;

    /**
     * Constructor.
     *
     * @param activity   the context activity.
     * @param classToken the invocation class token.
     */
    DefaultAndroidRoutineBuilder(@Nonnull final FragmentActivity activity,
            @Nonnull ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        this((Object) activity, classToken);
    }

    /**
     * Constructor.
     *
     * @param fragment   the context fragment.
     * @param classToken the invocation class token.
     */
    DefaultAndroidRoutineBuilder(@Nonnull final Fragment fragment,
            @Nonnull ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        this((Object) fragment, classToken);
    }

    private DefaultAndroidRoutineBuilder(@Nonnull final Object context,
            @Nonnull ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        mContext = new WeakReference<Object>(context);
        mConstructor = Reflection.findConstructor(classToken.getRawClass());
        mBuilder = new DefaultConfigurationBuilder().runBy(Runners.mainRunner())
                                                    .inputOrder(DataOrder.INSERTION);
    }

    @Nonnull
    @Override
    public Routine<INPUT, OUTPUT> buildRoutine() {

        final RoutineConfiguration configuration = mBuilder.buildConfiguration();
        final Runner syncRunner = (configuration.getSyncRunner(null) == RunnerType.SEQUENTIAL)
                ? Runners.sequentialRunner() : Runners.queuedRunner();
        final ClashResolution resolution =
                (mClashResolution == ClashResolution.DEFAULT) ? ClashResolution.RESTART
                        : mClashResolution;
        final ResultCache cacheType =
                (mCacheType == ResultCache.DEFAULT) ? ResultCache.CLEAR : mCacheType;
        return new AndroidRoutine<INPUT, OUTPUT>(configuration, syncRunner, mContext, mLoaderId,
                                                 resolution, cacheType, mConstructor);
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> logLevel(@Nonnull final LogLevel level) {

        mBuilder.logLevel(level);
        return this;
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> loggedWith(@Nullable final Log log) {

        mBuilder.loggedWith(log);
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public AndroidRoutineBuilder<INPUT, OUTPUT> onClash(@Nonnull final ClashResolution resolution) {

        if (resolution == null) {

            throw new NullPointerException("the clash resolution type must not be null");
        }

        mClashResolution = resolution;
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public AndroidRoutineBuilder<INPUT, OUTPUT> onComplete(@Nonnull final ResultCache cacheType) {

        if (cacheType == null) {

            throw new NullPointerException("the result cache type must not be null");
        }

        mCacheType = cacheType;
        return this;
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> withId(final int id) {

        mLoaderId = id;
        return this;
    }

    /**
     * Routine implementation delegating to Android loaders the asynchronous processing.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AndroidRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

        private final ResultCache mCacheType;

        private final ClashResolution mClashResolution;

        private final Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

        private final WeakReference<Object> mContext;

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
         */
        private AndroidRoutine(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Runner syncRunner, @Nonnull final WeakReference<Object> context,
                final int loaderId, @Nonnull final ClashResolution resolution,
                @Nonnull final ResultCache cacheType,
                @Nonnull final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor) {

            super(configuration, syncRunner);

            mContext = context;
            mLoaderId = loaderId;
            mClashResolution = resolution;
            mCacheType = cacheType;
            mConstructor = constructor;
            mLogger = Logger.create(configuration.getLog(null), configuration.getLogLevel(null),
                                    this);
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
                                                           mCacheType, mConstructor, logger);
            }

            try {

                final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor = mConstructor;
                logger.dbg("creating a new instance of class: %s", constructor.getDeclaringClass());
                return constructor.newInstance();

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
}
