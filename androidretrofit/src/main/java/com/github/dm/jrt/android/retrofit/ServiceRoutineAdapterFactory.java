/*
 * Copyright (c) 2016. Davide Maestroni
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

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.builder.ServiceConfigurableBuilder;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.retrofit.ExecuteCall;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;

/**
 * Implementation of a call adapter factory supporting {@code OutputChannel} and
 * {@code StreamChannel} return types.
 * <br>
 * If properly configured, the routine invocations will run on a dedicated Android service.
 * <p>
 * Created by davide-maestroni on 05/16/2016.
 */
public class ServiceRoutineAdapterFactory extends CallAdapter.Factory {

    private static final ExecuteCall<?> sCallInvocation = new ExecuteCall<Object>();

    private static final TargetInvocationFactory<Call<Object>, Object> sCallTarget =
            factoryOf(new ClassToken<ExecuteCall<Object>>() {});

    private static final ServiceRoutineAdapterFactory sDefault =
            new ServiceRoutineAdapterFactory(null, InvocationConfiguration.defaultConfiguration(),
                    ServiceConfiguration.defaultConfiguration());

    private final InvocationConfiguration mInvocationConfiguration;

    private final ServiceConfiguration mServiceConfiguration;

    private final ServiceContext mServiceContext;

    /**
     * Constructor.
     *
     * @param context                 the service context.
     * @param invocationConfiguration the invocation configuration.
     * @param serviceConfiguration    the service configuration.
     */
    private ServiceRoutineAdapterFactory(@Nullable final ServiceContext context,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final ServiceConfiguration serviceConfiguration) {

        mServiceContext = context;
        mInvocationConfiguration = invocationConfiguration;
        mServiceConfiguration = serviceConfiguration;
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
    public static ServiceRoutineAdapterFactory defaultFactory() {

        return sDefault;
    }

    /**
     * Returns the a factory instance with default configuration.
     *
     * @param context the service context.
     * @return the factory instance.
     */
    @NotNull
    public static ServiceRoutineAdapterFactory defaultFactory(
            @Nullable final ServiceContext context) {

        return (context == null) ? defaultFactory() : new ServiceRoutineAdapterFactory(context,
                InvocationConfiguration.defaultConfiguration(),
                ServiceConfiguration.defaultConfiguration());
    }

    @Override
    public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
            final Retrofit retrofit) {

        if (returnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) returnType;
            final Type rawType = parameterizedType.getRawType();
            if (StreamChannel.class == rawType) {
                return new StreamChannelAdapter(buildRoutine(annotations),
                        parameterizedType.getActualTypeArguments()[1]);

            } else if (OutputChannel.class == rawType) {
                return new OutputChannelAdapter(buildRoutine(annotations),
                        parameterizedType.getActualTypeArguments()[0]);
            }

        } else if (returnType instanceof Class) {
            if (StreamChannel.class == returnType) {
                return new StreamChannelAdapter(buildRoutine(annotations), Object.class);

            } else if (OutputChannel.class == returnType) {
                return new OutputChannelAdapter(buildRoutine(annotations), Object.class);
            }
        }

        return null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Routine<? extends Call<?>, ?> buildRoutine(@NotNull final Annotation[] annotations) {

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, annotations);
        final ServiceContext serviceContext = mServiceContext;
        if (serviceContext == null) {
            return JRoutineCore.on(sCallInvocation)
                               .invocationConfiguration()
                               .with(invocationConfiguration)
                               .apply()
                               .buildRoutine();
        }

        return JRoutineService.with(serviceContext)
                              .on(sCallTarget)
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
     * @see com.github.dm.jrt.object.annotation Annotations
     */
    public static class Builder implements ServiceConfigurableBuilder<Builder>,
            InvocationConfiguration.Configurable<Builder>,
            ServiceConfiguration.Configurable<Builder> {

        private InvocationConfiguration mInvocationConfiguration =
                InvocationConfiguration.defaultConfiguration();

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
        public ServiceRoutineAdapterFactory buildFactory() {

            return new ServiceRoutineAdapterFactory(mServiceContext, mInvocationConfiguration,
                    mServiceConfiguration);
        }

        @NotNull
        public InvocationConfiguration.Builder<? extends Builder> invocationConfiguration() {

            return new InvocationConfiguration.Builder<Builder>(this, mInvocationConfiguration);
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
         */
        @NotNull
        public Builder with(@Nullable final ServiceContext context) {

            mServiceContext = context;
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
     * Output channel adapter implementation.
     */
    private static class OutputChannelAdapter extends BaseAdapter<OutputChannel> {

        /**
         * Constructor.
         *
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        private OutputChannelAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
        }

        public <R> OutputChannel adapt(final Call<R> call) {

            return getRoutine().asyncCall(call);
        }
    }

    /**
     * Stream channel adapter implementation.
     */
    private static class StreamChannelAdapter extends BaseAdapter<StreamChannel> {

        /**
         * Constructor.
         *
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        private StreamChannelAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
        }

        public <R> StreamChannel adapt(final Call<R> call) {

            return Streams.streamOf(call).map(getRoutine());
        }
    }
}
