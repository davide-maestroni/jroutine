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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class storing the routine configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * Created by davide on 11/15/14.
 */
public class RoutineConfiguration {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = Integer.MIN_VALUE;

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final RoutineConfiguration EMPTY_CONFIGURATION = builder().buildConfiguration();

    private final Runner mAsyncRunner;

    private final TimeDuration mAvailTimeout;

    private final int mCoreInvocations;

    private final int mInputMaxSize;

    private final OrderType mInputOrder;

    private final TimeDuration mInputTimeout;

    private final Log mLog;

    private final LogLevel mLogLevel;

    private final int mMaxInvocations;

    private final int mOutputMaxSize;

    private final OrderType mOutputOrder;

    private final TimeDuration mOutputTimeout;

    private final TimeDuration mReadTimeout;

    private final Runner mSyncRunner;

    private final TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param syncRunner      the runner used for synchronous invocations.
     * @param asyncRunner     the runner used for asynchronous invocations.
     * @param maxInvocations  the maximum number of parallel running invocations. Must be positive.
     * @param coreInvocations the maximum number of retained invocation instances. Must be 0 or a
     *                        positive number.
     * @param availTimeout    the maximum timeout while waiting for an invocation instance to be
     *                        available.
     * @param readTimeout     the action to be taken if the timeout elapses before a readable result
     *                        is available.
     * @param actionType      the timeout for an invocation instance to produce a result.
     * @param inputOrder      the order in which input data are collected from the input channel.
     * @param inputMaxSize    the maximum number of buffered input data. Must be positive.
     * @param inputTimeout    the maximum timeout while waiting for an input to be passed to the
     *                        input channel.
     * @param outputOrder     the order in which output data are collected from the result channel.
     * @param outputMaxSize   the maximum number of buffered output data. Must be positive.
     * @param outputTimeout   the maximum timeout while waiting for an output to be passed to the
     *                        result channel.
     * @param log             the log instance.
     * @param logLevel        the log level.
     */
    private RoutineConfiguration(@Nullable final Runner syncRunner,
            @Nullable final Runner asyncRunner, final int maxInvocations, final int coreInvocations,
            @Nullable final TimeDuration availTimeout, @Nullable final TimeDuration readTimeout,
            @Nullable final TimeoutActionType actionType, @Nullable final OrderType inputOrder,
            final int inputMaxSize, @Nullable final TimeDuration inputTimeout,
            @Nullable final OrderType outputOrder, final int outputMaxSize,
            @Nullable final TimeDuration outputTimeout, @Nullable final Log log,
            @Nullable final LogLevel logLevel) {

        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mMaxInvocations = maxInvocations;
        mCoreInvocations = coreInvocations;
        mAvailTimeout = availTimeout;
        mReadTimeout = readTimeout;
        mTimeoutActionType = actionType;
        mInputOrder = inputOrder;
        mInputMaxSize = inputMaxSize;
        mInputTimeout = inputTimeout;
        mOutputOrder = outputOrder;
        mOutputMaxSize = outputMaxSize;
        mOutputTimeout = outputTimeout;
        mLog = log;
        mLogLevel = logLevel;
    }

    /**
     * Returns a routine configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Returns a routine configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder builderFrom(@Nonnull final RoutineConfiguration initialConfiguration) {

        return new Builder(initialConfiguration);
    }

    /**
     * Returns the specified configuration or the empty one if the former is null.
     *
     * @param configuration the routine configuration.
     * @return the configuration.
     */
    @Nonnull
    public static RoutineConfiguration notNull(@Nullable final RoutineConfiguration configuration) {

        return (configuration != null) ? configuration : EMPTY_CONFIGURATION;
    }

