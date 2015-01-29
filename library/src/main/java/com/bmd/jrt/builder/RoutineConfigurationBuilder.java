/**
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
package com.bmd.jrt.builder;

import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routine configurations.
 * <p/>
 * Created by davide on 11/15/14.
 */
public class RoutineConfigurationBuilder implements RoutineChannelBuilder {

    private TimeDuration mAvailTimeout = null;

    private int mInputMaxSize = RoutineConfiguration.DEFAULT;

    private DataOrder mInputOrder = DataOrder.DEFAULT;

    private TimeDuration mInputTimeout = null;

    private Log mLog = null;

    private LogLevel mLogLevel = LogLevel.DEFAULT;

    private int mMaxRetained = RoutineConfiguration.DEFAULT;

    private int mMaxRunning = RoutineConfiguration.DEFAULT;

    private int mOutputMaxSize = RoutineConfiguration.DEFAULT;

    private DataOrder mOutputOrder = DataOrder.DEFAULT;

    private TimeDuration mOutputTimeout = null;

    private TimeDuration mReadTimeout = null;

    private Runner mRunner = null;

    private RunnerType mRunnerType = RunnerType.DEFAULT;

    private TimeoutAction mTimeoutAction = TimeoutAction.DEFAULT;

    /**
     * Constructor.
     */
    public RoutineConfigurationBuilder() {

    }

