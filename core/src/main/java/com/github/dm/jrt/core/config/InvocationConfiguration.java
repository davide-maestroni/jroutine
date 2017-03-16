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

package com.github.dm.jrt.core.config;

import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.DurationMeasure.fromUnit;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the invocation configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The asynchronous runner used to execute the invocations.</li>
 * <li>The invocations priority. Every invocation will age each time an higher priority one takes
 * the precedence, so that older invocations slowly increases their priority. Such mechanism has
 * been implemented to avoid starvation of low priority invocations. Hence, when assigning priority
 * values, it is important to keep in mind that the difference between two priorities corresponds to
 * the maximum age the lower priority invocation will have, before getting precedence over the
 * higher priority one.</li>
 * <li>The invocation mode: simple, where all the inputs are passed to the same invocation instance,
 * and parallel, where each input is processed by a different invocation.</li>
 * <li>The core number of invocation instances to be retained in order to be re-used when needed.
 * When an invocation completes without being discarded, the instance is retained for future
 * executions.</li>
 * <li>The maximum number of invocation instances running at the same time. When the limit is
 * exceeded, the new invocation execution is delayed until one instance becomes available.</li>
 * <li>The order in which data are dispatched through the invocation channel. The order of input
 * data is not guaranteed. Nevertheless, it is possible to force data to be delivered in the same
 * order as they are passed to the channels, at the cost of a slightly increase in memory usage and
 * computation.</li>
 * <li>The backoff policy to be applied to the calling thread when the buffered input data exceed
 * the specified limit.</li>
 * <li>The maximum number of input data buffered in the invocation channel. When the number of data
 * exceeds it, a {@link com.github.dm.jrt.core.common.DeadlockException DeadlockException} will be
 * thrown.</li>
 * <li>The order in which data are dispatched through the result channel. The order of input data is
 * not guaranteed. Nevertheless, it is possible to force data to be delivered in the same order as
 * they are passed to the channels, at the cost of a slightly increase in memory usage and
 * computation.</li>
 * <li>The backoff policy to be applied to the calling thread when the buffered output data exceed
 * the specified limit.</li>
 * <li>The maximum number of output data buffered in the result channel. When the number of data
 * exceeds it, a {@link com.github.dm.jrt.core.common.DeadlockException DeadlockException} will be
 * thrown.</li>
 * <li>The maximum timeout while waiting for a new output to be available before performing the
 * specified action.</li>
 * <li>The action to be taken when no output becomes available before the timeout elapses.</li>
 * <li>The log instance to be used to trace the log messages.</li>
 * <li>The log level to be used to filter the log messages.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 11/15/2014.
 */
@SuppressWarnings("WeakerAccess")
public final class InvocationConfiguration extends DeepEqualObject {

  // TODO: 15/03/2017 instance backoff

  /**
   * Constant indicating the default value of an integer attribute.
   */
  public static final int DEFAULT = Integer.MIN_VALUE;

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private static final InvocationConfiguration sDefaultConfiguration =
      builder().buildConfiguration();

  private final int mCoreInvocations;

  private final Backoff mInputBackoff;

  private final int mInputMaxSize;

  private final OrderType mInputOrderType;

  private final InvocationModeType mInvocationMode;

  private final Log mLog;

  private final Level mLogLevel;

  private final int mMaxInvocations;

  private final Backoff mOutputBackoff;

  private final int mOutputMaxSize;

  private final OrderType mOutputOrderType;

  private final DurationMeasure mOutputTimeout;

  private final int mPriority;

  private final Runner mRunner;

  private final TimeoutActionType mTimeoutActionType;

