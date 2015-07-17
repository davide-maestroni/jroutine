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
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.util.TimeDuration.fromUnit;

/**
 * Class storing the invocation configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration.
 * <p/>
 * The configuration has a synchronous and an asynchronous runner associated. The synchronous
 * implementation already included in the library are queued and sequential.<br/>
 * The queued one maintains an internal buffer of executions that are consumed only when the
 * last one completes, thus avoiding overflowing the call stack because of nested calls to other
 * routines.<br/>
 * The sequential one simply runs the executions as soon as they are invoked.<br/>
 * While the latter is less memory and CPU consuming, it might greatly increase the depth of the
 * call stack, and blocks execution of the calling thread during delayed executions.<br/>
 * In both cases the executions are run inside the calling thread.<br/>
 * The default asynchronous runner is shared among all the routines, but a custom one can be set
 * through the builder.
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
 * limit is reached and an additional instance is required, the call is blocked until one becomes
 * available or the timeout set through the builder elapses.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * In case the timeout elapses before an invocation instance becomes available, an
 * {@link com.gh.bmd.jrt.invocation.InvocationTimeoutException InvocationTimeoutException} will
 * be thrown.
 * <p/>
 * Finally, the number of input and output data buffered in the corresponding channel can be
 * limited in order to avoid excessive memory consumption. In case the maximum number is reached
 * when passing an input or output, the call blocks until enough data are consumed or the specified
 * timeout elapses. In the latter case, a {@link com.gh.bmd.jrt.channel.TimeoutException
 * TimeoutException} will be thrown.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * The order of input and output data is not guaranteed. Nevertheless, it is possible to force data
 * to be delivered in the same order as they are passed to the channels, at the cost of a slightly
 * increase in memory usage and computation.
 * <p/>
 * Created by davide-maestroni on 11/15/14.
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

    private final Runner mAsyncRunner;

    private final TimeDuration mAvailableTimeout;

    private final int mCoreInstances;

    private final TimeDuration mExecutionTimeout;

    private final int mInputMaxSize;

    private final OrderType mInputOrderType;

    private final TimeDuration mInputTimeout;

    private final Log mLog;

    private final LogLevel mLogLevel;

    private final int mMaxInstances;

    private final int mOutputMaxSize;

    private final OrderType mOutputOrderType;

    private final TimeDuration mOutputTimeout;

    private final int mPriority;

    private final Runner mSyncRunner;

    private final TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param syncRunner       the runner used for synchronous invocations.
     * @param asyncRunner      the runner used for asynchronous invocations.
     * @param priority         the invocation priority.
     * @param maxInstances     the maximum number of parallel running invocations. Must be positive.
     * @param coreInstances    the maximum number of retained invocation instances. Must be 0 or a
     *                         positive number.
     * @param availableTimeout the maximum timeout while waiting for an invocation instance to be
     *                         available.
     * @param executionTimeout the timeout for an invocation instance to produce a result.
     * @param actionType       the action to be taken if the timeout elapses before a readable
     *                         result is available.
     * @param inputOrderType   the order in which input data are collected from the input channel.
     * @param inputMaxSize     the maximum number of buffered input data. Must be positive.
     * @param inputTimeout     the maximum timeout while waiting for an input to be passed to the
     *                         input channel.
     * @param outputOrderType  the order in which output data are collected from the result channel.
     * @param outputMaxSize    the maximum number of buffered output data. Must be positive.
     * @param outputTimeout    the maximum timeout while waiting for an output to be passed to the
     *                         result channel.
     * @param log              the log instance.
     * @param logLevel         the log level.
     */
    private InvocationConfiguration(@Nullable final Runner syncRunner,
            @Nullable final Runner asyncRunner, final int priority, final int maxInstances,
            final int coreInstances, @Nullable final TimeDuration availableTimeout,
            @Nullable final TimeDuration executionTimeout,
            @Nullable final TimeoutActionType actionType, @Nullable final OrderType inputOrderType,
            final int inputMaxSize, @Nullable final TimeDuration inputTimeout,
            @Nullable final OrderType outputOrderType, final int outputMaxSize,
            @Nullable final TimeDuration outputTimeout, @Nullable final Log log,
            @Nullable final LogLevel logLevel) {

        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mPriority = priority;
        mMaxInstances = maxInstances;
        mCoreInstances = coreInstances;
        mAvailableTimeout = availableTimeout;
        mExecutionTimeout = executionTimeout;
        mTimeoutActionType = actionType;
        mInputOrderType = inputOrderType;
        mInputMaxSize = inputMaxSize;
        mInputTimeout = inputTimeout;
        mOutputOrderType = outputOrderType;
        mOutputMaxSize = outputMaxSize;
        mOutputTimeout = outputTimeout;
        mLog = log;
        mLogLevel = logLevel;
    }

    /**
     * Returns an invocation configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder<InvocationConfiguration> builder() {

        return new Builder<InvocationConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns an invocation configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
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
    @Nonnull
    public Builder<InvocationConfiguration> builderFrom() {

        return builderFrom(this);
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
    public TimeDuration getAvailInstanceTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration availableTimeout = mAvailableTimeout;
        return (availableTimeout != null) ? availableTimeout : valueIfNotSet;
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
     * Returns the action to be taken if the timeout elapses before a readable result is available
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the action type.
     */
    public TimeoutActionType getExecutionTimeoutActionOr(
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
    public TimeDuration getExecutionTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration executionTimeout = mExecutionTimeout;
        return (executionTimeout != null) ? executionTimeout : valueIfNotSet;
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
        result = 31 * result + (mAvailableTimeout != null ? mAvailableTimeout.hashCode() : 0);
        result = 31 * result + mCoreInstances;
        result = 31 * result + (mExecutionTimeout != null ? mExecutionTimeout.hashCode() : 0);
        result = 31 * result + mInputMaxSize;
        result = 31 * result + (mInputOrderType != null ? mInputOrderType.hashCode() : 0);
        result = 31 * result + (mInputTimeout != null ? mInputTimeout.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
        result = 31 * result + mMaxInstances;
        result = 31 * result + mOutputMaxSize;
        result = 31 * result + (mOutputOrderType != null ? mOutputOrderType.hashCode() : 0);
        result = 31 * result + (mOutputTimeout != null ? mOutputTimeout.hashCode() : 0);
        result = 31 * result + mPriority;
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

        if (!(o instanceof InvocationConfiguration)) {

            return false;
        }

        final InvocationConfiguration that = (InvocationConfiguration) o;

        if (mCoreInstances != that.mCoreInstances) {

            return false;
        }

        if (mInputMaxSize != that.mInputMaxSize) {

            return false;
        }

        if (mMaxInstances != that.mMaxInstances) {

            return false;
        }

        if (mOutputMaxSize != that.mOutputMaxSize) {

            return false;
        }

        if (mPriority != that.mPriority) {

            return false;
        }

        if (mAsyncRunner != null ? !mAsyncRunner.equals(that.mAsyncRunner)
                : that.mAsyncRunner != null) {

            return false;
        }

        if (mAvailableTimeout != null ? !mAvailableTimeout.equals(that.mAvailableTimeout)
                : that.mAvailableTimeout != null) {

            return false;
        }

        if (mExecutionTimeout != null ? !mExecutionTimeout.equals(that.mExecutionTimeout)
                : that.mExecutionTimeout != null) {

            return false;
        }

        if (mInputOrderType != that.mInputOrderType) {

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

        if (mOutputOrderType != that.mOutputOrderType) {

            return false;
        }

        if (mOutputTimeout != null ? !mOutputTimeout.equals(that.mOutputTimeout)
                : that.mOutputTimeout != null) {

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

        return "InvocationConfiguration{" +
                "mAsyncRunner=" + mAsyncRunner +
                ", mAvailableTimeout=" + mAvailableTimeout +
                ", mCoreInstances=" + mCoreInstances +
                ", mExecutionTimeout=" + mExecutionTimeout +
                ", mInputMaxSize=" + mInputMaxSize +
                ", mInputOrderType=" + mInputOrderType +
                ", mInputTimeout=" + mInputTimeout +
                ", mLog=" + mLog +
                ", mLogLevel=" + mLogLevel +
                ", mMaxInstances=" + mMaxInstances +
                ", mOutputMaxSize=" + mOutputMaxSize +
                ", mOutputOrderType=" + mOutputOrderType +
                ", mOutputTimeout=" + mOutputTimeout +
                ", mPriority=" + mPriority +
                ", mSyncRunner=" + mSyncRunner +
                ", mTimeoutActionType=" + mTimeoutActionType +
                '}';
    }

    /**
     * Creates a new logger.
     *
     * @param context the context.
     * @return the new logger.
     */
    @Nonnull
    public Logger newLogger(@Nonnull final Object context) {

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
         * delivered based on their delay.
         */
        BY_DELAY,
        /**
         * Order by chance.<br/>
         * There is no guarantee about the data order.
         */
        BY_CHANCE
    }

    /**
     * Enumeration indicating the type of action to be taken on output channel timeout.
     */
    public enum TimeoutActionType {

        /**
         * Throw.<br/>
         * If no result is available after the specified timeout, the called method will throw an
         * {@link com.gh.bmd.jrt.channel.ExecutionTimeoutException ExecutionTimeoutException}.
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
        @Nonnull
        TYPE setConfiguration(@Nonnull InvocationConfiguration configuration);
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

        private Runner mAsyncRunner;

        private TimeDuration mAvailableTimeout;

        private int mCoreInstances;

        private TimeDuration mExecutionTimeout;

        private int mInputMaxSize;

        private OrderType mInputOrderType;

        private TimeDuration mInputTimeout;

        private Log mLog;

        private LogLevel mLogLevel;

        private int mMaxInstances;

        private int mOutputMaxSize;

        private OrderType mOutputOrderType;

        private TimeDuration mOutputTimeout;

        private int mPriority;

        private Runner mSyncRunner;

        private TimeoutActionType mTimeoutActionType;

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
            mPriority = DEFAULT;
            mMaxInstances = DEFAULT;
            mCoreInstances = DEFAULT;
            mInputMaxSize = DEFAULT;
            mOutputMaxSize = DEFAULT;
        }

        /**
         * Constructor.
         *
         * @param configurable         the configurable instance.
         * @param initialConfiguration the initial configuration.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable,
                @Nonnull final InvocationConfiguration initialConfiguration) {

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
         * @param configuration the invocation configuration.
         * @return this builder.
         */
        @Nonnull
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
         * Sets the asynchronous runner instance. A null value means that it is up to the specific
         * implementation to choose a default one.
         *
         * @param runner the runner instance.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withAsyncRunner(@Nullable final Runner runner) {

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
         */
        @Nonnull
        public Builder<TYPE> withAvailInstanceTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return withAvailInstanceTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for an invocation instance to become available. A null value means that
         * it is up to the specific implementation to choose a default one.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withAvailInstanceTimeout(@Nullable final TimeDuration timeout) {

            mAvailableTimeout = timeout;
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
        @Nonnull
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
         * Sets the timeout for an invocation instance to produce a readable result.<br/>
         * Note that this is just the initial configuration of the invocation, since the output
         * timeout can be dynamically changed through the dedicated methods.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         */
        @Nonnull
        public Builder<TYPE> withExecutionTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return withExecutionTimeout(fromUnit(timeout, timeUnit));
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
        @Nonnull
        public Builder<TYPE> withExecutionTimeout(@Nullable final TimeDuration timeout) {

            mExecutionTimeout = timeout;
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
        @Nonnull
        public Builder<TYPE> withExecutionTimeoutAction(
                @Nullable final TimeoutActionType actionType) {

            mTimeoutActionType = actionType;
            return this;
        }

        /**
         * Sets the maximum number of data that the input channel can retain before they are
         * consumed. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up
         * to the specific implementation to choose a default one.
         *
         * @param inputMaxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @Nonnull
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
        @Nonnull
        public Builder<TYPE> withInputOrder(@Nullable final OrderType orderType) {

            mInputOrderType = orderType;
            return this;
        }

        /**
         * Sets the timeout for an input channel to have room for additional data.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         */
        @Nonnull
        public Builder<TYPE> withInputTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return withInputTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for an input channel to have room for additional data. A null value
         * means that it is up to the specific implementation to choose a default one.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withInputTimeout(@Nullable final TimeDuration timeout) {

            mInputTimeout = timeout;
            return this;
        }

        /**
         * Sets the log instance. A null value means that it is up to the specific implementation to
         * choose a default one.
         *
         * @param log the log instance.
         * @return this builder.
         */
        @Nonnull
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
        @Nonnull
        public Builder<TYPE> withLogLevel(@Nullable final LogLevel level) {

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
        @Nonnull
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
         * Sets the maximum number of data that the result channel can retain before they are
         * consumed. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is up
         * to the specific implementation to choose a default one.
         *
         * @param outputMaxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @Nonnull
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
        @Nonnull
        public Builder<TYPE> withOutputOrder(@Nullable final OrderType orderType) {

            mOutputOrderType = orderType;
            return this;
        }

        /**
         * Sets the timeout for a result channel to have room for additional data.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         */
        @Nonnull
        public Builder<TYPE> withOutputTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return withOutputTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for a result channel to have room for additional data. A null value
         * means that it is up to the specific implementation to choose a default one.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withOutputTimeout(@Nullable final TimeDuration timeout) {

            mOutputTimeout = timeout;
            return this;
        }

        /**
         * Sets the invocation priority. A {@link InvocationConfiguration#DEFAULT DEFAULT} value
         * means that the invocations will be executed with no specific priority.
         *
         * @param priority the priority.
         * @return this builder.
         * @see com.gh.bmd.jrt.runner.PriorityRunner PriorityRunner
         */
        @Nonnull
        public Builder<TYPE> withPriority(final int priority) {

            mPriority = priority;
            return this;
        }

        /**
         * Sets the synchronous runner instance. A null value means that it is up to the specific
         * implementation to choose a default one.
         *
         * @param runner the runner instance.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withSyncRunner(@Nullable final Runner runner) {

            mSyncRunner = runner;
            return this;
        }

        private void applyBaseConfiguration(@Nonnull final InvocationConfiguration configuration) {

            final Runner syncRunner = configuration.mSyncRunner;

            if (syncRunner != null) {

                withSyncRunner(syncRunner);
            }

            final Runner asyncRunner = configuration.mAsyncRunner;

            if (asyncRunner != null) {

                withAsyncRunner(asyncRunner);
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

            final TimeDuration availTimeout = configuration.mAvailableTimeout;

            if (availTimeout != null) {

                withAvailInstanceTimeout(availTimeout);
            }

            final TimeDuration executionTimeout = configuration.mExecutionTimeout;

            if (executionTimeout != null) {

                withExecutionTimeout(executionTimeout);
            }

            final TimeoutActionType timeoutActionType = configuration.mTimeoutActionType;

            if (timeoutActionType != null) {

                withExecutionTimeoutAction(timeoutActionType);
            }
        }

        private void applyChannelConfiguration(
                @Nonnull final InvocationConfiguration configuration) {

            final OrderType inputOrderType = configuration.mInputOrderType;

            if (inputOrderType != null) {

                withInputOrder(inputOrderType);
            }

            final int inputSize = configuration.mInputMaxSize;

            if (inputSize != DEFAULT) {

                withInputMaxSize(inputSize);
            }

            final TimeDuration inputTimeout = configuration.mInputTimeout;

            if (inputTimeout != null) {

                withInputTimeout(inputTimeout);
            }

            final OrderType outputOrderType = configuration.mOutputOrderType;

            if (outputOrderType != null) {

                withOutputOrder(outputOrderType);
            }

            final int outputSize = configuration.mOutputMaxSize;

            if (outputSize != DEFAULT) {

                withOutputMaxSize(outputSize);
            }

            final TimeDuration outputTimeout = configuration.mOutputTimeout;

            if (outputTimeout != null) {

                withOutputTimeout(outputTimeout);
            }
        }

        private void applyLogConfiguration(@Nonnull final InvocationConfiguration configuration) {

            final Log log = configuration.mLog;

            if (log != null) {

                withLog(log);
            }

            final LogLevel logLevel = configuration.mLogLevel;

            if (logLevel != null) {

                withLogLevel(logLevel);
            }
        }

        @Nonnull
        private InvocationConfiguration buildConfiguration() {

            return new InvocationConfiguration(mSyncRunner, mAsyncRunner, mPriority, mMaxInstances,
                                               mCoreInstances, mAvailableTimeout, mExecutionTimeout,
                                               mTimeoutActionType, mInputOrderType, mInputMaxSize,
                                               mInputTimeout, mOutputOrderType, mOutputMaxSize,
                                               mOutputTimeout, mLog, mLogLevel);
        }

        private void setConfiguration(@Nonnull final InvocationConfiguration configuration) {

            mSyncRunner = configuration.mSyncRunner;
            mAsyncRunner = configuration.mAsyncRunner;
            mPriority = configuration.mPriority;
            mMaxInstances = configuration.mMaxInstances;
            mCoreInstances = configuration.mCoreInstances;
            mAvailableTimeout = configuration.mAvailableTimeout;
            mExecutionTimeout = configuration.mExecutionTimeout;
            mTimeoutActionType = configuration.mTimeoutActionType;
            mInputOrderType = configuration.mInputOrderType;
            mInputMaxSize = configuration.mInputMaxSize;
            mInputTimeout = configuration.mInputTimeout;
            mOutputOrderType = configuration.mOutputOrderType;
            mOutputMaxSize = configuration.mOutputMaxSize;
            mOutputTimeout = configuration.mOutputTimeout;
            mLog = configuration.mLog;
            mLogLevel = configuration.mLogLevel;
        }
    }

    /**
     * Default configurable implementation.
     */
    private static class DefaultConfigurable implements Configurable<InvocationConfiguration> {

        @Nonnull
        public InvocationConfiguration setConfiguration(
                @Nonnull final InvocationConfiguration configuration) {

            return configuration;
        }
    }
}
