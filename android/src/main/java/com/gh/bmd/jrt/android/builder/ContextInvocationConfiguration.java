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
 * Class storing the context invocation configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * Created by davide on 19/04/15.
 */
public class ContextInvocationConfiguration {

    //TODO: see annotations?
    //TODO: move withArgs to RoutineConfiguration (withInvocationArgs???)
    //TODO: check ...

    /**
     * Constant identifying an invocation ID computed from the executor class and the input
     * parameters.
     */
    public static final int AUTO = Integer.MIN_VALUE;

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final ContextInvocationConfiguration EMPTY_CONFIGURATION =
            builder().buildConfiguration();

    private final int mInvocationId;

    private final ClashResolutionType mResolutionType;

    private final CacheStrategyType mStrategyType;

    /**
     * Constructor.
     *
     * @param invocationId   the the invocation ID.
     * @param resolutionType the type of resolution.
     * @param strategyType   the cache strategy type.
     */
    private ContextInvocationConfiguration(final int invocationId,
            @Nullable final ClashResolutionType resolutionType,
            @Nullable final CacheStrategyType strategyType) {

        mInvocationId = invocationId;
        mResolutionType = resolutionType;
        mStrategyType = strategyType;
    }

    /**
     * Returns a context invocation configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Returns a context invocation configuration builder initialized with the specified
     * configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder builderFrom(
            @Nonnull final ContextInvocationConfiguration initialConfiguration) {

        return new Builder(initialConfiguration);
    }

    /**
     * Returns the specified configuration or the empty one if the former is null.
     *
     * @param configuration the invocation configuration.
     * @return the configuration.
     */
    @Nonnull
    public static ContextInvocationConfiguration notNull(
            @Nullable final ContextInvocationConfiguration configuration) {

        return (configuration != null) ? configuration : EMPTY_CONFIGURATION;
    }

    /**
     * Short for <b><code>builder().onClash(resolutionType)</code></b>.
     *
     * @param resolutionType the type of resolution.
     * @return the context invocation configuration builder.
     */
    @Nonnull
    public static Builder onClash(@Nullable final ClashResolutionType resolutionType) {

        return builder().onClash(resolutionType);
    }

    /**
     * Short for <b><code>builder().onComplete(strategyType)</code></b>.
     *
     * @param strategyType the cache strategy type.
     * @return the context invocation configuration builder.
     */
    @Nonnull
    public static Builder onComplete(@Nullable final CacheStrategyType strategyType) {

        return builder().onComplete(strategyType);
    }

    /**
     * Short for <b><code>builder().withId(invocationId)</code></b>.
     *
     * @param invocationId the invocation ID.
     * @return the context invocation configuration builder.
     */
    @Nonnull
    public static Builder withId(final int invocationId) {

        return builder().withId(invocationId);
    }

    /**
     * Returns a context invocation configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder builderFrom() {

        return new Builder(this);
    }

    @Override
    public boolean equals(final Object o) {

        // auto-generated code
        if (this == o) {

            return true;
        }

        if (!(o instanceof ContextInvocationConfiguration)) {

            return false;
        }

        final ContextInvocationConfiguration that = (ContextInvocationConfiguration) o;
        return mInvocationId == that.mInvocationId && mResolutionType == that.mResolutionType
                && mStrategyType == that.mStrategyType;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mInvocationId;
        result = 31 * result + (mResolutionType != null ? mResolutionType.hashCode() : 0);
        result = 31 * result + (mStrategyType != null ? mStrategyType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {

        return "ContextInvocationConfiguration{" +
                "mInvocationId=" + mInvocationId +
                ", mResolutionType=" + mResolutionType +
                ", mStrategyType=" + mStrategyType +
                '}';
    }

    /**
     * Returns the invocation ID (AUTO by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the invocation ID.
     */
    public int getInvocationIdOr(final int valueIfNotSet) {

        final int invocationId = mInvocationId;
        return (invocationId != AUTO) ? invocationId : valueIfNotSet;
    }

    /**
     * Returns the type of clash resolution (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the clash resolution type.
     */
    public ClashResolutionType getResolutionTypeOr(
            @Nullable final ClashResolutionType valueIfNotSet) {

        final ClashResolutionType resolutionType = mResolutionType;
        return (resolutionType != null) ? resolutionType : valueIfNotSet;
    }

    /**
     * Returns the type of cache strategy (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the cache strategy type.
     */
    public CacheStrategyType getStrategyTypeOr(@Nullable final CacheStrategyType valueIfNotSet) {

        final CacheStrategyType strategyType = mStrategyType;
        return (strategyType != null) ? strategyType : valueIfNotSet;
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
     * Builder of context invocation configurations.
     */
    public static class Builder {

        private int mInvocationId;

        private ClashResolutionType mResolutionType;

        private CacheStrategyType mStrategyType;

        /**
         * Constructor.
         */
        private Builder() {

            mInvocationId = AUTO;
        }

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial configuration.
         * @throws java.lang.NullPointerException if the specified configuration instance is null.
         */
        private Builder(@Nonnull final ContextInvocationConfiguration initialConfiguration) {

            mInvocationId = initialConfiguration.mInvocationId;
            mResolutionType = initialConfiguration.mResolutionType;
            mStrategyType = initialConfiguration.mStrategyType;
        }

        /**
         * Builds and return the configuration instance.
         *
         * @return the context invocation configuration instance.
         */
        @Nonnull
        public ContextInvocationConfiguration buildConfiguration() {

            return new ContextInvocationConfiguration(mInvocationId, mResolutionType,
                                                      mStrategyType);
        }

        /**
         * Applies the specified configuration to this builder.
         *
         * @param configuration the invocation configuration.
         * @return this builder.
         * @throws java.lang.NullPointerException if the specified configuration is null.
         */
        @Nonnull
        public Builder configure(@Nonnull final ContextInvocationConfiguration configuration) {

            final int invocationId = configuration.mInvocationId;

            if (invocationId != AUTO) {

                withId(invocationId);
            }

            final ClashResolutionType resolutionType = configuration.mResolutionType;

            if (resolutionType != null) {

                onClash(resolutionType);
            }

            final CacheStrategyType strategyType = configuration.mStrategyType;

            if (strategyType != null) {

                onComplete(strategyType);
            }

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
        public Builder onClash(@Nullable final ClashResolutionType resolutionType) {

            mResolutionType = resolutionType;
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
        public Builder onComplete(@Nullable final CacheStrategyType strategyType) {

            mStrategyType = strategyType;
            return this;
        }

        /**
         * Tells the builder to identify the invocation with the specified ID.
         *
         * @param invocationId the invocation ID.
         * @return this builder.
         */
        @Nonnull
        public Builder withId(final int invocationId) {

            mInvocationId = invocationId;
            return this;
        }
    }
}