  /**
   * Constructor.
   *
   * @param runner          the runner used for asynchronous invocations.
   * @param priority        the invocation priority.
   * @param invocationMode  the invocation mode.
   * @param maxInvocations  the maximum number of parallel running invocations. Must be positive.
   * @param coreInvocations the maximum number of retained invocation instances. Must not be
   *                        negative.
   * @param outputTimeout   the timeout for an invocation instance to produce a result.
   * @param actionType      the action to be taken if the timeout elapses before a readable
   *                        result is available.
   * @param inputOrderType  the order in which input data are collected from the input channel.
   * @param inputBackoff    the backoff policy to apply while waiting for an input to be passed to
   *                        the input channel.
   * @param inputMaxSize    the maximum number of buffered input data. Must be positive.
   * @param outputOrderType the order in which output data are collected from the result channel.
   * @param outputBackoff   the backoff policy to apply while waiting for an output to be passed
   *                        to the result channel.
   * @param outputMaxSize   the maximum number of buffered output data. Must be positive.
   * @param log             the log instance.
   * @param logLevel        the log level.
   */
  private InvocationConfiguration(@Nullable final Runner runner, final int priority,
      @Nullable final InvocationModeType invocationMode, final int maxInvocations,
      final int coreInvocations, @Nullable final DurationMeasure outputTimeout,
      @Nullable final TimeoutActionType actionType, @Nullable final OrderType inputOrderType,
      @Nullable final Backoff inputBackoff, final int inputMaxSize,
      @Nullable final OrderType outputOrderType, @Nullable final Backoff outputBackoff,
      final int outputMaxSize, @Nullable final Log log, @Nullable final Level logLevel) {
    super(asArgs(runner, priority, invocationMode, maxInvocations, coreInvocations, outputTimeout,
        actionType, inputOrderType, inputBackoff, inputMaxSize, outputOrderType, outputBackoff,
        outputMaxSize, log, logLevel));
    mRunner = runner;
    mInvocationMode = invocationMode;
    mPriority = priority;
    mMaxInvocations = maxInvocations;
    mCoreInvocations = coreInvocations;
    mOutputTimeout = outputTimeout;
    mTimeoutActionType = actionType;
    mInputOrderType = inputOrderType;
    mInputBackoff = inputBackoff;
    mInputMaxSize = inputMaxSize;
    mOutputOrderType = outputOrderType;
    mOutputBackoff = outputBackoff;
    mOutputMaxSize = outputMaxSize;
    mLog = log;
    mLogLevel = logLevel;
  }

  /**
   * Returns an invocation configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  public static Builder<InvocationConfiguration> builder() {
    return new Builder<InvocationConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns an invocation configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<InvocationConfiguration> builderFrom(
      @Nullable final InvocationConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? builder()
        : new Builder<InvocationConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns an invocation configuration builder initialized with the specified input
   * configuration.
   *
   * @param initialConfiguration the initial configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<InvocationConfiguration> builderFromInput(
      @Nullable final ChannelConfiguration initialConfiguration) {
    final Builder<InvocationConfiguration> builder = builder();
    if (initialConfiguration != null) {
      builder.withRunner(initialConfiguration.getRunnerOrElse(null))
             .withInputBackoff(initialConfiguration.getBackoffOrElse(null))
             .withInputMaxSize(initialConfiguration.getMaxSizeOrElse(DEFAULT))
             .withInputOrder(initialConfiguration.getOrderTypeOrElse(null))
             .withLog(initialConfiguration.getLogOrElse(null))
             .withLogLevel(initialConfiguration.getLogLevelOrElse(null))
             .withOutputTimeout(initialConfiguration.getOutputTimeoutOrElse(null))
             .withOutputTimeoutAction(initialConfiguration.getOutputTimeoutActionOrElse(null));
    }

    return builder;
  }

  /**
   * Returns an invocation configuration builder initialized with the specified output
   * configuration.
   *
   * @param initialConfiguration the initial configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<InvocationConfiguration> builderFromOutput(
      @Nullable final ChannelConfiguration initialConfiguration) {
    final Builder<InvocationConfiguration> builder = builder();
    if (initialConfiguration != null) {
      builder.withRunner(initialConfiguration.getRunnerOrElse(null))
             .withOutputBackoff(initialConfiguration.getBackoffOrElse(null))
             .withOutputMaxSize(initialConfiguration.getMaxSizeOrElse(DEFAULT))
             .withOutputOrder(initialConfiguration.getOrderTypeOrElse(null))
             .withLog(initialConfiguration.getLogOrElse(null))
             .withLogLevel(initialConfiguration.getLogLevelOrElse(null))
             .withOutputTimeout(initialConfiguration.getOutputTimeoutOrElse(null))
             .withOutputTimeoutAction(initialConfiguration.getOutputTimeoutActionOrElse(null));
    }

    return builder;
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static InvocationConfiguration defaultConfiguration() {
    return sDefaultConfiguration;
  }

  /**
   * Returns a configuration with just the specified invocation mode set as option.
   *
   * @param invocationMode the invocation mode.
   * @return the configuration instance.
   */
  @NotNull
  public static InvocationConfiguration withMode(
      @Nullable final InvocationModeType invocationMode) {
    return builder().withInvocationMode(invocationMode).apply();
  }

