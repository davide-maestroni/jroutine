/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.builder;

import android.os.Looper;

import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.runner.Runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class storing the service configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * Created by davide on 20/04/15.
 */
public class ServiceConfiguration {

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final ServiceConfiguration EMPTY_CONFIGURATION = builder().buildConfiguration();

    private final Class<? extends Log> mLogClass;

    private final Looper mLooper;

    private final Class<? extends Runner> mRunnerClass;

    private final Class<? extends RoutineService> mServiceClass;

    /**
     * Constructor.
     *
     * @param looper       the looper instance.
     * @param serviceClass the service class.
     * @param runnerClass  the runner class.
     * @param logClass     the log class.
     */
    private ServiceConfiguration(@Nullable final Looper looper,
            @Nullable final Class<? extends RoutineService> serviceClass,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Class<? extends Log> logClass) {

        mServiceClass = serviceClass;
        mLooper = looper;
        mRunnerClass = runnerClass;
        mLogClass = logClass;
    }

    /**
     * Returns a service configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Returns a service configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder builderFrom(@Nonnull final ServiceConfiguration initialConfiguration) {

        return new Builder(initialConfiguration);
    }

    /**
     * Returns the specified configuration or the empty one if the former is null.
     *
     * @param configuration the invocation configuration.
     * @return the configuration.
     */
    @Nonnull
    public static ServiceConfiguration notNull(@Nullable final ServiceConfiguration configuration) {

        return (configuration != null) ? configuration : EMPTY_CONFIGURATION;
    }

    /**
     * Returns a service configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder builderFrom() {

        return new Builder(this);
    }

    /**
     * Builder of service configurations.
     */
    public static class Builder {

        private Class<? extends Log> mLogClass;

        private Looper mLooper;

        private Class<? extends Runner> mRunnerClass;

        private Class<? extends RoutineService> mServiceClass;

        /**
         * Constructor.
         */
        private Builder() {

        }

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial configuration.
         * @throws java.lang.NullPointerException if the specified configuration instance is null.
         */
        private Builder(@Nonnull final ServiceConfiguration initialConfiguration) {

            mLooper = initialConfiguration.mLooper;
            mServiceClass = initialConfiguration.mServiceClass;
            mRunnerClass = initialConfiguration.mRunnerClass;
            mLogClass = initialConfiguration.mLogClass;
        }

        /**
         * Builds and return the configuration instance.
         *
         * @return the service configuration instance.
         */
        @Nonnull
        public ServiceConfiguration buildConfiguration() {

            return new ServiceConfiguration(mLooper, mServiceClass, mRunnerClass, mLogClass);
        }

        /**
         * Applies the specified configuration to this builder.
         *
         * @param configuration the service configuration.
         * @return this builder.
         * @throws java.lang.NullPointerException if the specified configuration is null.
         */
        @Nonnull
        public Builder configure(@Nonnull final ServiceConfiguration configuration) {

            final Looper looper = configuration.mLooper;

            if (looper != null) {

                dispatchingOn(looper);
            }

            final Class<? extends RoutineService> serviceClass = configuration.mServiceClass;

            if (serviceClass != null) {

                withServiceClass(serviceClass);
            }

            final Class<? extends Runner> runnerClass = configuration.mRunnerClass;

            if (runnerClass != null) {

                withRunnerClass(runnerClass);
            }

            final Class<? extends Log> logClass = configuration.mLogClass;

            if (logClass != null) {

                withLogClass(logClass);
            }

            return this;
        }

        /**
         * Sets the looper on which the results from the service are dispatched. A null value
         * means that
         * results will be dispatched on the main thread (as by default).
         *
         * @param looper the looper instance.
         * @return this builder.
         */
        @Nonnull
        public Builder dispatchingOn(@Nullable Looper looper) {

            mLooper = looper;
            return this;
        }

        /**
         * Sets the log class. A null value means that it is up to the framework to choose a default
         * implementation.
         *
         * @param logClass the log class.
         * @return this builder.
         */
        @Nonnull
        public Builder withLogClass(@Nullable Class<? extends Log> logClass) {

            mLogClass = logClass;
            return this;
        }

        /**
         * Sets the runner class. A null value means that it is up to the framework to choose a
         * default
         * implementation.
         *
         * @param runnerClass the runner class.
         * @return this builder.
         */
        @Nonnull
        public Builder withRunnerClass(@Nullable Class<? extends Runner> runnerClass) {

            mRunnerClass = runnerClass;
            return this;
        }

        /**
         * Sets the class of the service executing the built routine. A null value means that it
         * is up
         * to the framework to choose the default service class.
         *
         * @param serviceClass the service class.
         * @return this builder.
         */
        @Nonnull
        public Builder withServiceClass(@Nullable Class<? extends RoutineService> serviceClass) {

            mServiceClass = serviceClass;
            return this;
        }
    }
}
