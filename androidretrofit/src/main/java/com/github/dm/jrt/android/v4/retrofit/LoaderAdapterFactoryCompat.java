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
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.object.builder.AndroidBuilders;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamChannelCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamsCompat;
import com.github.dm.jrt.core.builder.ConfigurableBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.retrofit.AbstractAdapterFactory;
import com.github.dm.jrt.retrofit.ConfigurableAdapterFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Implementation of a call adapter factory supporting {@code OutputChannel}, {@code StreamChannel}
 * and {@code LoaderStreamChannelCompat} return types.
 * <br>
 * If properly configured, the routine invocations will run in a dedicated Android loader.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
public class LoaderAdapterFactoryCompat extends AbstractAdapterFactory {

    private static final TemplateContextInvocation<Call<Object>, Object> sCallInvocation =
            new TemplateContextInvocation<Call<Object>, Object>() {

                public void onInput(final Call<Object> input,
                        @NotNull final ResultChannel<Object> result) throws IOException {

                    result.pass(input.execute().body());
                }
            };

    private static final ContextInvocationFactory<Call<Object>, Object> sCallInvocationFactory =
            new ContextInvocationFactory<Call<Object>, Object>(null) {

                @NotNull
                @Override
                public ContextInvocation<Call<Object>, Object> newInvocation() {

                    return sCallInvocation;
                }
            };

    private static final LoaderAdapterFactoryCompat sFactory =
            new LoaderAdapterFactoryCompat(null, null,
                    InvocationConfiguration.defaultConfiguration(),
                    LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC);

    private final ConfigurableAdapterFactory mDelegateFactory;

    private final LoaderConfiguration mLoaderConfiguration;

    private final LoaderContextCompat mLoaderContext;

