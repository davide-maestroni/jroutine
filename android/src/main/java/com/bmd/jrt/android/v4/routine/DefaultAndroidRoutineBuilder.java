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
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.Reflection;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.routine.Routine;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

/**
 * Default implementation of an Android routine builder.
 * <p/>
 * Created by davide on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultAndroidRoutineBuilder<INPUT, OUTPUT> implements AndroidRoutineBuilder<INPUT, OUTPUT> {

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
    }

    @Nonnull
    @Override
    public Routine<INPUT, OUTPUT> buildRoutine() {

        final ClashResolution resolution =
                (mClashResolution == ClashResolution.DEFAULT) ? ClashResolution.RESTART
                        : mClashResolution;
        final ResultCache cacheType =
                (mCacheType == ResultCache.DEFAULT) ? ResultCache.CLEAR : mCacheType;
        return JRoutine.on(new LoaderInvocationToken<INPUT, OUTPUT>())
                       .runBy(Runners.mainRunner())
                       .inputOrder(DataOrder.INSERTION)
                       .withArgs(mContext, mLoaderId, resolution, cacheType, mConstructor)
                       .buildRoutine();
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
     * Loader invocation class token.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    public static final class LoaderInvocationToken<INPUT, OUTPUT>
            extends ClassToken<LoaderInvocation<INPUT, OUTPUT>> {

    }
}
