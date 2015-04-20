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
 * Class storing the share configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * Created by davide on 20/04/15.
 */
public class ShareConfiguration {

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final ShareConfiguration EMPTY_CONFIGURATION = builder().buildConfiguration();

    private final String mGroupName;

    /**
     * Constructor.
     *
     * @param groupName the share group name.
     */
    private ShareConfiguration(@Nullable final String groupName) {

        mGroupName = groupName;
    }

    /**
     * Returns a share configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Returns a share configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder builderFrom(@Nonnull final ShareConfiguration initialConfiguration) {

        return new Builder(initialConfiguration);
    }

    /**
     * Returns the specified configuration or the empty one if the former is null.
     *
     * @param configuration the share configuration.
     * @return the configuration.
     */
    @Nonnull
    public static ShareConfiguration notNull(@Nullable final ShareConfiguration configuration) {

        return (configuration != null) ? configuration : EMPTY_CONFIGURATION;
    }

    /**
     * Short for <b><code>builder().withGroup(groupName)</code></b>.
     *
     * @param groupName the group name.
     * @return the share configuration builder.
     */
    @Nonnull
    public static Builder withGroup(@Nullable final String groupName) {

        return builder().withGroup(groupName);
    }

    /**
     * Returns a share configuration builder initialized with this configuration.
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
    public String getGroupOr(@Nullable final String valueIfNotSet) {

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

        if (!(o instanceof ShareConfiguration)) {

            return false;
        }

        final ShareConfiguration that = (ShareConfiguration) o;
        return !(mGroupName != null ? !mGroupName.equals(that.mGroupName)
                : that.mGroupName != null);
    }

    @Override
    public String toString() {

        return "ShareConfiguration{" +
                "mGroupName='" + mGroupName + '\'' +
                '}';
    }

    /**
     * Builder of share configurations.
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
        private Builder(@Nonnull final ShareConfiguration initialConfiguration) {

            mGroupName = initialConfiguration.mGroupName;
        }

        /**
         * Builds and return the configuration instance.
         *
         * @return the share configuration instance.
         */
        @Nonnull
        public ShareConfiguration buildConfiguration() {

            return new ShareConfiguration(mGroupName);
        }

        /**
         * Applies the specified configuration to this builder.
         *
         * @param configuration the share configuration.
         * @return this builder.
         * @throws java.lang.NullPointerException if the specified configuration is null.
         */
        @Nonnull
        public Builder withConfiguration(@Nonnull final ShareConfiguration configuration) {

            final String groupName = configuration.mGroupName;

            if (groupName != null) {

                withGroup(groupName);
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
        public Builder withGroup(@Nullable final String groupName) {

            mGroupName = groupName;
            return this;
        }
    }
}
