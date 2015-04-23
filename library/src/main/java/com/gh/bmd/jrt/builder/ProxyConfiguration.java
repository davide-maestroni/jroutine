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
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Returns a proxy configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder builderFrom(@Nonnull final ProxyConfiguration initialConfiguration) {

        return new Builder(initialConfiguration);
    }

    /**
     * Returns the specified configuration or the empty one if the former is null.
     *
     * @param configuration the proxy configuration.
     * @return the configuration.
     */
    @Nonnull
    public static ProxyConfiguration notNull(@Nullable final ProxyConfiguration configuration) {

        return (configuration != null) ? configuration : EMPTY_CONFIGURATION;
    }

    /**
     * Short for <b><code>builder().withShareGroup(groupName)</code></b>.
     *
     * @param groupName the group name.
     * @return the proxy configuration builder.
     */
    @Nonnull
    public static Builder withShareGroup(@Nullable final String groupName) {

        return builder().withShareGroup(groupName);
    }

    /**
     * Returns a proxy configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder builderFrom() {

        return new Builder(this);
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
     * Builder of proxy configurations.
     */
    public static class Builder {

        private String mGroupName;

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
        private Builder(@Nonnull final ProxyConfiguration initialConfiguration) {

            mGroupName = initialConfiguration.mGroupName;
        }

        /**
         * Applies the specified configuration to this builder.
         *
         * @param configuration the proxy configuration.
         * @return this builder.
         */
        @Nonnull
        public Builder apply(@Nullable final ProxyConfiguration configuration) {

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
         * Builds and return the configuration instance.
         *
         * @return the proxy configuration instance.
         */
        @Nonnull
        public ProxyConfiguration buildConfiguration() {

            return new ProxyConfiguration(mGroupName);
        }

        /**
         * Sets the share group name. A null value means that it is up to the framework to choose a
         * default value.
         *
         * @param groupName the group name.
         * @return this builder.
         */
        @Nonnull
        public Builder withShareGroup(@Nullable final String groupName) {

            mGroupName = groupName;
            return this;
        }
    }
}
