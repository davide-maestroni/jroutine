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
import com.gh.bmd.jrt.routine.InvocationDeadlockException;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.Arrays;
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
 * The configuration has a synchronous and an asynchronous runner associated. The synchronous
 * implementation already included in the library are queued (the default one) and sequential.<br/>
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
 * Additionally, a recycling mechanism is provided so that, when an invocation successfully
 * completes, the instance is retained for future executions. Moreover, the maximum running
 * invocation instances at one time can be limited by calling the specific builder method. When the
 * limit is reached and an additional instance is requires, the call is blocked until one becomes
 * available or the timeout set through the builder elapses.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * In case the timeout elapses before an invocation instance becomes available, a
 * {@link InvocationDeadlockException} will be thrown.
 * <p/>
 * Finally, the number of input and output data buffered in the corresponding channel can be
 * limited in order to avoid excessive memory consumption. In case the maximum number is reached
 * when passing an input or output, the call blocks until enough data are consumed or the specified
 * timeout elapses. In the latter case a {@link com.gh.bmd.jrt.common.DeadlockException} will be
 * thrown.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * The order of input and output data is not guaranteed. Nevertheless, it is possible to force data
 * to be delivered in the same order as they are passed to the channels, at the cost of a slightly
 * increased memory usage and computation.
 * <p/>
 * Created by davide on 11/15/14.
 */
public final class RoutineConfiguration {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = Integer.MIN_VALUE;

    private static final Configurable<RoutineConfiguration> sDefaultConfigurable =
            new Configurable<RoutineConfiguration>() {

                @Nonnull
                public RoutineConfiguration setConfiguration(
                        @Nonnull final RoutineConfiguration configuration) {

                    return configuration;
                }
            };

    /**
     * Empty configuration constant.<br/>The configuration has all the values set to their default.
     */
    public static final RoutineConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

    private final Runner mAsyncRunner;

    private final TimeDuration mAvailableTimeout;

    private final int mCoreInvocations;

    private final int mInputMaxSize;

    private final OrderType mInputOrderType;

    private final TimeDuration mInputTimeout;

    private final Log mLog;

    private final LogLevel mLogLevel;

    private final int mMaxInvocations;

    private final int mOutputMaxSize;

    private final OrderType mOutputOrderType;

    private final TimeDuration mOutputTimeout;

    private final TimeDuration mReadTimeout;

    private final Runner mSyncRunner;

    private final TimeoutActionType mTimeoutActionType;

    private Object[] mFactoryArgs;

