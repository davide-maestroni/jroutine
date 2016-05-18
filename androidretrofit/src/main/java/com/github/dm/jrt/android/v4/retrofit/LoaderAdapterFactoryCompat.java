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

package com.github.dm.jrt.android.v4.retrofit;

import com.github.dm.jrt.android.core.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.ContextInvocationWrapper;
import com.github.dm.jrt.android.object.builder.AndroidBuilders;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamChannelCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamsCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ConfigurableBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.annotation.Invoke;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.retrofit.RetrofitCallInvocation;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Created by davide-maestroni on 05/18/2016.
 */
public class LoaderAdapterFactoryCompat extends CallAdapter.Factory {

    private static final RetrofitCallInvocation<Object> sCallInvocation =
            new RetrofitCallInvocation<Object>();

    private static final LoaderAdapterFactoryCompat sFactory =
            new LoaderAdapterFactoryCompat(null, InvocationConfiguration.defaultConfiguration(),
                    LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC);

    private static final ContextInvocationWrapper<Call<Object>, Object> sInvocationWrapper =
            new ContextInvocationWrapper<Call<Object>, Object>(sCallInvocation);

    private static final ContextInvocationFactory<Call<Object>, Object> sCallInvocationFactory =
            new ContextInvocationFactory<Call<Object>, Object>(null) {

                @NotNull
                @Override
                @SuppressWarnings("unchecked")
                public ContextInvocation<Call<Object>, Object> newInvocation() throws Exception {

                    return sInvocationWrapper;
                }
            };

    private final InvocationConfiguration mInvocationConfiguration;

    private final InvocationMode mInvocationMode;

    private final LoaderConfiguration mLoaderConfiguration;

    private final LoaderContextCompat mLoaderContext;

    /**
     * Constructor.
     *
     * @param context                 the loader context.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the service configuration.
     * @param invocationMode          the invocation mode.
     */
    private LoaderAdapterFactoryCompat(@Nullable final LoaderContextCompat context,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode) {

        mLoaderContext = context;
        mInvocationConfiguration = invocationConfiguration;
        mLoaderConfiguration = loaderConfiguration;
        mInvocationMode = invocationMode;
    }

    /**
     * Returns an adapter factory builder.
     *
     * @return the builder instance.
     */
    @NotNull
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Returns the default factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public static LoaderAdapterFactoryCompat defaultFactory() {

        return sFactory;
    }

    /**
     * Returns the a factory instance with default configuration.
     *
     * @param context the loader context.
     * @return the factory instance.
     */
    @NotNull
    public static LoaderAdapterFactoryCompat defaultFactory(
            @Nullable final LoaderContextCompat context) {

        return (context == null) ? defaultFactory() : new LoaderAdapterFactoryCompat(context,
                InvocationConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC);
    }

