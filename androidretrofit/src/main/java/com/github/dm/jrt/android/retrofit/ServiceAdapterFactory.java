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

package com.github.dm.jrt.android.retrofit;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.builder.ServiceConfigurableBuilder;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.retrofit.service.RetrofitInvocationService;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.object.annotation.Invoke;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.retrofit.RoutineAdapterFactory;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Implementation of a call adapter factory supporting {@code OutputChannel} and
 * {@code StreamChannel} return types.
 * <br>
 * If properly configured, the routine invocations will run in a dedicated Android service.
 * <br>
 * Note, however, that the service class specified in the context must inherit from
 * {@link RetrofitInvocationService}.
 * <p>
 * Created by davide-maestroni on 05/16/2016.
 */
public class ServiceAdapterFactory extends CallAdapter.Factory {

    private static final ServiceAdapterFactory sFactory =
            new ServiceAdapterFactory(null, InvocationConfiguration.defaultConfiguration(),
                    ServiceConfiguration.defaultConfiguration(), InvocationMode.ASYNC);

    private final InvocationConfiguration mInvocationConfiguration;

    private final InvocationMode mInvocationMode;

    private final ServiceConfiguration mServiceConfiguration;

    private final ServiceContext mServiceContext;

    /**
     * Constructor.
     *
     * @param context                 the service context.
     * @param invocationConfiguration the invocation configuration.
     * @param serviceConfiguration    the service configuration.
     * @param invocationMode          the invocation mode.
     */
    private ServiceAdapterFactory(@Nullable final ServiceContext context,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final ServiceConfiguration serviceConfiguration,
            @NotNull final InvocationMode invocationMode) {

        mServiceContext = context;
        mInvocationConfiguration = invocationConfiguration;
        mServiceConfiguration = serviceConfiguration;
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
    public static ServiceAdapterFactory defaultFactory() {

        return sFactory;
    }

    /**
     * Returns the a factory instance with default configuration.
     *
     * @param context the service context.
     * @return the factory instance.
     * @throws java.lang.IllegalArgumentException if the service class specified in the context
     *                                            does inherit from
     *                                            {@link RetrofitInvocationService}.
     */
    @NotNull
    public static ServiceAdapterFactory defaultFactory(@Nullable final ServiceContext context) {

        return (context == null) ? defaultFactory()
                : new ServiceAdapterFactory(verifyContext(context),
                        InvocationConfiguration.defaultConfiguration(),
                        ServiceConfiguration.defaultConfiguration(), InvocationMode.ASYNC);
    }

    @Nullable
    private static ServiceContext verifyContext(@Nullable final ServiceContext context) {

        if (context != null) {
            final String className = context.getServiceIntent().getComponent().getClassName();
            try {
                final Class<?> serviceClass = Class.forName(className);
                if (!RetrofitInvocationService.class.isAssignableFrom(serviceClass)) {
                    throw new IllegalArgumentException("service class must inherit from "
                            + RetrofitInvocationService.class.getName());
                }

            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return context;
    }

    @Override
    public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
            final Retrofit retrofit) {

        InvocationMode invocationMode = mInvocationMode;
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType() == Invoke.class) {
                invocationMode = ((Invoke) annotation).value();
            }
        }

        final ServiceContext serviceContext = mServiceContext;
        if ((serviceContext == null) || (invocationMode == InvocationMode.SYNC) || (invocationMode
                == InvocationMode.SERIAL)) {
            return RoutineAdapterFactory.builder()
                                        .invocationMode(invocationMode)
                                        .invocationConfiguration()
                                        .with(mInvocationConfiguration)
                                        .apply()
                                        .buildFactory()
                                        .get(returnType, annotations, retrofit);
        }

        if (returnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) returnType;
            final Type rawType = parameterizedType.getRawType();
            if (StreamChannel.class == rawType) {
                final Type type = parameterizedType.getActualTypeArguments()[1];
                return new StreamChannelAdapter(mInvocationConfiguration, invocationMode,
                        retrofit.responseBodyConverter(type, annotations),
                        buildRoutine(annotations), type);

            } else if (OutputChannel.class == rawType) {
                final Type type = parameterizedType.getActualTypeArguments()[0];
                return new OutputChannelAdapter(mInvocationConfiguration, invocationMode,
                        retrofit.responseBodyConverter(type, annotations),
                        buildRoutine(annotations), type);
            }

        } else if (returnType instanceof Class) {
            if (StreamChannel.class == returnType) {
                return new StreamChannelAdapter(mInvocationConfiguration, invocationMode,
                        retrofit.responseBodyConverter(Object.class, annotations),
                        buildRoutine(annotations), Object.class);

            } else if (OutputChannel.class == returnType) {
                return new OutputChannelAdapter(mInvocationConfiguration, invocationMode,
                        retrofit.responseBodyConverter(Object.class, annotations),
                        buildRoutine(annotations), Object.class);
            }
        }

