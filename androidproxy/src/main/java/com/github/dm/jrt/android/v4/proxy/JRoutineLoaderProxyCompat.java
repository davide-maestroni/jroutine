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

package com.github.dm.jrt.android.v4.proxy;

import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;

import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods, bound to a context lifecycle.
 * <p/>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.<br/>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p/>
 * Created by davide-maestroni on 05/06/2015.
 *
 * @see com.github.dm.jrt.android.object.annotation Android Annotations
 * @see com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat LoaderProxyCompat
 * @see com.github.dm.jrt.object.annotation Annotations
 */
public class JRoutineLoaderProxyCompat {

    private static final WeakHashMap<LoaderContextCompat, ProxyContextBuilderCompat> sBuilders =
            new WeakHashMap<LoaderContextCompat, ProxyContextBuilderCompat>();

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineLoaderProxyCompat() {

    }

    /**
     * Returns a context based builder of loader proxy routine builders.
     *
     * @param context the service context.
     * @return the context builder.
     */
    @NotNull
    public static ProxyContextBuilderCompat with(@NotNull final LoaderContextCompat context) {

        synchronized (sBuilders) {
            final WeakHashMap<LoaderContextCompat, ProxyContextBuilderCompat> builders = sBuilders;
            ProxyContextBuilderCompat contextBuilder = builders.get(context);
            if (contextBuilder == null) {
                contextBuilder = new ProxyContextBuilderCompat(context);
                builders.put(context, contextBuilder);
            }

            return contextBuilder;
        }
    }

    /**
     * Context based builder of loader proxy routine builders.
     */
    public static class ProxyContextBuilderCompat {

        private final LoaderContextCompat mContext;

        /**
         * Constructor.
         *
         * @param context the loader context.
         */
        @SuppressWarnings("ConstantConditions")
        private ProxyContextBuilderCompat(@NotNull final LoaderContextCompat context) {

            if (context == null) {
                throw new NullPointerException("the context must not be null");
            }

            mContext = context;
        }

        /**
         * Returns a builder of routines bound to the builder context, wrapping the specified target
         * object.<br/>
         * In order to customize the object creation, the caller must employ an implementation of a
         * {@link com.github.dm.jrt.android.object.builder.FactoryContext FactoryContext} as the
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
