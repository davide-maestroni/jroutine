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
package com.gh.bmd.jrt.android.v4.routine;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.builder.AndroidChannelBuilder;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.CacheStrategy;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.ClassToken;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of an Android channel builder.
 * <p/>
 * Created by davide on 1/14/15.
 */
class DefaultAndroidChannelBuilder implements AndroidChannelBuilder {

    private final WeakReference<Object> mContext;

    private final int mLoaderId;

    private CacheStrategy mCacheStrategy;

    private RoutineConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param activity the context activity.
     * @param loaderId the loader ID.
     * @throws java.lang.NullPointerException if the activity is null.
     */
    DefaultAndroidChannelBuilder(@Nonnull final FragmentActivity activity, final int loaderId) {

        this((Object) activity, loaderId);
    }

    /**
     * Constructor.
     *
     * @param fragment the context fragment.
     * @param loaderId the loader ID.
     * @throws java.lang.NullPointerException if the fragment is null.
     */
    DefaultAndroidChannelBuilder(@Nonnull final Fragment fragment, final int loaderId) {

        this((Object) fragment, loaderId);
    }

    /**
     * Constructor.
     *
     * @param context  the context instance.
     * @param loaderId the loader ID.
     * @throws java.lang.NullPointerException if the context is null.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultAndroidChannelBuilder(@Nonnull final Object context, final int loaderId) {

        if (context == null) {

            throw new NullPointerException("the channel context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mLoaderId = loaderId;
    }

    @Nonnull
    @Override
    public <OUTPUT> OutputChannel<OUTPUT> buildChannel() {

        final Object context = mContext.get();

        if (context == null) {

            return JRoutine.on(MissingLoaderInvocation.<OUTPUT, OUTPUT>factoryOf())
                           .buildRoutine()
                           .callSync();
        }

        final AndroidRoutineBuilder<OUTPUT, OUTPUT> builder;

        if (context instanceof FragmentActivity) {

            final FragmentActivity activity = (FragmentActivity) context;
            builder = JRoutine.onActivity(activity, new MissingToken<OUTPUT>()).withId(mLoaderId);

        } else if (context instanceof Fragment) {

            final Fragment fragment = (Fragment) context;
            builder = JRoutine.onFragment(fragment, new MissingToken<OUTPUT>()).withId(mLoaderId);

        } else {

            throw new IllegalArgumentException(
                    "invalid context type: " + context.getClass().getCanonicalName());
        }

        return builder.withConfiguration(mConfiguration)
                      .onClash(ClashResolution.KEEP_THAT)
                      .onComplete(mCacheStrategy)
                      .buildRoutine()
                      .callAsync();
    }

    @Nonnull
    @Override
    public AndroidChannelBuilder onComplete(@Nullable final CacheStrategy cacheStrategy) {

        mCacheStrategy = cacheStrategy;
        return this;
    }

    @Nonnull
    @Override
    public AndroidChannelBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    /**
     * Missing loader invocation token.
     *
     * @param <DATA> the data type.
     */
    private static class MissingToken<DATA>
            extends ClassToken<MissingLoaderInvocation<DATA, DATA>> {

    }
}
