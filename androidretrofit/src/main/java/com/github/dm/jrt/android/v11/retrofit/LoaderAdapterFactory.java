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

import com.github.dm.jrt.android.core.builder.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.object.builder.AndroidBuilders;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.android.retrofit.ContextAdapterFactory;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.JRoutineLoaderStream;
import com.github.dm.jrt.android.v11.stream.LoaderStreamBuilder;
import com.github.dm.jrt.core.builder.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.stream.modifier.Modifiers.output;

/**
 * Implementation of a call adapter factory supporting {@code Channel}, {@code StreamBuilder} and
 * {@code LoaderStreamBuilder} return types.
 * <br>
 * The routine invocations will run in a dedicated Android loader.
 * <br>
 * Note that the routines generated through the returned builders will ignore any input.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
public class LoaderAdapterFactory extends ContextAdapterFactory {

    private final LoaderConfiguration mLoaderConfiguration;

    private final LoaderContext mLoaderContext;

    /**
     * Constructor.
     *
     * @param context                 the loader context.
     * @param delegateFactory         the delegate factory.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the service configuration.
     * @param invocationMode          the invocation mode.
     */
    private LoaderAdapterFactory(@NotNull final LoaderContext context,
            @Nullable final CallAdapter.Factory delegateFactory,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode) {
        super(delegateFactory, invocationConfiguration, invocationMode);
        mLoaderContext = context;
        mLoaderConfiguration = loaderConfiguration;
    }

    /**
     * Returns an adapter factory builder.
     *
     * @param context the loader context.
     * @return the builder instance.
     */
    @NotNull
    public static Builder on(@NotNull final LoaderContext context) {
        return new Builder(context);
    }

    @NotNull
    @Override
    protected Routine<? extends Call<?>, ?> buildRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {
        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(configuration, annotations);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.withAnnotations(mLoaderConfiguration, annotations);
        final ContextInvocationFactory<Call<Object>, Object> factory =
                getFactory(configuration, invocationMode, responseType, annotations, retrofit);
        return JRoutineLoader.on(mLoaderContext)
                             .with(factory)
                             .invocationConfiguration()
                             .with(invocationConfiguration)
                             .configured()
                             .loaderConfiguration()
                             .with(loaderConfiguration)
                             .configured()
                             .buildRoutine();
    }

    @Nullable
    @Override
    protected Type extractResponseType(@NotNull final ParameterizedType returnType) {
        if (LoaderStreamBuilder.class == returnType.getRawType()) {
            return returnType.getActualTypeArguments()[1];
        }

        return super.extractResponseType(returnType);
    }

    @Nullable
    @Override
    protected CallAdapter<?> get(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @NotNull final Type returnRawType,
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {
        if (LoaderStreamBuilder.class == returnRawType) {
            return new LoaderStreamBuilderAdapter(invocationMode,
                    buildRoutine(configuration, invocationMode, returnRawType, responseType,
                            annotations, retrofit), responseType);
        }

        final CallAdapter<?> callAdapter =
                super.get(configuration, invocationMode, returnRawType, responseType, annotations,
                        retrofit);
        return (callAdapter != null) ? ComparableCall.wrap(callAdapter) : null;
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
            implements InvocationConfigurable<Builder>, LoaderConfigurable<Builder>,
            InvocationConfiguration.Configurable<Builder>,
            LoaderConfiguration.Configurable<Builder> {

        private final LoaderContext mLoaderContext;

        private CallAdapter.Factory mDelegateFactory;

        private InvocationConfiguration mInvocationConfiguration =
                InvocationConfiguration.defaultConfiguration();

        private InvocationMode mInvocationMode = InvocationMode.ASYNC;

        private LoaderConfiguration mLoaderConfiguration =
                LoaderConfiguration.defaultConfiguration();

        /**
         * Constructor.
         *
         * @param context the loader context.
         */
        private Builder(@NotNull final LoaderContext context) {
            mLoaderContext = ConstantConditions.notNull("loader context", context);
        }

        @NotNull
        @Override
        public Builder apply(@NotNull final InvocationConfiguration configuration) {
            mInvocationConfiguration =
                    ConstantConditions.notNull("invocation configuration", configuration);
            return this;
        }

        @NotNull
        @Override
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
            return new LoaderAdapterFactory(mLoaderContext, mDelegateFactory,
                    mInvocationConfiguration, mLoaderConfiguration, mInvocationMode);
        }

        /**
         * Sets the delegate factory to be used to execute the calls.
         *
         * @param factory the factory instance.
         * @return this builder.
         */
        @NotNull
        public Builder delegateFactory(@Nullable final CallAdapter.Factory factory) {
            mDelegateFactory = factory;
            return this;
        }

        @NotNull
        @Override
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
        @Override
        public LoaderConfiguration.Builder<? extends Builder> loaderConfiguration() {
            return new LoaderConfiguration.Builder<Builder>(this, mLoaderConfiguration);
        }
    }

    /**
     * Loader stream builder adapter implementation.
     */
    private static class LoaderStreamBuilderAdapter extends BaseAdapter<LoaderStreamBuilder> {

        private final InvocationMode mInvocationMode;

        /**
         * Constructor.
         *
         * @param invocationMode the invocation mode.
         * @param routine        the routine instance.
         * @param responseType   the response type.
         */
        private LoaderStreamBuilderAdapter(@NotNull final InvocationMode invocationMode,
                @NotNull final Routine<? extends Call<?>, ?> routine,
                @NotNull final Type responseType) {
            super(routine, responseType);
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        @Override
        public <OUT> LoaderStreamBuilder adapt(final Call<OUT> call) {
            return JRoutineLoaderStream.withStream()
                                       .let(output(ComparableCall.of(call)))
                                       .invocationMode(mInvocationMode)
                                       .map(getRoutine());
        }
    }
}
