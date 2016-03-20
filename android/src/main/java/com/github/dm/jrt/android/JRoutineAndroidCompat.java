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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.v4.channel.SparseChannelsCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;

/**
 * Created by davide-maestroni on 03/06/2016.
 */
public class JRoutineAndroidCompat extends SparseChannelsCompat {

    @NotNull
    public static ServiceContextBuilder with(@NotNull final Context context) {

        return with(serviceFrom(context));
    }

    @NotNull
    public static ServiceContextBuilder with(@NotNull final Context context,
            @NotNull final Class<? extends InvocationService> serviceClass) {

        return with(serviceFrom(context, serviceClass));
    }

    @NotNull
    public static ServiceContextBuilder with(@NotNull final Context context,
            @NotNull final Intent service) {

        return with(serviceFrom(context, service));
    }

    @NotNull
    public static LoaderContextBuilderCompat with(@NotNull final Fragment fragment) {

        return with(loaderFrom(fragment));
    }

    @NotNull
    public static LoaderContextBuilderCompat with(@NotNull final Fragment fragment,
            @NotNull final Context context) {

        return with(loaderFrom(fragment, context));
    }

    @NotNull
    public static LoaderContextBuilderCompat with(@NotNull final FragmentActivity activity) {

        return with(loaderFrom(activity));
    }

    @NotNull
    public static LoaderContextBuilderCompat with(@NotNull final FragmentActivity activity,
            @NotNull final Context context) {

        return with(loaderFrom(activity, context));
    }

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param context the loader context.
     * @return the context builder.
     */
    @NotNull
    public static LoaderContextBuilderCompat with(@NotNull final LoaderContextCompat context) {

        return new LoaderContextBuilderCompat(context);
    }

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

    public static class LoaderContextBuilderCompat {

        private final LoaderContextCompat mContext;

        /**
         * Constructor.
         *
         * @param context the loader context.
         */
        @SuppressWarnings("ConstantConditions")
        private LoaderContextBuilderCompat(@NotNull final LoaderContextCompat context) {

            if (context == null) {
                throw new NullPointerException("the loader context must not be null");
            }

            mContext = context;
        }

        /**
         * Returns a routine builder based on an invocation factory creating instances of the
         * specified class.
         * <p/>
         * Note that inner and anonymous classes can be passed as well. Remember however that Java
         * creates synthetic constructors for such classes, so be sure to specify the correct
         * arguments to guarantee proper instantiation.
         *
         * @param invocationClass the invocation class.
         * @param <IN>            the input data type.
         * @param <OUT>           the output data type.
         * @return the routine builder instance.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation has
         *                                            not a static scope or no default construct is
         *                                            found.
         */
        @NotNull
        public <IN, OUT> LoaderRoutineBuilder<IN, OUT> on(
                @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass) {

            return on(factoryOf(invocationClass));
        }

        /**
         * Returns a routine builder based on an invocation factory creating instances of the
         * specified class by passing the specified arguments to the class constructor.
         * <p/>
         * Note that inner and anonymous classes can be passed as well. Remember however that Java
         * creates synthetic constructors for such classes, so be sure to specify the correct
         * arguments to guarantee proper instantiation.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         * @param <IN>            the input data type.
         * @param <OUT>           the output data type.
         * @return the routine builder instance.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation has
         *                                            not a static scope or no construct constructor
         *                                            taking the specified objects as parameters is
         *                                            found.
         */
        @NotNull
        public <IN, OUT> LoaderRoutineBuilder<IN, OUT> on(
                @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass,
                @Nullable final Object... args) {

            return on(factoryOf(invocationClass, args));
        }