    /**
     * Short for <b><code>builder().onReadTimeout(action)</code></b>.
     *
     * @param action the action type.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder onReadTimeout(@Nullable final TimeoutActionType action) {

        return builder().onReadTimeout(action);
    }

    /**
     * Short for <b><code>builder().withAsyncRunner(runner)</code></b>.
     *
     * @param runner the runner instance.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withAsyncRunner(@Nullable final Runner runner) {

        return builder().withAsyncRunner(runner);
    }

    /**
     * Short for <b><code>builder().withAvailableTimeout(timeout, timeUnit)</code></b>.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public static Builder withAvailableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return withAvailableTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Short for <b><code>builder().withAvailableTimeout(timeout)</code></b>.
     *
     * @param timeout the timeout.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withAvailableTimeout(@Nullable final TimeDuration timeout) {

        return builder().withAvailableTimeout(timeout);
    }

    /**
     * Short for <b><code>builder().withCoreInvocations(coreInvocations)</code></b>.
     *
     * @param coreInvocations the max number of instances.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the number is negative.
     */
    @Nonnull
    public static Builder withCoreInvocations(final int coreInvocations) {

        return builder().withCoreInvocations(coreInvocations);
    }

    /**
     * Short for <b><code>builder().withInputOrder(order)</code></b>.
     *
     * @param order the order type.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withInputOrder(@Nullable final OrderType order) {

        return builder().withInputOrder(order);
    }

    /**
     * Short for <b><code>builder().withInputSize(inputMaxSize)</code></b>.
     *
     * @param inputMaxSize the maximum size.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public static Builder withInputSize(final int inputMaxSize) {

        return builder().withInputSize(inputMaxSize);
    }

    /**
     * Short for <b><code>builder().withInputTimeout(timeout, timeUnit)</code></b>.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public static Builder withInputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        return withInputTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Short for <b><code>builder().withInputTimeout(timeout)</code></b>.
     *
     * @param timeout the timeout.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withInputTimeout(@Nullable final TimeDuration timeout) {

        return builder().withInputTimeout(timeout);
    }

    /**
     * Short for <b><code>builder().withLog(log)</code></b>.
     *
     * @param log the log instance.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withLog(@Nullable final Log log) {

        return builder().withLog(log);
    }

    /**
     * Short for <b><code>builder().withLogLevel(level)</code></b>.
     *
     * @param level the log level.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withLogLevel(@Nullable final LogLevel level) {

        return builder().withLogLevel(level);
    }

    /**
     * Short for <b><code>builder().withMaxInvocations(maxInvocations)</code></b>.
     *
     * @param maxInvocations the max number of instances.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public static Builder withMaxInvocations(final int maxInvocations) {

        return builder().withMaxInvocations(maxInvocations);
    }

    /**
     * Short for <b><code>builder().withOutputOrder(order)</code></b>.
     *
     * @param order the order type.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withOutputOrder(@Nullable final OrderType order) {

        return builder().withOutputOrder(order);
    }

    /**
     * Short for <b><code>builder().withOutputSize(outputMaxSize)</code></b>.
     *
     * @param outputMaxSize the maximum size.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public static Builder withOutputSize(final int outputMaxSize) {

        return builder().withOutputSize(outputMaxSize);
    }

    /**
     * Short for <b><code>builder().withOutputTimeout(timeout, timeUnit)</code></b>.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public static Builder withOutputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        return withOutputTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Short for <b><code>builder().withOutputTimeout(timeout)</code></b>.
     *
     * @param timeout the timeout.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withOutputTimeout(@Nullable final TimeDuration timeout) {

        return builder().withOutputTimeout(timeout);
    }

    /**
     * Short for <b><code>builder().withReadTimeout(timeout, timeUnit)</code></b>.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return the routine configuration builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public static Builder withReadTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        return withReadTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Short for <b><code>builder().withReadTimeout(timeout)</code></b>.
     *
     * @param timeout the timeout.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withReadTimeout(@Nullable final TimeDuration timeout) {

        return builder().withReadTimeout(timeout);
    }

    /**
     * Short for <b><code>builder().withSyncRunner(runner)</code></b>.
     *
     * @param runner the runner instance.
     * @return the routine configuration builder.
     */
    @Nonnull
    public static Builder withSyncRunner(@Nullable final Runner runner) {

        return builder().withSyncRunner(runner);
    }

