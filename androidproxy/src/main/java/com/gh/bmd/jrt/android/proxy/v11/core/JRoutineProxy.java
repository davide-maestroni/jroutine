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
package com.gh.bmd.jrt.android.proxy.v11.core;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.proxy.annotation.V4Proxy;
import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;

import javax.annotation.Nonnull;

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
 * See {@link com.gh.bmd.jrt.android.proxy.v4.core.JRoutineProxy} for support of API levels less
 * than {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
 * <p/>
 * Created by davide on 06/05/15.
 *
 * @see V4Proxy
 * @see com.gh.bmd.jrt.android.annotation.CacheStrategy
 * @see com.gh.bmd.jrt.android.annotation.ClashResolution
 * @see com.gh.bmd.jrt.android.annotation.LoaderId
 * @see com.gh.bmd.jrt.annotation.Bind
 * @see com.gh.bmd.jrt.annotation.Param
 * @see com.gh.bmd.jrt.annotation.ShareGroup
 * @see com.gh.bmd.jrt.annotation.Timeout
 * @see com.gh.bmd.jrt.annotation.TimeoutAction
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending functionalities of another utility class")
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
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder onActivity(@Nonnull final Activity activity,
            @Nonnull final Class<?> target) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.proxy.v4.core.JRoutineProxy class "
                            + "instead");
        }

        return new DefaultLoaderProxyRoutineBuilder(activity, target);
    }

    /**
     * Returns a builder of routines bound to the specified fragment, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext} as application.
     *
     * @param fragment the invocation fragment context.
     * @param target   the wrapped object class.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder onFragment(@Nonnull final Fragment fragment,
            @Nonnull final Class<?> target) {

        return new DefaultLoaderProxyRoutineBuilder(fragment, target);
    }
}
