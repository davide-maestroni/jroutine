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

import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls, bound to a context lifecycle, of their methods.
 * <p/>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.<br/>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p/>
 * Created by davide-maestroni on 06/05/15.
 *
 * @see com.gh.bmd.jrt.android.proxy.annotation.V4Proxy
 * @see com.gh.bmd.jrt.android.annotation.CacheStrategy
 * @see com.gh.bmd.jrt.android.annotation.ClashResolution
 * @see com.gh.bmd.jrt.android.annotation.LoaderId
 * @see com.gh.bmd.jrt.annotation.Alias
 * @see com.gh.bmd.jrt.annotation.Input
 * @see com.gh.bmd.jrt.annotation.Inputs
 * @see com.gh.bmd.jrt.annotation.Output
 * @see com.gh.bmd.jrt.annotation.Priority
 * @see com.gh.bmd.jrt.annotation.ShareGroup
 * @see com.gh.bmd.jrt.annotation.Timeout
 * @see com.gh.bmd.jrt.annotation.TimeoutAction
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class JRoutineProxy extends com.gh.bmd.jrt.android.proxy.core.JRoutineProxy {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineProxy() {

    }

    /**
     * Returns a builder of routines bound to the specified activity, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext} as application.
     *
     * @param activity the invocation activity context.
     * @param target   the wrapped object class.
     * @param args     the object factory arguments.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder onActivity(@Nonnull final FragmentActivity activity,
            @Nonnull final Class<?> target, @Nullable final Object... args) {

        return new DefaultLoaderProxyRoutineBuilder(activity, target, args);
    }

    /**
     * Returns a builder of routines bound to the specified fragment, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext} as application.
     *
     * @param fragment the invocation fragment context.
     * @param target   the wrapped object class.
     * @param args     the object factory arguments.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder onFragment(@Nonnull final Fragment fragment,
            @Nonnull final Class<?> target, @Nullable final Object... args) {

        return new DefaultLoaderProxyRoutineBuilder(fragment, target, args);
    }
}
