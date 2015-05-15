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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class storing the invocation loader configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * The configuration is used to set a specific ID to each invocation created by a routine.<br/>
 * Moreover, it is possible to set a specific type of resolution when two invocations clashes, that
 * is, they share the same ID, and to set a specific type of caching of the invocation results.
 * <p/>
 * Created by davide on 19/04/15.
 */
public final class LoaderConfiguration {

    // TODO: looper configuration?

    /**
     * Constant identifying an loader ID computed from the executor class and the input parameters.
     */
    public static final int AUTO = Integer.MIN_VALUE;

    private static final Configurable<LoaderConfiguration> sDefaultConfigurable =
            new Configurable<LoaderConfiguration>() {

                @Nonnull
                public LoaderConfiguration setConfiguration(
                        @Nonnull final LoaderConfiguration configuration) {

                    return configuration;
                }
            };

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final LoaderConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

    private final int mLoaderId;

    private final ClashResolutionType mResolutionType;

    private final CacheStrategyType mStrategyType;

    /**
     * Constructor.
     *
     * @param loaderId       the the loader ID.
     * @param resolutionType the type of resolution.
     * @param strategyType   the cache strategy type.
     */
    private LoaderConfiguration(final int loaderId,
            @Nullable final ClashResolutionType resolutionType,
            @Nullable final CacheStrategyType strategyType) {

        mLoaderId = loaderId;
        mResolutionType = resolutionType;
        mStrategyType = strategyType;
    }

    /**
     * Returns a loader configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder<LoaderConfiguration> builder() {

        return new Builder<LoaderConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns a loader configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial loader configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder<LoaderConfiguration> builderFrom(
            @Nullable final LoaderConfiguration initialConfiguration) {

        return (initialConfiguration == null) ? builder()
                : new Builder<LoaderConfiguration>(sDefaultConfigurable, initialConfiguration);
    }

    /**
     * Returns a loader configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder<LoaderConfiguration> builderFrom() {

        return builderFrom(this);
    }

    @Override
    public boolean equals(final Object o) {

        // auto-generated code
        if (this == o) {

            return true;
        }

        if (!(o instanceof LoaderConfiguration)) {

            return false;
        }

        final LoaderConfiguration that = (LoaderConfiguration) o;
        return mLoaderId == that.mLoaderId && mResolutionType == that.mResolutionType
                && mStrategyType == that.mStrategyType;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mLoaderId;
        result = 31 * result + (mResolutionType != null ? mResolutionType.hashCode() : 0);
        result = 31 * result + (mStrategyType != null ? mStrategyType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {

        return "LoaderConfiguration{" +
                "mLoaderId=" + mLoaderId +
                ", mResolutionType=" + mResolutionType +
                ", mStrategyType=" + mStrategyType +
                '}';
    }

    /**
     * Returns the type of cache strategy (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the cache strategy type.
     */
    public CacheStrategyType getCacheStrategyTypeOr(
            @Nullable final CacheStrategyType valueIfNotSet) {

        final CacheStrategyType strategyType = mStrategyType;
        return (strategyType != null) ? strategyType : valueIfNotSet;
    }

    /**
     * Returns the type of clash resolution (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the clash resolution type.
     */
    public ClashResolutionType getClashResolutionTypeOr(
            @Nullable final ClashResolutionType valueIfNotSet) {

        final ClashResolutionType resolutionType = mResolutionType;
        return (resolutionType != null) ? resolutionType : valueIfNotSet;
    }

    /**
     * Returns the loader ID (AUTO by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the loader ID.
     */
    public int getLoaderIdOr(final int valueIfNotSet) {

        final int loaderId = mLoaderId;
        return (loaderId != AUTO) ? loaderId : valueIfNotSet;
    }

    /**
     * Result cache type enumeration.<br/>
     * The cache strategy type indicates what will happen to the result of an invocation after its
     * completion.
     */
    public enum CacheStrategyType {