    /**
     * Returns a routine configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder builderFrom() {

        return new Builder(this);
    }

    /**
     * Returns the runner used for asynchronous invocations (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the runner instance.
     */
    public Runner getAsyncRunnerOr(@Nullable final Runner valueIfNotSet) {

        final Runner runner = mAsyncRunner;
        return (runner != null) ? runner : valueIfNotSet;
    }

    /**
     * Returns the maximum timeout while waiting for an invocation instance to be available (null
     * by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getAvailTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration availTimeout = mAvailTimeout;
        return (availTimeout != null) ? availTimeout : valueIfNotSet;
    }

    /**
     * Returns the maximum number of retained invocation instances (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum number.
     */
    public int getCoreInvocationsOr(final int valueIfNotSet) {

        final int coreInvocations = mCoreInvocations;
        return (coreInvocations != DEFAULT) ? coreInvocations : valueIfNotSet;
    }

    /**
     * Returns the input data order (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public OrderType getInputOrderOr(@Nullable final OrderType valueIfNotSet) {

        final OrderType orderedInput = mInputOrder;
        return (orderedInput != null) ? orderedInput : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered input data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getInputSizeOr(final int valueIfNotSet) {

        final int inputMaxSize = mInputMaxSize;
        return (inputMaxSize != DEFAULT) ? inputMaxSize : valueIfNotSet;
    }

    /**
     * Returns the maximum timeout while waiting for an input to be passed to the input channel
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getInputTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration inputTimeout = mInputTimeout;
        return (inputTimeout != null) ? inputTimeout : valueIfNotSet;
    }

    /**
     * Returns the log level (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the log level.
     */
    public LogLevel getLogLevelOr(@Nullable final LogLevel valueIfNotSet) {

        final LogLevel logLevel = mLogLevel;
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
     * Returns the maximum number of parallel running invocations (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum number.
     */
    public int getMaxInvocationsOr(final int valueIfNotSet) {

        final int maxInvocations = mMaxInvocations;
        return (maxInvocations != DEFAULT) ? maxInvocations : valueIfNotSet;
    }

    /**
     * Returns the output data order (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public OrderType getOutputOrderOr(@Nullable final OrderType valueIfNotSet) {

        final OrderType orderedOutput = mOutputOrder;
        return (orderedOutput != null) ? orderedOutput : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered output data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getOutputSizeOr(final int valueIfNotSet) {

        final int outputMaxSize = mOutputMaxSize;
        return (outputMaxSize != DEFAULT) ? outputMaxSize : valueIfNotSet;
    }

    /**
     * Returns the maximum timeout while waiting for an output to be passed to the result channel
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getOutputTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration outputTimeout = mOutputTimeout;
        return (outputTimeout != null) ? outputTimeout : valueIfNotSet;
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
     * Returns the runner used for synchronous invocations (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the runner instance.
     */
    public Runner getSyncRunnerOr(@Nullable final Runner valueIfNotSet) {

        final Runner runner = mSyncRunner;
        return (runner != null) ? runner : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mAsyncRunner != null ? mAsyncRunner.hashCode() : 0;
        result = 31 * result + (mAvailTimeout != null ? mAvailTimeout.hashCode() : 0);
        result = 31 * result + mCoreInvocations;
        result = 31 * result + mInputMaxSize;
        result = 31 * result + (mInputOrder != null ? mInputOrder.hashCode() : 0);
        result = 31 * result + (mInputTimeout != null ? mInputTimeout.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
        result = 31 * result + mMaxInvocations;
        result = 31 * result + mOutputMaxSize;
        result = 31 * result + (mOutputOrder != null ? mOutputOrder.hashCode() : 0);
        result = 31 * result + (mOutputTimeout != null ? mOutputTimeout.hashCode() : 0);
        result = 31 * result + (mReadTimeout != null ? mReadTimeout.hashCode() : 0);
        result = 31 * result + (mSyncRunner != null ? mSyncRunner.hashCode() : 0);
        result = 31 * result + (mTimeoutActionType != null ? mTimeoutActionType.hashCode() : 0);
        return result;
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(final Object o) {

        // auto-generated code
        if (this == o) {

            return true;
        }

        if (!(o instanceof RoutineConfiguration)) {

            return false;
        }

        final RoutineConfiguration that = (RoutineConfiguration) o;

        if (mCoreInvocations != that.mCoreInvocations) {

            return false;
        }

        if (mInputMaxSize != that.mInputMaxSize) {

            return false;
        }

        if (mMaxInvocations != that.mMaxInvocations) {

            return false;
        }

        if (mOutputMaxSize != that.mOutputMaxSize) {

            return false;
        }

        if (mAsyncRunner != null ? !mAsyncRunner.equals(that.mAsyncRunner)
                : that.mAsyncRunner != null) {

            return false;
        }

        if (mAvailTimeout != null ? !mAvailTimeout.equals(that.mAvailTimeout)
                : that.mAvailTimeout != null) {

            return false;
        }

        if (mInputOrder != that.mInputOrder) {

            return false;
        }

        if (mInputTimeout != null ? !mInputTimeout.equals(that.mInputTimeout)
                : that.mInputTimeout != null) {

            return false;
        }

        if (mLog != null ? !mLog.equals(that.mLog) : that.mLog != null) {

            return false;
        }

        if (mLogLevel != that.mLogLevel) {

            return false;
        }

        if (mOutputOrder != that.mOutputOrder) {

            return false;
        }

        if (mOutputTimeout != null ? !mOutputTimeout.equals(that.mOutputTimeout)
                : that.mOutputTimeout != null) {

            return false;
        }

        if (mReadTimeout != null ? !mReadTimeout.equals(that.mReadTimeout)
                : that.mReadTimeout != null) {

            return false;
        }

        if (mSyncRunner != null ? !mSyncRunner.equals(that.mSyncRunner)
                : that.mSyncRunner != null) {

            return false;
        }

        return mTimeoutActionType == that.mTimeoutActionType;
    }

    @Override
    public String toString() {

        return "RoutineConfiguration{" +
                "mAsyncRunner=" + mAsyncRunner +
                ", mAvailTimeout=" + mAvailTimeout +
                ", mCoreInvocations=" + mCoreInvocations +
                ", mInputMaxSize=" + mInputMaxSize +
                ", mInputOrder=" + mInputOrder +
                ", mInputTimeout=" + mInputTimeout +
                ", mLog=" + mLog +
                ", mLogLevel=" + mLogLevel +
                ", mMaxInvocations=" + mMaxInvocations +
                ", mOutputMaxSize=" + mOutputMaxSize +
                ", mOutputOrder=" + mOutputOrder +
                ", mOutputTimeout=" + mOutputTimeout +
                ", mReadTimeout=" + mReadTimeout +
                ", mSyncRunner=" + mSyncRunner +
                ", mTimeoutActionType=" + mTimeoutActionType +
                '}';
    }

    /**
     * Enumeration defining how data are ordered inside a channel.
     */
    public enum OrderType {

        /**
         * Passing order.<br/>
         * Data are returned in the same order as they are passed to the channel, independently from
         * the specific delay.
         */
        PASSING_ORDER,
        /**
         * No order.<br/>
         * There is no guarantee about the data order.
         */
        NONE,
    }

    /**
     * Enumeration indicating the type of action to be taken on output channel timeout.
     */
    public enum TimeoutActionType {

        /**
         * Deadlock.<br/>
         * If no result is available after the specified timeout, the called method will throw a
         * {@link com.gh.bmd.jrt.channel.ReadDeadlockException}.
         */
        DEADLOCK,
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
     * Builder of routine configurations.
     */
    public static class Builder {

        private Runner mAsyncRunner;

        private TimeDuration mAvailTimeout;

        private int mCoreInvocations;

        private int mInputMaxSize;

        private OrderType mInputOrder;

        private TimeDuration mInputTimeout;

        private Log mLog;

        private LogLevel mLogLevel;

        private int mMaxInvocations;

        private int mOutputMaxSize;

        private OrderType mOutputOrder;

        private TimeDuration mOutputTimeout;

        private TimeDuration mReadTimeout;

        private Runner mSyncRunner;

        private TimeoutActionType mTimeoutActionType;

        /**
         * Constructor.
         */
        private Builder() {

            mMaxInvocations = DEFAULT;
            mCoreInvocations = DEFAULT;
            mInputMaxSize = DEFAULT;
            mOutputMaxSize = DEFAULT;
        }

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial configuration.
         * @throws java.lang.NullPointerException if the specified configuration instance is null.
         */
        private Builder(@Nonnull final RoutineConfiguration initialConfiguration) {

            mSyncRunner = initialConfiguration.getSyncRunnerOr(null);
            mAsyncRunner = initialConfiguration.getAsyncRunnerOr(null);
            mMaxInvocations = initialConfiguration.getMaxInvocationsOr(DEFAULT);
            mCoreInvocations = initialConfiguration.getCoreInvocationsOr(DEFAULT);
            mAvailTimeout = initialConfiguration.getAvailTimeoutOr(null);
            mReadTimeout = initialConfiguration.getReadTimeoutOr(null);
            mTimeoutActionType = initialConfiguration.getReadTimeoutActionOr(null);
            mInputOrder = initialConfiguration.getInputOrderOr(null);
            mInputMaxSize = initialConfiguration.getInputSizeOr(DEFAULT);
            mInputTimeout = initialConfiguration.getInputTimeoutOr(null);
            mOutputOrder = initialConfiguration.getOutputOrderOr(null);
            mOutputMaxSize = initialConfiguration.getOutputSizeOr(DEFAULT);
            mOutputTimeout = initialConfiguration.getOutputTimeoutOr(null);
            mLog = initialConfiguration.getLogOr(null);
            mLogLevel = initialConfiguration.getLogLevelOr(null);
        }

        /**
         * Builds and return the configuration instance.
         *
         * @return the routine configuration instance.
         */
        @Nonnull
        public RoutineConfiguration buildConfiguration() {

            return new RoutineConfiguration(mSyncRunner, mAsyncRunner, mMaxInvocations,
                                            mCoreInvocations, mAvailTimeout, mReadTimeout,
                                            mTimeoutActionType, mInputOrder, mInputMaxSize,
                                            mInputTimeout, mOutputOrder, mOutputMaxSize,
                                            mOutputTimeout, mLog, mLogLevel);
        }

        /**
         * Sets the action to be taken if the timeout elapses before a result can be read from the
         * output channel.
         *
         * @param action the action type.
         * @return this builder.
         */
        @Nonnull
        public Builder onReadTimeout(@Nullable final TimeoutActionType action) {

            mTimeoutActionType = action;
            return this;
        }

        /**
         * Sets the asynchronous runner instance. A null value means that it is up to the framework
         * to choose a default instance.
         *
         * @param runner the runner instance.
         * @return this builder.
         */
        @Nonnull
        public Builder withAsyncRunner(@Nullable final Runner runner) {

            mAsyncRunner = runner;
            return this;
        }

        /**
         * Sets the timeout for an invocation instance to become available.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder withAvailableTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

            return withAvailableTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for an invocation instance to become available. A null value means that
         * it is up to the framework to choose a default duration.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder withAvailableTimeout(@Nullable final TimeDuration timeout) {

            mAvailTimeout = timeout;
            return this;
        }

        /**
         * Applies the specified configuration to this builder.
         *
         * @param configuration the routine configuration.
         * @return this builder.
         * @throws java.lang.NullPointerException if the specified configuration is null.
         */
        @Nonnull
        public Builder withConfiguration(@Nonnull final RoutineConfiguration configuration) {

            applyInvocationConfiguration(configuration);
            applyChannelConfiguration(configuration);
            applyLogConfiguration(configuration);
            return this;
        }

        /**
         * Sets the number of invocation instances which represents the core pool of reusable
         * invocations. A {@link RoutineConfiguration#DEFAULT} value means that it is up to the
         * framework to choose a default number.
         *
         * @param coreInvocations the max number of instances.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is negative.
         */
        @Nonnull
        public Builder withCoreInvocations(final int coreInvocations) {

            if ((coreInvocations != DEFAULT) && (coreInvocations < 0)) {

                throw new IllegalArgumentException(
                        "the maximum number of retained instances cannot be negative: "
                                + coreInvocations);
            }

            mCoreInvocations = coreInvocations;
            return this;
        }

        /**
         * Sets the order in which input data are collected from the input channel. A null value
         * means that it is up to the framework to choose a default order type.
         *
         * @param order the order type.
         * @return this builder.
         */
        @Nonnull
        public Builder withInputOrder(@Nullable final OrderType order) {

            mInputOrder = order;
            return this;
        }

        /**
         * Sets the maximum number of data that the input channel can retain before they are
         * consumed. A {@link RoutineConfiguration#DEFAULT} value means that it is up to the
         * framework to choose a default size.
         *
         * @param inputMaxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @Nonnull
        public Builder withInputSize(final int inputMaxSize) {

            if ((inputMaxSize != DEFAULT) && (inputMaxSize <= 0)) {

                throw new IllegalArgumentException(
                        "the input buffer size cannot be 0 or negative: " + inputMaxSize);
            }

            mInputMaxSize = inputMaxSize;
            return this;
        }

        /**
         * Sets the timeout for an input channel to have room for additional data.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder withInputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

            return withInputTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for an input channel to have room for additional data. A null value
         * means that it is up to the framework to choose a default.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder withInputTimeout(@Nullable final TimeDuration timeout) {

            mInputTimeout = timeout;
            return this;
        }

        /**
         * Sets the log instance. A null value means that it is up to the framework to choose a
         * default implementation.
         *
         * @param log the log instance.
         * @return this builder.
         */
        @Nonnull
        public Builder withLog(@Nullable final Log log) {

            mLog = log;
            return this;
        }

        /**
         * Sets the log level. A null value means that it is up to the framework to choose a default
         * level.
         *
         * @param level the log level.
         * @return this builder.
         */
        @Nonnull
        public Builder withLogLevel(@Nullable final LogLevel level) {

            mLogLevel = level;
            return this;
        }

        /**
         * Sets the max number of concurrently running invocation instances. A
         * {@link RoutineConfiguration#DEFAULT} value means that it is up to the framework to choose
         * a default number.
         *
         * @param maxInvocations the max number of instances.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @Nonnull
        public Builder withMaxInvocations(final int maxInvocations) {

            if ((maxInvocations != DEFAULT) && (maxInvocations < 1)) {

                throw new IllegalArgumentException(
                        "the maximum number of concurrently running instances cannot be less than"
                                + " 1: " + maxInvocations);
            }

            mMaxInvocations = maxInvocations;
            return this;
        }

        /**
         * Sets the order in which output data are collected from the result channel. A null value
         * means that it is up to the framework to choose a default order type.
         *
         * @param order the order type.
         * @return this builder.
         */
        @Nonnull
        public Builder withOutputOrder(@Nullable final OrderType order) {

            mOutputOrder = order;
            return this;
        }

        /**
         * Sets the maximum number of data that the result channel can retain before they are
         * consumed. A {@link RoutineConfiguration#DEFAULT} value means that it is up to the
         * framework to choose a default size.
         *
         * @param outputMaxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @Nonnull
        public Builder withOutputSize(final int outputMaxSize) {

            if ((outputMaxSize != DEFAULT) && (outputMaxSize <= 0)) {

                throw new IllegalArgumentException(
                        "the output buffer size cannot be 0 or negative: " + outputMaxSize);
            }

            mOutputMaxSize = outputMaxSize;
            return this;
        }

        /**
         * Sets the timeout for a result channel to have room for additional data.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder withOutputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

            return withOutputTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for a result channel to have room for additional data. A null value
         * means that it is up to the framework to choose a default.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder withOutputTimeout(@Nullable final TimeDuration timeout) {

            mOutputTimeout = timeout;
            return this;
        }

        /**
         * Sets the timeout for an invocation instance to produce a readable result.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder withReadTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

            return withReadTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for an invocation instance to produce a readable result. A null value
         * means that it is up to the framework to choose a default duration.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder withReadTimeout(@Nullable final TimeDuration timeout) {

            mReadTimeout = timeout;
            return this;
        }

        /**
         * Sets the synchronous runner instance. A null value means that it is up to the framework
         * to choose a default instance.
         *
         * @param runner the runner instance.
         * @return this builder.
         */
        @Nonnull
        public Builder withSyncRunner(@Nullable final Runner runner) {

            mSyncRunner = runner;
            return this;
        }

        private void applyChannelConfiguration(@Nonnull final RoutineConfiguration configuration) {

            final OrderType inputOrder = configuration.getInputOrderOr(null);

            if (inputOrder != null) {

                withInputOrder(inputOrder);
            }

            final int inputSize = configuration.getInputSizeOr(DEFAULT);

            if (inputSize != DEFAULT) {

                withInputSize(inputSize);
            }

            final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

            if (inputTimeout != null) {

                withInputTimeout(inputTimeout);
            }

            final OrderType outputOrder = configuration.getOutputOrderOr(null);

            if (outputOrder != null) {

                withOutputOrder(outputOrder);
            }

            final int outputSize = configuration.getOutputSizeOr(DEFAULT);

            if (outputSize != DEFAULT) {

                withOutputSize(outputSize);
            }

            final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

            if (outputTimeout != null) {

                withOutputTimeout(outputTimeout);
            }
        }

        private void applyInvocationConfiguration(
                @Nonnull final RoutineConfiguration configuration) {

            final Runner syncRunner = configuration.getSyncRunnerOr(null);

            if (syncRunner != null) {

                withSyncRunner(syncRunner);
            }

            final Runner asyncRunner = configuration.getAsyncRunnerOr(null);

            if (asyncRunner != null) {

                withAsyncRunner(asyncRunner);
            }

            final int maxInvocations = configuration.getMaxInvocationsOr(DEFAULT);

            if (maxInvocations != DEFAULT) {

                withMaxInvocations(maxInvocations);
            }

            final int coreInvocations = configuration.getCoreInvocationsOr(DEFAULT);

            if (coreInvocations != DEFAULT) {

                withCoreInvocations(coreInvocations);
            }

            final TimeDuration availTimeout = configuration.getAvailTimeoutOr(null);

            if (availTimeout != null) {

                withAvailableTimeout(availTimeout);
            }

            final TimeDuration readTimeout = configuration.getReadTimeoutOr(null);

            if (readTimeout != null) {

                withReadTimeout(readTimeout);
            }

            final TimeoutActionType timeoutActionType = configuration.getReadTimeoutActionOr(null);

            if (timeoutActionType != null) {

                onReadTimeout(timeoutActionType);
            }
        }

        private void applyLogConfiguration(@Nonnull final RoutineConfiguration configuration) {

            final Log log = configuration.getLogOr(null);

            if (log != null) {

                withLog(log);
            }

            final LogLevel logLevel = configuration.getLogLevelOr(null);

            if (logLevel != null) {

                withLogLevel(logLevel);
            }
        }
    }
}
