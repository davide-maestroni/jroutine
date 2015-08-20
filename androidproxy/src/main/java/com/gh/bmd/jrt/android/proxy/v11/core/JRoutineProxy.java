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

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.gh.bmd.jrt.android.v11.core.LoaderContext;

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
 * See {@link com.gh.bmd.jrt.android.proxy.v4.core.JRoutineProxy JRoutineProxy} for support of
 * API levels less than {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
 * <p/>
 * Created by davide-maestroni on 06/05/15.
 *
 * @see com.gh.bmd.jrt.android.annotation.CacheStrategy CacheStrategy
 * @see com.gh.bmd.jrt.android.annotation.ClashResolution ClashResolution
 * @see com.gh.bmd.jrt.android.annotation.InputClashResolution InputClashResolution
 * @see com.gh.bmd.jrt.android.annotation.LoaderId LoaderId
 * @see com.gh.bmd.jrt.android.annotation.StaleTime StaleTime
 * @see com.gh.bmd.jrt.android.proxy.annotation.V11Proxy V11Proxy
 * @see com.gh.bmd.jrt.annotation.Alias Alias
 * @see com.gh.bmd.jrt.annotation.Input Input
 * @see com.gh.bmd.jrt.annotation.Inputs Inputs
 * @see com.gh.bmd.jrt.annotation.Output Output
 * @see com.gh.bmd.jrt.annotation.Priority Priority
 * @see com.gh.bmd.jrt.annotation.ShareGroup ShareGroup
 * @see com.gh.bmd.jrt.annotation.Timeout Timeout
 * @see com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction
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
     * Returns a builder of routines bound to the specified context, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext FactoryContext} as the application
     * context.
     *
     * @param context the routine context.
     * @param target  the wrapped object class.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder on(@Nonnull final LoaderContext context,
            @Nonnull final Class<?> target) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.proxy.v4.core.JRoutineProxy class "
                            + "instead");
        }

        return on(context, target, (Object[]) null);
    }

    /**
     * Returns a builder of routines bound to the specified context, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext FactoryContext} as the application
     * context.
     *
     * @param context     the routine context.
     * @param target      the wrapped object class.
     * @param factoryArgs the object factory arguments.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder on(@Nonnull final LoaderContext context,
            @Nonnull final Class<?> target, @Nullable final Object... factoryArgs) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.proxy.v4.core.JRoutineProxy class "
                            + "instead");
        }

        return new DefaultLoaderProxyRoutineBuilder(context, target, factoryArgs);
    }
}