    /**
     * Constructor.
     *
     * @param context                 the loader context.
     * @param delegateFactory         the delegate factory.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the service configuration.
     * @param invocationMode          the invocation mode.
     */
    private LoaderAdapterFactoryCompat(@Nullable final LoaderContextCompat context,
            @Nullable final ConfigurableAdapterFactory delegateFactory,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode) {

        super(delegateFactory, invocationConfiguration, invocationMode);
        mLoaderContext = context;
        mLoaderConfiguration = loaderConfiguration;
        mDelegateFactory = delegateFactory;
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
     * Returns the a factory instance with default configuration.
     *
     * @param context the loader context.
     * @return the factory instance.
     */
    @NotNull
    public static LoaderAdapterFactoryCompat with(@Nullable final LoaderContextCompat context) {

        return (context == null) ? sFactory : new LoaderAdapterFactoryCompat(context, null,
                InvocationConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public CallAdapter<?> get(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        if (LoaderStreamChannelCompat.class == returnRawType) {
            return new LoaderStreamChannelAdapter(mLoaderContext, invocationMode,
                    buildRoutine(configuration, invocationMode, returnRawType, responseType,
                            annotations, retrofit), responseType);
        }

        final CallAdapter<?> callAdapter =
                super.get(configuration, invocationMode, returnRawType, responseType, annotations,
                        retrofit);
        return (callAdapter != null) ? new CallAdapterDecorator<Object>(
                (CallAdapter<Object>) callAdapter) : null;
    }

    @NotNull
    @Override
    protected Routine<? extends Call<?>, ?> buildRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        final LoaderContextCompat loaderContext = mLoaderContext;
        if (loaderContext != null) {
            // Use annotations to configure the routine
            final InvocationConfiguration invocationConfiguration =
                    Builders.withAnnotations(configuration, annotations);
            final LoaderConfiguration loaderConfiguration =
                    AndroidBuilders.withAnnotations(mLoaderConfiguration, annotations);
            final ContextInvocationFactory<Call<Object>, Object> factory =
                    getFactory(configuration, invocationMode, responseType, annotations, retrofit);
            return JRoutineLoaderCompat.with(loaderContext)
                                       .on(factory)
                                       .invocationConfiguration()
                                       .with(invocationConfiguration)
                                       .apply()
                                       .loaderConfiguration()
                                       .with(loaderConfiguration)
                                       .apply()
                                       .buildRoutine();
        }

        return super.buildRoutine(configuration, invocationMode, returnRawType, responseType,
                annotations, retrofit);
    }

    @Nullable
    @Override
    protected Type extractResponseType(@NotNull final ParameterizedType returnType) {

        if (LoaderStreamChannelCompat.class == returnType.getRawType()) {
            return returnType.getActualTypeArguments()[1];
        }

        return super.extractResponseType(returnType);
    }

    @NotNull
    private ContextInvocationFactory<Call<Object>, Object> getFactory(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type responseType,
            @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {

        final ConfigurableAdapterFactory delegateFactory = mDelegateFactory;
        if (delegateFactory == null) {
            return sCallInvocationFactory;
        }

        @SuppressWarnings("unchecked") final CallAdapter<OutputChannel> callAdapter =
                (CallAdapter<OutputChannel>) delegateFactory.get(configuration, invocationMode,
                        OutputChannel.class, responseType, annotations, retrofit);
        return (callAdapter != null) ? new CallAdapterInvocationFactory(
                asArgs(delegateFactory, configuration, invocationMode, responseType, annotations,
                        retrofit), callAdapter) : sCallInvocationFactory;
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

        private ConfigurableAdapterFactory mDelegateFactory;

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

            return new LoaderAdapterFactoryCompat(mLoaderContext, mDelegateFactory,
                    mInvocationConfiguration, mLoaderConfiguration, mInvocationMode);
        }

        /**
         * Sets the delegate factory to be used to execute the calls.
         *
         * @param factory the factory instance.
         * @return this builder.
         */
        @NotNull
        public Builder delegateFactory(@Nullable final ConfigurableAdapterFactory factory) {

            mDelegateFactory = factory;
            return this;
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
     * Call adapter decorator making the adapted instance comparable.
     *
     * @param <T> the adapted type.
     */
    private static class CallAdapterDecorator<T> implements CallAdapter<T> {

        private final CallAdapter<T> mAdapter;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped adapter instance.
         */
        private CallAdapterDecorator(@NotNull final CallAdapter<T> wrapped) {

            mAdapter = wrapped;
        }

        public Type responseType() {

            return mAdapter.responseType();
        }

        public <R> T adapt(final Call<R> call) {

            return mAdapter.adapt(ComparableCall.of(call));
        }
    }

    /**
     * Context invocation employing a call adapter.
     */
    private static class CallAdapterInvocation
            extends TemplateContextInvocation<Call<Object>, Object> {

        private final CallAdapter<OutputChannel> mCallAdapter;

        /**
         * Constructor.
         *
         * @param callAdapter the call adapter instance.
         */
        private CallAdapterInvocation(@NotNull final CallAdapter<OutputChannel> callAdapter) {

            mCallAdapter = callAdapter;
        }

        public void onInput(final Call<Object> input, @NotNull final ResultChannel<Object> result) {

            result.pass(mCallAdapter.adapt(input));
        }
    }

    /**
     * Context invocation factory employing a call adapter.
     */
    private static class CallAdapterInvocationFactory
            extends ContextInvocationFactory<Call<Object>, Object> {

        private final TemplateContextInvocation<Call<Object>, Object> mInvocation;

        /**
         * Constructor.
         *
         * @param args        the factory arguments.
         * @param callAdapter the call adapter instance.
         */
        private CallAdapterInvocationFactory(@Nullable final Object[] args,
                @NotNull final CallAdapter<OutputChannel> callAdapter) {

            super(args);
            mInvocation = new CallAdapterInvocation(callAdapter);
        }

        @NotNull
        @Override
        public ContextInvocation<Call<Object>, Object> newInvocation() {

            return mInvocation;
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
            final Call<OUT> comparableCall = ComparableCall.of(call);
            final LoaderStreamChannelCompat<Call<OUT>, Call<OUT>> stream =
                    LoaderStreamsCompat.streamOf(comparableCall).with(mContext);
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
