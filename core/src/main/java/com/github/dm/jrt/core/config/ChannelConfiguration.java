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
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;

/**
 * Class storing the channel configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The asynchronous runner used to dispatch delayed data.</li>
 * <li>The order in which data are dispatched through the channel. The order of input data is not
 * guaranteed. Nevertheless, it is possible to force data to be delivered in the same order as they
 * are passed to the channels, at the cost of a slightly increase in memory usage and computation.
 * </li>
 * <li>The backoff policy to be applied to the calling thread when the buffered data exceed the
 * specified limit.</li>
 * <li>The maximum number of input data buffered in the channel.When the number of data exceeds it,
 * a {@link com.github.dm.jrt.core.common.DeadlockException DeadlockException} will be thrown.</li>
 * <li>The maximum timeout while waiting for a new output to be available before performing the
 * specified action.</li>
 * <li>The action to be taken when no output becomes available before the timeout elapses.</li>
 * <li>The log instance to be used to trace the log messages.</li>
 * <li>The log level to be used to filter the log messages.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 07/03/2015.
 */
@SuppressWarnings("WeakerAccess")
public final class ChannelConfiguration extends DeepEqualObject {

  /**
   * Constant indicating the default value of an integer attribute.
   */
  public static final int DEFAULT = Integer.MIN_VALUE;

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private static final ChannelConfiguration sDefaultConfiguration = builder().buildConfiguration();

  private final Backoff mChannelBackoff;

  private final int mChannelMaxSize;

  private final OrderType mChannelOrderType;

  private final Log mLog;

  private final Level mLogLevel;

  private final UnitDuration mOutputTimeout;

  private final Runner mRunner;

  private final TimeoutActionType mTimeoutActionType;

  /**
   * Constructor.
   *
   * @param runner           the runner used for asynchronous inputs.
   * @param outputTimeout    the timeout for the channel to produce an output.
   * @param actionType       the action to be taken if the timeout elapses before a readable
   *                         output is available.
   * @param channelOrderType the order in which data are collected from the output channel.
   * @param channelBackoff   the backoff policy to apply while waiting for an object to be passed
   *                         to the channel.
   * @param channelMaxSize   the maximum number of buffered data. Must be positive.
   * @param log              the log instance.
   * @param logLevel         the log level.
   */
  private ChannelConfiguration(@Nullable final Runner runner,
      @Nullable final UnitDuration outputTimeout, @Nullable final TimeoutActionType actionType,
      @Nullable final OrderType channelOrderType, @Nullable final Backoff channelBackoff,
      final int channelMaxSize, @Nullable final Log log, @Nullable final Level logLevel) {
    super(
        asArgs(runner, outputTimeout, actionType, channelOrderType, channelBackoff, channelMaxSize,
            log, logLevel));
    mRunner = runner;
    mOutputTimeout = outputTimeout;
    mTimeoutActionType = actionType;
    mChannelOrderType = channelOrderType;
    mChannelBackoff = channelBackoff;
    mChannelMaxSize = channelMaxSize;
    mLog = log;
    mLogLevel = logLevel;
  }

