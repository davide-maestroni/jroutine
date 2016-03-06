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

package com.github.dm.jrt.android;

import android.content.Context;
import android.content.Intent;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.CallContextInvocationFactory;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;

/**
 * Created by davide-maestroni on 03/06/2016.
 */
public class JRoutineAndroid extends SparseChannels {

    /**
     * Returns a context based builder of service routine builders.
     *
     * @param context the service context.
     * @return the context builder.
     */
    @NotNull
    public static ServiceContextBuilder with(@NotNull final ServiceContext context) {

        return new ServiceContextBuilder(context);
    }

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param context the loader context.
     * @return the context builder.
     */
    @NotNull
    public static LoaderContextBuilder with(@NotNull final LoaderContext context) {

        return new LoaderContextBuilder(context);
    }

    @NotNull
    public static ServiceContextBuilder withService(@NotNull final Context context,
            @NotNull final Class<? extends InvocationService> serviceClass) {

        return new ServiceContextBuilder(serviceFrom(context, serviceClass));
    }

    @NotNull
    public static ServiceContextBuilder withService(@NotNull final Context context,
            @NotNull final Intent service) {

        return new ServiceContextBuilder(serviceFrom(context, service));
    }

    @NotNull
    public static ServiceContextBuilder withService(@NotNull final Context context) {

        return new ServiceContextBuilder(serviceFrom(context));
    }

    public static class LoaderContextBuilder {

        private final LoaderContext mContext;

        /**
         * Constructor.
         *
         * @param context the loader context.
         */
        @SuppressWarnings("ConstantConditions")
        private LoaderContextBuilder(@NotNull final LoaderContext context) {

            if (context == null) {
                throw new NullPointerException("the loader context must not be null");
            }

            mContext = context;
        }

        /**
         * Returns a builder of routines bound to the builder context.<br/>
         * In order to prevent undesired leaks, the class of the specified factory must have a
         * static scope.<br/>
         * Note that the built routine results will be always dispatched on the configured looper
         * thread, thus waiting for the outputs immediately after its invocation may result in a
         * deadlock.
         *
         * @param factory the invocation factory.
         * @param <IN>    the input data type.
         * @param <OUT>   the output data type.
         * @return the routine builder instance.
         * @throws java.lang.IllegalArgumentException if the class of the specified factory has not
         *                                            a static scope.
         */
        @NotNull
        public <IN, OUT> LoaderRoutineBuilder<IN, OUT> on(
                @NotNull final CallContextInvocationFactory<IN, OUT> factory) {

            return JRoutineLoader.with(mContext).on(factory);
        }

        /**
         * Returns a builder of output channels bound to the loader identified by the specified ID.
         * <br/>
         * If no invocation with the specified ID is running at the time of the channel creation,
         * the output will be aborted with a
         * {@link com.github.dm.jrt.android.core.invocation.MissingInvocationException
         * MissingInvocationException}.<br/>
         * Note that the built routine results will be always dispatched on the configured looper
         * thread, thus waiting for the outputs immediately after its invocation may result in a
         * deadlock.
         *
         * @param loaderId the loader ID.
         * @return the channel builder instance.
         */
        @NotNull
        public LoaderChannelBuilder onId(final int loaderId) {

            return JRoutineLoader.with(mContext).onId(loaderId);
        }
    }
}
