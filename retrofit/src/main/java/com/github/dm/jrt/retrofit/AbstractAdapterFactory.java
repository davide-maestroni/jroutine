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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.object.annotation.Invoke;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.decorate;

/**
 * Abstract implementation of a call adapter factory supporting {@code Channel} and
 * {@code StreamBuilder} return types.
 * <br>
 * Note that the routines generated through the returned builders will ignore any input.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
public abstract class AbstractAdapterFactory extends CallAdapter.Factory {

    private static final MappingInvocation<Call<Object>, Object> sCallInvocation =
            new MappingInvocation<Call<Object>, Object>(null) {

                public void onInput(final Call<Object> input,
                        @NotNull final Channel<Object, ?> result) throws IOException {
                    final Response<Object> response = input.execute();
                    if (response.isSuccessful()) {
                        result.pass(response.body());

                    } else {
                        result.abort(new ErrorResponseException(response));
                    }
                }
            };

    private final InvocationConfiguration mConfiguration;

    private final CallAdapter.Factory mDelegateFactory;

    private final InvocationMode mInvocationMode;

    /**
     * Constructor.
     *
     * @param delegateFactory the delegate factory.
     * @param configuration   the invocation configuration.
     * @param invocationMode  the invocation mode.
     */
    protected AbstractAdapterFactory(@Nullable final CallAdapter.Factory delegateFactory,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode) {
        mDelegateFactory = delegateFactory;
        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
    }

    /**
     * Returns a lifting function making sure the specified call will the only stream output.
     *
     * @param call  the Retrofit call.
     * @param <OUT> the output data type.
     * @return the lifting function.
     */
    @NotNull
    public static <OUT> Function<Function<Channel<?, Object>, Channel<?, Object>>,
            Function<Channel<?, Object>, Channel<?, Call<OUT>>>> outputCall(
            @NotNull final Call<OUT> call) {
        return new LiftOutputCall<OUT>(ConstantConditions.notNull("call instance", call));
    }

    /**
     * Returns a type representing a channel with the specified output data type.
     *
     * @param outputType the output data type.
     * @return the parameterized type.
     */
    @NotNull
    protected static ParameterizedType getChannelType(@NotNull final Type outputType) {
        return new ChannelType(ConstantConditions.notNull("outputs type", outputType));
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
        return JRoutineCore.with(factory)
                           .invocationConfiguration()
                           .with(invocationConfiguration)
                           .applied()
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
        if ((Channel.class == rawType) || (StreamBuilder.class == rawType)) {
            return returnType.getActualTypeArguments()[1];
        }

        return null;
    }

    /**
     * Gets the adapter used to convert a Retrofit call into the method return type.
     *
     * @param configuration  the invocation configuration.
     * @param invocationMode the invocation mode.
     * @param returnRawType  the return raw type to be adapted.
     * @param responseType   the type of the call response.
     * @param annotations    the method annotations.
     * @param retrofit       the Retrofit instance.
     * @return the call adapter or null.
     */
    @Nullable
    protected CallAdapter<?> get(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {
        if (Channel.class == returnRawType) {
            return new ChannelAdapter(invocationMode,
                    buildRoutine(configuration, invocationMode, returnRawType, responseType,
                            annotations, retrofit), responseType);

        } else if (StreamBuilder.class == returnRawType) {
            return new StreamBuilderAdapter(invocationMode,
                    buildRoutine(configuration, invocationMode, returnRawType, responseType,
                            annotations, retrofit), responseType);
        }

        return null;
    }

    @NotNull
    private MappingInvocation<Call<Object>, Object> getFactory(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type responseType,
            @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {
        final CallAdapter.Factory delegateFactory = mDelegateFactory;
        if (delegateFactory == null) {
            return sCallInvocation;
        }

        @SuppressWarnings("unchecked") final CallAdapter<Channel<?, ?>> channelAdapter =
                (CallAdapter<Channel<?, ?>>) delegateFactory.get(new ChannelType(responseType),
                        annotations, retrofit);
        if (channelAdapter != null) {
            return new ChannelAdapterInvocation(
                    asArgs(delegateFactory, configuration, invocationMode, responseType,
                            annotations, retrofit), channelAdapter);
        }

        final CallAdapter<?> bodyAdapter = delegateFactory.get(responseType, annotations, retrofit);
        if (bodyAdapter != null) {
            return new BodyAdapterInvocation(
                    asArgs(delegateFactory, configuration, invocationMode, responseType,
                            annotations, retrofit), bodyAdapter);
        }

        throw new IllegalArgumentException(
                "The delegate factory does not support any of the required return types: "
                        + delegateFactory.getClass().getName());
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
     * Binding function making sure the specified call will the only stream output.
     *
     * @param <OUT> the output data type.
     */
    private static class BindOutputCall<OUT>
            implements Function<Channel<?, Object>, Channel<?, Call<OUT>>> {

        private final Call<OUT> mCall;

        /**
         * Constructor.
         *
         * @param call the Retrofit call.
         */
        private BindOutputCall(@NotNull final Call<OUT> call) {
            mCall = call;
        }

        public Channel<?, Call<OUT>> apply(final Channel<?, Object> objects) throws Exception {
            return JRoutineCore.io().of(mCall);
        }
    }

    /**
     * Mapping invocation employing a call adapter.
     */
    private static class BodyAdapterInvocation extends MappingInvocation<Call<Object>, Object> {

        private final CallAdapter<?> mCallAdapter;

        /**
         * Constructor.
         *
         * @param args        the factory arguments.
         * @param callAdapter the call adapter instance.
         */
        private BodyAdapterInvocation(@Nullable final Object[] args,
                @NotNull final CallAdapter<?> callAdapter) {
            super(args);
            mCallAdapter = callAdapter;
        }

        public void onInput(final Call<Object> input, @NotNull final Channel<Object, ?> result) {
            result.pass(mCallAdapter.adapt(input));
        }
    }

    /**
     * Channel adapter implementation.
     */
    private static class ChannelAdapter extends BaseAdapter<Channel> {

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private ChannelAdapter(@NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {
            super(routine, responseType);
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        public <OUT> Channel adapt(final Call<OUT> call) {
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
     * Mapping invocation employing a call adapter.
     */
    private static class ChannelAdapterInvocation extends MappingInvocation<Call<Object>, Object> {

        private final CallAdapter<Channel<?, ?>> mCallAdapter;

        /**
         * Constructor.
         *
         * @param args        the factory arguments.
         * @param callAdapter the call adapter instance.
         */
        private ChannelAdapterInvocation(@Nullable final Object[] args,
                @NotNull final CallAdapter<Channel<?, ?>> callAdapter) {
            super(args);
            mCallAdapter = callAdapter;
        }

        public void onInput(final Call<Object> input, @NotNull final Channel<Object, ?> result) {
            result.pass(mCallAdapter.adapt(input));
        }
    }

    /**
     * Parameterized type implementation mimicking a channel type.
     */
    private static class ChannelType implements ParameterizedType {

        private final Type[] mTypeArguments;

        /**
         * Constructor.
         *
         * @param outputType the output data type.
         */
        private ChannelType(@NotNull final Type outputType) {
            mTypeArguments = new Type[]{Object.class, outputType};
        }

        public Type[] getActualTypeArguments() {
            return mTypeArguments.clone();
        }

        public Type getRawType() {
            return Channel.class;
        }

        public Type getOwnerType() {
            return null;
        }
    }

    /**
     * Lifting function making sure the specified call will the only stream output.
     *
     * @param <OUT> the output data type.
     */
    private static class LiftOutputCall<OUT> implements
            Function<Function<Channel<?, Object>, Channel<?, Object>>, Function<Channel<?,
                    Object>, Channel<?, Call<OUT>>>> {

        private final Call<OUT> mCall;

        /**
         * Constructor.
         *
         * @param call the Retrofit call.
         */
        private LiftOutputCall(@NotNull final Call<OUT> call) {
            mCall = call;
        }

        public Function<Channel<?, Object>, Channel<?, Call<OUT>>> apply(
                final Function<Channel<?, Object>, Channel<?, Object>> bindingFunction) throws
                Exception {
            return decorate(bindingFunction).andThen(new BindOutputCall<OUT>(mCall));
        }
    }

    /**
     * Stream builder adapter implementation.
     */
    private static class StreamBuilderAdapter extends BaseAdapter<StreamBuilder> {

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private StreamBuilderAdapter(@NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {
            super(routine, responseType);
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        public <OUT> StreamBuilder adapt(final Call<OUT> call) {
            return JRoutineStream.withStream()
                                 .lift(outputCall(call))
                                 .invocationMode(mInvocationMode)
                                 .map(getRoutine());
        }
    }
}
