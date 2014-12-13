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

import com.bmd.jrt.android.invocator.RoutineInvocator;
import com.bmd.jrt.android.runner.Runners;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.Reflection;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.routine.Routine;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

/**
 * Default implementation of an Android routine invocator.
 * <p/>
 * Created by davide on 12/9/14.
 */
class DefaultRoutineInvocator implements RoutineInvocator {

    private final WeakReference<Object> mContext;

    private int mLoaderId = RoutineInvocator.GENERATED_ID;

    private ClashResolution mResolution = ClashResolution.DEFAULT;

    /**
     * Constructor.
     *
     * @param activity the context activity.
     * @throws NullPointerException if the specified activity is null.
     */
    DefaultRoutineInvocator(@Nonnull final FragmentActivity activity) {

        mContext = new WeakReference<Object>(activity);
    }

    /**
     * Constructor.
     *
     * @param fragment the context fragment.
     * @throws NullPointerException if the specified fragment is null.
     */
    DefaultRoutineInvocator(@Nonnull final Fragment fragment) {

        mContext = new WeakReference<Object>(fragment);
    }

    @Nonnull
    @Override
    public <INPUT, OUTPUT> ParameterChannel<INPUT, OUTPUT> invoke(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        final ClashResolution resolution =
                (mResolution == ClashResolution.DEFAULT) ? ClashResolution.RESTART : mResolution;
        final Routine<INPUT, OUTPUT> routine =
                JRoutine.on(new ClassToken<LoaderInvocation<INPUT, OUTPUT>>() {})
                        .runBy(Runners.mainRunner(null))
                        .inputOrder(DataOrder.INSERTION)
                        .withArgs(mContext, mLoaderId, resolution,
                                  Reflection.findConstructor(classToken.getRawClass()))
                        .buildRoutine();

        return routine.invokeAsync();
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineInvocator onClash(@Nonnull final ClashResolution resolution) {

        if (resolution == null) {

            throw new NullPointerException("the clash resolution type must not be null");
        }

        mResolution = resolution;

        return this;
    }

    @Nonnull
    @Override
    public RoutineInvocator withId(final int id) {

        mLoaderId = id;

        return this;
    }
}
