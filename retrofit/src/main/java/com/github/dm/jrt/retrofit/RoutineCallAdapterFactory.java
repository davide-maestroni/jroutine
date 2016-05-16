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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ConfigurableBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Abstract implementation of a call adapter factory providing boilerplate code for common features,
 * like extraction of the response type and adapter instantiation.
 * <p>
 * Created by davide-maestroni on 03/26/2016.
 */
public class RoutineCallAdapterFactory extends CallAdapter.Factory {

    private static final ExecuteCall<?> sCallInvocation = new ExecuteCall<Object>();

    private final InvocationConfiguration mConfiguration;

    /**
     * Constructor.
     */
    private RoutineCallAdapterFactory(@NotNull final InvocationConfiguration configuration) {

        mConfiguration = configuration;
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
    private Routine<? extends Call<?>, ?> buildRoutine(@NotNull final Annotation[] annotations) {

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mConfiguration, annotations);
        return JRoutineCore.on(sCallInvocation)
                           .invocationConfiguration()
                           .with(invocationConfiguration)
                           .apply()
                           .buildRoutine();
    }

    public static class Builder implements ConfigurableBuilder<Builder>, Configurable<Builder> {

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.defaultConfiguration();

        /**
         * Constructor.
         */
        private Builder() {

        }

        @NotNull
        public Builder apply(@NotNull final InvocationConfiguration configuration) {

            mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
            return this;
        }

        /**
         * Builds and return a new factory instance.
         *
         * @return the factory instance.
         */
        @NotNull
        public RoutineCallAdapterFactory buildFactory() {

            return new RoutineCallAdapterFactory(mConfiguration);
        }

        @NotNull
        public InvocationConfiguration.Builder<? extends Builder> invocationConfiguration() {

            return new InvocationConfiguration.Builder<Builder>(this, mConfiguration);
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
