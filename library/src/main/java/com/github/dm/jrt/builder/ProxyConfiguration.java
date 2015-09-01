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
package com.github.dm.jrt.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class storing the proxy configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration.
 * <p/>
 * The configuration has a share group associated. Every method within a specific group is protected
 * so that shared class members can be safely accessed only from the other methods sharing the same
 * group name. That means that the invocation of methods within the same group cannot happen in
 * parallel. In a dual way, methods belonging to different groups can be invoked in parallel but
 * should not access the same members to avoid concurrency issues.
 * <p/>
 * Created by davide-maestroni on 04/20/15.
 */
public final class ProxyConfiguration {

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    /**
     * Empty configuration constant.<br/>The configuration has all the options set to their default.
     */
    public static final ProxyConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

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
     * @param initialConfiguration the initial proxy configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder<ProxyConfiguration> builderFrom(
            @Nullable final ProxyConfiguration initialConfiguration) {

        return (initialConfiguration == null) ? builder()
                : new Builder<ProxyConfiguration>(sDefaultConfigurable, initialConfiguration);
    }

    /**
     * Returns a proxy configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder<ProxyConfiguration> builderFrom() {

        return builderFrom(this);
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

        // AUTO-GENERATED CODE
        return mGroupName != null ? mGroupName.hashCode() : 0;
    }

    @Override
    public boolean equals(final Object o) {

        // AUTO-GENERATED CODE
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

        // AUTO-GENERATED CODE
        return "ProxyConfiguration{" +
                "mGroupName='" + mGroupName + '\'' +
                '}';
    }

    /**
     * Interface defining a configurable object.
     *
     * @param <TYPE> the configurable object type.
     */
    public interface Configurable<TYPE> {

        /**
         * Sets the specified configuration and returns the configurable instance.
         *
         * @param configuration the configuration.
         * @return the configurable instance.
         */
        @Nonnull
        TYPE setConfiguration(@Nonnull ProxyConfiguration configuration);
    }

    /**
     * Builder of proxy configurations.
     *
     * @param <TYPE> the configurable object type.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private String mGroupName;

        /**
         * Constructor.
         *
         * @param configurable the configurable instance.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable) {

            if (configurable == null) {

                throw new NullPointerException("the configurable instance must no be null");
            }

            mConfigurable = configurable;
        }

        /**
         * Constructor.
         *
         * @param configurable         the configurable instance.
         * @param initialConfiguration the initial proxy configuration.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable,
                @Nonnull final ProxyConfiguration initialConfiguration) {

            if (configurable == null) {

                throw new NullPointerException("the configurable instance must no be null");
            }

            mConfigurable = configurable;
            setConfiguration(initialConfiguration);
        }

        /**
         * Sets the configuration and returns the configurable object.
         *
         * @return the configurable object.
         */
        @Nonnull
        public TYPE set() {

            return mConfigurable.setConfiguration(buildConfiguration());
        }

        /**
         * Applies the specified configuration to this builder. A null value means that all the
         * configuration options need to be set to their default value, otherwise only the set
         * options will be applied.
         *
         * @param configuration the proxy configuration.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> with(@Nullable final ProxyConfiguration configuration) {

            if (configuration == null) {

                setConfiguration(DEFAULT_CONFIGURATION);
                return this;
            }

            final String groupName = configuration.mGroupName;

            if (groupName != null) {

                withShareGroup(groupName);
            }

            return this;
        }

        /**
         * Sets the share group name. A null value means that it is up to the specific
         * implementation to choose a default one.
         *
         * @param groupName the group name.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withShareGroup(@Nullable final String groupName) {

            mGroupName = groupName;
            return this;
        }

        @Nonnull
        private ProxyConfiguration buildConfiguration() {

            return new ProxyConfiguration(mGroupName);
        }

        private void setConfiguration(@Nonnull final ProxyConfiguration configuration) {

            mGroupName = configuration.mGroupName;
        }
    }

    /**
     * Default configurable implementation.
     */
    private static class DefaultConfigurable implements Configurable<ProxyConfiguration> {

        @Nonnull
        public ProxyConfiguration setConfiguration(
                @Nonnull final ProxyConfiguration configuration) {

            return configuration;
        }
    }
}