  /**
   * Returns a configuration with just the specified runner set as option.
   *
   * @param runner the runner instance.
   * @return the configuration instance.
   */
  @NotNull
  public static InvocationConfiguration withRunner(@Nullable final Runner runner) {
    return builder().withRunner(runner).apply();
  }

  /**
   * Returns a configuration with just the specified runner and invocation mode set as options.
   *
   * @param runner         the runner instance.
   * @param invocationMode the invocation mode.
   * @return the configuration instance.
   */
  @NotNull
  public static InvocationConfiguration withRunnerAndMode(@Nullable final Runner runner,
      @Nullable final InvocationModeType invocationMode) {
    return builder().withRunner(runner).withInvocationMode(invocationMode).apply();
  }

  /**
   * Returns an invocation configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<InvocationConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the maximum number of retained invocation instances (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the maximum number.
   */
  public int getCoreInvocationsOrElse(final int valueIfNotSet) {
    final int coreInvocations = mCoreInvocations;
    return (coreInvocations != DEFAULT) ? coreInvocations : valueIfNotSet;
  }

  /**
   * Returns the backoff policy to apply while waiting for an input to be processed (null by
   * default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the backoff policy.
   */
  public Backoff getInputBackoffOrElse(@Nullable final Backoff valueIfNotSet) {
    final Backoff inputBackoff = mInputBackoff;
    return (inputBackoff != null) ? inputBackoff : valueIfNotSet;
  }

  /**
   * Returns the maximum number of buffered input data (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the maximum size.
   */
  public int getInputMaxSizeOrElse(final int valueIfNotSet) {
    final int inputMaxSize = mInputMaxSize;
    return (inputMaxSize != DEFAULT) ? inputMaxSize : valueIfNotSet;
  }

  /**
   * Returns the input data order (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the order type.
   */
  public OrderType getInputOrderTypeOrElse(@Nullable final OrderType valueIfNotSet) {
    final OrderType inputOrderType = mInputOrderType;
    return (inputOrderType != null) ? inputOrderType : valueIfNotSet;
  }

  /**
   * Returns the invocation mode (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return invocation mode.
   */
  public InvocationModeType getInvocationModeOrElse(
      @Nullable final InvocationModeType valueIfNotSet) {
    final InvocationModeType invocationMode = mInvocationMode;
    return (invocationMode != null) ? invocationMode : valueIfNotSet;
  }

  /**
   * Returns the log level (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the log level.
   */
  public Level getLogLevelOrElse(@Nullable final Level valueIfNotSet) {
    final Level logLevel = mLogLevel;
    return (logLevel != null) ? logLevel : valueIfNotSet;
  }

  /**
   * Returns the log instance (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the log instance.
   */
  public Log getLogOrElse(@Nullable final Log valueIfNotSet) {
    final Log log = mLog;
    return (log != null) ? log : valueIfNotSet;
  }

