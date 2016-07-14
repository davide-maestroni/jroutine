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

import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Backoffs.constantDelay;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;

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
 * <li>The core number of invocation instances to be retained in order to be re-used when needed.
 * When an invocation completes without being discarded, the instance is retained for future
 * executions.</li>
 * <li>The maximum number of invocation instances running at the same time. When the limit is
 * exceeded, the new invocation execution is delayed until one instance becomes available.</li>
 * <li>The order in which data are dispatched through the invocation channel. The order of input
 * data is not guaranteed. Nevertheless, it is possible to force data to be delivered in the same
 * order as they are passed to the channels, at the cost of a slightly increase in memory usage and
 * computation.</li>
 * <li>The core number of input data buffered in the invocation channel. The channel buffer can be
 * limited in order to avoid excessive memory consumption. In case the maximum number is exceeded
 * when passing an input, the call will block until enough data are consumed or the specified delay
 * elapses.</li>
 * <li>The backoff policy to be applied to the calling thread when the buffered input data exceed
 * the invocation channel core limit.</li>
 * <li>The maximum number of input data buffered in the invocation channel. When the number of data
 * exceeds it, a {@link com.github.dm.jrt.core.error.DeadlockException DeadlockException} will be
 * thrown.</li>
 * <li>The order in which data are dispatched through the result channel. The order of input data is
 * not guaranteed. Nevertheless, it is possible to force data to be delivered in the same order as
 * they are passed to the channels, at the cost of a slightly increase in memory usage and
 * computation.</li>
 * <li>The core number of output data buffered in the result channel. The channel buffer can be
 * limited in order to avoid excessive memory consumption. In case the maximum number is exceeded
 * when passing an output, the call will block until enough data are consumed or the specified delay
 * elapses.</li>
 * <li>The backoff policy to be applied to the calling thread when the buffered output data exceed
 * the result channel core limit.</li>
 * <li>The maximum number of output data buffered in the result channel. When the number of data
 * exceeds it, a {@link com.github.dm.jrt.core.error.DeadlockException DeadlockException} will be
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
public final class InvocationConfiguration extends DeepEqualObject {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = Integer.MIN_VALUE;

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    private static final InvocationConfiguration sDefaultConfiguration =
            builder().buildConfiguration();

    private final int mCoreInstances;

    private final Backoff mInputBackoff;

    private final int mInputLimit;

    private final int mInputMaxSize;

    private final OrderType mInputOrderType;

    private final Log mLog;

    private final Level mLogLevel;

    private final int mMaxInstances;

    private final Backoff mOutputBackoff;

    private final int mOutputLimit;

    private final int mOutputMaxSize;

    private final OrderType mOutputOrderType;

    private final UnitDuration mOutputTimeout;

    private final int mPriority;

    private final Runner mRunner;

