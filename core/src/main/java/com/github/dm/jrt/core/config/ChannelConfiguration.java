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

import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Backoff.constantDelay;
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
 * <li>The core number of input data buffered in the channel. The channel buffer can be limited in
 * order to avoid excessive memory consumption. In case the maximum number is reached when passing
 * an input, the call will block until enough data are consumed or the specified delay elapses.</li>
 * <li>The backoff policy to be applied to the calling thread when the buffered data exceed the
 * channel core limit.</li>
 * <li>The maximum number of input data buffered in the channel.When the number of data exceeds it,
 * a {@link com.github.dm.jrt.core.error.DeadlockException DeadlockException} will be thrown.</li>
 * <li>The maximum timeout while waiting for a new output to be available before performing the
 * specified action.</li>
 * <li>The action to be taken when no output becomes available before the timeout elapses.</li>
 * <li>The log instance to be used to trace the log messages.</li>
 * <li>The log level to be used to filter the log messages.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 07/03/2015.
 */
public final class ChannelConfiguration extends DeepEqualObject {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = InvocationConfiguration.DEFAULT;

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    private static final ChannelConfiguration sDefaultConfiguration =
            builder().buildConfiguration();

    private final Backoff mChannelBackoff;

    private final int mChannelLimit;

    private final int mChannelMaxSize;

    private final OrderType mChannelOrderType;

    private final Log mLog;

    private final Level mLogLevel;

    private final UnitDuration mReadTimeout;

    private final Runner mRunner;