  /**
   * Returns the maximum number of concurrently running invocation instances (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the maximum number.
   */
  public int getMaxInvocationsOrElse(final int valueIfNotSet) {
    final int maxInvocations = mMaxInvocations;
    return (maxInvocations != DEFAULT) ? maxInvocations : valueIfNotSet;
  }

  /**
   * Returns the backoff policy to apply while waiting for an output to be read from the output
   * channel (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the delay.
   */
  public Backoff getOutputBackoffOrElse(@Nullable final Backoff valueIfNotSet) {
    final Backoff outputBackoff = mOutputBackoff;
    return (outputBackoff != null) ? outputBackoff : valueIfNotSet;
  }

  /**
   * Returns the maximum number of buffered output data (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the maximum size.
   */
  public int getOutputMaxSizeOrElse(final int valueIfNotSet) {
    final int outputMaxSize = mOutputMaxSize;
    return (outputMaxSize != DEFAULT) ? outputMaxSize : valueIfNotSet;
  }

  /**
   * Returns the output data order (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the order type.
   */
  public OrderType getOutputOrderTypeOrElse(@Nullable final OrderType valueIfNotSet) {
    final OrderType outputOrderType = mOutputOrderType;
    return (outputOrderType != null) ? outputOrderType : valueIfNotSet;
  }

  /**
   * Returns the action to be taken if the timeout elapses before a readable result is available
   * (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the action type.
   */
  public TimeoutActionType getOutputTimeoutActionOrElse(
      @Nullable final TimeoutActionType valueIfNotSet) {
    final TimeoutActionType timeoutActionType = mTimeoutActionType;
    return (timeoutActionType != null) ? timeoutActionType : valueIfNotSet;
  }

  /**
   * Returns the timeout for an invocation instance to produce a readable result (null by
   * default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the timeout.
   */
  public DurationMeasure getOutputTimeoutOrElse(@Nullable final DurationMeasure valueIfNotSet) {
    final DurationMeasure outputTimeout = mOutputTimeout;
    return (outputTimeout != null) ? outputTimeout : valueIfNotSet;
  }

  /**
   * Returns the invocation priority (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the priority.
   */
  public int getPriorityOrElse(final int valueIfNotSet) {
    final int priority = mPriority;
    return (priority != DEFAULT) ? priority : valueIfNotSet;
  }

  /**
   * Returns the runner used for asynchronous invocations (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the runner instance.
   */
  public Runner getRunnerOrElse(@Nullable final Runner valueIfNotSet) {
    final Runner runner = mRunner;
    return (runner != null) ? runner : valueIfNotSet;
  }

  /**
   * Returns a channel configuration builder initialized with output related options converted from
   * this configuration ones.
   *
   * @return the builder.
   */
  @NotNull
  public ChannelConfiguration.Builder<ChannelConfiguration> inputConfigurationBuilder() {
    return ChannelConfiguration.builder()
                               .withRunner(getRunnerOrElse(null))
                               .withBackoff(getInputBackoffOrElse(null))
                               .withMaxSize(getInputMaxSizeOrElse(ChannelConfiguration.DEFAULT))
                               .withOrder(getInputOrderTypeOrElse(null))
                               .withLog(getLogOrElse(null))
                               .withLogLevel(getLogLevelOrElse(null))
                               .withOutputTimeout(getOutputTimeoutOrElse(null))
                               .withOutputTimeoutAction(getOutputTimeoutActionOrElse(null));
  }

  /**
   * Creates a new logger based on this configuration.
   *
   * @param context the context.
   * @return the new logger.
   */
  @NotNull
  public Logger newLogger(@NotNull final Object context) {
    return Logger.newLogger(getLogOrElse(null), getLogLevelOrElse(null), context);
  }

