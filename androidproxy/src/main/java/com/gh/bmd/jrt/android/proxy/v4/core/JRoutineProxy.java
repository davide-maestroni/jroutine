/*
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
package com.gh.bmd.jrt.android.proxy.v4.core;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.proxy.builder.ContextProxyRoutineBuilder;

import javax.annotation.Nonnull;

/**
 * Created by davide on 06/05/15.
 */
public class JRoutineProxy {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineProxy() {

    }

    @Nonnull
    public static ContextProxyRoutineBuilder onActivity(@Nonnull final FragmentActivity activity,
            @Nonnull final Class<?> targetClass) {

        return new DefaultContextProxyRoutineBuilder(activity, targetClass);
    }

    @Nonnull
    public static ContextProxyRoutineBuilder onFragment(@Nonnull final Fragment fragment,
            @Nonnull final Class<?> targetClass) {

        return new DefaultContextProxyRoutineBuilder(fragment, targetClass);
    }
}