        /**
         * Returns a routine builder based on an invocation factory creating instances of the
         * specified class token.
         * <p/>
         * Note that inner and anonymous classes can be passed as well. Remember however that Java
         * creates synthetic constructors for such classes, so be sure to specify the correct
         * arguments to guarantee proper instantiation.
         *
         * @param invocationToken the invocation class token.
         * @param <IN>            the input data type.
         * @param <OUT>           the output data type.
         * @return the routine builder instance.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation has
         *                                            not a static scope or no default construct is
         *                                            found.
         */
        @NotNull
        public <IN, OUT> LoaderRoutineBuilder<IN, OUT> on(
                @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken) {

            return on(factoryOf(invocationToken));
        }

        /**
         * Returns a routine builder based on an invocation factory creating instances of the
         * specified class token by passing the specified arguments to the class constructor.
         * <p/>
         * Note that inner and anonymous classes can be passed as well. Remember however that Java
         * creates synthetic constructors for such classes, so be sure to specify the correct
         * arguments to guarantee proper instantiation.
         *
         * @param invocationToken the invocation class token.
         * @param args            the invocation constructor arguments.
         * @param <IN>            the input data type.
         * @param <OUT>           the output data type.
         * @return the routine builder instance.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation has
         *                                            not a static scope or no construct constructor
         *                                            taking the specified objects as parameters is
         *                                            found.
         */
        @NotNull
        public <IN, OUT> LoaderRoutineBuilder<IN, OUT> on(
                @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken,
                @Nullable final Object... args) {

            return on(factoryOf(invocationToken, args));
        }

        /**
         * Returns a builder of routines bound to the builder context.<br/>
         * In order to prevent undesired leaks, the class of the specified factory must have a
         * static scope.
         * <p/>
         * Note that the built routine results will be always dispatched on the configured looper
         * thread, thus waiting for the outputs immediately after its invocation may result in a
         * deadlock.<br/>
         * Note also that the input data passed to the invocation channel will be cached, and the
         * results will be produced only after the invocation channel is closed, so be sure to avoid
         * streaming inputs in order to prevent starvation or out of memory errors.
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
                @NotNull final ContextInvocationFactory<IN, OUT> factory) {

            return JRoutineLoaderCompat.with(mContext).on(factory);
        }

        /**
         * Returns a builder of routines bound to the builder context, wrapping the specified
         * target object.<br/>
         * In order to customize the object creation, the caller must employ an implementation of a
         * {@link com.github.dm.jrt.android.object.builder.FactoryContext FactoryContext} as the
         * application context.
         * <p/>
         * Note that the built routine results will be always dispatched on the configured looper
         * thread, thus waiting for the outputs immediately after its invocation may result in a
         * deadlock.
         *
         * @param target the invocation target.
         * @return the routine builder instance.
         */
        @NotNull
        public LoaderTargetRoutineBuilder on(@NotNull final ContextInvocationTarget<?> target) {

            return new DefaultLoaderTargetRoutineBuilderCompat(mContext, target);
        }

        @NotNull
        public LoaderTargetRoutineBuilder onClass(@NotNull final Class<?> targetClass) {

            return on(classOfType(targetClass));
        }

        /**
         * Returns a builder of output channels bound to the loader identified by the specified ID.
         * <br/>
         * If no invocation with the specified ID is running at the time of the channel creation,
         * the output will be aborted with a
         * {@link com.github.dm.jrt.android.core.invocation.MissingLoaderException
         * MissingLoaderException}.
         * <p/>
         * Note that the built routine results will be always dispatched on the configured looper
         * thread, thus waiting for the outputs immediately after its invocation may result in a
         * deadlock.
         *
         * @param loaderId the loader ID.
         * @return the channel builder instance.
         */
        @NotNull
        public LoaderChannelBuilder onId(final int loaderId) {

            return JRoutineLoaderCompat.with(mContext).onId(loaderId);
        }

        @NotNull
        public LoaderTargetRoutineBuilder onInstance(@NotNull final Class<?> targetClass) {

            return on(instanceOf(targetClass));
        }

        @NotNull
        public LoaderTargetRoutineBuilder onInstance(@NotNull final Class<?> targetClass,
                @Nullable final Object... factoryArgs) {

            return on(instanceOf(targetClass, factoryArgs));
        }
    }
}