    private final TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param runner          the runner used for asynchronous invocations.
     * @param priority        the invocation priority.
     * @param maxInstances    the maximum number of parallel running invocations. Must be positive.
     * @param coreInstances   the maximum number of retained invocation instances. Must not be
     *                        negative.
     * @param outputTimeout   the timeout for an invocation instance to produce a result.
     * @param actionType      the action to be taken if the timeout elapses before a readable
     *                        result is available.
     * @param inputOrderType  the order in which input data are collected from the input channel.
     * @param inputLimit      the maximum number of buffered input data before applying a delay to
     *                        the feeding thread. Must not be negative.
     * @param inputBackoff    the backoff policy to apply while waiting for an input to be passed to
     *                        the input channel.
     * @param inputMaxSize    the maximum number of buffered input data. Must be positive.
     * @param outputOrderType the order in which output data are collected from the result channel.
     * @param outputLimit     the maximum number of buffered output data before applying a delay to
     *                        the feeding thread. Must not be negative.
     * @param outputBackoff   the backoff policy to apply while waiting for an output to be passed
     *                        to the result channel.
     * @param outputMaxSize   the maximum number of buffered output data. Must be positive.
     * @param log             the log instance.
     * @param logLevel        the log level.
     */
    private InvocationConfiguration(@Nullable final Runner runner, final int priority,
            final int maxInstances, final int coreInstances,
            @Nullable final UnitDuration outputTimeout,
            @Nullable final TimeoutActionType actionType, @Nullable final OrderType inputOrderType,
            final int inputLimit, @Nullable final Backoff inputBackoff, final int inputMaxSize,
            @Nullable final OrderType outputOrderType, final int outputLimit,
            @Nullable final Backoff outputBackoff, final int outputMaxSize, @Nullable final Log log,
            @Nullable final Level logLevel) {
        super(asArgs(runner, priority, maxInstances, coreInstances, outputTimeout, actionType,
                inputOrderType, inputLimit, inputBackoff, inputMaxSize, outputOrderType,
                outputLimit, outputBackoff, outputMaxSize, log, logLevel));
        mRunner = runner;
        mPriority = priority;
        mMaxInstances = maxInstances;
        mCoreInstances = coreInstances;
        mOutputTimeout = outputTimeout;
        mTimeoutActionType = actionType;
        mInputOrderType = inputOrderType;
        mInputLimit = inputLimit;
        mInputBackoff = inputBackoff;
        mInputMaxSize = inputMaxSize;
        mOutputOrderType = outputOrderType;
        mOutputLimit = outputLimit;
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
                   .withInputLimit(initialConfiguration.getLimitOrElse(DEFAULT))
                   .withInputMaxSize(initialConfiguration.getMaxSizeOrElse(DEFAULT))
                   .withInputOrder(initialConfiguration.getOrderTypeOrElse(null))
                   .withLog(initialConfiguration.getLogOrElse(null))
                   .withLogLevel(initialConfiguration.getLogLevelOrElse(null))
                   .withOutputTimeout(initialConfiguration.getOutputTimeoutOrElse(null))
                   .withOutputTimeoutAction(
                           initialConfiguration.getOutputTimeoutActionOrElse(null));
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
                   .withOutputLimit(initialConfiguration.getLimitOrElse(DEFAULT))
                   .withOutputMaxSize(initialConfiguration.getMaxSizeOrElse(DEFAULT))
                   .withOutputOrder(initialConfiguration.getOrderTypeOrElse(null))
                   .withLog(initialConfiguration.getLogOrElse(null))
                   .withLogLevel(initialConfiguration.getLogLevelOrElse(null))
                   .withOutputTimeout(initialConfiguration.getOutputTimeoutOrElse(null))
                   .withOutputTimeoutAction(
                           initialConfiguration.getOutputTimeoutActionOrElse(null));
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
    public int getCoreInstancesOrElse(final int valueIfNotSet) {
        final int coreInstances = mCoreInstances;
        return (coreInstances != DEFAULT) ? coreInstances : valueIfNotSet;
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
     * Returns the limit of buffered input data (DEFAULT by default) before starting to apply a
     * delay to the feeding thread.
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the limit.
     */
    public int getInputLimitOrElse(final int valueIfNotSet) {
        final int inputLimit = mInputLimit;
        return (inputLimit != DEFAULT) ? inputLimit : valueIfNotSet;
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
     * Returns the maximum number of parallel running invocation instances (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum number.
     */
    public int getMaxInstancesOrElse(final int valueIfNotSet) {
        final int maxInstances = mMaxInstances;
        return (maxInstances != DEFAULT) ? maxInstances : valueIfNotSet;
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
     * Returns the limit of buffered output data (DEFAULT by default) before starting to apply a
     * delay to the feeding thread.
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the limit.
     */
    public int getOutputLimitOrElse(final int valueIfNotSet) {
        final int outputLimit = mOutputLimit;
        return (outputLimit != DEFAULT) ? outputLimit : valueIfNotSet;
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
    public UnitDuration getOutputTimeoutOrElse(@Nullable final UnitDuration valueIfNotSet) {
        final UnitDuration outputTimeout = mOutputTimeout;
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
     * Returns a channel configuration builder initialized with output related options converted
     * from this configuration ones.
     *
     * @return the builder.
     */
    @NotNull
    public ChannelConfiguration.Builder<ChannelConfiguration> inputConfigurationBuilder() {
        return ChannelConfiguration.builder()
                                   .withRunner(getRunnerOrElse(null))
                                   .withBackoff(getInputBackoffOrElse(null))
                                   .withLimit(getInputLimitOrElse(ChannelConfiguration.DEFAULT))
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
                                   .withLimit(getOutputLimitOrElse(ChannelConfiguration.DEFAULT))
                                   .withMaxSize(
                                           getOutputMaxSizeOrElse(ChannelConfiguration.DEFAULT))
                                   .withOrder(getOutputOrderTypeOrElse(null))
                                   .withLog(getLogOrElse(null))
                                   .withLogLevel(getLogLevelOrElse(null))
                                   .withOutputTimeout(getOutputTimeoutOrElse(null))
                                   .withOutputTimeoutAction(getOutputTimeoutActionOrElse(null));
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

        private int mCoreInstances;

        private Backoff mInputBackoff;

        private int mInputLimit;

        private int mInputMaxSize;

        private OrderType mInputOrderType;

        private Log mLog;

        private Level mLogLevel;

        private int mMaxInstances;

        private Backoff mOutputBackoff;

        private int mOutputLimit;

        private int mOutputMaxSize;

        private OrderType mOutputOrderType;

        private UnitDuration mOutputTimeout;

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
            mMaxInstances = DEFAULT;
            mCoreInstances = DEFAULT;
            mInputLimit = DEFAULT;
            mInputMaxSize = DEFAULT;
            mOutputLimit = DEFAULT;
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
        public TYPE configured() {
            return mConfigurable.apply(buildConfiguration());
        }

        /**
         * Applies the specified configuration to this builder. A null value means that all the
         * configuration options will be reset to their default, otherwise only the non-default
         * options will be applied.
         *
         * @param configuration the invocation configuration.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> with(@Nullable final InvocationConfiguration configuration) {
            if (configuration == null) {
                setConfiguration(defaultConfiguration());
                return this;
            }

            applyBaseConfiguration(configuration);
            applyChannelConfiguration(configuration);
            applyLogConfiguration(configuration);
            return this;
        }

        /**
         * Sets the number of invocation instances, which represents the core pool of reusable
         * invocations. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is
         * up to the specific implementation to choose a default one.
         *
         * @param coreInstances the core number of instances.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is negative.
         */
        @NotNull
        public Builder<TYPE> withCoreInstances(final int coreInstances) {
            if (coreInstances != DEFAULT) {
                ConstantConditions.notNegative("maximum number of retained instances",
                        coreInstances);
            }

            mCoreInstances = coreInstances;
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
         * Sets the constant delay to apply while waiting for the invocation channel to have room
         * for additional data.
         * <p>
         * This configuration option should be used on conjunction with the input limit, or it might
         * have no effect on the invocation execution.
         *
         * @param delay    the delay.
         * @param timeUnit the delay time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified delay is negative.
         * @see #withInputMaxSize(int)
         */
        @NotNull
        public Builder<TYPE> withInputBackoff(final long delay, @NotNull final TimeUnit timeUnit) {
            return withInputBackoff(constantDelay(delay, timeUnit));
        }

        /**
         * Sets the constant delay to apply while waiting for the invocation channel to have room
         * for additional data.
         * <p>
         * This configuration option should be used on conjunction with the input limit, or it might
         * have no effect on the invocation execution.
         *
         * @param delay the delay.
         * @return this builder.
         * @see #withInputMaxSize(int)
         */
        @NotNull
        public Builder<TYPE> withInputBackoff(@Nullable final UnitDuration delay) {
            return withInputBackoff((delay != null) ? constantDelay(delay) : null);
        }

        /**
         * Sets the limit of data that the invocation channel can retain before starting to slow
         * down the feeding thread. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means
         * that it is up to the specific implementation to choose a default one.
         * <p>
         * This configuration option is used to slow down the process feeding the routine invocation
         * when its execution time increases. Note, however, that it is not allowed to block the
         * invocation execution thread, so make sure that the feeding routine and this one does not
         * share the same runner.
         *
         * @param inputLimit the limit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the limit is negative.
         */
        @NotNull
        public Builder<TYPE> withInputLimit(final int inputLimit) {
            if (inputLimit != DEFAULT) {
                ConstantConditions.notNegative("input limit", inputLimit);
            }

            mInputLimit = inputLimit;
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
         * @param maxInstances the max number of instances.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @NotNull
        public Builder<TYPE> withMaxInstances(final int maxInstances) {
            if (maxInstances != DEFAULT) {
                ConstantConditions.positive("maximum number of concurrently running instances",
                        maxInstances);
            }

            mMaxInstances = maxInstances;
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
         * Sets the constant delay to apply while waiting for the result channel to have room for
         * additional data.
         * <p>
         * This configuration option should be used on conjunction with the output limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay    the delay.
         * @param timeUnit the delay time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified delay is negative.
         * @see #withOutputMaxSize(int)
         */
        @NotNull
        public Builder<TYPE> withOutputBackoff(final long delay, @NotNull final TimeUnit timeUnit) {
            return withOutputBackoff(constantDelay(delay, timeUnit));
        }

        /**
         * Sets the constant delay to apply while waiting for the result channel to have room for
         * additional data.
         * <p>
         * This configuration option should be used on conjunction with the output limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay the delay.
         * @return this builder.
         * @see #withOutputMaxSize(int)
         */
        @NotNull
        public Builder<TYPE> withOutputBackoff(@Nullable final UnitDuration delay) {
            return withOutputBackoff((delay != null) ? constantDelay(delay) : null);
        }

        /**
         * Sets the limit of data that the result channel can retain before starting to slow down
         * the feeding thread. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it
         * is up to the specific implementation to choose a default one.
         * <p>
         * This configuration option is useful when the results coming from the invocation execution
         * are meant to be explicitly read through its output channel. The execution will slow down
         * until enough data are consumed. Note, however, that binding the channel to an output
         * consumer might make the option ineffective.
         *
         * @param outputLimit the limit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the limit is negative.
         */
        @NotNull
        public Builder<TYPE> withOutputLimit(final int outputLimit) {
            if (outputLimit != DEFAULT) {
                ConstantConditions.notNegative("output limit", outputLimit);
            }

            mOutputLimit = outputLimit;
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
        public Builder<TYPE> withOutputTimeout(final long timeout,
                @NotNull final TimeUnit timeUnit) {
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
        public Builder<TYPE> withOutputTimeout(@Nullable final UnitDuration timeout) {
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

            final int maxInvocations = configuration.mMaxInstances;
            if (maxInvocations != DEFAULT) {
                withMaxInstances(maxInvocations);
            }

            final int coreInvocations = configuration.mCoreInstances;
            if (coreInvocations != DEFAULT) {
                withCoreInstances(coreInvocations);
            }

            final UnitDuration outputTimeout = configuration.mOutputTimeout;
            if (outputTimeout != null) {
                withOutputTimeout(outputTimeout);
            }

            final TimeoutActionType timeoutActionType = configuration.mTimeoutActionType;
            if (timeoutActionType != null) {
                withOutputTimeoutAction(timeoutActionType);
            }
        }

        private void applyChannelConfiguration(
                @NotNull final InvocationConfiguration configuration) {
            final OrderType inputOrderType = configuration.mInputOrderType;
            if (inputOrderType != null) {
                withInputOrder(inputOrderType);
            }

            final int inputLimit = configuration.mInputLimit;
            if (inputLimit != DEFAULT) {
                withInputLimit(inputLimit);
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

            final int outputLimit = configuration.mOutputLimit;
            if (outputLimit != DEFAULT) {
                withOutputLimit(outputLimit);
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
            return new InvocationConfiguration(mRunner, mPriority, mMaxInstances, mCoreInstances,
                    mOutputTimeout, mTimeoutActionType, mInputOrderType, mInputLimit, mInputBackoff,
                    mInputMaxSize, mOutputOrderType, mOutputLimit, mOutputBackoff, mOutputMaxSize,
                    mLog, mLogLevel);
        }

        private void setConfiguration(@NotNull final InvocationConfiguration configuration) {
            mRunner = configuration.mRunner;
            mPriority = configuration.mPriority;
            mMaxInstances = configuration.mMaxInstances;
            mCoreInstances = configuration.mCoreInstances;
            mOutputTimeout = configuration.mOutputTimeout;
            mTimeoutActionType = configuration.mTimeoutActionType;
            mInputOrderType = configuration.mInputOrderType;
            mInputLimit = configuration.mInputLimit;
            mInputBackoff = configuration.mInputBackoff;
            mInputMaxSize = configuration.mInputMaxSize;
            mOutputOrderType = configuration.mOutputOrderType;
            mOutputLimit = configuration.mOutputLimit;
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
