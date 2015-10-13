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
import com.github.dm.jrt.annotation.ReadTimeout;
import com.github.dm.jrt.annotation.ReadTimeoutAction;

import org.jetbrains.annotations.NotNull;

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
 * API levels less than {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p/>
 * Created by davide-maestroni on 05/06/2015.
 *
 * @see com.github.dm.jrt.android.annotation.CacheStrategy CacheStrategy
 * @see com.github.dm.jrt.android.annotation.ClashResolution ClashResolution
 * @see com.github.dm.jrt.android.annotation.InputClashResolution InputClashResolution
 * @see com.github.dm.jrt.android.annotation.LoaderId LoaderId
 * @see com.github.dm.jrt.android.annotation.ResultStaleTime ResultStaleTime
 * @see com.github.dm.jrt.android.proxy.annotation.V11Proxy V11Proxy
 * @see com.github.dm.jrt.annotation.Alias Alias
 * @see com.github.dm.jrt.annotation.CoreInstances CoreInstances
 * @see com.github.dm.jrt.annotation.Input Input
 * @see com.github.dm.jrt.annotation.InputMaxSize InputMaxSize
 * @see com.github.dm.jrt.annotation.InputOrder InputOrder
 * @see com.github.dm.jrt.annotation.Inputs Inputs
 * @see com.github.dm.jrt.annotation.InputTimeout InputTimeout
 * @see com.github.dm.jrt.annotation.MaxInstances MaxInstances
 * @see com.github.dm.jrt.annotation.Invoke Invoke
 * @see com.github.dm.jrt.annotation.Output Output
 * @see com.github.dm.jrt.annotation.OutputMaxSize OutputMaxSize
 * @see com.github.dm.jrt.annotation.OutputOrder OutputOrder
 * @see com.github.dm.jrt.annotation.OutputTimeout OutputTimeout
 * @see com.github.dm.jrt.annotation.Priority Priority
 * @see com.github.dm.jrt.annotation.SharedFields SharedFields
 * @see ReadTimeout ReadTimeout
 * @see ReadTimeoutAction ReadTimeoutAction
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
     * Returns a context based builder of loader proxy routine builders.
     *
     * @param context the service context.
     * @return the context builder.
     */
    @NotNull
    public static ContextBuilder with(@NotNull final LoaderContext context) {

        return new ContextBuilder(context);
    }

    /**
     * Context based builder of loader proxy routine builders.
     */
    public static class ContextBuilder {

        private final LoaderContext mContext;

        /**
         * Constructor.
         *
         * @param context the loader context.
         */
        @SuppressWarnings("ConstantConditions")
        private ContextBuilder(@NotNull final LoaderContext context) {

            if (context == null) {

                throw new NullPointerException("the context must not be null");
            }

            mContext = context;
        }

        /**
         * Returns a builder of routines bound to the builder context, wrapping the specified target
         * object.<br/>
         * In order to customize the object creation, the caller must employ an implementation of a
         * {@link com.github.dm.jrt.android.builder.FactoryContext FactoryContext} as the
         * application context.
         * <p/>
         * Note that it is responsibility of the caller to retain a strong reference to the target
         * instance to prevent it from being garbage collected.<br/>
         * Note also that the invocation input data will be cached, and the results will be produced
         * only after the invocation channel is closed, so be sure to avoid streaming inputs in
         * order to prevent starvation or out of memory errors.
         *
         * @param target the invocation target.
         * @return the routine builder instance.
         */
        @NotNull
        public LoaderProxyRoutineBuilder on(@NotNull final ContextInvocationTarget<?> target) {

            return new DefaultLoaderProxyRoutineBuilder(mContext, target);
        }
    }
}