        return null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Routine<ParcelableRequest, ParcelableSelectable<Object>> buildRoutine(
            @Nullable final Annotation[] annotations) {

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, annotations);
        return JRoutineService.with(ConstantConditions.notNull("service context", mServiceContext))
                              .on(factoryOf(ServiceCallInvocation.class))
                              .invocationConfiguration()
                              .with(invocationConfiguration)
                              .apply()
                              .serviceConfiguration()
                              .with(mServiceConfiguration)
                              .apply()
                              .buildRoutine();
    }

    /**
     * Builder of service routine adapter factory instances.
     * <p>
     * The options set through the builder configuration will be applied to all the routine handling
     * the Retrofit calls, unless they are overwritten by specific annotations.
     *
     * @see Builders#getInvocationMode(Method)
     * @see Builders#withAnnotations(InvocationConfiguration, Annotation...)
     */
    public static class Builder implements ServiceConfigurableBuilder<Builder>,
            InvocationConfiguration.Configurable<Builder>,
            ServiceConfiguration.Configurable<Builder> {

        private InvocationConfiguration mInvocationConfiguration =
                InvocationConfiguration.defaultConfiguration();

        private InvocationMode mInvocationMode = InvocationMode.ASYNC;

        private ServiceConfiguration mServiceConfiguration =
                ServiceConfiguration.defaultConfiguration();

        private ServiceContext mServiceContext;

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
        public Builder apply(@NotNull final ServiceConfiguration configuration) {

            mServiceConfiguration =
                    ConstantConditions.notNull("service configuration", configuration);
            return this;
        }

        /**
         * Builds and return a new factory instance.
         *
         * @return the factory instance.
         */
        @NotNull
        public ServiceAdapterFactory buildFactory() {

            return new ServiceAdapterFactory(mServiceContext, mInvocationConfiguration,
                    mServiceConfiguration, mInvocationMode);
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
        public ServiceConfiguration.Builder<? extends Builder> serviceConfiguration() {

            return new ServiceConfiguration.Builder<Builder>(this, mServiceConfiguration);
        }

        /**
         * Sets the factory service context.
         *
         * @param context the service context.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the service class specified in the context
         *                                            does inherit from
         *                                            {@link RetrofitInvocationService}.
         */
        @NotNull
        public Builder with(@Nullable final ServiceContext context) {

            mServiceContext = verifyContext(context);
            return this;
        }
    }

    /**
     * Base adapter implementation.
     */
    private static abstract class BaseAdapter<T> implements CallAdapter<T> {

        private final Type mResponseType;

        private final Routine<ParcelableRequest, ParcelableSelectable<Object>> mRoutine;

        /**
         * Constructor.
         *
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        private BaseAdapter(
                @NotNull final Routine<ParcelableRequest, ParcelableSelectable<Object>> routine,
                @NotNull final Type responseType) {

            mResponseType = responseType;
            mRoutine = routine;
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
        protected Routine<ParcelableRequest, ParcelableSelectable<Object>> getRoutine() {

            return mRoutine;
        }
    }

    /**
     * Bind function used to create the stream channel instances.
     */
    private static class BindService
            implements Function<OutputChannel<ParcelableRequest>, OutputChannel<Object>> {

        private final ChannelConfiguration mConfiguration;

        private final Converter<ResponseBody, ?> mConverter;

        private final InvocationMode mInvocationMode;

        private final Routine<ParcelableRequest, ParcelableSelectable<Object>> mRoutine;

        /**
         * Constructor.
         *
         * @param configuration  the channel configuration.
         * @param invocationMode the invocation mode.
         * @param converter      the body converter.
         * @param routine        the routine instance.
         */
        private BindService(@NotNull final ChannelConfiguration configuration,
                @NotNull final InvocationMode invocationMode,
                @NotNull final Converter<ResponseBody, ?> converter,
                @NotNull final Routine<ParcelableRequest, ParcelableSelectable<Object>> routine) {

            mConfiguration = configuration;
            mInvocationMode = invocationMode;
            mConverter = converter;
            mRoutine = routine;
        }

        public OutputChannel<Object> apply(final OutputChannel<ParcelableRequest> channel) {

            final InvocationMode invocationMode = mInvocationMode;
            final Routine<ParcelableRequest, ParcelableSelectable<Object>> routine = mRoutine;
            final OutputChannel<ParcelableSelectable<Object>> outputChannel;
            if (invocationMode == InvocationMode.PARALLEL) {
                outputChannel = routine.parallelCall(channel);

            } else {
                outputChannel = routine.asyncCall(channel);
            }

            final IOChannel<Object> ioChannel = JRoutineCore.io()
                                                            .channelConfiguration()
                                                            .with(mConfiguration)
                                                            .apply()
                                                            .buildChannel();
            outputChannel.bind(new ConverterOutputConsumer(mConverter, ioChannel));
            return ioChannel;
        }
    }

    /**
     * Output channel adapter implementation.
     */
    private static class OutputChannelAdapter extends BaseAdapter<OutputChannel> {

        private final ChannelConfiguration mConfiguration;

        private final Converter<ResponseBody, ?> mConverter;

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param configuration  the invocation configuration.
         * @param invocationMode the invocation mode.
         * @param converter      the body converter.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private OutputChannelAdapter(@NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationMode invocationMode,
                @NotNull final Converter<ResponseBody, ?> converter,
                @NotNull final Routine<ParcelableRequest, ParcelableSelectable<Object>> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mConfiguration = builderFromOutputChannel(configuration).apply();
            mInvocationMode = invocationMode;
            mConverter = converter;
        }

        public <OUT> OutputChannel adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final Routine<ParcelableRequest, ParcelableSelectable<Object>> routine = getRoutine();
            final OutputChannel<ParcelableSelectable<Object>> outputChannel;
            if (invocationMode == InvocationMode.PARALLEL) {
                outputChannel = routine.parallelCall(ParcelableRequest.of(call.request()));

            } else {
                outputChannel = routine.asyncCall(ParcelableRequest.of(call.request()));
            }

            final IOChannel<Object> ioChannel = JRoutineCore.io()
                                                            .channelConfiguration()
                                                            .with(mConfiguration)
                                                            .apply()
                                                            .buildChannel();
            outputChannel.bind(new ConverterOutputConsumer(mConverter, ioChannel));
            return ioChannel;
        }
    }

    /**
     * Stream channel adapter implementation.
     */
    private static class StreamChannelAdapter extends BaseAdapter<StreamChannel> {

        private final ChannelConfiguration mChannelConfiguration;

        private final Converter<ResponseBody, ?> mConverter;

        private final InvocationConfiguration mInvocationConfiguration;

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param configuration  the invocation configuration.
         * @param invocationMode the invocation mode.
         * @param converter      the body converter.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private StreamChannelAdapter(@NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationMode invocationMode,
                @NotNull final Converter<ResponseBody, ?> converter,
                @NotNull final Routine<ParcelableRequest, ParcelableSelectable<Object>> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mInvocationConfiguration = configuration;
            mChannelConfiguration = builderFromOutputChannel(configuration).apply();
            mInvocationMode = invocationMode;
            mConverter = converter;
        }

        public <OUT> StreamChannel adapt(final Call<OUT> call) {

            return Streams.streamOf(ParcelableRequest.of(call.request()))
                          .invocationConfiguration()
                          .with(mInvocationConfiguration)
                          .apply()
                          .simpleTransform(
                                  new Function<Function<OutputChannel<ParcelableRequest>,
                                          OutputChannel<ParcelableRequest>>,
                                          Function<OutputChannel<ParcelableRequest>,
                                                  OutputChannel<Object>>>() {

                                      public Function<OutputChannel<ParcelableRequest>,
                                              OutputChannel<Object>> apply(
                                              final Function<OutputChannel<ParcelableRequest>,
                                                      OutputChannel<ParcelableRequest>> function) {

                                          return wrap(function).andThen(
                                                  new BindService(mChannelConfiguration,
                                                          mInvocationMode, mConverter,
                                                          getRoutine()));
                                      }
                                  });
        }
    }
}