        /**
         * On completion the invocation results are cleared.
         */
        CLEAR,
        /**
         * Only in case of error the results are cleared, otherwise they are retained.
         */
        CACHE_IF_SUCCESS,
        /**
         * Only in case of successful completion the results are cleared, otherwise they are
         * retained.
         */
        CACHE_IF_ERROR,
        /**
         * On completion the invocation results are retained.
         */
        CACHE,
    }

    /**
     * Invocation clash resolution enumeration.<br/>
     * The clash of two invocation happens when the same ID is already in use at the time of the
     * routine execution. The possible outcomes are:
     * <ul>
     * <li>the running invocation is aborted</li>
     * <li>the running invocation is retained, ignoring the input data</li>
     * <li>the current invocation is aborted</li>
     * <li>the running invocation is aborted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * <li>the current invocation is aborted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * </ul>
     */
    public enum ClashResolutionType {

        /**
         * The clash is resolved by aborting the running invocation.
         */
        ABORT_THAT,
        /**
         * The clash is resolved by keeping the running invocation.
         */
        KEEP_THAT,
        /**
         * The clash is resolved by aborting the invocation with an {@link InputClashException}.
         */
        ABORT_THIS,
        /**
         * The clash is resolved by aborting the running invocation, only in case its input data are
         * different from the current ones.
         */
        ABORT_THAT_INPUT,
        /**
         * The clash is resolved by aborting the invocation with an {@link InputClashException},
         * only in case its input data are different from the current ones.
         */
        ABORT_THIS_INPUT,
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
        TYPE setConfiguration(@Nonnull LoaderConfiguration configuration);
    }

    /**
     * Builder of loader configurations.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private int mLoaderId;

        private ClashResolutionType mResolutionType;

        private CacheStrategyType mStrategyType;

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
            mLoaderId = AUTO;
        }

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial loader configuration.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable,
                @Nonnull final LoaderConfiguration initialConfiguration) {

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
         * @param configuration the loader configuration.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> with(@Nullable final LoaderConfiguration configuration) {

            if (configuration == null) {

                setConfiguration(DEFAULT_CONFIGURATION);
                return this;
            }

            final int loaderId = configuration.mLoaderId;

            if (loaderId != AUTO) {

                withId(loaderId);
            }

            final ClashResolutionType resolutionType = configuration.mResolutionType;

            if (resolutionType != null) {

                withClashResolution(resolutionType);
            }

            final CacheStrategyType strategyType = configuration.mStrategyType;

            if (strategyType != null) {

                withCacheStrategy(strategyType);
            }

            return this;
        }

        /**
         * Tells the builder how to cache the invocation result after its completion. A null value
         * means that it is up to the framework to choose a default strategy.
         *
         * @param strategyType the cache strategy type.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withCacheStrategy(@Nullable final CacheStrategyType strategyType) {

            mStrategyType = strategyType;
            return this;
        }

        /**
         * Tells the builder how to resolve clashes of invocations. A clash happens when an
         * invocation of the same type and with the same ID is still running. A null value means
         * that it is up to the framework to choose a default resolution type.
         *
         * @param resolutionType the type of resolution.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withClashResolution(
                @Nullable final ClashResolutionType resolutionType) {

            mResolutionType = resolutionType;
            return this;
        }

        /**
         * Tells the builder to identify the loader with the specified ID.
         *
         * @param loaderId the loader ID.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withId(final int loaderId) {

            mLoaderId = loaderId;
            return this;
        }

        @Nonnull
        private LoaderConfiguration buildConfiguration() {

            return new LoaderConfiguration(mLoaderId, mResolutionType, mStrategyType);
        }

        private void setConfiguration(@Nonnull final LoaderConfiguration configuration) {

            mLoaderId = configuration.mLoaderId;
            mResolutionType = configuration.mResolutionType;
            mStrategyType = configuration.mStrategyType;
        }
    }
}
