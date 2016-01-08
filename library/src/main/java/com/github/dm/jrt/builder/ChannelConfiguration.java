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
package com.github.dm.jrt.builder;

import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.util.TimeDuration.fromUnit;

/**
 * Class storing the channel configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration.
 * <p/>
 * The configuration has an asynchronous runner associated.<br/>
 * The number of input data buffered in the channel can be limited in order to avoid excessive
 * memory consumption. In case the maximum number is reached when passing an input, the call blocks
 * until enough data are consumed or the specified  timeout elapses. In the latter case, a
 * {@link com.github.dm.jrt.channel.TimeoutException TimeoutException} will be thrown.<br/>
 * By default the timeout is set to 0 so to avoid unexpected deadlocks.<br/>
 * The order of input data is not guaranteed. Nevertheless, it is possible to force data to be
 * delivered in the same order as they are passed to the channels, at the cost of a slightly
 * increase in memory usage and computation.
 * <p/>
 * Created by davide-maestroni on 07/03/2015.
 */
public final class ChannelConfiguration {

    /**
     * Constant indicating the default value of an integer attribute.
     */
    public static final int DEFAULT = InvocationConfiguration.DEFAULT;

    private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

    /**
     * Empty configuration constant.<br/>The configuration has all the options set to their default.
     */
    public static final ChannelConfiguration DEFAULT_CONFIGURATION = builder().buildConfiguration();

    private final int mChannelLimit;

    private final TimeDuration mChannelMaxDelay;

    private final int mChannelMaxSize;

    private final OrderType mChannelOrderType;

    private final Log mLog;

    private final Level mLogLevel;

