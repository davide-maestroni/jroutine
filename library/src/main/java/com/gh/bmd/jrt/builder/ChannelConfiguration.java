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

import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.util.TimeDuration.fromUnit;

/**
 * Class storing the transport channel configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration.
 * <p/>
 * The configuration has an asynchronous runner associated.<br/>
 * The number of input data buffered in the channel can be also limited in order to avoid excessive
 * memory consumption. In case the maximum number is reached hen passing an input, the call blocks
 * until enough data are consumed or the specified  timeout elapses. In the latter case, a
 * {@link com.gh.bmd.jrt.channel.DeadlockException DeadlockException} will be thrown.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * The order of input data is not guaranteed. Nevertheless, it is possible to force data to be
 * delivered in the same order as they are passed to the channels, at the cost of a slightly
 * increased memory usage and computation.
 * <p/>
 * Created by davide-maestroni on 03/07/15.
 */
public class ChannelConfiguration {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = InvocationConfiguration.DEFAULT;

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    /**
     * Empty configuration constant.<br/>The configuration has all the options set to their default.
     */
    public static final ChannelConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

    private final Runner mAsyncRunner;

    private final int mChannelMaxSize;

    private final OrderType mChannelOrderType;

    private final TimeDuration mChannelTimeout;

    private final Log mLog;

    private final LogLevel mLogLevel;

    private final TimeDuration mReadTimeout;

    private final TimeoutActionType mTimeoutActionType;

    /**
     * Constructor.
     *
     * @param asyncRunner      the runner used for asynchronous inputs.
     * @param readTimeout      the timeout for the channel to produce a result.
     * @param actionType       the action to be taken if the timeout elapses before a readable
     *                         result is available.
     * @param channelOrderType the order in which data are collected from the output channel.
     * @param channelMaxSize   the maximum number of buffered data. Must be positive.
     * @param channelTimeout   the maximum timeout while waiting for an object to be passed to the
     *                         channel.
     * @param log              the log instance.
     * @param logLevel         the log level.
     */
    private ChannelConfiguration(@Nullable final Runner asyncRunner,
            @Nullable final TimeDuration readTimeout, @Nullable final TimeoutActionType actionType,
            @Nullable final OrderType channelOrderType, final int channelMaxSize,
            @Nullable final TimeDuration channelTimeout, @Nullable final Log log,
            @Nullable final LogLevel logLevel) {

        mAsyncRunner = asyncRunner;
        mReadTimeout = readTimeout;
        mTimeoutActionType = actionType;
        mChannelOrderType = channelOrderType;
        mChannelMaxSize = channelMaxSize;
        mChannelTimeout = channelTimeout;
        mLog = log;
        mLogLevel = logLevel;
    }

    /**
     * Returns a channel configuration builder.
     *
     * @return the builder.
     */
    @Nonnull
    public static Builder<ChannelConfiguration> builder() {

        return new Builder<ChannelConfiguration>(sDefaultConfigurable);
    }

    /**
     * Returns a channel configuration builder initialized with the specified configuration.
     *
     * @param initialConfiguration the initial configuration.
     * @return the builder.
     */
    @Nonnull
    public static Builder<ChannelConfiguration> builderFrom(
            @Nullable final ChannelConfiguration initialConfiguration) {

        return (initialConfiguration == null) ? builder()
                : new Builder<ChannelConfiguration>(sDefaultConfigurable, initialConfiguration);
    }

