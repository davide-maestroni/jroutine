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
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.time.TimeDuration;

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

    private final Constructor<? extends AndroidInvocation<INPUT, OUTPUT>> mConstructor;

    private final WeakReference<Object> mContext;

    private CacheStrategy mCacheStrategy;

    private ClashResolution mClashResolution;

    private RoutineConfiguration mConfiguration;

    private int mLoaderId = AndroidRoutineBuilder.AUTO;

    /**
     * Constructor.
     *
     * @param activity   the context activity.
     * @param classToken the invocation class token.
     * @throws java.lang.NullPointerException if the activity or class token are null.
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
     * @throws java.lang.NullPointerException if the fragment or class token are null.
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
     * @throws java.lang.NullPointerException if the context or class token are null.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultAndroidRoutineBuilder(@Nonnull final Object context,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mConstructor = findConstructor(classToken.getRawClass());
    }

    @Nonnull
    @Override
    public Routine<INPUT, OUTPUT> buildRoutine() {

        final Builder builder =
                RoutineConfiguration.builderFrom(RoutineConfiguration.notNull(mConfiguration));
        final RoutineConfiguration routineConfiguration = builder.withRunner(Runners.mainRunner())
                                                                 .withInputSize(Integer.MAX_VALUE)
                                                                 .withInputTimeout(
                                                                         TimeDuration.INFINITY)
                                                                 .withOutputSize(Integer.MAX_VALUE)
                                                                 .withOutputTimeout(
                                                                         TimeDuration.INFINITY)
                                                                 .buildConfiguration();
        return new AndroidRoutine<INPUT, OUTPUT>(routineConfiguration, mContext, mLoaderId,
                                                 mClashResolution, mCacheStrategy, mConstructor);
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> onClash(
            @Nullable final ClashResolution resolution) {

        mClashResolution = resolution;
        return this;
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> onComplete(
            @Nullable final CacheStrategy cacheStrategy) {

        mCacheStrategy = cacheStrategy;
        return this;
    }

    /**
     * Note that all the options related to the output and input channels size and timeout will be
     * ignored.
     *
     * @param configuration the configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> withId(final int id) {

        mLoaderId = id;
        return this;
    }
}
