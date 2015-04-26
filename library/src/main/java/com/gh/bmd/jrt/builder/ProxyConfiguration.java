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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class storing the proxy configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * The configuration has a share group associated. Every method within a specific group is protected
 * so that shared class members can be safely accessed only from the other methods sharing the same
 * group name. That means that the invocation of methods within the same group cannot happen in
 * parallel. In a dual way, methods belonging to different groups can be invoked in parallel but
 * should not access the same members to avoid concurrency issues.
 * <p/>
 * Created by davide on 20/04/15.
 */
public class ProxyConfiguration {

    private static final Configurable<ProxyConfiguration> sDefaultConfigurable =
            new Configurable<ProxyConfiguration>() {

                @Nonnull
                public ProxyConfiguration configureWith(
                        @Nonnull final RoutineConfiguration configuration) {

                    return ProxyConfiguration.EMPTY_CONFIGURATION;
                }

                @Nonnull
                public ProxyConfiguration configureWith(
                        @Nonnull final ProxyConfiguration configuration) {

                    return configuration;
                }
            };

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final ProxyConfiguration EMPTY_CONFIGURATION = builder().buildConfiguration();

    private final String mGroupName;

    /**
     * Constructor.
     *
     * @param groupName the share group name.
     */
    private ProxyConfiguration(@Nullable final String groupName) {

        mGroupName = groupName;
    }

    /**
     * Returns a proxy configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder<ProxyConfiguration> builder() {

        return new Builder<ProxyConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns a proxy configuration builder initialized with the specified configuration.
     *
     * @param routineConfiguration the initial routine configuration.
     * @param proxyConfiguration   the initial proxy configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder<ProxyConfiguration> builderFrom(
            @Nullable final RoutineConfiguration routineConfiguration,
            @Nullable final ProxyConfiguration proxyConfiguration) {

        return new Builder<ProxyConfiguration>(sDefaultConfigurable, (routineConfiguration == null)
                ? RoutineConfiguration.EMPTY_CONFIGURATION : routineConfiguration,
                                               (proxyConfiguration == null)
                                                       ? ProxyConfiguration.EMPTY_CONFIGURATION
                                                       : proxyConfiguration);
    }

    /**
     * Returns a proxy configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder builderFrom() {

        return builderFrom(RoutineConfiguration.EMPTY_CONFIGURATION, this);
    }

    /**
     * Returns the share group name (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the group name.
     */
    public String getShareGroupOr(@Nullable final String valueIfNotSet) {

        final String groupName = mGroupName;
        return (groupName != null) ? groupName : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        return mGroupName != null ? mGroupName.hashCode() : 0;
    }

    @Override
    public boolean equals(final Object o) {

        // auto-generated code
        if (this == o) {

            return true;
        }

        if (!(o instanceof ProxyConfiguration)) {

            return false;
        }

        final ProxyConfiguration that = (ProxyConfiguration) o;
        return !(mGroupName != null ? !mGroupName.equals(that.mGroupName)
                : that.mGroupName != null);
    }

    @Override
    public String toString() {

        return "ProxyConfiguration{" +
                "mGroupName='" + mGroupName + '\'' +
                '}';
    }

    /**
     * TODO
     *
     * @param <TYPE>
     */
    public interface Configurable<TYPE> extends RoutineConfiguration.Configurable<TYPE> {

        @Nonnull
        TYPE configureWith(@Nonnull ProxyConfiguration configuration);
    }

    /**
     * Builder of proxy configurations.
     */
    public static class Builder<TYPE> extends RoutineConfiguration.Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private String mGroupName;

        /**
         * Constructor.
         *
         * @param configurable the configurable instance.
         * @throws java.lang.NullPointerException if the specified configurable instance is null.
         */
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable) {

            super(configurable);
            mConfigurable = configurable;
        }

        /**
         * Constructor.
         *
         * @param configurable         the configurable instance.
         * @param routineConfiguration the initial routine configuration.
         * @param proxyConfiguration   the initial proxy configuration.
         * @throws java.lang.NullPointerException if any of the specified parameters is null.
         */
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable,
                @Nonnull final RoutineConfiguration routineConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration) {

            super(configurable, routineConfiguration);
            mConfigurable = configurable;
            mGroupName = proxyConfiguration.mGroupName;
        }

        @Nonnull
        @Override
        public Builder<TYPE> onReadTimeout(@Nullable final TimeoutActionType action) {

            super.onReadTimeout(action);
            return this;
        }

        /**
         * TODO
         *
         * @return
         */
        @Nonnull
        public TYPE then() {

            super.then();
            return mConfigurable.configureWith(buildConfiguration());
        }

        @Nonnull
        @Override
        public Builder<TYPE> with(@Nullable final RoutineConfiguration configuration) {

            super.with(configuration);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withAsyncRunner(@Nullable final Runner runner) {

            super.withAsyncRunner(runner);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withAvailableTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            super.withAvailableTimeout(timeout, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withAvailableTimeout(@Nullable final TimeDuration timeout) {

            super.withAvailableTimeout(timeout);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withCoreInvocations(final int coreInvocations) {

            super.withCoreInvocations(coreInvocations);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withFactoryArgs(@Nullable final Object... args) {

            super.withFactoryArgs(args);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withInputOrder(@Nullable final OrderType orderType) {

            super.withInputOrder(orderType);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withInputSize(final int inputMaxSize) {

            super.withInputSize(inputMaxSize);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withInputTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            super.withInputTimeout(timeout, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withInputTimeout(@Nullable final TimeDuration timeout) {

            super.withInputTimeout(timeout);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withLog(@Nullable final Log log) {

            super.withLog(log);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withLogLevel(@Nullable final LogLevel level) {

            super.withLogLevel(level);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withMaxInvocations(final int maxInvocations) {

            super.withMaxInvocations(maxInvocations);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withOutputOrder(@Nullable final OrderType orderType) {

            super.withOutputOrder(orderType);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withOutputSize(final int outputMaxSize) {

            super.withOutputSize(outputMaxSize);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withOutputTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            super.withOutputTimeout(timeout, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withOutputTimeout(@Nullable final TimeDuration timeout) {

            super.withOutputTimeout(timeout);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withReadTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

            super.withReadTimeout(timeout, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withReadTimeout(@Nullable final TimeDuration timeout) {

            super.withReadTimeout(timeout);
            return this;
        }

        @Nonnull
        @Override
        public Builder<TYPE> withSyncRunner(@Nullable final Runner runner) {

            super.withSyncRunner(runner);
            return this;
        }

        /**
         * Applies the specified configuration to this builder.
         *
         * @param configuration the proxy configuration.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> with(@Nullable final ProxyConfiguration configuration) {

            if (configuration == null) {

                return this;
            }

            final String groupName = configuration.mGroupName;

            if (groupName != null) {

                withShareGroup(groupName);
            }

            return this;
        }

        /**
         * Sets the share group name. A null value means that it is up to the framework to choose a
         * default value.
         *
         * @param groupName the group name.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withShareGroup(@Nullable final String groupName) {

            mGroupName = groupName;
            return this;
        }

        /**
         * Builds and return the configuration instance.
         *
         * @return the proxy configuration instance.
         */
        @Nonnull
        private ProxyConfiguration buildConfiguration() {

            return new ProxyConfiguration(mGroupName);
        }
    }
}
