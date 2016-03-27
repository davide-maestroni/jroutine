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

package com.github.dm.jrt.android.core.config;

import android.os.Looper;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.TimeDuration.fromUnit;

/**
 * Class storing the invocation loader configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * The configuration is used to set a specific loader ID to each invocation created by a routine, or
 * to override the factory {@code equals()} and {@code hashCode()} by specifying a routine ID.<br/>
 * Moreover, it is possible to set a specific type of resolution when two invocations clashes, that
 * is, they share the same loader ID, and to set a specific type of caching of the invocation
 * results.<br/>
 * In case a clash is resolved by joining the two invocations, it is possible to specify a maximum
 * time after which the results of the first invocation are considered to be stale, and thus the
 * invocation is repeated.<br/>
 * Finally, a specific looper, other than the main thread one, can be chosen to dispatch the results
 * coming from the invocations.
 * <p/>
 * Created by davide-maestroni on 04/19/2015.
 */
public final class LoaderConfiguration {

    /**
     * Constant identifying a loader ID computed from the factory class and the input parameters.
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

    private final int mRoutineId;

    private final TimeDuration mStaleTime;

    private final CacheStrategyType mStrategyType;

    /**
     * Constructor.
     *
     * @param looper              the looper instance.
     * @param loaderId            the the loader ID.
     * @param routineId           the the routine ID.
     * @param resolutionType      the type of resolution.
     * @param inputResolutionType the type of input resolution.
     * @param strategyType        the cache strategy type.
     * @param staleTime           the stale time.
     */
    private LoaderConfiguration(@Nullable final Looper looper, final int loaderId,
            final int routineId, @Nullable final ClashResolutionType resolutionType,
            @Nullable final ClashResolutionType inputResolutionType,
            @Nullable final CacheStrategyType strategyType,
            @Nullable final TimeDuration staleTime) {

        mLooper = looper;
        mLoaderId = loaderId;
        mRoutineId = routineId;
        mResolutionType = resolutionType;
        mInputResolutionType = inputResolutionType;
        mStrategyType = strategyType;
        mStaleTime = staleTime;
    }

