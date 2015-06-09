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
 * is, they share the same ID, and to set a specific type of caching of the invocation results.<br/>
 * Finally, a specific looper, other than the main thread one, can be chosen to dispatch the results
 * coming from the invocation.
 * <p/>
 * Created by davide-maestroni on 19/04/15.
 */
public final class LoaderConfiguration {

    /**
     * Constant identifying an loader ID computed from the executor class and the input parameters.
     */
    public static final int AUTO = Integer.MIN_VALUE;

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final LoaderConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

    private final ClashResolutionType mInputResolutionType;

    private final int mLoaderId;

    private final Looper mLooper;

    private final ClashResolutionType mResolutionType;

    private final CacheStrategyType mStrategyType;

    /**
     * Constructor.
     *
     * @param looper              the looper instance.
     * @param loaderId            the the loader ID.
     * @param resolutionType      the type of resolution.
     * @param inputResolutionType the type of input resolution.
     * @param strategyType        the cache strategy type.
     */
    private LoaderConfiguration(@Nullable final Looper looper, final int loaderId,
            @Nullable final ClashResolutionType resolutionType,
            @Nullable final ClashResolutionType inputResolutionType,
            @Nullable final CacheStrategyType strategyType) {

        mLooper = looper;
        mLoaderId = loaderId;
        mResolutionType = resolutionType;
        mInputResolutionType = inputResolutionType;
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
        return mLoaderId == that.mLoaderId && mInputResolutionType == that.mInputResolutionType
                && mResolutionType == that.mResolutionType && mStrategyType == that.mStrategyType
                && !(mLooper != null ? !mLooper.equals(that.mLooper) : that.mLooper != null);
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mInputResolutionType != null ? mInputResolutionType.hashCode() : 0;
        result = 31 * result + mLoaderId;
        result = 31 * result + (mResolutionType != null ? mResolutionType.hashCode() : 0);
        result = 31 * result + (mStrategyType != null ? mStrategyType.hashCode() : 0);
        result = 31 * result + (mLooper != null ? mLooper.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {

        return "LoaderConfiguration{" +
                "mInputResolutionType=" + mInputResolutionType +
                ", mLoaderId=" + mLoaderId +
                ", mResolutionType=" + mResolutionType +
                ", mStrategyType=" + mStrategyType +
                ", mLooper=" + mLooper +
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
     * Returns the type of input clash resolution (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the clash resolution type.
     */
    public ClashResolutionType getInputClashResolutionTypeOr(
            @Nullable final ClashResolutionType valueIfNotSet) {

        final ClashResolutionType resolutionType = mInputResolutionType;
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
     * Returns the looper used for dispatching the results from the loader (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the looper instance.
     */
    public Looper getResultLooperOr(@Nullable final Looper valueIfNotSet) {

        final Looper looper = mLooper;
        return (looper != null) ? looper : valueIfNotSet;
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
        CACHE
    }

    /**
     * Invocation clash resolution enumeration.<br/>
     * The clash of two invocations happens when the same loader ID is already in use at the time of
     * the routine execution. The possible outcomes are:
     * <ul>
     * <li>the running invocation is retained and merged with the current one</li>
     * <li>the running invocation is aborted</li>
     * <li>the current invocation is aborted</li>
     * <li>both running and current invocations are aborted</li>
     * </ul>
     * Two different types of resolution can be set based on whether the input data are different or
     * not.
     */
    public enum ClashResolutionType {

        /**
         * The clash is resolved by merging the two invocations.
         */
        MERGE,
        /**
         * The clash is resolved by aborting the running invocation.
         */
        ABORT_THAT,
        /**
         * The clash is resolved by aborting the invocation with an
         * {@link com.gh.bmd.jrt.android.invocation.InvocationClashException}.
         */
        ABORT_THIS,
        /**
         * The clash is resolved by aborting both the invocations.
         */
        ABORT
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

        private ClashResolutionType mInputResolutionType;

        private int mLoaderId;

        private Looper mLooper;

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

            final Looper looper = configuration.mLooper;

            if (looper != null) {

                withResultLooper(looper);
            }

            final int loaderId = configuration.mLoaderId;

            if (loaderId != AUTO) {

                withId(loaderId);
            }

            final ClashResolutionType resolutionType = configuration.mResolutionType;

            if (resolutionType != null) {

                withClashResolution(resolutionType);
            }

            final ClashResolutionType inputResolutionType = configuration.mInputResolutionType;

            if (inputResolutionType != null) {

                withInputClashResolution(inputResolutionType);
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
         * Tells the builder how to resolve clashes of invocations with different inputs. A clash
         * happens when a loader with the same ID is still running. A null value means that it is up
         * to the framework to choose a default resolution type.
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

        /**
         * Tells the builder how to resolve clashes of invocations with same inputs. A clash happens
         * when a loader with the same ID is still running. A null value means that it is up to the
         * framework to choose a default resolution type.
         *
         * @param resolutionType the type of resolution.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withInputClashResolution(
                @Nullable final ClashResolutionType resolutionType) {

            mInputResolutionType = resolutionType;
            return this;
        }

        /**
         * Sets the looper on which the results from the loader are dispatched. A null value means
         * that results will be dispatched on the main thread (as by default).
         *
         * @param looper the looper instance.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withResultLooper(@Nullable final Looper looper) {

            mLooper = looper;
            return this;
        }

        @Nonnull
        private LoaderConfiguration buildConfiguration() {

            return new LoaderConfiguration(mLooper, mLoaderId, mResolutionType,
                                           mInputResolutionType, mStrategyType);
        }

        private void setConfiguration(@Nonnull final LoaderConfiguration configuration) {

            mLooper = configuration.mLooper;
            mLoaderId = configuration.mLoaderId;
            mResolutionType = configuration.mResolutionType;
            mInputResolutionType = configuration.mInputResolutionType;
            mStrategyType = configuration.mStrategyType;
        }
    }

    /**
     * Default configurable implementation.
     */
    private static class DefaultConfigurable implements Configurable<LoaderConfiguration> {

        @Nonnull
        public LoaderConfiguration setConfiguration(
                @Nonnull final LoaderConfiguration configuration) {

            return configuration;
        }
    }
}