    private final TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param runner           the runner used for asynchronous inputs.
     * @param readTimeout      the timeout for the channel to produce an output.
     * @param actionType       the action to be taken if the timeout elapses before a readable
     *                         output is available.
     * @param channelOrderType the order in which data are collected from the output channel.
     * @param channelLimit     the maximum number of buffered data before applying a delay to the
     *                         feeding thread. Must not be negative.
     * @param channelBackoff   the backoff policy to apply while waiting for an object to be passed
     *                         to the channel.
     * @param channelMaxSize   the maximum number of buffered data. Must be positive.
     * @param log              the log instance.
     * @param logLevel         the log level.
     */
    private ChannelConfiguration(@Nullable final Runner runner,
            @Nullable final UnitDuration readTimeout, @Nullable final TimeoutActionType actionType,
            @Nullable final OrderType channelOrderType, final int channelLimit,
            @Nullable final Backoff channelBackoff, final int channelMaxSize,
            @Nullable final Log log, @Nullable final Level logLevel) {

        super(asArgs(runner, readTimeout, actionType, channelOrderType, channelLimit,
                channelBackoff, channelMaxSize, log, logLevel));
        mRunner = runner;
        mReadTimeout = readTimeout;
        mTimeoutActionType = actionType;
        mChannelOrderType = channelOrderType;
        mChannelLimit = channelLimit;
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
     * Returns a channel configuration builder initialized with a channel configuration converted
     * from the specified one by applying the matching input channel options.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @NotNull
    public static Builder<ChannelConfiguration> builderFromInputChannel(
            @Nullable final InvocationConfiguration initialConfiguration) {

        if (initialConfiguration == null) {
            return builder();
        }

        final Builder<ChannelConfiguration> builder = builderFromInvocation(initialConfiguration);
        return builder.withChannelOrder(initialConfiguration.getInputOrderTypeOrElse(null))
                      .withChannelLimit(initialConfiguration.getInputLimitOrElse(
                              ChannelConfiguration.DEFAULT))
                      .withChannelBackoff(initialConfiguration.getInputBackoffOrElse(null))
                      .withChannelMaxSize(initialConfiguration.getInputMaxSizeOrElse(
                              ChannelConfiguration.DEFAULT));
    }

    /**
     * Returns a channel configuration builder initialized with a channel configuration converted
     * from the specified one by applying the matching options.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @NotNull
    public static Builder<ChannelConfiguration> builderFromInvocation(
            @Nullable final InvocationConfiguration initialConfiguration) {

        return (initialConfiguration == null) ? builder()
                : builder().withRunner(initialConfiguration.getRunnerOrElse(null))
                           .withReadTimeout(initialConfiguration.getReadTimeoutOrElse(null))
                           .withReadTimeoutAction(
                                   initialConfiguration.getReadTimeoutActionOrElse(null))
                           .withLog(initialConfiguration.getLogOrElse(null))
                           .withLogLevel(initialConfiguration.getLogLevelOrElse(null));
    }

    /**
     * Returns a channel configuration builder initialized with a channel configuration converted
     * from the specified one by applying the output channel matching options.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @NotNull
    public static Builder<ChannelConfiguration> builderFromOutputChannel(
            @Nullable final InvocationConfiguration initialConfiguration) {

        if (initialConfiguration == null) {
            return builder();
        }

        final Builder<ChannelConfiguration> builder = builderFromInvocation(initialConfiguration);
        return builder.withChannelOrder(initialConfiguration.getOutputOrderTypeOrElse(null))
                      .withChannelLimit(initialConfiguration.getOutputLimitOrElse(
                              ChannelConfiguration.DEFAULT))
                      .withChannelBackoff(initialConfiguration.getOutputBackoffOrElse(null))
                      .withChannelMaxSize(initialConfiguration.getOutputMaxSizeOrElse(
                              ChannelConfiguration.DEFAULT));
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
    public Backoff getChannelBackoffOrElse(@Nullable final Backoff valueIfNotSet) {

        final Backoff channelBackoff = mChannelBackoff;
        return (channelBackoff != null) ? channelBackoff : valueIfNotSet;
    }

    /**
     * Returns the limit of buffered data (DEFAULT by default) before starting to apply a delay to
     * the feeding thread.
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the limit.
     */
    public int getChannelLimitOrElse(final int valueIfNotSet) {

        final int limit = mChannelLimit;
        return (limit != DEFAULT) ? limit : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getChannelMaxSizeOrElse(final int valueIfNotSet) {

        final int maxSize = mChannelMaxSize;
        return (maxSize != DEFAULT) ? maxSize : valueIfNotSet;
    }

    /**
     * Returns the data order (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public OrderType getChannelOrderTypeOrElse(@Nullable final OrderType valueIfNotSet) {

        final OrderType orderType = mChannelOrderType;
        return (orderType != null) ? orderType : valueIfNotSet;
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
     * Returns the action to be taken if the timeout elapses before a readable output is available
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the action type.
     */
    public TimeoutActionType getReadTimeoutActionOrElse(
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
    public UnitDuration getReadTimeoutOrElse(@Nullable final UnitDuration valueIfNotSet) {

        final UnitDuration readTimeout = mReadTimeout;
        return (readTimeout != null) ? readTimeout : valueIfNotSet;
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
     * Converts this configuration into an invocation one by applying the matching options to the
     * invocation input channel.
     *
     * @return the invocation configuration.
     */
    @NotNull
    public InvocationConfiguration toInputChannelConfiguration() {

        return toInvocationConfiguration().builderFrom()
                                          .withInputOrder(getChannelOrderTypeOrElse(null))
                                          .withInputLimit(getChannelLimitOrElse(
                                                  InvocationConfiguration.DEFAULT))
                                          .withInputBackoff(getChannelBackoffOrElse(null))
                                          .withInputMaxSize(getChannelMaxSizeOrElse(
                                                  InvocationConfiguration.DEFAULT))
                                          .apply();
    }

    /**
     * Converts this configuration into an invocation one by mapping the matching options.
     *
     * @return the invocation configuration.
     */
    @NotNull
    public InvocationConfiguration toInvocationConfiguration() {

        return InvocationConfiguration.builder()
                                      .withRunner(getRunnerOrElse(null))
                                      .withReadTimeout(getReadTimeoutOrElse(null))
                                      .withReadTimeoutAction(getReadTimeoutActionOrElse(null))
                                      .withLog(getLogOrElse(null))
                                      .withLogLevel(getLogLevelOrElse(null))
                                      .apply();
    }

    /**
     * Converts this configuration into an invocation one by applying the matching options to the
     * invocation output channel.
     *
     * @return the invocation configuration.
     */
    @NotNull
    public InvocationConfiguration toOutputChannelConfiguration() {

        return toInvocationConfiguration().builderFrom()
                                          .withOutputOrder(getChannelOrderTypeOrElse(null))
                                          .withOutputLimit(getChannelLimitOrElse(
                                                  InvocationConfiguration.DEFAULT))
                                          .withOutputBackoff(getChannelBackoffOrElse(null))
                                          .withOutputMaxSize(getChannelMaxSizeOrElse(
                                                  InvocationConfiguration.DEFAULT))
                                          .apply();
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

        private int mChannelLimit;

        private int mChannelMaxSize;

        private OrderType mChannelOrderType;

        private Log mLog;

        private Level mLogLevel;

        private UnitDuration mReadTimeout;

        private Runner mRunner;

        private TimeoutActionType mTimeoutActionType;

        /**
         * Constructor.
         *
         * @param configurable the configurable instance.
         */
        public Builder(@NotNull final Configurable<? extends TYPE> configurable) {

            mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
            mChannelLimit = DEFAULT;
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
        public TYPE apply() {

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

            final UnitDuration readTimeout = configuration.mReadTimeout;
            if (readTimeout != null) {
                withReadTimeout(readTimeout);
            }

            final TimeoutActionType timeoutActionType = configuration.mTimeoutActionType;
            if (timeoutActionType != null) {
                withReadTimeoutAction(timeoutActionType);
            }

            final OrderType orderType = configuration.mChannelOrderType;
            if (orderType != null) {
                withChannelOrder(orderType);
            }

            final int limit = configuration.mChannelLimit;
            if (limit != DEFAULT) {
                withChannelLimit(limit);
            }

            final Backoff channelBackoff = configuration.mChannelBackoff;
            if (channelBackoff != null) {
                withChannelBackoff(channelBackoff);
            }

            final int maxSize = configuration.mChannelMaxSize;
            if (maxSize != DEFAULT) {
                withChannelMaxSize(maxSize);
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
         * Sets the constant delay to apply while waiting for the channel to have room for
         * additional data.
         * <p>
         * This configuration option should be used on conjunction with the channel limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay    the delay.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified delay is negative.
         */
        @NotNull
        public Builder<TYPE> withChannelBackoff(final long delay,
                @NotNull final TimeUnit timeUnit) {

            return withChannelBackoff(constantDelay(delay, timeUnit));
        }

        /**
         * Sets the backoff policy to apply while waiting for the channel to have room for
         * additional data.
         * <p>
         * This configuration option should be used on conjunction with the channel limit, or it
         * might have no effect on the invocation execution.
         *
         * @param backoff the backoff policy.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withChannelBackoff(@Nullable final Backoff backoff) {

            mChannelBackoff = backoff;
            return this;
        }

        /**
         * Sets the constant delay to apply while waiting for the channel to have room for
         * additional data.
         * <p>
         * This configuration option should be used on conjunction with the channel limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay the delay.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withChannelBackoff(@Nullable final UnitDuration delay) {

            return withChannelBackoff((delay != null) ? constantDelay(delay) : null);
        }

        /**
         * Sets the limit of data that the channel can retain before starting to slow down the
         * feeding thread. A {@link ChannelConfiguration#DEFAULT DEFAULT} value means that it is
         * up to the specific implementation to choose a default one.
         * <p>
         * This configuration option is useful when the data coming from the invocation execution
         * are meant to be explicitly read through this channel. The execution will slow down until
         * enough data are consumed. Note, however, that binding the channel to an output consumer
         * will make the option ineffective.
         *
         * @param limit the limit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the limit is negative.
         */
        @NotNull
        public Builder<TYPE> withChannelLimit(final int limit) {

            if (limit != DEFAULT) {
                ConstantConditions.notNegative("channel limit", limit);
            }

            mChannelLimit = limit;
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
        public Builder<TYPE> withChannelMaxSize(final int maxSize) {

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
        public Builder<TYPE> withChannelOrder(@Nullable final OrderType orderType) {

            mChannelOrderType = orderType;
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
        public Builder<TYPE> withReadTimeout(final long timeout, @NotNull final TimeUnit timeUnit) {

            return withReadTimeout(fromUnit(timeout, timeUnit));
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
        public Builder<TYPE> withReadTimeout(@Nullable final UnitDuration timeout) {

            mReadTimeout = timeout;
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
        public Builder<TYPE> withReadTimeoutAction(@Nullable final TimeoutActionType actionType) {

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

            return new ChannelConfiguration(mRunner, mReadTimeout, mTimeoutActionType,
                    mChannelOrderType, mChannelLimit, mChannelBackoff, mChannelMaxSize, mLog,
                    mLogLevel);
        }

        private void setConfiguration(@NotNull final ChannelConfiguration configuration) {

            mRunner = configuration.mRunner;
            mReadTimeout = configuration.mReadTimeout;
            mTimeoutActionType = configuration.mTimeoutActionType;
            mChannelOrderType = configuration.mChannelOrderType;
            mChannelLimit = configuration.mChannelLimit;
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
