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

    private static final CallMappingInvocation sInvocation = new CallMappingInvocation();

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

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, annotations);
        final ServiceContext serviceContext = mServiceContext;
        if ((serviceContext == null) || (invocationMode == InvocationMode.SYNC) || (invocationMode
                == InvocationMode.SERIAL)) {
            return RoutineAdapterFactory.builder()
                                        .invocationMode(invocationMode)
                                        .invocationConfiguration()
                                        .with(invocationConfiguration)
                                        .apply()
                                        .buildFactory()
                                        .get(returnType, annotations, retrofit);
        }

        Type rawType = null;
        Type responseType = Object.class;
        if (returnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) returnType;
            rawType = parameterizedType.getRawType();
            if (StreamChannel.class == rawType) {
                responseType = parameterizedType.getActualTypeArguments()[1];

            } else if (OutputChannel.class == rawType) {
                responseType = parameterizedType.getActualTypeArguments()[0];
            }

        } else if (returnType instanceof Class) {
            rawType = returnType;
        }

        if (StreamChannel.class == rawType) {
            return new StreamChannelAdapter(invocationConfiguration,
                    retrofit.responseBodyConverter(responseType, annotations),
                    buildRoutine(invocationConfiguration), responseType);

        } else if (OutputChannel.class == rawType) {
            return new OutputChannelAdapter(invocationConfiguration,
                    retrofit.responseBodyConverter(responseType, annotations),
                    buildRoutine(invocationConfiguration), responseType);
        }

        return null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> buildRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration) {

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

        private final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> mRoutine;

        /**
         * Constructor.
         *
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        private BaseAdapter(
                @NotNull final Routine<ParcelableSelectable<Object>,
                        ParcelableSelectable<Object>> routine,
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
        protected Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> getRoutine() {

            return mRoutine;
        }
    }

    /**
     * Bind function used to create the stream channel instances.
     */
    private static class BindService implements
            Function<OutputChannel<ParcelableSelectable<Object>>, OutputChannel<Object>> {

        private final ChannelConfiguration mConfiguration;

        private final Converter<ResponseBody, ?> mConverter;

        private final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> mRoutine;

        /**
         * Constructor.
         *
         * @param configuration the channel configuration.
         * @param converter     the body converter.
         * @param routine       the routine instance.
         */
        private BindService(@NotNull final ChannelConfiguration configuration,
                @NotNull final Converter<ResponseBody, ?> converter,
                @NotNull final Routine<ParcelableSelectable<Object>,
                        ParcelableSelectable<Object>> routine) {

            mConfiguration = configuration;
            mConverter = converter;
            mRoutine = routine;
        }

        public OutputChannel<Object> apply(
                final OutputChannel<ParcelableSelectable<Object>> channel) {

            final IOChannel<Object> ioChannel = JRoutineCore.io()
                                                            .channelConfiguration()
                                                            .with(mConfiguration)
                                                            .apply()
                                                            .buildChannel();
            mRoutine.asyncCall(channel).bind(new ConverterOutputConsumer(mConverter, ioChannel));
            return ioChannel;
        }
    }

    /**
     * Output channel adapter implementation.
     */
    private static class OutputChannelAdapter extends BaseAdapter<OutputChannel> {

        private final ChannelConfiguration mChannelConfiguration;

        private final Converter<ResponseBody, ?> mConverter;

        private final InvocationConfiguration mInvocationConfiguration;

        /**
         * Constructor.
         *
         * @param configuration the invocation configuration.
         * @param converter     the body converter.
         * @param routine       the routine instance.
         * @param responseType  the response type.
         */
        private OutputChannelAdapter(@NotNull final InvocationConfiguration configuration,
                @NotNull final Converter<ResponseBody, ?> converter,
                @NotNull final Routine<ParcelableSelectable<Object>,
                        ParcelableSelectable<Object>> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mInvocationConfiguration = configuration;
            mChannelConfiguration = builderFromOutputChannel(configuration).apply();
            mConverter = converter;
        }

        @NotNull
        private OutputChannel<ParcelableSelectable<Object>> invokeCall(final Call<?> call) {

            return JRoutineCore.on(sInvocation)
                               .invocationConfiguration()
                               .with(mInvocationConfiguration)
                               .apply()
                               .asyncCall(call);
        }

        public <OUT> OutputChannel adapt(final Call<OUT> call) {

            final IOChannel<Object> ioChannel = JRoutineCore.io()
                                                            .channelConfiguration()
                                                            .with(mChannelConfiguration)
                                                            .apply()
                                                            .buildChannel();
            getRoutine().asyncCall(invokeCall(call))
                        .bind(new ConverterOutputConsumer(mConverter, ioChannel));
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

        /**
         * Constructor.
         *
         * @param configuration the invocation configuration.
         * @param converter     the body converter.
         * @param routine       the routine instance.
         * @param responseType  the response type.
         */
        private StreamChannelAdapter(@NotNull final InvocationConfiguration configuration,
                @NotNull final Converter<ResponseBody, ?> converter,
                @NotNull final Routine<ParcelableSelectable<Object>,
                        ParcelableSelectable<Object>> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mInvocationConfiguration = configuration;
            mChannelConfiguration = builderFromOutputChannel(configuration).apply();
            mConverter = converter;
        }

        public <OUT> StreamChannel adapt(final Call<OUT> call) {

            final Function<Function<OutputChannel<Call<OUT>>,
                    OutputChannel<ParcelableSelectable<Object>>>,
                    Function<OutputChannel<Call<OUT>>, OutputChannel<Object>>>
                    function =
                    new Function<Function<OutputChannel<Call<OUT>>,
                            OutputChannel<ParcelableSelectable<Object>>>,
                            Function<OutputChannel<Call<OUT>>, OutputChannel<Object>>>() {

                        public Function<OutputChannel<Call<OUT>>, OutputChannel<Object>> apply(
                                final Function<OutputChannel<Call<OUT>>,
                                        OutputChannel<ParcelableSelectable<Object>>> function) {

                            return wrap(function).andThen(
                                    new BindService(mChannelConfiguration, mConverter,
                                            getRoutine()));
                        }
                    };
            return Streams.streamOf(call)
                          .invocationConfiguration()
                          .with(mInvocationConfiguration)
                          .apply()
                          .map(sInvocation)
                          .simpleTransform(function);
        }
    }
}