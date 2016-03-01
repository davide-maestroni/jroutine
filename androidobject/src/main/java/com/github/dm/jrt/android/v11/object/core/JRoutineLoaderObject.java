/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.v11.object.core;

import com.github.dm.jrt.android.object.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.object.core.ContextInvocationTarget;
import com.github.dm.jrt.android.v11.core.LoaderContext;

import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

/**
 * Utility class extending the base one in order to support additional routine builders specific to
 * the Android platform.<br/>
 * Routine invocations created through the returned builders can be safely restored after a change
 * in the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will be dispatched on the configured looper thread, no matter the calling one
 * was, so that, waiting for the outputs right after the routine invocation, may result in a
 * deadlock.<br/>
 * Note that the configuration of the maximum number of concurrent invocations might not work as
 * expected. In fact, the number of running loaders cannot be computed.<br/>
 * Note also that the input data will be cached, and the results will be produced only after the
 * invocation channel is closed, so be sure to avoid streaming inputs in order to prevent starvation
 * or out of memory errors.
 * <p/>
 * The {@code equals()} and {@code hashCode()} methods of the input parameter objects and the
 * invocation factory, might be employed to check for clashing of invocation instances or compute
 * the loader ID.<br/>
 * In case the caller could not guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or an input independent clash resolution should be used in
 * order to avoid unexpected results.
 * <p/>
 * The routine invocations will be identified by an ID number. In case a clash is detected, that is,
 * an already running loader with the same ID exists at the time the new invocation is executed,
 * the clash is resolved based on the strategy specified through the builder. When a clash cannot be
 * resolved, for example when invocations with different implementations share the same ID, the new
 * invocation is aborted with a {@link com.github.dm.jrt.android.invocation.InvocationTypeException
 * InvocationTypeException}.
 * <p/>
 * The class provides an additional way to build a routine, based on the asynchronous invocation of
 * a method of an existing class or object via reflection.<br/>
 * It is possible to annotate selected methods to be asynchronously invoked, or to simply select
 * a method through its signature. It is also possible to build a proxy object whose methods will
 * in turn asynchronously invoke the target object ones.
 * <p/>
 * Note however that, since the method might be invoked outside the calling context, it is not
 * possible to pass along the actual instance, but just the information needed to get or instantiate
 * it inside the loader instance.
 * <p/>
 * See {@link com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat JRoutineLoaderCompat} for
 * support of API levels lower than {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
 * <p/>
 * Created by davide-maestroni on 12/08/2014.
 *
 * @see com.github.dm.jrt.object.core.JRoutineObject JRoutineObject
 */
public class JRoutineLoaderObject {

    private static final WeakHashMap<LoaderContext, ContextBuilder> sBuilders =
            new WeakHashMap<LoaderContext, ContextBuilder>();

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param context the loader context.
     * @return the context builder.
     */
    @NotNull
    public static ContextBuilder with(@NotNull final LoaderContext context) {

        synchronized (sBuilders) {
            final WeakHashMap<LoaderContext, ContextBuilder> builders = sBuilders;
            ContextBuilder contextBuilder = builders.get(context);
            if (contextBuilder == null) {
                contextBuilder = new ContextBuilder(context);
                builders.put(context, contextBuilder);
            }

            return contextBuilder;
        }
    }

    /**
     * Context based builder of loader routine builders.
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
                throw new NullPointerException("the loader context must not be null");
            }

            mContext = context;
        }

        /**
         * Returns a builder of routines bound to the builder context, wrapping the specified
         * target object.<br/>
         * In order to customize the object creation, the caller must employ an implementation of a
         * {@link com.github.dm.jrt.android.object.builder.FactoryContext FactoryContext} as the
         * application context.<br/>
         * Note that the built routine results will be always dispatched on the configured looper
         * thread, thus waiting for the outputs immediately after its invocation may result in a
         * deadlock.
         *
         * @param target the invocation target.
         * @return the routine builder instance.
         */
        @NotNull
        public LoaderObjectRoutineBuilder on(@NotNull final ContextInvocationTarget<?> target) {

            return new DefaultLoaderObjectRoutineBuilder(mContext, target);
        }
    }
}
