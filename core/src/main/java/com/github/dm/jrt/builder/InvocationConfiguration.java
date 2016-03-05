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

package com.github.dm.jrt.builder;

import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.util.TimeDuration.fromUnit;

/**
 * Class storing the invocation configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration.
 * <p/>
 * The configuration has an asynchronous runner associated.<br/>
 * The default runner is shared among all the routines.
 * <p/>
 * A specific priority can be set. Every invocation will age each time an higher priority one takes
 * the precedence, so that older invocations slowly increases their priority. Such mechanism has
 * been implemented to avoid starvation of low priority invocations. Hence, when assigning
 * priority values, it is important to keep in mind that the difference between two priorities
 * corresponds to the maximum age the lower priority invocation will have, before getting precedence
 * over the higher priority one.
 * <p/>
 * Additionally, a recycling mechanism is provided so that, when an invocation successfully
 * completes, the instance is retained for future executions. Moreover, the maximum running
 * invocation instances at one time can be limited by calling the specific builder method. When the
 * limit is reached and an additional instance is required, the call is delayed until one becomes
 * available.
 * <p/>
 * The maximum time of execution can also be set so that, if no result is produced before the
 * timeout elapses, an action will be taken choosing between throwing an
 * {@link com.github.dm.jrt.core.channel.ExecutionTimeoutException ExecutionTimeoutException},
 * aborting the invocation, or continuing with the execution.
 * <p/>
 * Finally, the number of input and output data buffered in the corresponding channel can be
 * limited in order to avoid excessive memory consumption. In case the limit is reached when passing
 * an input or output, the call blocks until enough data are consumed or the specified maximum delay
 * elapses. A maximum size can additionally be set so that, when the number of buffered data
 * exceeds it, a {@link com.github.dm.jrt.common.DeadlockException DeadlockException} will be
 * thrown.<br/>
 * By default the delay is set to 0.<br/>
 * The order of input and output data is not guaranteed. Nevertheless, it is possible to force data
 * to be delivered in the same order as they are passed to the channels, at the cost of a slightly
 * increase in memory usage and computation time.
 * <p/>
 * Created by davide-maestroni on 11/15/2014.
 */
public final class InvocationConfiguration {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = Integer.MIN_VALUE;

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    /**
     * Empty configuration constant.<br/>The configuration has all the options set to their default.
     */
    public static final InvocationConfiguration DEFAULT_CONFIGURATION =
            builder().buildConfiguration();

    private final int mCoreInstances;

    private final int mInputLimit;

    private final TimeDuration mInputMaxDelay;

    private final int mInputMaxSize;

    private final OrderType mInputOrderType;

    private final Log mLog;

    private final Level mLogLevel;

    private final int mMaxInstances;

    private final int mOutputLimit;

    private final TimeDuration mOutputMaxDelay;

    private final int mOutputMaxSize;

    private final OrderType mOutputOrderType;

    private final int mPriority;

