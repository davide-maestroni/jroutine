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

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.DurationMeasure.fromUnit;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the invocation Loader configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The Loader ID backing the routine invocation. If set to {@link #AUTO} (the default behavior)
 * the invocation factory and the inputs {@code hashCode()} will be used to compute the ID.</li>
 * <li>The invocation ID which will override the factory {@code hashCode()} in the Loader ID
 * computation.</li>
 * <li>The clash resolution to apply in case a Loader with the same ID is running at the moment the
 * routine is invoked.</li>
 * <li>The clash resolution to apply in case a Loader with the same ID and with the same inputs is
 * running at the moment the routine is invoked.</li>
 * <li>The cache strategy to adopt on the invocation results.</li>
 * <li>The maximum time after which a cached result is considered to be stale and need to be
 * refreshed.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 04/19/2015.
 */
public final class LoaderConfiguration extends DeepEqualObject {

  /**
   * Constant identifying a Loader ID computed from the factory class and the input parameters.
   */
  public static final int AUTO = Integer.MIN_VALUE;

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private static final LoaderConfiguration sDefaultConfiguration = builder().buildConfiguration();

  private final ClashResolutionType mClashResolutionType;

  private final int mInvocationId;

  private final int mLoaderId;

  private final ClashResolutionType mMatchResolutionType;

  private final DurationMeasure mStaleTime;

  private final CacheStrategyType mStrategyType;

  /**
   * Constructor.
   *
   * @param loaderId            the Loader ID.
   * @param invocationId        the invocation ID.
   * @param clashResolutionType the type of clash resolution.
   * @param matchResolutionType the type of match resolution.
   * @param strategyType        the cache strategy type.
   * @param staleTime           the stale time.
   */
  private LoaderConfiguration(final int loaderId, final int invocationId,
      @Nullable final ClashResolutionType clashResolutionType,
      @Nullable final ClashResolutionType matchResolutionType,
      @Nullable final CacheStrategyType strategyType, @Nullable final DurationMeasure staleTime) {
    super(asArgs(loaderId, invocationId, clashResolutionType, matchResolutionType, strategyType,
        staleTime));
    mLoaderId = loaderId;
    mInvocationId = invocationId;
    mClashResolutionType = clashResolutionType;
    mMatchResolutionType = matchResolutionType;
    mStrategyType = strategyType;
    mStaleTime = staleTime;
  }

  /**
   * Returns a Loader configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  public static Builder<LoaderConfiguration> builder() {
    return new Builder<LoaderConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns a Loader configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial Loader configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<LoaderConfiguration> builderFrom(
      @Nullable final LoaderConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? builder()
        : new Builder<LoaderConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static LoaderConfiguration defaultConfiguration() {
    return sDefaultConfiguration;
  }

  /**
   * Returns a Loader configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<LoaderConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the type of cache strategy (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the cache strategy type.
   */
  public CacheStrategyType getCacheStrategyTypeOrElse(
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
  public ClashResolutionType getClashResolutionTypeOrElse(
      @Nullable final ClashResolutionType valueIfNotSet) {
    final ClashResolutionType resolutionType = mClashResolutionType;
    return (resolutionType != null) ? resolutionType : valueIfNotSet;
  }

  /**
   * Returns the invocation ID (AUTO by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the invocation ID.
   */
  public int getInvocationIdOrElse(final int valueIfNotSet) {
    final int invocationId = mInvocationId;
    return (invocationId != AUTO) ? invocationId : valueIfNotSet;
  }

  /**
   * Returns the Loader ID (AUTO by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the Loader ID.
   */
  public int getLoaderIdOrElse(final int valueIfNotSet) {
    final int loaderId = mLoaderId;
    return (loaderId != AUTO) ? loaderId : valueIfNotSet;
  }

  /**
   * Returns the type of resolution when invocation types and inputs match (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the clash resolution type.
   */
  public ClashResolutionType getMatchResolutionTypeOrElse(
      @Nullable final ClashResolutionType valueIfNotSet) {
    final ClashResolutionType resolutionType = mMatchResolutionType;
    return (resolutionType != null) ? resolutionType : valueIfNotSet;
  }

  /**
   * Returns the time after which results are considered to be stale (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the results stale time.
   */
  public DurationMeasure getResultStaleTimeOrElse(@Nullable final DurationMeasure valueIfNotSet) {
    final DurationMeasure staleTime = mStaleTime;
    return (staleTime != null) ? staleTime : valueIfNotSet;
  }

  /**
   * Result cache type enumeration.
   * <br>
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
     * Only in case of successful completion the results are cleared, otherwise they are retained.
     */
    CACHE_IF_ERROR,
    /**
     * On completion the invocation results are retained.
     */
    CACHE
  }

  /**
   * Invocation clash resolution enumeration.
   * <br>
   * The clash of two invocations happens when the same Loader ID is already in use at the time of
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
    ABORT_OTHER,
    /**
     * The clash is resolved by aborting the invocation with an
     * {@link com.github.dm.jrt.android.core.invocation.InvocationClashException
     * InvocationClashException}.
     */
    ABORT_THIS,
    /**
     * The clash is resolved by aborting both the invocations.
     */
    ABORT_BOTH
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
    TYPE apply(@NotNull LoaderConfiguration configuration);
  }

  /**
   * Builder of Loader configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private ClashResolutionType mClashResolutionType;

    private int mInvocationId;

    private int mLoaderId;

    private ClashResolutionType mMatchResolutionType;

    private DurationMeasure mStaleTime;

    private CacheStrategyType mStrategyType;

    /**
     * Constructor.
     *
     * @param configurable the configurable instance.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      mLoaderId = AUTO;
      mInvocationId = AUTO;
    }

    /**
     * Constructor.
     *
     * @param configurable         the configurable instance.
     * @param initialConfiguration the initial Loader configuration.
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
    public TYPE apply() {
      return mConfigurable.apply(buildConfiguration());
    }

    /**
     * Applies the specified configuration to this builder. A null value means that all the
     * configuration options will be reset to their default, otherwise only the non-default
     * options will be applied.
     *
     * @param configuration the Loader configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> with(@Nullable final LoaderConfiguration configuration) {
      if (configuration == null) {
        setConfiguration(defaultConfiguration());
        return this;
      }

      final int loaderId = configuration.mLoaderId;
      if (loaderId != AUTO) {
        withLoaderId(loaderId);
      }

      final int invocationId = configuration.mInvocationId;
      if (invocationId != AUTO) {
        withInvocationId(invocationId);
      }

      final ClashResolutionType clashResolutionType = configuration.mClashResolutionType;
      if (clashResolutionType != null) {
        withClashResolution(clashResolutionType);
      }

      final ClashResolutionType matchResolutionType = configuration.mMatchResolutionType;
      if (matchResolutionType != null) {
        withMatchResolution(matchResolutionType);
      }

      final CacheStrategyType strategyType = configuration.mStrategyType;
      if (strategyType != null) {
        withCacheStrategy(strategyType);
      }

      final DurationMeasure staleTime = configuration.mStaleTime;
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
     * happens when a Loader with the same ID is still running. A null value means that it is up
     * to the specific implementation to choose a default resolution type.
     *
     * @param resolutionType the type of resolution.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withClashResolution(@Nullable final ClashResolutionType resolutionType) {
      mClashResolutionType = resolutionType;
      return this;
    }

    /**
     * Tells the builder to identify the backing invocation with the specified ID.
     *
     * @param invocationId the invocation ID.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withInvocationId(final int invocationId) {
      mInvocationId = invocationId;
      return this;
    }

    /**
     * Tells the builder to identify the Loader with the specified ID.
     *
     * @param loaderId the Loader ID.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withLoaderId(final int loaderId) {
      mLoaderId = loaderId;
      return this;
    }

    /**
     * Tells the builder how to resolve clashes of invocations with same invocation type and
     * inputs. A clash happens when a Loader with the same ID is still running. A null value
     * means that it is up to the specific implementation to choose a default resolution type.
     *
     * @param resolutionType the type of resolution.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withMatchResolution(@Nullable final ClashResolutionType resolutionType) {
      mMatchResolutionType = resolutionType;
      return this;
    }

    /**
     * Sets the time after which results are considered to be stale. In case a clash is resolved
     * by joining the two invocations, and the results of the running one are stale, the invocation
     * execution is repeated. A null value means that the results are always valid.
     *
     * @param staleTime the stale time.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withResultStaleTime(@Nullable final DurationMeasure staleTime) {
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
    public Builder<TYPE> withResultStaleTime(final long time, @NotNull final TimeUnit timeUnit) {
      return withResultStaleTime(fromUnit(time, timeUnit));
    }

    @NotNull
    private LoaderConfiguration buildConfiguration() {
      return new LoaderConfiguration(mLoaderId, mInvocationId, mClashResolutionType,
          mMatchResolutionType, mStrategyType, mStaleTime);
    }

    private void setConfiguration(@NotNull final LoaderConfiguration configuration) {
      mLoaderId = configuration.mLoaderId;
      mInvocationId = configuration.mInvocationId;
      mClashResolutionType = configuration.mClashResolutionType;
      mMatchResolutionType = configuration.mMatchResolutionType;
      mStrategyType = configuration.mStrategyType;
      mStaleTime = configuration.mStaleTime;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<LoaderConfiguration> {

    @NotNull
    public LoaderConfiguration apply(@NotNull final LoaderConfiguration configuration) {
      return configuration;
    }
  }
}