    /**
     * Returns a channel configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @Nonnull
    public Builder<ChannelConfiguration> builderFrom() {

        return builderFrom(this);
    }

    /**
     * Returns the runner used for asynchronous inputs (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the runner instance.
     */
    public Runner getAsyncRunnerOr(@Nullable final Runner valueIfNotSet) {

        final Runner runner = mAsyncRunner;
        return (runner != null) ? runner : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getChannelMaxSizeOr(final int valueIfNotSet) {

        final int maxSize = mChannelMaxSize;
        return (maxSize != DEFAULT) ? maxSize : valueIfNotSet;
    }

    /**
     * Returns the data order (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public OrderType getChannelOrderTypeOr(@Nullable final OrderType valueIfNotSet) {

        final OrderType orderType = mChannelOrderType;
        return (orderType != null) ? orderType : valueIfNotSet;
    }

    /**
     * Returns the maximum timeout while waiting for an object to be passed to the channel (null by
     * default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getChannelTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration timeout = mChannelTimeout;
        return (timeout != null) ? timeout : valueIfNotSet;
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
     * Returns the timeout for the channel to produce a readable result (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getReadTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration readTimeout = mReadTimeout;
        return (readTimeout != null) ? readTimeout : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mAsyncRunner != null ? mAsyncRunner.hashCode() : 0;
        result = 31 * result + mChannelMaxSize;
        result = 31 * result + (mChannelOrderType != null ? mChannelOrderType.hashCode() : 0);
        result = 31 * result + (mChannelTimeout != null ? mChannelTimeout.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
        result = 31 * result + (mReadTimeout != null ? mReadTimeout.hashCode() : 0);
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

        if (!(o instanceof ChannelConfiguration)) {

            return false;
        }

        final ChannelConfiguration that = (ChannelConfiguration) o;

        if (mChannelMaxSize != that.mChannelMaxSize) {

            return false;
        }

        if (mAsyncRunner != null ? !mAsyncRunner.equals(that.mAsyncRunner)
                : that.mAsyncRunner != null) {

            return false;
        }

        if (mChannelOrderType != that.mChannelOrderType) {

            return false;
        }

        if (mChannelTimeout != null ? !mChannelTimeout.equals(that.mChannelTimeout)
                : that.mChannelTimeout != null) {

            return false;
        }

        if (mLog != null ? !mLog.equals(that.mLog) : that.mLog != null) {

            return false;
        }

        if (mLogLevel != that.mLogLevel) {

            return false;
        }

        if (mReadTimeout != null ? !mReadTimeout.equals(that.mReadTimeout)
                : that.mReadTimeout != null) {

            return false;
        }

        return mTimeoutActionType == that.mTimeoutActionType;
    }

    @Override
    public String toString() {

        return "ChannelConfiguration{" +
                "mAsyncRunner=" + mAsyncRunner +
                ", mChannelMaxSize=" + mChannelMaxSize +
                ", mChannelOrderType=" + mChannelOrderType +
                ", mChannelTimeout=" + mChannelTimeout +
                ", mLog=" + mLog +
                ", mLogLevel=" + mLogLevel +
                ", mReadTimeout=" + mReadTimeout +
                ", mTimeoutActionType=" + mTimeoutActionType +
                '}';
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
        TYPE setConfiguration(@Nonnull ChannelConfiguration configuration);
    }

    /**
     * Builder of channel configurations.
     *
     * @param <TYPE> the configurable object type.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private Runner mAsyncRunner;

        private int mChannelMaxSize;

        private OrderType mChannelOrderType;

        private TimeDuration mChannelTimeout;

        private Log mLog;

        private LogLevel mLogLevel;

        private TimeDuration mReadTimeout;

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
            mChannelMaxSize = DEFAULT;
        }

        /**
         * Constructor.
         *
         * @param configurable         the configurable instance.
         * @param initialConfiguration the initial configuration.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@Nonnull final Configurable<? extends TYPE> configurable,
                @Nonnull final ChannelConfiguration initialConfiguration) {

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
         * @param configuration the channel configuration.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> with(@Nullable final ChannelConfiguration configuration) {

            if (configuration == null) {

                setConfiguration(DEFAULT_CONFIGURATION);
                return this;
            }

            final Runner asyncRunner = configuration.mAsyncRunner;

            if (asyncRunner != null) {

                withAsyncRunner(asyncRunner);
            }

            final OrderType orderType = configuration.mChannelOrderType;

            if (orderType != null) {

                withChannelOrder(orderType);
            }

            final int maxSize = configuration.mChannelMaxSize;

            if (maxSize != DEFAULT) {

                withChannelMaxSize(maxSize);
            }

            final TimeDuration channelTimeout = configuration.mChannelTimeout;

            if (channelTimeout != null) {

                withChannelTimeout(channelTimeout);
            }

            final Log log = configuration.mLog;

            if (log != null) {

                withLog(log);
            }

            final LogLevel logLevel = configuration.mLogLevel;

            if (logLevel != null) {

                withLogLevel(logLevel);
            }

            return this;
        }

        /**
         * Sets the asynchronous runner instance. A null value means that it is up to the framework
         * to choose a default one.
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
         * Sets the maximum number of data that the channel can retain before they are consumed. A
         * {@link ChannelConfiguration#DEFAULT} value means that it is up to the framework to
         * choose a default one.
         *
         * @param maxSize the maximum size.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the number is less than 1.
         */
        @Nonnull
        public Builder<TYPE> withChannelMaxSize(final int maxSize) {

            if ((maxSize != DEFAULT) && (maxSize <= 0)) {

                throw new IllegalArgumentException(
                        "the channel buffer size cannot be 0 or negative: " + maxSize);
            }

            mChannelMaxSize = maxSize;
            return this;
        }

        /**
         * Sets the order in which data are collected from the channel. A null value means that it
         * is up to the framework to choose a default order one.<br/>
         * Note that this is just the initial configuration, since the channel order can be
         * dynamically changed through the dedicated methods.
         *
         * @param orderType the order type.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withChannelOrder(@Nullable final OrderType orderType) {

            mChannelOrderType = orderType;
            return this;
        }

        /**
         * Sets the timeout for the channel to have room for additional data.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         */
        @Nonnull
        public Builder<TYPE> withChannelTimeout(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return withChannelTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for the channel to have room for additional data. A null value means
         * that it is up to the framework to choose a default one.
         *
         * @param timeout the timeout.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withChannelTimeout(@Nullable final TimeDuration timeout) {

            mChannelTimeout = timeout;
            return this;
        }

        /**
         * Sets the log instance. A null value means that it is up to the framework to choose a
         * default one.
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
         * one.
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
         * Sets the timeout for the channel instance to produce a readable result.<br/>
         * Note that this is just the initial configuration, since the output timeout can be
         * dynamically changed through the dedicated methods.
         *
         * @param timeout  the timeout.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
         */
        @Nonnull
        public Builder<TYPE> withReadTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

            return withReadTimeout(fromUnit(timeout, timeUnit));
        }

        /**
         * Sets the timeout for the channel instance to produce a readable result. A null value
         * means that it is up to the framework to choose a default one.<br/>
         * Note that this is just the initial configuration, since the output timeout can be
         * dynamically changed through the dedicated methods.
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
         * output channel. A null value means that it is up to the framework to choose a default
         * one.<br/>
         * Note that this is just the initial configuration, since the output timeout action can be
         * dynamically changed through the dedicated methods.
         *
         * @param actionType the action type.
         * @return this builder.
         */
        @Nonnull
        public Builder<TYPE> withReadTimeoutAction(@Nullable final TimeoutActionType actionType) {

            mTimeoutActionType = actionType;
            return this;
        }

        @Nonnull
        private ChannelConfiguration buildConfiguration() {

            return new ChannelConfiguration(mAsyncRunner, mReadTimeout, mTimeoutActionType,
                                            mChannelOrderType, mChannelMaxSize, mChannelTimeout,
                                            mLog, mLogLevel);
        }

        private void setConfiguration(@Nonnull final ChannelConfiguration configuration) {

            mAsyncRunner = configuration.mAsyncRunner;
            mReadTimeout = configuration.mReadTimeout;
            mTimeoutActionType = configuration.mTimeoutActionType;
            mChannelOrderType = configuration.mChannelOrderType;
            mChannelMaxSize = configuration.mChannelMaxSize;
            mChannelTimeout = configuration.mChannelTimeout;
            mLog = configuration.mLog;
            mLogLevel = configuration.mLogLevel;
        }
    }

    /**
     * Default configurable implementation.
     */
    private static class DefaultConfigurable implements Configurable<ChannelConfiguration> {

        @Nonnull
        public ChannelConfiguration setConfiguration(
                @Nonnull final ChannelConfiguration configuration) {

            return configuration;
        }
    }
}