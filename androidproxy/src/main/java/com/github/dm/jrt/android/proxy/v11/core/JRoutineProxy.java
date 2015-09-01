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
package com.github.dm.jrt.android.proxy.v11.core;

import com.github.dm.jrt.android.core.ContextInvocationTarget;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods, bound to a context lifecycle.
 * <p/>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.<br/>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p/>
 * See {@link com.github.dm.jrt.android.proxy.v4.core.JRoutineProxy JRoutineProxy} for support of
 * API levels less than {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
 * <p/>
 * Created by davide-maestroni on 05/06/15.
 *
 * @see com.github.dm.jrt.android.annotation.CacheStrategy CacheStrategy
 * @see com.github.dm.jrt.android.annotation.ClashResolution ClashResolution
 * @see com.github.dm.jrt.android.annotation.InputClashResolution InputClashResolution
 * @see com.github.dm.jrt.android.annotation.LoaderId LoaderId
 * @see com.github.dm.jrt.android.annotation.StaleTime StaleTime
 * @see com.github.dm.jrt.android.proxy.annotation.V11Proxy V11Proxy
 * @see com.github.dm.jrt.annotation.Alias Alias
 * @see com.github.dm.jrt.annotation.Input Input
 * @see com.github.dm.jrt.annotation.Inputs Inputs
 * @see com.github.dm.jrt.annotation.Output Output
 * @see com.github.dm.jrt.annotation.Priority Priority
 * @see com.github.dm.jrt.annotation.ShareGroup ShareGroup
 * @see com.github.dm.jrt.annotation.Timeout Timeout
 * @see com.github.dm.jrt.annotation.TimeoutAction TimeoutAction
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class JRoutineProxy extends com.github.dm.jrt.android.proxy.core.JRoutineProxy {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineProxy() {

    }

    /**
     * Returns a builder of routines bound to the specified context, wrapping the specified target
     * object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.github.dm.jrt.android.builder.FactoryContext FactoryContext} as the application
     * context.
     *
     * @param context the routine context.
     * @param target  the invocation target.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderProxyRoutineBuilder on(@Nonnull final LoaderContext context,
            @Nonnull final ContextInvocationTarget target) {

        return new DefaultLoaderProxyRoutineBuilder(context, target);
    }
}
