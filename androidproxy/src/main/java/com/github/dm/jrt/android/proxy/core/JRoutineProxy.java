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
package com.github.dm.jrt.android.proxy.core;

import com.github.dm.jrt.android.core.ContextInvocationTarget;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods in a dedicated service.
 * <p/>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.<br/>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p/>
 * Created by davide-maestroni on 05/13/2015.
 *
 * @see com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy
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
public class JRoutineProxy extends com.github.dm.jrt.proxy.core.JRoutineProxy {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineProxy() {

    }

    /**
     * Returns a context based builder of service proxy routine builders.
     *
     * @param context the service context.
     * @return the context builder.
     */
    @Nonnull
    public static ContextBuilder on(@Nonnull final ServiceContext context) {

        return new ContextBuilder(context);
    }

    /**
     * Context based builder of service routine builders.
     */
    public static class ContextBuilder {

        private final ServiceContext mContext;

        /**
         * Constructor.
         *
         * @param context the service context.
         */
        @SuppressWarnings("ConstantConditions")
        private ContextBuilder(@Nonnull final ServiceContext context) {

            if (context == null) {

                throw new NullPointerException("the context must not be null");
            }

            mContext = context;
        }

        /**
         * Returns a builder of routines, wrapping the specified object instance, running in a
         * service based on the builder context.<br/>
         * In order to customize the object creation, the caller must employ an implementation of a
         * {@link com.github.dm.jrt.android.builder.FactoryContext FactoryContext} as the invocation
         * service.
         * <p/>
         * Note that the built routine results will be dispatched into the configured looper, thus,
         * waiting for the outputs on the very same looper thread, immediately after its invocation,
         * will result in a deadlock.<br/>
         * By default, output results are dispatched in the main looper.
         *
         * @param target the invocation target.
         * @return the routine builder instance.
         */
        @Nonnull
        public ServiceProxyRoutineBuilder with(@Nonnull final ContextInvocationTarget target) {

            return new DefaultServiceProxyRoutineBuilder(mContext, target);
        }
    }
}