    /**
     * Constructor.
     *
     * @param initialConfiguration the initial configuration.
     * @throws NullPointerException if the specified configuration instance is null.
     */
    public RoutineConfigurationBuilder(@Nonnull final RoutineConfiguration initialConfiguration) {

        mRunner = initialConfiguration.getRunnerOr(mRunner);
        mRunnerType = initialConfiguration.getSyncRunnerOr(mRunnerType);
        mMaxRunning = initialConfiguration.getMaxRunningOr(mMaxRunning);
        mMaxRetained = initialConfiguration.getMaxRetainedOr(mMaxRetained);
        mAvailTimeout = initialConfiguration.getAvailTimeoutOr(mAvailTimeout);
        mReadTimeout = initialConfiguration.getReadTimeoutOr(mReadTimeout);
        mTimeoutAction = initialConfiguration.getReadTimeoutActionOr(mTimeoutAction);
        mInputOrder = initialConfiguration.getInputOrderOr(mInputOrder);
        mInputMaxSize = initialConfiguration.getInputSizeOr(mInputMaxSize);
        mInputTimeout = initialConfiguration.getInputTimeoutOr(mInputTimeout);
        mOutputOrder = initialConfiguration.getOutputOrderOr(mOutputOrder);
        mOutputMaxSize = initialConfiguration.getOutputSizeOr(mOutputMaxSize);
        mOutputTimeout = initialConfiguration.getOutputTimeoutOr(mOutputTimeout);
        mLog = initialConfiguration.getLogOr(mLog);
        mLogLevel = initialConfiguration.getLogLevelOr(mLogLevel);
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        final Runner runner = configuration.getRunnerOr(null);

        if (runner != null) {

            runBy(runner);
        }

        final RunnerType syncRunner = configuration.getSyncRunnerOr(RunnerType.DEFAULT);

        if (syncRunner != RunnerType.DEFAULT) {

            syncRunner(syncRunner);
        }

        final int maxRunning = configuration.getMaxRunningOr(RoutineConfiguration.DEFAULT);

        if (maxRunning != RoutineConfiguration.DEFAULT) {

            maxRunning(maxRunning);
        }

        final int maxRetained = configuration.getMaxRetainedOr(RoutineConfiguration.DEFAULT);

        if (maxRetained != RoutineConfiguration.DEFAULT) {

            maxRetained(maxRetained);
        }

        final TimeDuration availTimeout = configuration.getAvailTimeoutOr(null);

        if (availTimeout != null) {

            availableTimeout(availTimeout);
        }

        final TimeDuration readTimeout = configuration.getReadTimeoutOr(null);

        if (readTimeout != null) {

            readTimeout(readTimeout);
        }

        final TimeoutAction timeoutAction =
                configuration.getReadTimeoutActionOr(TimeoutAction.DEFAULT);

        if (timeoutAction != TimeoutAction.DEFAULT) {

            onReadTimeout(timeoutAction);
        }

        final DataOrder inputOrder = configuration.getInputOrderOr(DataOrder.DEFAULT);

        if (inputOrder != DataOrder.DEFAULT) {

            inputOrder(inputOrder);
        }

        final int inputSize = configuration.getInputSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            inputSize(inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            inputTimeout(inputTimeout);
        }

        final DataOrder outputOrder = configuration.getOutputOrderOr(DataOrder.DEFAULT);

        if (outputOrder != DataOrder.DEFAULT) {

            outputOrder(outputOrder);
        }

        final int outputSize = configuration.getOutputSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            outputSize(outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            outputTimeout(outputTimeout);
        }

        final Log log = configuration.getLogOr(null);

        if (log != null) {

            loggedWith(log);
        }

        final LogLevel logLevel = configuration.getLogLevelOr(LogLevel.DEFAULT);

        if (logLevel != LogLevel.DEFAULT) {

            logLevel(logLevel);
        }

        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return availableTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder availableTimeout(@Nullable final TimeDuration timeout) {

        mAvailTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder logLevel(@Nonnull final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        mLogLevel = level;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder loggedWith(@Nullable final Log log) {

        mLog = log;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder maxRetained(final int maxRetainedInstances) {

        if ((maxRetainedInstances != RoutineConfiguration.DEFAULT) && (maxRetainedInstances < 0)) {

            throw new IllegalArgumentException(
                    "the maximum number of retained instances cannot be negative");
        }

        mMaxRetained = maxRetainedInstances;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder maxRunning(final int maxRunningInstances) {

        if ((maxRunningInstances != RoutineConfiguration.DEFAULT) && (maxRunningInstances < 1)) {

            throw new IllegalArgumentException(
                    "the maximum number of concurrently running instances cannot be less than 1");
        }

        mMaxRunning = maxRunningInstances;
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder onReadTimeout(@Nonnull final TimeoutAction action) {

        if (action == null) {

            throw new NullPointerException("the result timeout action must not be null");
        }

        mTimeoutAction = action;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder readTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return readTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder readTimeout(@Nullable final TimeDuration timeout) {

        mReadTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder runBy(@Nullable final Runner runner) {

        mRunner = runner;
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder syncRunner(@Nonnull final RunnerType type) {

        if (type == null) {

            throw new NullPointerException("the synchronous runner type must not be null");
        }

        mRunnerType = type;
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder inputOrder(@Nonnull final DataOrder order) {

        if (order == null) {

            throw new NullPointerException("the input order type must not be null");
        }

        mInputOrder = order;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder inputSize(final int inputMaxSize) {

        if ((inputMaxSize != RoutineConfiguration.DEFAULT) && (inputMaxSize <= 0)) {

            throw new IllegalArgumentException("the input buffer size cannot be 0 or negative");
        }

        mInputMaxSize = inputMaxSize;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder inputTimeout(@Nullable final TimeDuration timeout) {

        mInputTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder inputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return inputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder outputOrder(@Nonnull final DataOrder order) {

        if (order == null) {

            throw new NullPointerException("the output order type must not be null");
        }

        mOutputOrder = order;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder outputSize(final int outputMaxSize) {

        if ((outputMaxSize != RoutineConfiguration.DEFAULT) && (outputMaxSize <= 0)) {

            throw new IllegalArgumentException("the output buffer size cannot be 0 or negative");
        }

        mOutputMaxSize = outputMaxSize;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder outputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return outputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder outputTimeout(@Nullable final TimeDuration timeout) {

        mOutputTimeout = timeout;
        return this;
    }

    /**
     * Builds and return the configuration instance.
     *
     * @return the routine configuration instance.
     */
    @Nonnull
    public RoutineConfiguration buildConfiguration() {

        return new RoutineConfiguration(mRunner, mRunnerType, mMaxRunning, mMaxRetained,
                                        mAvailTimeout, mReadTimeout, mTimeoutAction, mInputMaxSize,
                                        mInputTimeout, mInputOrder, mOutputMaxSize, mOutputTimeout,
                                        mOutputOrder, mLog, mLogLevel);
    }
}