  /**
   * Returns a channel configuration builder initialized with input related options converted from
   * this configuration ones.
   *
   * @return the builder.
   */
  @NotNull
  public ChannelConfiguration.Builder<ChannelConfiguration> outputConfigurationBuilder() {
    return ChannelConfiguration.builder()
                               .withRunner(getRunnerOrElse(null))
                               .withBackoff(getOutputBackoffOrElse(null))
                               .withMaxSize(getOutputMaxSizeOrElse(ChannelConfiguration.DEFAULT))
                               .withOrder(getOutputOrderTypeOrElse(null))
                               .withLog(getLogOrElse(null))
                               .withLogLevel(getLogLevelOrElse(null))
                               .withOutputTimeout(getOutputTimeoutOrElse(null))
                               .withOutputTimeoutAction(getOutputTimeoutActionOrElse(null));
  }

  /**
   * Enumeration defining the mode in which the invocation is executed.
   */
  public enum InvocationModeType {

    /**
     * All the input passed to the invocation channel are processed by the same invocation instance.
     */
    SIMPLE,

    /**
     * Each input passed to the invocation channel is processed by a different invocation instance.
     */
    PARALLEL
  }

  /**
   * Interface exposing constants which can be used as a common set of priorities.
   * <p>
   * Note that, since the priority value can be any in an integer range, it is always possible to
   * customize the values so to create a personalized set.
   */
  public interface AgingPriority {

    /**
     * High priority.
     */
    int HIGH_PRIORITY = 10;

    /**
     * Highest priority.
     */
    int HIGHEST_PRIORITY = HIGH_PRIORITY << 1;

    /**
     * Low priority.
     */
    int LOWEST_PRIORITY = -HIGHEST_PRIORITY;

    /**
     * Lowest priority.
     */
    int LOW_PRIORITY = -HIGH_PRIORITY;

    /**
     * Normal priority.
     */
    int NORMAL_PRIORITY = 0;
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
    TYPE apply(@NotNull InvocationConfiguration configuration);
  }

  /**
   * Interface exposing constants which can be used as a set of priorities ignoring the aging of
   * executions.
   * <p>
   * Note that, since the priority value can be any in an integer range, it is always possible to
   * customize the values so to create a personalized set.
   */
  public interface NotAgingPriority {

    /**
     * Highest priority.
     */
    int HIGHEST_PRIORITY = Integer.MAX_VALUE;

    /**
     * High priority.
     */
    int HIGH_PRIORITY = HIGHEST_PRIORITY >> 1;

    /**
     * Low priority.
     */
    int LOWEST_PRIORITY = -HIGHEST_PRIORITY;

    /**
     * Lowest priority.
     */
    int LOW_PRIORITY = -HIGH_PRIORITY;

    /**
     * Normal priority.
     */
    int NORMAL_PRIORITY = 0;
  }

  /**
   * Builder of invocation configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private int mCoreInvocations;

    private Backoff mInputBackoff;

    private int mInputMaxSize;

    private OrderType mInputOrderType;

    private InvocationModeType mInvocationMode;

    private Log mLog;

    private Level mLogLevel;

    private int mMaxInvocations;

    private Backoff mOutputBackoff;

    private int mOutputMaxSize;

    private OrderType mOutputOrderType;

    private DurationMeasure mOutputTimeout;

    private int mPriority;

    private Runner mRunner;

    private TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param configurable the configurable instance.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      mPriority = DEFAULT;
      mMaxInvocations = DEFAULT;
      mCoreInvocations = DEFAULT;
      mInputMaxSize = DEFAULT;
      mOutputMaxSize = DEFAULT;
    }

    /**
     * Constructor.
     *
     * @param configurable         the configurable instance.
     * @param initialConfiguration the initial configuration.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable,
        @NotNull final InvocationConfiguration initialConfiguration) {
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
     * Sets the number of invocation instances which represents the core pool of reusable
     * invocations. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is
     * up to the specific implementation to choose a default one.
     *
     * @param coreInvocations the core number of invocation instances.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is negative.
     */
    @NotNull
    public Builder<TYPE> withCoreInvocations(final int coreInvocations) {
      if (coreInvocations != DEFAULT) {
        ConstantConditions.notNegative("maximum number of retained instances", coreInvocations);
      }

      mCoreInvocations = coreInvocations;
      return this;
    }

