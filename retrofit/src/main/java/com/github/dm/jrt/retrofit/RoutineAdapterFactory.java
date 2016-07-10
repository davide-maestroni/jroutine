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

import com.github.dm.jrt.core.builder.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import retrofit2.CallAdapter;

/**
 * Abstract implementation of a call adapter factory supporting {@code Channel} and
 * {@code StreamBuilder} return types.
 * <br>
 * Note that the routines generated through the returned builders will ignore any input.
 * <p>
 * Created by davide-maestroni on 03/26/2016.
 */
public class RoutineAdapterFactory extends AbstractAdapterFactory {

    private static final RoutineAdapterFactory sFactory =
            new RoutineAdapterFactory(null, InvocationConfiguration.defaultConfiguration(),
                    InvocationMode.ASYNC);

    /**
     * Constructor.
     *
     * @param delegateFactory the delegate factory.
     * @param configuration   the invocation configuration.
     * @param invocationMode  the invocation mode.
     */
    private RoutineAdapterFactory(@Nullable final CallAdapter.Factory delegateFactory,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode) {
        super(delegateFactory, configuration, invocationMode);
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
    public static RoutineAdapterFactory defaultFactory() {
        return sFactory;
    }

    /**
     * Builder of routine adapter factory instances.
     * <p>
     * The options set through the builder configuration will be applied to all the routine handling
     * the Retrofit calls, unless they are overwritten by specific annotations.
     *
     * @see Builders#getInvocationMode(Method)
     * @see Builders#withAnnotations(InvocationConfiguration, Annotation...)
     */
    public static class Builder implements InvocationConfigurable<Builder>, Configurable<Builder> {

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.defaultConfiguration();

        private CallAdapter.Factory mDelegateFactory;

        private InvocationMode mInvocationMode = InvocationMode.ASYNC;

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
        public RoutineAdapterFactory buildFactory() {
            return new RoutineAdapterFactory(mDelegateFactory, mConfiguration, mInvocationMode);
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
        public InvocationConfiguration.Builder<? extends Builder> invocationConfiguration() {
            return new InvocationConfiguration.Builder<Builder>(this, mConfiguration);
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
    }
}