    /**
     * Returns a loader configuration builder.
     *
     * @return the builder.
     */
    @NotNull
    public static Builder<LoaderConfiguration> builder() {

        return new Builder<LoaderConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns a loader configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial loader configuration.
     * @return the builder.
     */
    @NotNull
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
    @NotNull
    public Builder<LoaderConfiguration> builderFrom() {

        return builderFrom(this);
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(final Object o) {

        // AUTO-GENERATED CODE
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LoaderConfiguration that = (LoaderConfiguration) o;
        if (mLoaderId != that.mLoaderId) {
            return false;
        }

        if (mRoutineId != that.mRoutineId) {
            return false;
        }

        if (mInputResolutionType != that.mInputResolutionType) {
            return false;
        }

        if (mLooper != null ? !mLooper.equals(that.mLooper) : that.mLooper != null) {
            return false;
        }

        if (mResolutionType != that.mResolutionType) {
            return false;
        }

        if (mStaleTime != null ? !mStaleTime.equals(that.mStaleTime) : that.mStaleTime != null) {
            return false;
        }

        return mStrategyType == that.mStrategyType;
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        int result = mInputResolutionType != null ? mInputResolutionType.hashCode() : 0;
        result = 31 * result + mLoaderId;
        result = 31 * result + (mLooper != null ? mLooper.hashCode() : 0);
        result = 31 * result + (mResolutionType != null ? mResolutionType.hashCode() : 0);
        result = 31 * result + mRoutineId;
        result = 31 * result + (mStaleTime != null ? mStaleTime.hashCode() : 0);
        result = 31 * result + (mStrategyType != null ? mStrategyType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {

        // AUTO-GENERATED CODE
        return "LoaderConfiguration{" +
                "mInputResolutionType=" + mInputResolutionType +
                ", mLoaderId=" + mLoaderId +
                ", mLooper=" + mLooper +
                ", mResolutionType=" + mResolutionType +
                ", mRoutineId=" + mRoutineId +
                ", mStaleTime=" + mStaleTime +
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
     * Returns the time after which results are considered to be stale (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the results stale time.
     */
    public TimeDuration getResultStaleTimeOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration staleTime = mStaleTime;
        return (staleTime != null) ? staleTime : valueIfNotSet;
    }

    /**
     * Returns the routine ID (AUTO by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the routine ID.
     */
    public int getRoutineIdOr(final int valueIfNotSet) {

        final int routineId = mRoutineId;
        return (routineId != AUTO) ? routineId : valueIfNotSet;
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
     * <li>the running invocation is retained and the current one joins it</li>
     * <li>the running invocation is aborted</li>
     * <li>the current invocation is aborted</li>
     * <li>both running and current invocations are aborted</li>
     * </ul>
     * Two different types of resolution can be set based on whether the input data are different or
     * not.
     */
    public enum ClashResolutionType {

        /**
         * The clash is resolved by joining the two invocations.
         */
        JOIN,
        /**
         * The clash is resolved by aborting the running invocation.
         */
        ABORT_THAT,
        /**
         * The clash is resolved by aborting the invocation with an
         * {@link com.github.dm.jrt.android.core.invocation.InvocationClashException
         * InvocationClashException}.
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
        @NotNull
        TYPE setConfiguration(@NotNull LoaderConfiguration configuration);
    }

    /**
     * Builder of loader configurations.
     *
     * @param <TYPE> the configurable object type.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private ClashResolutionType mInputResolutionType;

        private int mLoaderId;

        private Looper mLooper;

        private ClashResolutionType mResolutionType;

        private int mRoutineId;

        private TimeDuration mStaleTime;

        private CacheStrategyType mStrategyType;

        /**
         * Constructor.
         *
         * @param configurable the configurable instance.
         */
        public Builder(@NotNull final Configurable<? extends TYPE> configurable) {

            mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
            mLoaderId = AUTO;
            mRoutineId = AUTO;
        }

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial loader configuration.
         */
        public Builder(@NotNull final Configurable<? extends TYPE> configurable,
                @NotNull final LoaderConfiguration initialConfiguration) {

            mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
            setConfiguration(initialConfiguration);
        }

        /**
         * Applies this configuration and returns the configured object.
         *
         * @return the configured object.
         */
        @NotNull
        public TYPE setConfiguration() {

            return mConfigurable.setConfiguration(buildConfiguration());
        }

        /**
         * Applies the specified configuration to this builder. A null value means that all the
         * configuration options will be reset to their default, otherwise only the non-default
         * options will be applied.
         *
         * @param configuration the loader configuration.
         * @return this builder.
         */
        @NotNull
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
                withLoaderId(loaderId);
            }

            final int routineId = configuration.mRoutineId;
            if (routineId != AUTO) {
                withRoutineId(routineId);
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

            final TimeDuration staleTime = configuration.mStaleTime;
            if (staleTime != null) {
                withResultStaleTime(staleTime);
            }

            return this;
        }

        /**
         * Tells the builder how to cache the invocation result after its completion. A null value
         * means that it is up to the specific implementation to choose a default strategy.
         *
         * @param strategyType the cache strategy type.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withCacheStrategy(@Nullable final CacheStrategyType strategyType) {

            mStrategyType = strategyType;
            return this;
        }

        /**
         * Tells the builder how to resolve clashes of invocations with different inputs. A clash
         * happens when a loader with the same ID is still running. A null value means that it is up
         * to the specific implementation to choose a default resolution type.
         *
         * @param resolutionType the type of resolution.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withClashResolution(
                @Nullable final ClashResolutionType resolutionType) {

            mResolutionType = resolutionType;
            return this;
        }

        /**
         * Tells the builder how to resolve clashes of invocations with same inputs. A clash happens
         * when a loader with the same ID is still running. A null value means that it is up to the
         * specific implementation to choose a default resolution type.
         *
         * @param resolutionType the type of resolution.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withInputClashResolution(
                @Nullable final ClashResolutionType resolutionType) {

            mInputResolutionType = resolutionType;
            return this;
        }

        /**
         * Tells the builder to identify the loader with the specified ID.
         *
         * @param loaderId the loader ID.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withLoaderId(final int loaderId) {

            mLoaderId = loaderId;
            return this;
        }

        /**
         * Sets the looper on which the results from the loader are dispatched. A null value means
         * that results will be dispatched on the main thread (as by default).
         *
         * @param looper the looper instance.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withResultLooper(@Nullable final Looper looper) {

            mLooper = looper;
            return this;
        }

        /**
         * Sets the time after which results are considered to be stale. In case a clash is resolved
         * by joining the two invocations, and the results of the running one are stale, the
         * invocation execution is repeated. A null value means that the results are always valid.
         *
         * @param staleTime the stale time.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withResultStaleTime(@Nullable final TimeDuration staleTime) {

            mStaleTime = staleTime;
            return this;
        }

        /**
         * Sets the time after which results are considered to be stale.
         *
         * @param time     the time.
         * @param timeUnit the time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         */
        @NotNull
        public Builder<TYPE> withResultStaleTime(final long time,
                @NotNull final TimeUnit timeUnit) {

            return withResultStaleTime(fromUnit(time, timeUnit));
        }

        /**
         * Tells the builder to identify the routine with the specified ID.
         *
         * @param routineId the routine ID.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withRoutineId(final int routineId) {

            mRoutineId = routineId;
            return this;
        }

        @NotNull
        private LoaderConfiguration buildConfiguration() {

            return new LoaderConfiguration(mLooper, mLoaderId, mRoutineId, mResolutionType,
                    mInputResolutionType, mStrategyType, mStaleTime);
        }

        private void setConfiguration(@NotNull final LoaderConfiguration configuration) {

            mLooper = configuration.mLooper;
            mLoaderId = configuration.mLoaderId;
            mRoutineId = configuration.mRoutineId;
            mResolutionType = configuration.mResolutionType;
            mInputResolutionType = configuration.mInputResolutionType;
            mStrategyType = configuration.mStrategyType;
            mStaleTime = configuration.mStaleTime;
        }
    }

    /**
     * Default configurable implementation.
     */
    private static class DefaultConfigurable implements Configurable<LoaderConfiguration> {

        @NotNull
        public LoaderConfiguration setConfiguration(
                @NotNull final LoaderConfiguration configuration) {

            return configuration;
        }
    }
}