    /**
     * Constructor.
     *
     * @param factoryArgs      the invocation factory arguments.
     * @param syncRunner       the runner used for synchronous invocations.
     * @param asyncRunner      the runner used for asynchronous invocations.
     * @param maxInvocations   the maximum number of parallel running invocations. Must be positive.
     * @param coreInvocations  the maximum number of retained invocation instances. Must be 0 or a
     *                         positive number.
     * @param availableTimeout the maximum timeout while waiting for an invocation instance to be
     *                         available.
     * @param readTimeout      the action to be taken if the timeout elapses before a readable
     *                         result is available.
     * @param actionType       the timeout for an invocation instance to produce a result.
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
    private RoutineConfiguration(@Nullable final Object[] factoryArgs,
            @Nullable final Runner syncRunner, @Nullable final Runner asyncRunner,
            final int maxInvocations, final int coreInvocations,
            @Nullable final TimeDuration availableTimeout, @Nullable final TimeDuration readTimeout,
            @Nullable final TimeoutActionType actionType, @Nullable final OrderType inputOrderType,
            final int inputMaxSize, @Nullable final TimeDuration inputTimeout,
            @Nullable final OrderType outputOrderType, final int outputMaxSize,
            @Nullable final TimeDuration outputTimeout, @Nullable final Log log,
            @Nullable final LogLevel logLevel) {

        mFactoryArgs = factoryArgs;
        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mMaxInvocations = maxInvocations;
        mCoreInvocations = coreInvocations;
        mAvailableTimeout = availableTimeout;
        mReadTimeout = readTimeout;
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
     * Returns a routine configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder<RoutineConfiguration> builder() {

        return new Builder<RoutineConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns a routine configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder<RoutineConfiguration> builderFrom(
            @Nullable final RoutineConfiguration initialConfiguration) {

        return (initialConfiguration == null) ? builder()
                : new Builder<RoutineConfiguration>(sDefaultConfigurable, initialConfiguration);
    }

    /**
     * Returns a routine configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder<RoutineConfiguration> builderFrom() {

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
    public TimeDuration getAvailInvocationTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration availableTimeout = mAvailableTimeout;
        return (availableTimeout != null) ? availableTimeout : valueIfNotSet;
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
     * Returns the invocation factory arguments (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the arguments.
     */
    public Object[] getFactoryArgsOr(@Nullable final Object[] valueIfNotSet) {

        final Object[] args = mFactoryArgs;
        return (args != null) ? args : valueIfNotSet;
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
        result = 31 * result + (mAvailableTimeout != null ? mAvailableTimeout.hashCode() : 0);
        result = 31 * result + mCoreInvocations;
        result = 31 * result + mInputMaxSize;
        result = 31 * result + (mInputOrderType != null ? mInputOrderType.hashCode() : 0);
        result = 31 * result + (mInputTimeout != null ? mInputTimeout.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
        result = 31 * result + mMaxInvocations;
        result = 31 * result + mOutputMaxSize;
        result = 31 * result + (mOutputOrderType != null ? mOutputOrderType.hashCode() : 0);
        result = 31 * result + (mOutputTimeout != null ? mOutputTimeout.hashCode() : 0);
        result = 31 * result + (mReadTimeout != null ? mReadTimeout.hashCode() : 0);
        result = 31 * result + (mSyncRunner != null ? mSyncRunner.hashCode() : 0);
        result = 31 * result + (mTimeoutActionType != null ? mTimeoutActionType.hashCode() : 0);
        result = 31 * result + (mFactoryArgs != null ? Arrays.hashCode(mFactoryArgs) : 0);
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

        if (mAvailableTimeout != null ? !mAvailableTimeout.equals(that.mAvailableTimeout)
                : that.mAvailableTimeout != null) {

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

        if (mReadTimeout != null ? !mReadTimeout.equals(that.mReadTimeout)
                : that.mReadTimeout != null) {

            return false;
        }

        if (mSyncRunner != null ? !mSyncRunner.equals(that.mSyncRunner)
                : that.mSyncRunner != null) {

            return false;
        }

        if (mTimeoutActionType != that.mTimeoutActionType) {

            return false;
        }

        return Arrays.equals(mFactoryArgs, that.mFactoryArgs);
    }

    @Override
    public String toString() {

        return "RoutineConfiguration{" +
                "mAsyncRunner=" + mAsyncRunner +
                ", mAvailableTimeout=" + mAvailableTimeout +
                ", mCoreInvocations=" + mCoreInvocations +
                ", mInputMaxSize=" + mInputMaxSize +
                ", mInputOrderType=" + mInputOrderType +
                ", mInputTimeout=" + mInputTimeout +
                ", mLog=" + mLog +
                ", mLogLevel=" + mLogLevel +
                ", mMaxInvocations=" + mMaxInvocations +
                ", mOutputMaxSize=" + mOutputMaxSize +
                ", mOutputOrderType=" + mOutputOrderType +
                ", mOutputTimeout=" + mOutputTimeout +
                ", mReadTimeout=" + mReadTimeout +
                ", mSyncRunner=" + mSyncRunner +
                ", mTimeoutActionType=" + mTimeoutActionType +
                ", mFactoryArgs=" + Arrays.toString(mFactoryArgs) +
                '}';
    }

    /**
     * Creates a new logger.
     *
     * @param context the context.
     * @return the new logger.
     * @throws java.lang.NullPointerException if the specified configuration or context is null.
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
         * Pass order.<br/>
         * Data are returned in the same order as they are passed to the channel, independently from
         * the specific delay.
         */
        PASS_ORDER,
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
        TYPE setConfiguration(@Nonnull RoutineConfiguration configuration);
    }

    /**
     * Builder of routine configurations.
     *
     * @param <TYPE> the configurable object type.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private Object[] mArgs;

        private Runner mAsyncRunner;

        private TimeDuration mAvailableTimeout;

        private int mCoreInvocations;

        private int mInputMaxSize;

        private OrderType mInputOrderType;

        private TimeDuration mInputTimeout;

        private Log mLog;

        private LogLevel mLogLevel;

        private int mMaxInvocations;

        private int mOutputMaxSize;

        private OrderType mOutputOrderType;

        private TimeDuration mOutputTimeout;

        private TimeDuration mReadTimeout;

        private Runner mSyncRunner;

        private TimeoutActionType mTimeoutActionType;

        /**
         * Constructor.
         *
         * @param configurable the configurable instance.
         * @throws java.lang.NullPointerException if the specified configurable instance is null.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable) {

            if (configurable == null) {

                throw new NullPointerException("the configurable instance must no be null");
            }

            mConfigurable = configurable;
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
         * @throws java.lang.NullPointerException if the specified configurable instance is null.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable,
                @Nonnull final RoutineConfiguration initialConfiguration) {

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
         * @param configuration the routine configuration.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> with(@Nullable final RoutineConfiguration configuration) {

            if (configuration == null) {

                setConfiguration(DEFAULT_CONFIGURATION);
                return this;
            }

            applyInvocationConfiguration(configuration);
            applyChannelConfiguration(configuration);
            applyLogConfiguration(configuration);
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
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder<TYPE> withAvailInvocationTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return withAvailInvocationTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for an invocation instance to become available. A null value means that
         * it is up to the framework to choose a default duration.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withAvailInvocationTimeout(@Nullable final TimeDuration timeout) {

            mAvailableTimeout = timeout;
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
        public Builder<TYPE> withCoreInvocations(final int coreInvocations) {

            if ((coreInvocations != DEFAULT) && (coreInvocations < 0)) {

                throw new IllegalArgumentException(
                        "the maximum number of retained instances cannot be negative: "
                                + coreInvocations);
            }

            mCoreInvocations = coreInvocations;
            return this;
        }

        /**
         * Sets the arguments to be passed to the invocation factory.
         * <p/>
         * Note that, in case no constructor taking the specified arguments as parameters is found,
         * an exception will be thrown.<br/>
         * Note also that, the specified objects will be retained, so, they should be immutable or
         * never change their internal state in order to avoid concurrency issues.
         *
         * @param args the arguments.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withFactoryArgs(@Nullable final Object... args) {

            mArgs = (args != null) ? args.clone() : null;
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
         * means that it is up to the framework to choose a default order type.
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
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder<TYPE> withInputTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

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
        public Builder<TYPE> withInputTimeout(@Nullable final TimeDuration timeout) {

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
        public Builder<TYPE> withLog(@Nullable final Log log) {

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
        public Builder<TYPE> withLogLevel(@Nullable final LogLevel level) {

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
        public Builder<TYPE> withMaxInvocations(final int maxInvocations) {

            if ((maxInvocations != DEFAULT) && (maxInvocations < 1)) {

                throw new IllegalArgumentException(
                        "the maximum number of concurrently running instances cannot be less than"
                                + " 1: " + maxInvocations);
            }

            mMaxInvocations = maxInvocations;
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
         * means that it is up to the framework to choose a default order type.
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
         * @throws java.lang.NullPointerException     if the specified time unit is null.
         */
        @Nonnull
        public Builder<TYPE> withOutputTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

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
        public Builder<TYPE> withOutputTimeout(@Nullable final TimeDuration timeout) {

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
        public Builder<TYPE> withReadTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

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
        public Builder<TYPE> withReadTimeout(@Nullable final TimeDuration timeout) {

            mReadTimeout = timeout;
            return this;
        }

        /**
         * Sets the action to be taken if the timeout elapses before a result can be read from the
         * output channel.
         *
         * @param actionType the action type.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withReadTimeoutAction(@Nullable final TimeoutActionType actionType) {

            mTimeoutActionType = actionType;
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
        public Builder<TYPE> withSyncRunner(@Nullable final Runner runner) {

            mSyncRunner = runner;
            return this;
        }

        private void applyChannelConfiguration(@Nonnull final RoutineConfiguration configuration) {

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

        private void applyInvocationConfiguration(
                @Nonnull final RoutineConfiguration configuration) {

            final Object[] args = configuration.mFactoryArgs;

            if (args != null) {

                withFactoryArgs(args);
            }

            final Runner syncRunner = configuration.mSyncRunner;

            if (syncRunner != null) {

                withSyncRunner(syncRunner);
            }

            final Runner asyncRunner = configuration.mAsyncRunner;

            if (asyncRunner != null) {

                withAsyncRunner(asyncRunner);
            }

            final int maxInvocations = configuration.mMaxInvocations;

            if (maxInvocations != DEFAULT) {

                withMaxInvocations(maxInvocations);
            }

            final int coreInvocations = configuration.mCoreInvocations;

            if (coreInvocations != DEFAULT) {

                withCoreInvocations(coreInvocations);
            }

            final TimeDuration availTimeout = configuration.mAvailableTimeout;

            if (availTimeout != null) {

                withAvailInvocationTimeout(availTimeout);
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

        private void applyLogConfiguration(@Nonnull final RoutineConfiguration configuration) {

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
        private RoutineConfiguration buildConfiguration() {

            return new RoutineConfiguration(mArgs, mSyncRunner, mAsyncRunner, mMaxInvocations,
                                            mCoreInvocations, mAvailableTimeout, mReadTimeout,
                                            mTimeoutActionType, mInputOrderType, mInputMaxSize,
                                            mInputTimeout, mOutputOrderType, mOutputMaxSize,
                                            mOutputTimeout, mLog, mLogLevel);
        }

        private void setConfiguration(@Nonnull final RoutineConfiguration configuration) {

            mArgs = configuration.mFactoryArgs;
            mSyncRunner = configuration.mSyncRunner;
            mAsyncRunner = configuration.mAsyncRunner;
            mMaxInvocations = configuration.mMaxInvocations;
            mCoreInvocations = configuration.mCoreInvocations;
            mAvailableTimeout = configuration.mAvailableTimeout;
            mReadTimeout = configuration.mReadTimeout;
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
}