    @Override
    public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
            final Retrofit retrofit) {

        InvocationMode invocationMode = mInvocationMode;
        if (annotations != null) {
            for (final Annotation annotation : annotations) {
                if (annotation.annotationType() == Invoke.class) {
                    invocationMode = ((Invoke) annotation).value();
                }
            }
        }

        if (returnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) returnType;
            final Type rawType = parameterizedType.getRawType();
            if (LoaderStreamChannelCompat.class == rawType) {
                return new LoaderStreamChannelAdapter(mLoaderContext, invocationMode,
                        buildRoutine(annotations), parameterizedType.getActualTypeArguments()[1]);

            } else if (StreamChannel.class == rawType) {
                return new StreamChannelAdapter(invocationMode, buildRoutine(annotations),
                        parameterizedType.getActualTypeArguments()[1]);

            } else if (OutputChannel.class == rawType) {
                return new OutputChannelAdapter(invocationMode, buildRoutine(annotations),
                        parameterizedType.getActualTypeArguments()[0]);
            }

        } else if (returnType instanceof Class) {
            if (LoaderStreamChannelCompat.class == returnType) {
                return new LoaderStreamChannelAdapter(mLoaderContext, invocationMode,
                        buildRoutine(annotations), Object.class);

            } else if (StreamChannel.class == returnType) {
                return new StreamChannelAdapter(invocationMode, buildRoutine(annotations),
                        Object.class);

            } else if (OutputChannel.class == returnType) {
                return new OutputChannelAdapter(invocationMode, buildRoutine(annotations),
                        Object.class);
            }
        }

        return null;
    }

    @NotNull
    private Routine<? extends Call<?>, ?> buildRoutine(@Nullable final Annotation[] annotations) {

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, annotations);
        final LoaderContextCompat loaderContext = mLoaderContext;
        if (loaderContext == null) {
            return JRoutineCore.on(sCallInvocation)
                               .invocationConfiguration()
                               .with(mInvocationConfiguration)
                               .apply()
                               .buildRoutine();
        }

        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.withAnnotations(mLoaderConfiguration, annotations);
        return JRoutineLoaderCompat.with(
                ConstantConditions.notNull("loader context", loaderContext))
                                   .on(sCallInvocationFactory)
                                   .invocationConfiguration()
                                   .with(invocationConfiguration)
                                   .apply()
                                   .loaderConfiguration()
                                   .with(loaderConfiguration)
                                   .apply()
                                   .buildRoutine();
    }

    /**
     * Builder of routine adapter factory instances.
     * <p>
     * The options set through the builder configuration will be applied to all the routine handling
     * the Retrofit calls, unless they are overwritten by specific annotations.
     *
     * @see Builders#getInvocationMode(Method)
     * @see Builders#withAnnotations(InvocationConfiguration, Annotation...)
     * @see AndroidBuilders#withAnnotations(LoaderConfiguration, Annotation...)
     */
    public static class Builder
            implements ConfigurableBuilder<Builder>, LoaderConfigurableBuilder<Builder>,
            InvocationConfiguration.Configurable<Builder>,
            LoaderConfiguration.Configurable<Builder> {

        private InvocationConfiguration mInvocationConfiguration =
                InvocationConfiguration.defaultConfiguration();

        private InvocationMode mInvocationMode = InvocationMode.ASYNC;

        private LoaderConfiguration mLoaderConfiguration =
                LoaderConfiguration.defaultConfiguration();

        private LoaderContextCompat mLoaderContext;

        /**
         * Constructor.
         */
        private Builder() {

        }

        @NotNull
        public Builder apply(@NotNull final InvocationConfiguration configuration) {

            mInvocationConfiguration =
                    ConstantConditions.notNull("invocation configuration", configuration);
            return this;
        }

        @NotNull
        public Builder apply(@NotNull final LoaderConfiguration configuration) {

            mLoaderConfiguration =
                    ConstantConditions.notNull("loader configuration", configuration);
            return this;
        }

        /**
         * Builds and return a new factory instance.
         *
         * @return the factory instance.
         */
        @NotNull
        public LoaderAdapterFactoryCompat buildFactory() {

            return new LoaderAdapterFactoryCompat(mLoaderContext, mInvocationConfiguration,
                    mLoaderConfiguration, mInvocationMode);
        }

        @NotNull
        public InvocationConfiguration.Builder<? extends Builder> invocationConfiguration() {

            return new InvocationConfiguration.Builder<Builder>(this, mInvocationConfiguration);
        }

        /**
         * Sets the invocation mode to be used with the adapting routines (asynchronous by default).
         *
         * @param invocationMode the invocation mode.
         * @return this builder.
         */
        @NotNull
        public Builder invocationMode(@Nullable final InvocationMode invocationMode) {

            mInvocationMode = (invocationMode != null) ? invocationMode : InvocationMode.ASYNC;
            return this;
        }

        @NotNull
        public LoaderConfiguration.Builder<? extends Builder> loaderConfiguration() {

            return new LoaderConfiguration.Builder<Builder>(this, mLoaderConfiguration);
        }

        /**
         * Sets the factory loader context.
         *
         * @param context the loader context.
         * @return this builder.
         */
        @NotNull
        public Builder with(@Nullable final LoaderContextCompat context) {

            mLoaderContext = context;
            return this;
        }
    }

    /**
     * Base adapter implementation.
     */
    private static abstract class BaseAdapter<T> implements CallAdapter<T> {

        private final Type mResponseType;

        private final Routine<Call<?>, ?> mRoutine;

        /**
         * Constructor.
         *
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        @SuppressWarnings("unchecked")
        private BaseAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            mResponseType = responseType;
            mRoutine = (Routine<Call<?>, ?>) routine;
        }

        public Type responseType() {

            return mResponseType;
        }

        /**
         * Gets the adapter routine.
         *
         * @return the routine instance.
         */
        @NotNull
        protected Routine<Call<?>, ?> getRoutine() {

            return mRoutine;
        }
    }

    /**
     * Loader stream channel adapter implementation.
     */
    private static class LoaderStreamChannelAdapter extends BaseAdapter<LoaderStreamChannelCompat> {

        private final LoaderContextCompat mContext;

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param context        the loader context.
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private LoaderStreamChannelAdapter(@Nullable final LoaderContextCompat context,
                @NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mContext = context;
            mInvocationMode = invocationMode;
        }

        public <OUT> LoaderStreamChannelCompat adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final LoaderStreamChannelCompat<Call<OUT>, Call<OUT>> stream =
                    LoaderStreamsCompat.streamOf(call).with(mContext);
            if (invocationMode == InvocationMode.ASYNC) {
                stream.async();

            } else if (invocationMode == InvocationMode.SYNC) {
                stream.sync();

            } else if (invocationMode == InvocationMode.PARALLEL) {
                stream.parallel();

            } else {
                stream.serial();
            }

            return stream.map(getRoutine());
        }
    }

    /**
     * Output channel adapter implementation.
     */
    private static class OutputChannelAdapter extends BaseAdapter<OutputChannel> {

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private OutputChannelAdapter(@NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mInvocationMode = invocationMode;
        }

        public <OUT> OutputChannel adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final Routine<Call<?>, ?> routine = getRoutine();
            if (invocationMode == InvocationMode.ASYNC) {
                return routine.asyncCall(call);

            } else if (invocationMode == InvocationMode.SYNC) {
                return routine.syncCall(call);

            } else if (invocationMode == InvocationMode.PARALLEL) {
                return routine.parallelCall(call);
            }

            return routine.serialCall(call);
        }
    }

    /**
     * Stream channel adapter implementation.
     */
    private static class StreamChannelAdapter extends BaseAdapter<StreamChannel> {

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private StreamChannelAdapter(@NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mInvocationMode = invocationMode;
        }

        public <OUT> StreamChannel adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final StreamChannel<Call<OUT>, Call<OUT>> stream = Streams.streamOf(call);
            if (invocationMode == InvocationMode.ASYNC) {
                stream.async();

            } else if (invocationMode == InvocationMode.SYNC) {
                stream.sync();

            } else if (invocationMode == InvocationMode.PARALLEL) {
                stream.parallel();

            } else {
                stream.serial();
            }

            return stream.map(getRoutine());
        }
    }
}
