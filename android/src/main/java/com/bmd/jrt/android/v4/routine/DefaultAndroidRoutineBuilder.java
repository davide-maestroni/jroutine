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
import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.android.runner.Runners;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.routine.Routine;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.common.Reflection.findConstructor;

/**
 * Default implementation of an Android routine builder.
 * <p/>
 * Created by davide on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultAndroidRoutineBuilder<INPUT, OUTPUT> implements AndroidRoutineBuilder<INPUT, OUTPUT> {

    private final RoutineConfigurationBuilder mBuilder = new RoutineConfigurationBuilder();

    private final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private ResultCache mCacheType = ResultCache.DEFAULT;

    private ClashResolution mClashResolution = ClashResolution.DEFAULT;

    private int mLoaderId = AndroidRoutineBuilder.GENERATED_ID;

    /**
     * Constructor.
     *
     * @param activity   the context activity.
     * @param classToken the invocation class token.
     * @throws NullPointerException if the class token is null.
     */
    DefaultAndroidRoutineBuilder(@Nonnull final FragmentActivity activity,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        this((Object) activity, classToken);
    }

    /**
     * Constructor.
     *
     * @param fragment   the context fragment.
     * @param classToken the invocation class token.
     * @throws NullPointerException if the class token is null.
     */
    DefaultAndroidRoutineBuilder(@Nonnull final Fragment fragment,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        this((Object) fragment, classToken);
    }

    /**
     * Constructor.
     *
     * @param context    the context instance.
     * @param classToken the invocation class token.
     * @throws NullPointerException if the class token is null.
     */
    private DefaultAndroidRoutineBuilder(@Nonnull final Object context,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        mContext = new WeakReference<Object>(context);
        mConstructor = findConstructor(classToken.getRawClass());
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> apply(
            @Nonnull final RoutineConfiguration configuration) {

        mBuilder.apply(configuration);
        return this;
    }

    @Nonnull
    @Override
    public Routine<INPUT, OUTPUT> buildRoutine() {

        final RoutineConfiguration configuration =
                mBuilder.runBy(Runners.mainRunner()).buildConfiguration();
        final ClashResolution resolution =
                (mClashResolution == ClashResolution.DEFAULT) ? ClashResolution.RESTART_ON_INPUT
                        : mClashResolution;
        final ResultCache cacheType =
                (mCacheType == ResultCache.DEFAULT) ? ResultCache.CLEAR : mCacheType;
        return new AndroidRoutine<INPUT, OUTPUT>(configuration, mContext, mLoaderId, resolution,
                                                 cacheType, mConstructor);
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> inputOrder(@Nonnull final DataOrder order) {

        mBuilder.inputOrder(order);
        return this;
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
    public AndroidRoutineBuilder<INPUT, OUTPUT> outputOrder(@Nonnull final DataOrder order) {

        mBuilder.outputOrder(order);
        return this;
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> syncRunner(@Nonnull final RunnerType type) {

        mBuilder.syncRunner(type);
        return this;
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> withId(final int id) {

        mLoaderId = id;
        return this;
    }
}