    private final TimeDuration mReadTimeout;

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
     * @param channelMaxDelay  the maximum delay to apply while waiting for an object to be passed
     *                         to the channel.
     * @param channelMaxSize   the maximum number of buffered data. Must be positive.
     * @param log              the log instance.
     * @param logLevel         the log level.
     */
    private ChannelConfiguration(@Nullable final Runner runner,
            @Nullable final TimeDuration readTimeout, @Nullable final TimeoutActionType actionType,
            @Nullable final OrderType channelOrderType, final int channelLimit,
            @Nullable final TimeDuration channelMaxDelay, final int channelMaxSize,
            @Nullable final Log log, @Nullable final Level logLevel) {

        mRunner = runner;
        mReadTimeout = readTimeout;
        mTimeoutActionType = actionType;
        mChannelOrderType = channelOrderType;
        mChannelLimit = channelLimit;
        mChannelMaxDelay = channelMaxDelay;
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
     * Returns a channel configuration builder initialized with this configuration.
     *
     * @return the builder.
     */
    @NotNull
    public Builder<ChannelConfiguration> builderFrom() {

        return builderFrom(this);
    }

    /**
     * Returns the limit of buffered data (DEFAULT by default) before starting applying a delay to
     * the feeding thread.
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the limit.
     */
    public int getChannelLimitOr(final int valueIfNotSet) {

        final int limit = mChannelLimit;
        return (limit != DEFAULT) ? limit : valueIfNotSet;
    }

    /**
     * Returns the maximum delay to apply while waiting for an object to be passed to the channel
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the delay.
     */
    public TimeDuration getChannelMaxDelayOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration maxDelay = mChannelMaxDelay;
        return (maxDelay != null) ? maxDelay : valueIfNotSet;
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
     * Returns the action to be taken if the timeout elapses before a readable output is available
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
     * Returns the timeout for the channel to produce a readable output (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getReadTimeoutOr(@Nullable final TimeDuration valueIfNotSet) {

        final TimeDuration readTimeout = mReadTimeout;
        return (readTimeout != null) ? readTimeout : valueIfNotSet;
    }

    /**
     * Returns the runner used for asynchronous inputs (null by default).
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
        int result = mChannelLimit;
        result = 31 * result + (mChannelMaxDelay != null ? mChannelMaxDelay.hashCode() : 0);
        result = 31 * result + mChannelMaxSize;
        result = 31 * result + (mChannelOrderType != null ? mChannelOrderType.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
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

        final ChannelConfiguration that = (ChannelConfiguration) o;

        if (mChannelLimit != that.mChannelLimit) {

            return false;
        }

        if (mChannelMaxSize != that.mChannelMaxSize) {

            return false;
        }

        if (mChannelMaxDelay != null ? !mChannelMaxDelay.equals(that.mChannelMaxDelay)
                : that.mChannelMaxDelay != null) {

            return false;
        }

        if (mChannelOrderType != that.mChannelOrderType) {

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

        if (mRunner != null ? !mRunner.equals(that.mRunner) : that.mRunner != null) {

            return false;
        }

        return mTimeoutActionType == that.mTimeoutActionType;
    }

    @Override
    public String toString() {

        // AUTO-GENERATED CODE
        return "ChannelConfiguration{" +
                "mChannelLimit=" + mChannelLimit +
                ", mChannelMaxDelay=" + mChannelMaxDelay +
                ", mChannelMaxSize=" + mChannelMaxSize +
                ", mChannelOrderType=" + mChannelOrderType +
                ", mLog=" + mLog +
                ", mLogLevel=" + mLogLevel +
                ", mReadTimeout=" + mReadTimeout +
                ", mRunner=" + mRunner +
                ", mTimeoutActionType=" + mTimeoutActionType +
                '}';
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
                                          .withInputOrder(getChannelOrderTypeOr(null))
                                          .withInputLimit(getChannelLimitOr(
                                                  InvocationConfiguration.DEFAULT))
                                          .withInputMaxDelay(getChannelMaxDelayOr(null))
                                          .withInputMaxSize(getChannelMaxSizeOr(
                                                  InvocationConfiguration.DEFAULT))
                                          .set();
    }

    /**
     * Converts this configuration into an invocation one by mapping the matching options.
     *
     * @return the invocation configuration.
     */
    @NotNull
    public InvocationConfiguration toInvocationConfiguration() {

        return InvocationConfiguration.builder()
                                      .withRunner(getRunnerOr(null))
                                      .withReadTimeout(getReadTimeoutOr(null))
                                      .withReadTimeoutAction(getReadTimeoutActionOr(null))
                                      .withLog(getLogOr(null))
                                      .withLogLevel(getLogLevelOr(null))
                                      .set();
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
                                          .withOutputOrder(getChannelOrderTypeOr(null))
                                          .withOutputLimit(getChannelLimitOr(
                                                  InvocationConfiguration.DEFAULT))
                                          .withOutputMaxDelay(getChannelMaxDelayOr(null))
                                          .withOutputMaxSize(getChannelMaxSizeOr(
                                                  InvocationConfiguration.DEFAULT))
                                          .set();
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
        TYPE setConfiguration(@NotNull ChannelConfiguration configuration);
    }

    /**
     * Builder of channel configurations.
     *
     * @param <TYPE> the configurable object type.
     */
    public static final class Builder<TYPE> {

        private final Configurable<? extends TYPE> mConfigurable;

        private int mChannelLimit;

        private TimeDuration mChannelMaxDelay;

        private int mChannelMaxSize;

        private OrderType mChannelOrderType;

        private Log mLog;

        private Level mLogLevel;

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
            mChannelMaxSize = DEFAULT;
        }

        /**
         * Constructor.
         *
         * @param configurable         the configurable instance.
         * @param initialConfiguration the initial configuration.
         */
        @SuppressWarnings("ConstantConditions")
        public Builder(@NotNull final Configurable<? extends TYPE> configurable,
                @NotNull final ChannelConfiguration initialConfiguration) {

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
        @NotNull
        public TYPE set() {

            return mConfigurable.setConfiguration(buildConfiguration());
        }

        /**
         * Applies the specified configuration to this builder. A null value means that all the
         * configuration options will be reset to their default, otherwise only the set options will
         * be applied.
         *
         * @param configuration the channel configuration.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> with(@Nullable final ChannelConfiguration configuration) {

            if (configuration == null) {

                setConfiguration(DEFAULT_CONFIGURATION);
                return this;
            }

            final Runner runner = configuration.mRunner;

            if (runner != null) {

                withRunner(runner);
            }

            final TimeDuration readTimeout = configuration.mReadTimeout;

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

            final TimeDuration channelTimeout = configuration.mChannelMaxDelay;

            if (channelTimeout != null) {

                withChannelMaxDelay(channelTimeout);
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
         * Sets the limit of data that the channel can retain before starting to slow down the
         * feeding thread. A {@link InvocationConfiguration#DEFAULT DEFAULT} value means that it is
         * up to the specific implementation to choose a default one.
         * <p/>
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

            if ((limit != DEFAULT) && (limit < 0)) {

                throw new IllegalArgumentException(
                        "the channel limit cannot be negative: " + limit);
            }

            mChannelLimit = limit;
            return this;
        }

        /**
         * Sets the maximum delay to apply to the feeding thread waiting for the channel to have
         * room for additional data.
         * <p/>
         * This configuration option should be used on conjunction with the channel limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay    the delay.
         * @param timeUnit the timeout time unit.
         * @return this builder.
         * @throws java.lang.IllegalArgumentException if the specified delay is negative.
         */
        @NotNull
        public Builder<TYPE> withChannelMaxDelay(final long delay,
                @NotNull final TimeUnit timeUnit) {

            return withChannelMaxDelay(fromUnit(delay, timeUnit));
        }

        /**
         * Sets the maximum delay to apply to the feeding thread waiting for the channel to have
         * room for additional data.
         * <p/>
         * This configuration option should be used on conjunction with the channel limit, or it
         * might have no effect on the invocation execution.
         *
         * @param delay the delay.
         * @return this builder.
         */
        @NotNull
        public Builder<TYPE> withChannelMaxDelay(@Nullable final TimeDuration delay) {

            mChannelMaxDelay = delay;
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

            if ((maxSize != DEFAULT) && (maxSize <= 0)) {

                throw new IllegalArgumentException(
                        "the channel buffer size cannot be 0 or negative: " + maxSize);
            }

            mChannelMaxSize = maxSize;
            return this;
        }

        /**
         * Sets the order in which data are collected from the channel. A null value means that it
         * is up to the specific implementation to choose a default one.<br/>
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
         * Sets the timeout for the channel instance to produce a readable output.<br/>
         * Note that this is just the initial configuration, since the output timeout can be
         * dynamically changed through the dedicated methods.
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
         * means that it is up to the specific implementation to choose a default one.<br/>
         * Note that this is just the initial configuration, since the output timeout can be
         * dynamically changed through the dedicated methods.
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
         * Sets the action to be taken if the timeout elapses before an output can be read from the
         * output channel. A null value means that it is up to the specific implementation to choose
         * a default one.<br/>
         * Note that this is just the initial configuration, since the output timeout action can be
         * dynamically changed through the dedicated methods.
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
                                            mChannelOrderType, mChannelLimit, mChannelMaxDelay,
                                            mChannelMaxSize, mLog, mLogLevel);
        }

        private void setConfiguration(@NotNull final ChannelConfiguration configuration) {

            mRunner = configuration.mRunner;
            mReadTimeout = configuration.mReadTimeout;
            mTimeoutActionType = configuration.mTimeoutActionType;
            mChannelOrderType = configuration.mChannelOrderType;
            mChannelLimit = configuration.mChannelLimit;
            mChannelMaxDelay = configuration.mChannelMaxDelay;
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
        public ChannelConfiguration setConfiguration(
                @NotNull final ChannelConfiguration configuration) {

            return configuration;
        }
    }
}
