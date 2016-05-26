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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.annotation.Invoke;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a call adapter factory supporting {@code OutputChannel} and
 * {@code StreamChannel} return types.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
public abstract class AbstractAdapterFactory extends CallAdapter.Factory
        implements ConfigurableAdapterFactory {

    private static final MappingInvocation<Call<Object>, Object> sCallInvocation =
            new MappingInvocation<Call<Object>, Object>(null) {

                public void onInput(final Call<Object> input,
                        @NotNull final ResultChannel<Object> result) throws IOException {

                    result.pass(input.execute().body());
                }
            };

    private final InvocationConfiguration mConfiguration;

    private final ConfigurableAdapterFactory mDelegateFactory;

    private final InvocationMode mInvocationMode;

    /**
     * Constructor.
     *
     * @param delegateFactory the delegate factory.
     * @param configuration   the invocation configuration.
     * @param invocationMode  the invocation mode.
     */
    protected AbstractAdapterFactory(@Nullable final ConfigurableAdapterFactory delegateFactory,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode) {

        mDelegateFactory = delegateFactory;
        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
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

        if (returnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) returnType;
            final Type responseType = extractResponseType(parameterizedType);
            if (responseType != null) {
                return get(mConfiguration, invocationMode, parameterizedType.getRawType(),
                        responseType, annotations, retrofit);
            }

        } else if (returnType instanceof Class) {
            return get(mConfiguration, invocationMode, returnType, Object.class, annotations,
                    retrofit);
        }

        return null;
    }

    @Nullable
    public CallAdapter<?> get(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        if (OutputChannel.class == returnRawType) {
            return new OutputChannelAdapter(invocationMode,
                    buildRoutine(configuration, invocationMode, returnRawType, responseType,
                            annotations, retrofit), responseType);

        } else if (StreamChannel.class == returnRawType) {
            return new StreamChannelAdapter(invocationMode,
                    buildRoutine(configuration, invocationMode, returnRawType, responseType,
                            annotations, retrofit), responseType);
        }

        return null;
    }

    /**
     * Builds and returns a routine instance handling Retrofit calls.
     *
     * @param configuration  the invocation configuration.
     * @param invocationMode the invocation mode.
     * @param returnRawType  the return raw type to be adapted.
     * @param responseType   the type of the call response.
     * @param annotations    the method annotations.
     * @param retrofit       the Retrofit instance.
     * @return the routine instance.
     */
    @NotNull
    @SuppressWarnings("UnusedParameters")
    protected Routine<? extends Call<?>, ?> buildRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(configuration, annotations);
        final MappingInvocation<Call<Object>, Object> factory =
                getFactory(configuration, invocationMode, responseType, annotations, retrofit);
        return JRoutineCore.on(factory)
                           .invocationConfiguration()
                           .with(invocationConfiguration)
                           .apply()
                           .buildRoutine();
    }

    /**
     * Extracts the type of response from the method return type.
     *
     * @param returnType the return type to be adapted.
     * @return the response type or null.
     */
    @Nullable
    protected Type extractResponseType(@NotNull final ParameterizedType returnType) {

        final Type rawType = returnType.getRawType();
        if (OutputChannel.class == rawType) {
            return returnType.getActualTypeArguments()[0];

        } else if (StreamChannel.class == rawType) {
            return returnType.getActualTypeArguments()[1];
        }

        return null;
    }

    @NotNull
    private MappingInvocation<Call<Object>, Object> getFactory(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type responseType,
            @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {

        final ConfigurableAdapterFactory delegateFactory = mDelegateFactory;
        if (delegateFactory == null) {
            return sCallInvocation;
        }

        @SuppressWarnings("unchecked") final CallAdapter<OutputChannel<?>> callAdapter =
                (CallAdapter<OutputChannel<?>>) delegateFactory.get(configuration, invocationMode,
                        OutputChannel.class, responseType, annotations, retrofit);
        return (callAdapter != null) ? new CallAdapterInvocation(
                asArgs(delegateFactory, configuration, invocationMode, responseType, annotations,
                        retrofit), callAdapter) : sCallInvocation;
    }

    /**
     * Base adapter implementation.
     */
    protected static abstract class BaseAdapter<T> implements CallAdapter<T> {

        private final Type mResponseType;

        private final Routine<Call<?>, ?> mRoutine;

        /**
         * Constructor.
         *
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        @SuppressWarnings("unchecked")
        protected BaseAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            mResponseType = ConstantConditions.notNull("response type", responseType);
            mRoutine =
                    ConstantConditions.notNull("routine instance", (Routine<Call<?>, ?>) routine);
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
     * Mapping invocation employing a call adapter.
     */
    private static class CallAdapterInvocation extends MappingInvocation<Call<Object>, Object> {

        private final CallAdapter<OutputChannel<?>> mCallAdapter;

        /**
         * Constructor.
         *
         * @param args        the factory arguments.
         * @param callAdapter the call adapter instance.
         */
        private CallAdapterInvocation(@Nullable final Object[] args,
                @NotNull final CallAdapter<OutputChannel<?>> callAdapter) {

            super(args);
            mCallAdapter = callAdapter;
        }

        public void onInput(final Call<Object> input, @NotNull final ResultChannel<Object> result) {

            result.pass(mCallAdapter.adapt(input));
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
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        public <OUT> OutputChannel adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final Routine<Call<?>, ?> routine = getRoutine();
            if ((invocationMode == InvocationMode.ASYNC) || (invocationMode
                    == InvocationMode.PARALLEL)) {
                return routine.asyncCall(call);
            }

            return routine.syncCall(call);
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
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        public <OUT> StreamChannel adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final StreamChannel<Call<OUT>, Call<OUT>> stream = Streams.streamOf(call);
            if ((invocationMode == InvocationMode.ASYNC) || (invocationMode
                    == InvocationMode.PARALLEL)) {
                stream.async();

            } else {
                stream.sync();
            }

            return stream.map(getRoutine()).invocationMode(invocationMode);
        }
    }
}