    private final TimeDuration mReadTimeout;

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
     * @param readTimeout     the timeout for an invocation instance to produce a result.
     * @param actionType      the action to be taken if the timeout elapses before a readable
     *                        result is available.
     * @param inputOrderType  the order in which input data are collected from the input channel.
     * @param inputLimit      the maximum number of buffered input data before applying a delay to
     *                        the feeding thread. Must not be negative.
     * @param inputMaxDelay   the maximum delay to apply while waiting for an input to be passed to
     *                        the input channel.
     * @param inputMaxSize    the maximum number of buffered input data. Must be positive.
     * @param outputOrderType the order in which output data are collected from the result channel.
     * @param outputLimit     the maximum number of buffered output data before applying a delay to
     *                        the feeding thread. Must not be negative.
     * @param outputMaxDelay  the maximum delay to apply while waiting for an output to be passed to
     *                        the result channel.
     * @param outputMaxSize   the maximum number of buffered output data. Must be positive.
     * @param log             the log instance.
     * @param logLevel        the log level.
     */
    private InvocationConfiguration(@Nullable final Runner runner, final int priority,
            final int maxInstances, final int coreInstances,
            @Nullable final TimeDuration readTimeout, @Nullable final TimeoutActionType actionType,
            @Nullable final OrderType inputOrderType, final int inputLimit,
            @Nullable final TimeDuration inputMaxDelay, final int inputMaxSize,
            @Nullable final OrderType outputOrderType, final int outputLimit,
            @Nullable final TimeDuration outputMaxDelay, final int outputMaxSize,
            @Nullable final Log log, @Nullable final Level logLevel) {

        mRunner = runner;
        mPriority = priority;
        mMaxInstances = maxInstances;
        mCoreInstances = coreInstances;
        mReadTimeout = readTimeout;
        mTimeoutActionType = actionType;
        mInputOrderType = inputOrderType;
        mInputLimit = inputLimit;
        mInputMaxDelay = inputMaxDelay;
        mInputMaxSize = inputMaxSize;
        mOutputOrderType = outputOrderType;
        mOutputLimit = outputLimit;
        mOutputMaxDelay = outputMaxDelay;
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
    public int getCoreInstancesOr(final int valueIfNotSet) {

        final int coreInstances = mCoreInstances;
        return (coreInstances != DEFAULT) ? coreInstances : valueIfNotSet;
    }

    /**
     * Returns the limit of buffered input data (DEFAULT by default) before starting to apply a
     * delay to the feeding thread.
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the limit.
     */
    public int getInputLimitOr(final int valueIfNotSet) {

        final int inputLimit = mInputLimit;
        return (inputLimit != DEFAULT) ? inputLimit : valueIfNotSet;
    }

    /**
     * Returns the maximum delay to apply while waiting for an input to be processed (null by
     * default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the delay.
     */
    public TimeDuration getInputMaxDelayOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration inputMaxDelay = mInputMaxDelay;
        return (inputMaxDelay != null) ? inputMaxDelay : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered input data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getInputMaxSizeOr(final int valueIfNotSet) {

        final int inputMaxSize = mInputMaxSize;
        return (inputMaxSize != DEFAULT) ? inputMaxSize : valueIfNotSet;
    }

    /**
     * Returns the input data order (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public OrderType getInputOrderTypeOr(@Nullable final OrderType valueIfNotSet) {

        final OrderType inputOrderType = mInputOrderType;
        return (inputOrderType != null) ? inputOrderType : valueIfNotSet;
    }

    /**
     * Returns the log level (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the log level.
     */
    public Level getLogLevelOr(@Nullable final Level valueIfNotSet) {

        final Level logLevel = mLogLevel;
        return (logLevel != null) ? logLevel : valueIfNotSet;
    }

    /**
     * Returns the log instance (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the log instance.
     */
    public Log getLogOr(@Nullable final Log valueIfNotSet) {

        final Log log = mLog;
        return (log != null) ? log : valueIfNotSet;
    }

    /**
     * Returns the maximum number of parallel running invocation instances (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum number.
     */
    public int getMaxInstancesOr(final int valueIfNotSet) {

        final int maxInstances = mMaxInstances;
        return (maxInstances != DEFAULT) ? maxInstances : valueIfNotSet;
    }

    /**
     * Returns the limit of buffered output data (DEFAULT by default) before starting to apply a
     * delay to the feeding thread.
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the limit.
     */
    public int getOutputLimitOr(final int valueIfNotSet) {

        final int outputLimit = mOutputLimit;
        return (outputLimit != DEFAULT) ? outputLimit : valueIfNotSet;
    }

    /**
     * Returns the maximum delay to apply while waiting for an output to be read from the output
     * channel (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the delay.
     */
    public TimeDuration getOutputMaxDelayOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration outputMaxDelay = mOutputMaxDelay;
        return (outputMaxDelay != null) ? outputMaxDelay : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered output data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getOutputMaxSizeOr(final int valueIfNotSet) {

        final int outputMaxSize = mOutputMaxSize;
        return (outputMaxSize != DEFAULT) ? outputMaxSize : valueIfNotSet;
    }

    /**
     * Returns the output data order (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public OrderType getOutputOrderTypeOr(@Nullable final OrderType valueIfNotSet) {

        final OrderType outputOrderType = mOutputOrderType;
        return (outputOrderType != null) ? outputOrderType : valueIfNotSet;
    }

    /**
     * Returns the invocation priority (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the priority.
     */
    public int getPriorityOr(final int valueIfNotSet) {

        final int priority = mPriority;
        return (priority != DEFAULT) ? priority : valueIfNotSet;
    }

    /**
     * Returns the action to be taken if the timeout elapses before a readable result is available
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the action type.
     */
    public TimeoutActionType getReadTimeoutActionOr(
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
    public TimeDuration getReadTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration readTimeout = mReadTimeout;
        return (readTimeout != null) ? readTimeout : valueIfNotSet;
    }

    /**
     * Returns the runner used for asynchronous invocations (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the runner instance.
     */
    public Runner getRunnerOr(@Nullable final Runner valueIfNotSet) {

        final Runner runner = mRunner;
        return (runner != null) ? runner : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        int result = mCoreInstances;
        result = 31 * result + mInputLimit;
        result = 31 * result + (mInputMaxDelay != null ? mInputMaxDelay.hashCode() : 0);
        result = 31 * result + mInputMaxSize;
        result = 31 * result + (mInputOrderType != null ? mInputOrderType.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
        result = 31 * result + mMaxInstances;
        result = 31 * result + mOutputLimit;
        result = 31 * result + (mOutputMaxDelay != null ? mOutputMaxDelay.hashCode() : 0);
        result = 31 * result + mOutputMaxSize;
        result = 31 * result + (mOutputOrderType != null ? mOutputOrderType.hashCode() : 0);
        result = 31 * result + mPriority;
        result = 31 * result + (mReadTimeout != null ? mReadTimeout.hashCode() : 0);
        result = 31 * result + (mRunner != null ? mRunner.hashCode() : 0);
        result = 31 * result + (mTimeoutActionType != null ? mTimeoutActionType.hashCode() : 0);
        return result;
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

        final InvocationConfiguration that = (InvocationConfiguration) o;
        if (mCoreInstances != that.mCoreInstances) {
            return false;
        }

        if (mInputLimit != that.mInputLimit) {
            return false;
        }

        if (mInputMaxSize != that.mInputMaxSize) {
            return false;
        }

        if (mMaxInstances != that.mMaxInstances) {
            return false;
        }

        if (mOutputLimit != that.mOutputLimit) {
            return false;
        }

        if (mOutputMaxSize != that.mOutputMaxSize) {
            return false;
        }

        if (mPriority != that.mPriority) {
            return false;
        }

        if (mInputMaxDelay != null ? !mInputMaxDelay.equals(that.mInputMaxDelay)
                : that.mInputMaxDelay != null) {
            return false;
        }

        if (mInputOrderType != that.mInputOrderType) {
            return false;
        }

        if (mLog != null ? !mLog.equals(that.mLog) : that.mLog != null) {
            return false;
        }

        if (mLogLevel != that.mLogLevel) {
            return false;
        }

        if (mOutputMaxDelay != null ? !mOutputMaxDelay.equals(that.mOutputMaxDelay)
                : that.mOutputMaxDelay != null) {
            return false;
        }

        if (mOutputOrderType != that.mOutputOrderType) {
            return false;
        }

        if (mReadTimeout != null ? !mReadTimeout.equals(that.mReadTimeout)
                : that.mReadTimeout != null) {
            return false;
        }

        if (mRunner != null ? !mRunner.equals(that.mRunner) : that.mRunner != null) {
            return false;
        }

        return mTimeoutActionType == that.mTimeoutActionType;
    }

    @Override
    public String toString() {

        // AUTO-GENERATED CODE
        return "InvocationConfiguration{" +
                "mCoreInstances=" + mCoreInstances +
                ", mInputLimit=" + mInputLimit +
                ", mInputMaxDelay=" + mInputMaxDelay +
                ", mInputMaxSize=" + mInputMaxSize +
                ", mInputOrderType=" + mInputOrderType +
                ", mLog=" + mLog +
                ", mLogLevel=" + mLogLevel +
                ", mMaxInstances=" + mMaxInstances +
                ", mOutputLimit=" + mOutputLimit +
                ", mOutputMaxDelay=" + mOutputMaxDelay +
                ", mOutputMaxSize=" + mOutputMaxSize +
                ", mOutputOrderType=" + mOutputOrderType +
                ", mPriority=" + mPriority +
                ", mReadTimeout=" + mReadTimeout +
                ", mRunner=" + mRunner +
                ", mTimeoutActionType=" + mTimeoutActionType +
                '}';
    }

    /**
     * Creates a new logger.
     *
     * @param context the context.
     * @return the new logger.
     */
    @NotNull
    public Logger newLogger(@NotNull final Object context) {

        return Logger.newLogger(getLogOr(null), getLogLevelOr(null), context);
    }

    /**
     * Enumeration defining how data are ordered inside a channel.
     */
    public enum OrderType {

        /**
         * Order by call.<br/>
         * Data are passed to the invocation or the output consumer in the same order as they are
         * passed to the channel, independently from the specific delay.
         */
        BY_CALL,
        /**
         * Order by delay.<br/>
         * Data are passed to the invocation or the output consumer in the same order as they are
         * passed to the channel based on their delay.
         */
        BY_DELAY
    }

    /**
     * Enumeration indicating the type of action to be taken on output channel timeout.
     */
    public enum TimeoutActionType {

        /**
         * Throw.<br/>
         * If no result is available after the specified timeout, the called method will throw an
         * {@link com.github.dm.jrt.core.channel.ExecutionTimeoutException
         * ExecutionTimeoutException}.
         */
        THROW,
        /**
         * Break execution.<br/>
         * If no result is available after the specified timeout, the called method will stop its
         * execution and exit immediately.
         */
        EXIT,
        /**
         * Abort invocation.<br/>
         * If no result is available after the specified timeout, the invocation will be aborted and
         * the method will immediately exit.
         */
        ABORT
    }

    /**
     * Interface exposing constants which can be used as a common set of priorities.<br/>
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
        TYPE setConfiguration(@NotNull InvocationConfiguration configuration);
    }

    /**
     * Interface exposing constants which can be used as a set of priorities ignoring the aging of
     * executions.<br/>
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

        private int mInputLimit;

        private TimeDuration mInputMaxDelay;

        private int mInputMaxSize;

        private OrderType mInputOrderType;

        private Log mLog;

        private Level mLogLevel;

        private int mMaxInstances;

        private int mOutputLimit;

        private TimeDuration mOutputMaxDelay;

        private int mOutputMaxSize;

        private OrderType mOutputOrderType;

        private int mPriority;

        private TimeDuration mReadTimeout;

        private Runner mRunner;

        private TimeoutActionType mTimeoutActionType;

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
        @SuppressWarnings("ConstantConditions")
        public Builder(@NotNull final Configurable<? extends TYPE> configurable,
                @NotNull final InvocationConfiguration initialConfiguration) {

            if (configurable == null) {
                throw new NullPointerException("the configurable instance must no be null");
            }

            mConfigurable = configurable;
            setConfiguration(initialConfiguration);
        }

        /**
         * Sets the configuration and returns the configured object.
         *
         * @return the configured object.
         */
        @NotNull
        public TYPE getConfigured() {

            return mConfigurable.setConfiguration(buildConfiguration());
        }

        /**
         * Applies the specified configuration to this builder. A null value means that all the
         * configuration options will be reset to their default, otherwise only the set options will
         * be applied.
         *
         * @param configuration the invocation configuration.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> with(@Nullable final InvocationConfiguration configuration) {

            if (configuration == null) {
                setConfiguration(DEFAULT_CONFIGURATION);
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

            if ((coreInstances != DEFAULT) && (coreInstances < 0)) {
                throw new IllegalArgumentException(
                        "the maximum number of retained instances cannot be negative: "
                                + coreInstances);
            }

            mCoreInstances = coreInstances;
            return this;
        }

        /**
         * Sets the limit of data that the input channel can retain before starting to slow down the
         * feeding thread. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is
         * up to the specific implementation to choose a default one.
         * <p/>
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

            if ((inputLimit != DEFAULT) && (inputLimit < 0)) {
                throw new IllegalArgumentException(
                        "the input limit cannot be negative: " + inputLimit);
            }

            mInputLimit = inputLimit;
            return this;
        }

        /**
         * Sets the maximum delay to apply to the feeding thread, waiting for the input channel to
         * have room for additional data.
         * <p/>
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
        public Builder<TYPE> withInputMaxDelay(final long delay, @NotNull final TimeUnit timeUnit) {

            return withInputMaxDelay(fromUnit(delay, timeUnit));
        }

        /**
         * Sets the maximum delay to apply to the feeding thread, waiting for the input channel to
         * have room for additional data.
         * <p/>
         * This configuration option should be used on conjunction with the input limit, or it might
         * have no effect on the invocation execution.
         *
         * @param delay the delay.
         * @return this builder.
         * @see #withInputMaxSize(int)
         */
        @NotNull
        public Builder<TYPE> withInputMaxDelay(@Nullable final TimeDuration delay) {

            mInputMaxDelay = delay;
            return this;
        }

        /**
         * Sets the maximum number of data that the input channel can retain before they are
         * consumed. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up
         * to the specific implementation to choose a default one.<br/>
         * When the maximum capacity is reached, the invocation will be aborted with an
         * {@link com.github.dm.jrt.core.channel.InputDeadlockException InputDeadlockException}.
         *
         * @param inputMaxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @NotNull
        public Builder<TYPE> withInputMaxSize(final int inputMaxSize) {

            if ((inputMaxSize != DEFAULT) && (inputMaxSize <= 0)) {
                throw new IllegalArgumentException(
                        "the input buffer size cannot be 0 or negative: " + inputMaxSize);
            }

            mInputMaxSize = inputMaxSize;
            return this;
        }

        /**
         * Sets the order in which input data are collected from the input channel. A null value
         * means that it is up to the specific implementation to choose a default one.<br/>
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

            if ((maxInstances != DEFAULT) && (maxInstances < 1)) {
                throw new IllegalArgumentException(
                        "the maximum number of concurrently running instances cannot be less than"
                                + " 1: " + maxInstances);
            }

            mMaxInstances = maxInstances;
            return this;
        }

        /**
         * Sets the limit of data that the output channel can retain before starting to slow down
         * the feeding thread. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it
         * is up to the specific implementation to choose a default one.
         * <p/>
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

            if ((outputLimit != DEFAULT) && (outputLimit < 0)) {
                throw new IllegalArgumentException(
                        "the output limit cannot be negative: " + outputLimit);
            }

            mOutputLimit = outputLimit;
            return this;
        }

        /**
         * Sets the maximum delay to apply to the feeding thread, waiting for the result channel to
         * have room for additional data.
         * <p/>
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
        public Builder<TYPE> withOutputMaxDelay(final long delay,
                @NotNull final TimeUnit timeUnit) {

            return withOutputMaxDelay(fromUnit(delay, timeUnit));
        }

        /**
         * Sets the maximum delay to apply to the feeding thread, waiting for the result channel to
         * have room for additional data.
         * <p/>
         * This configuration option should be used on conjunction with the output limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay the delay.
         * @return this builder.
         * @see #withOutputMaxSize(int)
         */
        @NotNull
        public Builder<TYPE> withOutputMaxDelay(@Nullable final TimeDuration delay) {

            mOutputMaxDelay = delay;
            return this;
        }

        /**
         * Sets the maximum number of data that the result channel can retain before they are
         * consumed. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up
         * to the specific implementation to choose a default one.<br/>
         * When the maximum capacity is reached, the invocation will be aborted with an
         * {@link com.github.dm.jrt.core.channel.InputDeadlockException InputDeadlockException}.
         *
         * @param outputMaxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @NotNull
        public Builder<TYPE> withOutputMaxSize(final int outputMaxSize) {

            if ((outputMaxSize != DEFAULT) && (outputMaxSize <= 0)) {
                throw new IllegalArgumentException(
                        "the output buffer size cannot be 0 or negative: " + outputMaxSize);
            }

            mOutputMaxSize = outputMaxSize;
            return this;
        }

        /**
         * Sets the order in which output data are collected from the result channel. A null value
         * means that it is up to the specific implementation to choose a default order one.<br/>
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
         * Sets the invocation priority. A {@link InvocationConfiguration#DEFAULT DEFAULT} value
         * means that the invocations will be executed with no specific priority.
         *
         * @param priority the priority.
         * @return this builder.
         * @see com.github.dm.jrt.runner.PriorityRunner PriorityRunner
         */
        @NotNull
        public Builder<TYPE> withPriority(final int priority) {

            mPriority = priority;
            return this;
        }

        /**
         * Sets the timeout for an invocation instance to produce a readable result.<br/>
         * Note that this is just the initial configuration of the invocation, since the output
         * timeout can be dynamically changed through the dedicated methods.
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
         * Sets the timeout for an invocation instance to produce a readable result. A null value
         * means that it is up to the specific implementation to choose a default one.<br/>
         * Note that this is just the initial configuration of the invocation, since the output
         * timeout can be dynamically changed through the dedicated methods.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withReadTimeout(@Nullable final TimeDuration timeout) {

            mReadTimeout = timeout;
            return this;
        }

        /**
         * Sets the action to be taken if the timeout elapses before a result can be read from the
         * output channel. A null value means that it is up to the specific implementation to choose
         * a default one.<br/>
         * Note that this is just the initial configuration of the invocation, since the output
         * timeout action can be dynamically changed through the dedicated methods.
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

            final TimeDuration readTimeout = configuration.mReadTimeout;
            if (readTimeout != null) {
                withReadTimeout(readTimeout);
            }

            final TimeoutActionType timeoutActionType = configuration.mTimeoutActionType;
            if (timeoutActionType != null) {
                withReadTimeoutAction(timeoutActionType);
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

            final TimeDuration inputMaxDelay = configuration.mInputMaxDelay;
            if (inputMaxDelay != null) {
                withInputMaxDelay(inputMaxDelay);
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

            final TimeDuration outputTimeout = configuration.mOutputMaxDelay;
            if (outputTimeout != null) {
                withOutputMaxDelay(outputTimeout);
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
                                               mReadTimeout, mTimeoutActionType, mInputOrderType,
                                               mInputLimit, mInputMaxDelay, mInputMaxSize,
                                               mOutputOrderType, mOutputLimit, mOutputMaxDelay,
                                               mOutputMaxSize, mLog, mLogLevel);
        }

        private void setConfiguration(@NotNull final InvocationConfiguration configuration) {

            mRunner = configuration.mRunner;
            mPriority = configuration.mPriority;
            mMaxInstances = configuration.mMaxInstances;
            mCoreInstances = configuration.mCoreInstances;
            mReadTimeout = configuration.mReadTimeout;
            mTimeoutActionType = configuration.mTimeoutActionType;
            mInputOrderType = configuration.mInputOrderType;
            mInputLimit = configuration.mInputLimit;
            mInputMaxDelay = configuration.mInputMaxDelay;
            mInputMaxSize = configuration.mInputMaxSize;
            mOutputOrderType = configuration.mOutputOrderType;
            mOutputLimit = configuration.mOutputLimit;
            mOutputMaxDelay = configuration.mOutputMaxDelay;
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
        public InvocationConfiguration setConfiguration(
                @NotNull final InvocationConfiguration configuration) {

            return configuration;
        }
    }
}
