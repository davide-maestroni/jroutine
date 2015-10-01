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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class storing the proxy configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration.
 * <p/>
 * The configuration has a list of shared variables associated. Every method accessing a specific
 * variable is protected so that the related member fields can be safely accessed only from the
 * other methods sharing the same variable name. That means that the invocation of methods with the
 * same variable name cannot happen in parallel. In a dual way, methods whose variable names do not
 * overlap can be invoked in parallel, though, they should not access the same member fields in
 * order to avoid concurrency issues.
 * <p/>
 * Created by davide-maestroni on 04/20/2015.
 */
public final class ProxyConfiguration {

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    /**
     * Empty configuration constant.<br/>The configuration has all the options set to their default.
     */
    public static final ProxyConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

    private final List<String> mVarNames;

    /**
     * Constructor.
     *
     * @param varNames the shared variable names.
     */
    private ProxyConfiguration(@Nullable final List<String> varNames) {

        mVarNames = varNames;
    }

    /**
     * Returns a proxy configuration builder.
     *
     * @return the builder.
     */
    @NotNull
    public static Builder<ProxyConfiguration> builder() {

        return new Builder<ProxyConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns a proxy configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial proxy configuration.
     * @return the builder.
     */
    @NotNull
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
    @NotNull
    public Builder<ProxyConfiguration> builderFrom() {

        return builderFrom(this);
    }

    /**
     * Returns the shared variable names (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the variable names.
     */
    public List<String> getSharedVarsOr(@Nullable final List<String> valueIfNotSet) {

        final List<String> varNames = mVarNames;
        return (varNames != null) ? varNames : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        return mVarNames != null ? mVarNames.hashCode() : 0;
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
        return !(mVarNames != null ? !mVarNames.equals(that.mVarNames) : that.mVarNames != null);
    }

    @Override
    public String toString() {

        // AUTO-GENERATED CODE
        return "ProxyConfiguration{" +
                "mVarNames='" + mVarNames + '\'' +
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
        @NotNull
        TYPE setConfiguration(@NotNull ProxyConfiguration configuration);
    }

    /**
     * Builder of proxy configurations.
     *
     * @param <TYPE> the configurable object type.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private List<String> mVarNames;

        /**
         * Constructor.
         *
         * @param configurable the configurable instance.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@NotNull final Configurable<? extends TYPE> configurable) {

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
        public Builder(@NotNull final Configurable<? extends TYPE> configurable,
                @NotNull final ProxyConfiguration initialConfiguration) {

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
        @NotNull
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
        @NotNull
        public Builder<TYPE> with(@Nullable final ProxyConfiguration configuration) {

            if (configuration == null) {

                setConfiguration(DEFAULT_CONFIGURATION);
                return this;
            }

            final List<String> varNames = configuration.mVarNames;

            if (varNames != null) {

                withSharedVars(varNames);
            }

            return this;
        }

        /**
         * Sets the share variable names. A null value means that it is up to the specific
         * implementation to choose a default.
         *
         * @param varNames the variable names.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withSharedVars(@Nullable final String... varNames) {

            mVarNames = (varNames != null) ? Arrays.asList(varNames) : null;
            return this;
        }

        /**
         * Sets the share variable names. A null value means that it is up to the specific
         * implementation to choose a default.
         *
         * @param varNames the variable names.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withSharedVars(@Nullable final List<String> varNames) {

            mVarNames = (varNames != null) ? Collections.unmodifiableList(
                    new ArrayList<String>(varNames)) : null;
            return this;
        }

        @NotNull
        private ProxyConfiguration buildConfiguration() {

            return new ProxyConfiguration(mVarNames);
        }

        private void setConfiguration(@NotNull final ProxyConfiguration configuration) {

            mVarNames = configuration.mVarNames;
        }
    }

    /**
     * Default configurable implementation.
     */
    private static class DefaultConfigurable implements Configurable<ProxyConfiguration> {

        @NotNull
        public ProxyConfiguration setConfiguration(
                @NotNull final ProxyConfiguration configuration) {

            return configuration;
        }
    }
}