    /**
     * Resets all the options to their default values.
     *
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withDefaults() {
      setConfiguration(defaultConfiguration());
      return this;
    }

    /**
     * Sets the backoff policy to apply while waiting for the invocation channel to have room
     * for additional data.
     * <p>
     * Note that the backoff instance will be likely called from different threads, so, it's
     * responsibility of the implementing class to ensure that consistent delays are returned
     * based on the specified excess count.
     * <p>
     * This configuration option should be used on conjunction with the input limit, or it might
     * have no effect on the invocation execution.
     *
     * @param backoff the backoff policy.
     * @return this builder.
     * @see #withInputMaxSize(int)
     */
    @NotNull
    public Builder<TYPE> withInputBackoff(@Nullable final Backoff backoff) {
      mInputBackoff = backoff;
      return this;
    }

    /**
     * Sets the maximum number of data that the invocation channel can retain before they are
     * consumed. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up
     * to the specific implementation to choose a default one.
     * <br>
     * When the maximum capacity is exceeded, the invocation will be aborted with an
     * {@link com.github.dm.jrt.core.channel.InputDeadlockException InputDeadlockException}.
     *
     * @param inputMaxSize the maximum size.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @NotNull
    public Builder<TYPE> withInputMaxSize(final int inputMaxSize) {
      if (inputMaxSize != DEFAULT) {
        ConstantConditions.positive("input buffer size", inputMaxSize);
      }

      mInputMaxSize = inputMaxSize;
      return this;
    }

    /**
     * Sets the order in which input data are collected from the invocation channel. A null
     * value means that it is up to the specific implementation to choose a default one.
     * <p>
     * Note that this is just the initial configuration of the invocation, since the channel
     * order can be dynamically changed through the dedicated methods.
     *
     * @param orderType the order type.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withInputOrder(@Nullable final OrderType orderType) {
      mInputOrderType = orderType;
      return this;
    }

    /**
     * Sets the invocation mode to be used.
     * <p>
     * There are two different ways to invoke a routine:
     * <p>
     * <b>Simple invocation</b><br>
     * The routine starts an invocation employing the configured runner and delivers all the input
     * passed to the invocation channel to the same invocation instance.
     * <p>
     * <b>Parallel invocation</b><br>
     * The routine starts an invocation which in turn spawns another invocation for each input
     * passed to the invocation channel. This particular type of invocation obviously produces
     * meaningful results only for routines which takes a single input parameter and computes the
     * relative output results.
     *
     * @param invocationMode the invocation mode.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withInvocationMode(@Nullable final InvocationModeType invocationMode) {
      mInvocationMode = invocationMode;
      return this;
    }

    /**
     * Sets the log instance. A null value means that it is up to the specific implementation to
     * choose a default one.
     *
     * @param log the log instance.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withLog(@Nullable final Log log) {
      mLog = log;
      return this;
    }

    /**
     * Sets the log level. A null value means that it is up to the specific implementation to
     * choose a default one.
     *
     * @param level the log level.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withLogLevel(@Nullable final Level level) {
      mLogLevel = level;
      return this;
    }

    /**
     * Sets the max number of concurrently running invocation instances. A
     * {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up to the
     * specific implementation to choose a default one.
     *
     * @param maxInvocations the max number of invocation instances.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @NotNull
    public Builder<TYPE> withMaxInvocations(final int maxInvocations) {
      if (maxInvocations != DEFAULT) {
        ConstantConditions.positive("maximum number of concurrently running instances",
            maxInvocations);
      }

      mMaxInvocations = maxInvocations;
      return this;
    }

    /**
     * Sets the backoff policy to apply while waiting for the result channel to have room for
     * additional data.
     * <p>
     * Note that the backoff instance will be likely called from different threads, so, it's
     * responsibility of the implementing class to ensure that consistent delays are returned
     * based on the specified excess count.
     * <p>
     * This configuration option should be used on conjunction with the output limit, or it
     * might have no effect on the invocation execution.
     *
     * @param backoff the backoff policy.
     * @return this builder.
     * @see #withOutputMaxSize(int)
     */
    @NotNull
    public Builder<TYPE> withOutputBackoff(@Nullable final Backoff backoff) {
      mOutputBackoff = backoff;
      return this;
    }

