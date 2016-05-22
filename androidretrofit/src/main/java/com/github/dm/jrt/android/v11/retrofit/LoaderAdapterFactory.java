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

package com.github.dm.jrt.android.v11.retrofit;

import com.github.dm.jrt.android.core.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.object.builder.AndroidBuilders;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.LoaderStreamChannel;
import com.github.dm.jrt.android.v11.stream.LoaderStreams;
import com.github.dm.jrt.core.builder.ConfigurableBuilder;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.retrofit.AbstractAdapterFactory;

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

/**
 * Implementation of a call adapter factory supporting {@code OutputChannel}, {@code StreamChannel}
 * and {@code LoaderStreamChannel} return types.
 * <br>
 * If properly configured, the routine invocations will run in a dedicated Android loader.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
public class LoaderAdapterFactory extends AbstractAdapterFactory {

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

    private static final LoaderAdapterFactory sFactory =
            new LoaderAdapterFactory(null, InvocationConfiguration.defaultConfiguration(),
                    LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC);

    private final LoaderConfiguration mLoaderConfiguration;

    private final LoaderContext mLoaderContext;

    /**
     * Constructor.
     *
     * @param context                 the loader context.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the service configuration.
     * @param invocationMode          the invocation mode.
     */
    private LoaderAdapterFactory(@Nullable final LoaderContext context,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode) {

        super(invocationConfiguration, invocationMode);
        mLoaderContext = context;
        mLoaderConfiguration = loaderConfiguration;
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
    public static LoaderAdapterFactory with(@Nullable final LoaderContext context) {

        return (context == null) ? sFactory
                : new LoaderAdapterFactory(context, InvocationConfiguration.defaultConfiguration(),
                        LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC);
    }

    @NotNull
    @Override
    protected Routine<? extends Call<?>, ?> buildRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type responseType,
            @NotNull final Type returnRawType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        final LoaderContext loaderContext = mLoaderContext;
        if (loaderContext != null) {
            // Use annotations to configure the routine
            final InvocationConfiguration invocationConfiguration =
                    Builders.withAnnotations(configuration, annotations);
            final LoaderConfiguration loaderConfiguration =
                    AndroidBuilders.withAnnotations(mLoaderConfiguration, annotations);
            return JRoutineLoader.with(loaderContext)
                                 .on(sCallInvocationFactory)
                                 .invocationConfiguration()
                                 .with(invocationConfiguration)
                                 .apply()
                                 .loaderConfiguration()
                                 .with(loaderConfiguration)
                                 .apply()
                                 .buildRoutine();
        }

        return super.buildRoutine(configuration, invocationMode, responseType, returnRawType,
                annotations, retrofit);
    }

    @Nullable
    @Override
    protected Type extractResponseType(@NotNull final ParameterizedType returnType) {

        if (LoaderStreamChannel.class == returnType.getRawType()) {
            return returnType.getActualTypeArguments()[1];
        }

        return super.extractResponseType(returnType);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    protected CallAdapter<?> getAdapter(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type responseType,
            @NotNull final Type returnRawType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        if (LoaderStreamChannel.class == returnRawType) {
            return new LoaderStreamChannelAdapter(mLoaderContext, invocationMode,
                    buildRoutine(configuration, invocationMode, responseType, returnRawType,
                            annotations, retrofit), responseType);
        }

        final CallAdapter<?> callAdapter =
                super.getAdapter(configuration, invocationMode, responseType, returnRawType,
                        annotations, retrofit);
        return (callAdapter != null) ? new CallAdapterDecorator<Object>(
                (CallAdapter<Object>) callAdapter) : null;
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

        private LoaderContext mLoaderContext;

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
        public LoaderAdapterFactory buildFactory() {

            return new LoaderAdapterFactory(mLoaderContext, mInvocationConfiguration,
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
        public Builder with(@Nullable final LoaderContext context) {

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
     * Loader stream channel adapter implementation.
     */
    private static class LoaderStreamChannelAdapter extends BaseAdapter<LoaderStreamChannel> {

        private final LoaderContext mContext;

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param context        the loader context.
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private LoaderStreamChannelAdapter(@Nullable final LoaderContext context,
                @NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {

            super(routine, responseType);
            mContext = context;
            mInvocationMode = invocationMode;
        }

        public <OUT> LoaderStreamChannel adapt(final Call<OUT> call) {

            final InvocationMode invocationMode = mInvocationMode;
            final Call<OUT> comparableCall = ComparableCall.of(call);
            final LoaderStreamChannel<Call<OUT>, Call<OUT>> stream =
                    LoaderStreams.streamOf(comparableCall).with(mContext);
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