  /**
   * Returns a channel configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  public static Builder<ChannelConfiguration> builder() {
    return new Builder<ChannelConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns a channel configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<ChannelConfiguration> builderFrom(
      @Nullable final ChannelConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? builder()
        : new Builder<ChannelConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static ChannelConfiguration defaultConfiguration() {
    return sDefaultConfiguration;
  }

  /**
   * Returns a channel configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<ChannelConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the backoff policy to apply while waiting for an object to be passed to the channel
   * (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the delay.
   */
  public Backoff getBackoffOrElse(@Nullable final Backoff valueIfNotSet) {
    final Backoff channelBackoff = mChannelBackoff;
    return (channelBackoff != null) ? channelBackoff : valueIfNotSet;
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
   * Returns the maximum number of buffered data (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the maximum size.
   */
  public int getMaxSizeOrElse(final int valueIfNotSet) {
    final int maxSize = mChannelMaxSize;
    return (maxSize != DEFAULT) ? maxSize : valueIfNotSet;
  }

  /**
   * Returns the data order (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the order type.
   */
  public OrderType getOrderTypeOrElse(@Nullable final OrderType valueIfNotSet) {
    final OrderType orderType = mChannelOrderType;
    return (orderType != null) ? orderType : valueIfNotSet;
  }

  /**
   * Returns the action to be taken if the timeout elapses before a readable output is available
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
   * Returns the timeout for the channel to produce a readable output (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the timeout.
   */
  public UnitDuration getOutputTimeoutOrElse(@Nullable final UnitDuration valueIfNotSet) {
    final UnitDuration outputTimeout = mOutputTimeout;
    return (outputTimeout != null) ? outputTimeout : valueIfNotSet;
  }

  /**
   * Returns the runner used for asynchronous inputs (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the runner instance.
   */
  public Runner getRunnerOrElse(@Nullable final Runner valueIfNotSet) {
    final Runner runner = mRunner;
    return (runner != null) ? runner : valueIfNotSet;
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
   * Enumeration defining how data are ordered inside a channel.
   */
  public enum OrderType {

    /**
     * Sorted.
     * <br>
     * Data are passed to the invocation or the channel consumer in the same order as they are
     * passed to the channel, independently from the specific delay.
     */
    SORTED,
    /**
     * Unsorted.
     * <br>
     * Data are passed to the invocation or the channel consumer as soon as they are available.
     */
    UNSORTED
  }

  /**
   * Enumeration indicating the type of action to be taken on output channel timeout.
   */
  public enum TimeoutActionType {

    /**
     * Fail with exception.
     * <br>
     * If no result is available after the specified timeout, the called method will throw an
     * {@link com.github.dm.jrt.core.channel.OutputTimeoutException OutputTimeoutException}.
     */
    FAIL,
    /**
     * Continue execution.
     * <br>
     * If no result is available after the specified timeout, the called method will continue
     * its execution and eventually exit.
     */
    CONTINUE,
    /**
     * Abort invocation.
     * <br>
     * If no result is available after the specified timeout, the invocation will be aborted and
     * the method will immediately exit.
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
    TYPE apply(@NotNull ChannelConfiguration configuration);
  }

  /**
   * Builder of channel configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private Backoff mChannelBackoff;

    private int mChannelMaxSize;

    private OrderType mChannelOrderType;

    private Log mLog;

    private Level mLogLevel;

    private UnitDuration mOutputTimeout;

    private Runner mRunner;

    private TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param configurable the configurable instance.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      mChannelMaxSize = DEFAULT;
    }

    /**
     * Constructor.
     *
     * @param configurable         the configurable instance.
     * @param initialConfiguration the initial configuration.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable,
        @NotNull final ChannelConfiguration initialConfiguration) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      setConfiguration(initialConfiguration);
    }

    /**
     * Applies this configuration and returns the configured object.
     *
     * @return the configured object.
     */
    @NotNull
    public TYPE configured() {
      return mConfigurable.apply(buildConfiguration());
    }

    /**
     * Applies the specified configuration to this builder. A null value means that all the
     * configuration options will be reset to their default, otherwise only the non-default
     * options will be applied.
     *
     * @param configuration the channel configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> with(@Nullable final ChannelConfiguration configuration) {
      if (configuration == null) {
        setConfiguration(defaultConfiguration());
        return this;
      }

      final Runner runner = configuration.mRunner;
      if (runner != null) {
        withRunner(runner);
      }

      final UnitDuration outputTimeout = configuration.mOutputTimeout;
      if (outputTimeout != null) {
        withOutputTimeout(outputTimeout);
      }

      final TimeoutActionType timeoutActionType = configuration.mTimeoutActionType;
      if (timeoutActionType != null) {
        withOutputTimeoutAction(timeoutActionType);
      }

      final OrderType orderType = configuration.mChannelOrderType;
      if (orderType != null) {
        withOrder(orderType);
      }

      final Backoff channelBackoff = configuration.mChannelBackoff;
      if (channelBackoff != null) {
        withBackoff(channelBackoff);
      }

      final int maxSize = configuration.mChannelMaxSize;
      if (maxSize != DEFAULT) {
        withMaxSize(maxSize);
      }

      final Log log = configuration.mLog;
      if (log != null) {
        withLog(log);
      }

      final Level logLevel = configuration.mLogLevel;
      if (logLevel != null) {
        withLogLevel(logLevel);
      }

      return this;
    }

    /**
     * Sets the backoff policy to apply while waiting for the channel to have room for additional
     * data.
     * <p>
     * Note that the backoff instance will be likely called from different threads, so, it's
     * responsibility of the implementing class to ensure that consistent delays are returned
     * based on the specified excess count.
     * <p>
     * This configuration option should be used on conjunction with the channel limit, or it
     * might have no effect on the invocation execution.
     *
     * @param backoff the backoff policy.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withBackoff(@Nullable final Backoff backoff) {
      mChannelBackoff = backoff;
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
     * Sets the maximum number of data that the channel can retain before they are consumed. A
     * {@link ChannelConfiguration#DEFAULT DEFAULT} value means that it is up to the specific
     * implementation to choose a default one.
     *
     * @param maxSize the maximum size.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @NotNull
    public Builder<TYPE> withMaxSize(final int maxSize) {
      if (maxSize != DEFAULT) {
        ConstantConditions.positive("channel buffer size", maxSize);
      }

      mChannelMaxSize = maxSize;
      return this;
    }

    /**
     * Sets the order in which data are collected from the channel. A null value means that it
     * is up to the specific implementation to choose a default one.
     * <p>
     * Note that this is just the initial configuration, since the channel order can be
     * dynamically changed through the dedicated methods.
     *
     * @param orderType the order type.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withOrder(@Nullable final OrderType orderType) {
      mChannelOrderType = orderType;
      return this;
    }

    /**
     * Sets the timeout for the channel instance to produce a readable output.
     * <p>
     * Note that this is just the initial configuration, since the output timeout can be
     * dynamically changed through the dedicated channel methods.
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
     * Sets the timeout for the channel instance to produce a readable output. A null value
     * means that it is up to the specific implementation to choose a default one.
     * <p>
     * Note that this is just the initial configuration, since the output timeout can be
     * dynamically changed through the dedicated channel methods.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withOutputTimeout(@Nullable final UnitDuration timeout) {
      mOutputTimeout = timeout;
      return this;
    }

    /**
     * Sets the action to be taken if the timeout elapses before an output can be read from the
     * output channel. A null value means that it is up to the specific implementation to choose
     * a default one.
     * <p>
     * Note that this is just the initial configuration, since the output timeout action can be
     * dynamically changed through the dedicated channel methods.
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

    @NotNull
    private ChannelConfiguration buildConfiguration() {
      return new ChannelConfiguration(mRunner, mOutputTimeout, mTimeoutActionType,
          mChannelOrderType, mChannelBackoff, mChannelMaxSize, mLog, mLogLevel);
    }

    private void setConfiguration(@NotNull final ChannelConfiguration configuration) {
      mRunner = configuration.mRunner;
      mOutputTimeout = configuration.mOutputTimeout;
      mTimeoutActionType = configuration.mTimeoutActionType;
      mChannelOrderType = configuration.mChannelOrderType;
      mChannelBackoff = configuration.mChannelBackoff;
      mChannelMaxSize = configuration.mChannelMaxSize;
      mLog = configuration.mLog;
      mLogLevel = configuration.mLogLevel;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<ChannelConfiguration> {

    @NotNull
    public ChannelConfiguration apply(@NotNull final ChannelConfiguration configuration) {
      return configuration;
    }
  }
}