    /**
     * Sets the maximum number of data that the result channel can retain before they are
     * consumed. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up
     * to the specific implementation to choose a default one.
     * <br>
     * When the maximum capacity is exceeded, the invocation will be aborted with an
     * {@link com.github.dm.jrt.core.channel.InputDeadlockException InputDeadlockException}.
     *
     * @param outputMaxSize the maximum size.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @NotNull
    public Builder<TYPE> withOutputMaxSize(final int outputMaxSize) {
      if (outputMaxSize != DEFAULT) {
        ConstantConditions.positive("output buffer size", outputMaxSize);
      }

      mOutputMaxSize = outputMaxSize;
      return this;
    }

    /**
     * Sets the order in which output data are collected from the result channel. A null value
     * means that it is up to the specific implementation to choose a default order one.
     * <p>
     * Note that this is just the initial configuration of the invocation, since the channel
     * order can be dynamically changed through the dedicated methods.
     *
     * @param orderType the order type.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withOutputOrder(@Nullable final OrderType orderType) {
      mOutputOrderType = orderType;
      return this;
    }

    /**
     * Sets the timeout for an invocation to produce a readable result.
     * <p>
     * Note that this is just the initial configuration of the invocation, since the output
     * timeout can be dynamically changed through the dedicated methods.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     */
    @NotNull
    public Builder<TYPE> withOutputTimeout(final long timeout, @NotNull final TimeUnit timeUnit) {
      return withOutputTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Sets the timeout for an invocation to produce a readable result. A null value means that
     * it is up to the specific implementation to choose a default one.
     * <p>
     * Note that this is just the initial configuration of the invocation, since the output
     * timeout can be dynamically changed through the dedicated methods.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withOutputTimeout(@Nullable final DurationMeasure timeout) {
      mOutputTimeout = timeout;
      return this;
    }

    /**
     * Sets the action to be taken if the timeout elapses before a result can be read from the
     * invocation channel. A null value means that it is up to the specific implementation to
     * choose a default one.
     * <p>
     * Note that this is just the initial configuration of the invocation, since the output
     * timeout action can be dynamically changed through the dedicated methods.
     *
     * @param actionType the action type.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withOutputTimeoutAction(@Nullable final TimeoutActionType actionType) {
      mTimeoutActionType = actionType;
      return this;
    }

    /**
     * Applies the specified patch configuration to this builder. Only the non-default options will
     * be applied. A null value will have no effect.
     *
     * @param configuration the invocation configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withPatch(@Nullable final InvocationConfiguration configuration) {
      if (configuration == null) {
        return this;
      }

      applyBaseConfiguration(configuration);
      applyChannelConfiguration(configuration);
      applyLogConfiguration(configuration);
      return this;
    }

    /**
     * Sets the invocation priority. A {@link InvocationConfiguration#DEFAULT DEFAULT} value
     * means that the invocations will be executed with no specific priority.
     *
     * @param priority the priority.
     * @return this builder.
     * @see com.github.dm.jrt.core.runner.PriorityRunner PriorityRunner
     */
    @NotNull
    public Builder<TYPE> withPriority(final int priority) {
      mPriority = priority;
      return this;
    }

    /**
     * Sets the asynchronous runner instance. A null value means that it is up to the specific
     * implementation to choose a default one.
     *
     * @param runner the runner instance.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withRunner(@Nullable final Runner runner) {
      mRunner = runner;
      return this;
    }

    private void applyBaseConfiguration(@NotNull final InvocationConfiguration configuration) {
      final Runner runner = configuration.mRunner;
      if (runner != null) {
        withRunner(runner);
      }

      final int priority = configuration.mPriority;
      if (priority != DEFAULT) {
        withPriority(priority);
      }

      final InvocationModeType invocationMode = configuration.mInvocationMode;
      if (invocationMode != null) {
        withInvocationMode(invocationMode);
      }

      final int maxInvocations = configuration.mMaxInvocations;
      if (maxInvocations != DEFAULT) {
        withMaxInvocations(maxInvocations);
      }

      final int coreInvocations = configuration.mCoreInvocations;
      if (coreInvocations != DEFAULT) {
        withCoreInvocations(coreInvocations);
      }

      final DurationMeasure outputTimeout = configuration.mOutputTimeout;
      if (outputTimeout != null) {
        withOutputTimeout(outputTimeout);
      }

      final TimeoutActionType timeoutActionType = configuration.mTimeoutActionType;
      if (timeoutActionType != null) {
        withOutputTimeoutAction(timeoutActionType);
      }
    }

    private void applyChannelConfiguration(@NotNull final InvocationConfiguration configuration) {
      final OrderType inputOrderType = configuration.mInputOrderType;
      if (inputOrderType != null) {
        withInputOrder(inputOrderType);
      }

      final Backoff inputBackoff = configuration.mInputBackoff;
      if (inputBackoff != null) {
        withInputBackoff(inputBackoff);
      }

      final int inputSize = configuration.mInputMaxSize;
      if (inputSize != DEFAULT) {
        withInputMaxSize(inputSize);
      }

      final OrderType outputOrderType = configuration.mOutputOrderType;
      if (outputOrderType != null) {
        withOutputOrder(outputOrderType);
      }

      final Backoff outputBackoff = configuration.mOutputBackoff;
      if (outputBackoff != null) {
        withOutputBackoff(outputBackoff);
      }

      final int outputSize = configuration.mOutputMaxSize;
      if (outputSize != DEFAULT) {
        withOutputMaxSize(outputSize);
      }
    }

    private void applyLogConfiguration(@NotNull final InvocationConfiguration configuration) {
      final Log log = configuration.mLog;
      if (log != null) {
        withLog(log);
      }

      final Level logLevel = configuration.mLogLevel;
      if (logLevel != null) {
        withLogLevel(logLevel);
      }
    }

    @NotNull
    private InvocationConfiguration buildConfiguration() {
      return new InvocationConfiguration(mRunner, mPriority, mInvocationMode, mMaxInvocations,
          mCoreInvocations, mOutputTimeout, mTimeoutActionType, mInputOrderType, mInputBackoff,
          mInputMaxSize, mOutputOrderType, mOutputBackoff, mOutputMaxSize, mLog, mLogLevel);
    }

    private void setConfiguration(@NotNull final InvocationConfiguration configuration) {
      mRunner = configuration.mRunner;
      mPriority = configuration.mPriority;
      mInvocationMode = configuration.mInvocationMode;
      mMaxInvocations = configuration.mMaxInvocations;
      mCoreInvocations = configuration.mCoreInvocations;
      mOutputTimeout = configuration.mOutputTimeout;
      mTimeoutActionType = configuration.mTimeoutActionType;
      mInputOrderType = configuration.mInputOrderType;
      mInputBackoff = configuration.mInputBackoff;
      mInputMaxSize = configuration.mInputMaxSize;
      mOutputOrderType = configuration.mOutputOrderType;
      mOutputBackoff = configuration.mOutputBackoff;
      mOutputMaxSize = configuration.mOutputMaxSize;
      mLog = configuration.mLog;
      mLogLevel = configuration.mLogLevel;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<InvocationConfiguration> {

    @NotNull
    public InvocationConfiguration apply(@NotNull final InvocationConfiguration configuration) {
      return configuration;
    }
  }
}
