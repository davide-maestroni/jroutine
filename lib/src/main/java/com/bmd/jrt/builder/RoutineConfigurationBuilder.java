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

import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routine configurations.
 * <p/>
 * Created by davide on 11/15/14.
 */
public class RoutineConfigurationBuilder implements RoutineBuilder {

    private TimeDuration mAvailTimeout = null;

    private int mInputMaxSize = RoutineConfiguration.NOT_SET;

    private ChannelDataOrder mInputOrder = null;

    private TimeDuration mInputTimeout = null;

    private Log mLog = null;

    private LogLevel mLogLevel = null;

    private int mMaxRetained = RoutineConfiguration.NOT_SET;

    private int mMaxRunning = RoutineConfiguration.NOT_SET;

    private int mOutputMaxSize = RoutineConfiguration.NOT_SET;

    private ChannelDataOrder mOutputOrder = null;

    private TimeDuration mOutputTimeout = null;

    private Runner mRunner = null;

    private SyncRunnerType mRunnerType = null;

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
    public RoutineConfigurationBuilder(final RoutineConfiguration initialConfiguration) {

        mRunner = initialConfiguration.getRunner(mRunner);
        mRunnerType = initialConfiguration.getSyncRunner(mRunnerType);
        mMaxRunning = initialConfiguration.getMaxRunning(mMaxRunning);
        mMaxRetained = initialConfiguration.getMaxRetained(mMaxRetained);
        mAvailTimeout = initialConfiguration.getAvailTimeout(mAvailTimeout);
        mInputMaxSize = initialConfiguration.getInputMaxSize(mInputMaxSize);
        mInputTimeout = initialConfiguration.getInputTimeout(mInputTimeout);
        mInputOrder = initialConfiguration.getInputOrder(mInputOrder);
        mOutputMaxSize = initialConfiguration.getOutputMaxSize(mOutputMaxSize);
        mOutputTimeout = initialConfiguration.getOutputTimeout(mOutputTimeout);
        mOutputOrder = initialConfiguration.getOutputOrder(mOutputOrder);
        mLog = initialConfiguration.getLog(mLog);
        mLogLevel = initialConfiguration.getLogLevel(mLogLevel);
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return availableTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder availableTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mAvailTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder inputMaxSize(final int inputMaxSize) {

        if (inputMaxSize <= 0) {

            throw new IllegalArgumentException("the buffer size cannot be 0 or negative");
        }

        mInputMaxSize = inputMaxSize;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder inputOrder(@Nonnull final ChannelDataOrder order) {

        if (order == null) {

            throw new NullPointerException("the input order type must not be null");
        }

        mInputOrder = order;

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
    public RoutineConfigurationBuilder inputTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mInputTimeout = timeout;

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
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder loggedWith(@Nonnull final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;

        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder maxRetained(final int maxRetainedInstances) {

        if (maxRetainedInstances < 0) {

            throw new IllegalArgumentException(
                    "the maximum number of retained instances cannot be negative");
        }

        mMaxRetained = maxRetainedInstances;

        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder maxRunning(final int maxRunningInstances) {

        if (maxRunningInstances < 1) {

            throw new IllegalArgumentException(
                    "the maximum number of concurrently running instances cannot be less than 1");
        }

        mMaxRunning = maxRunningInstances;

        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder outputMaxSize(final int outputMaxSize) {

        if (outputMaxSize <= 0) {

            throw new IllegalArgumentException("the buffer size cannot be 0 or negative");
        }

        mOutputMaxSize = outputMaxSize;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder outputOrder(@Nonnull final ChannelDataOrder order) {

        if (order == null) {

            throw new NullPointerException("the output order type must not be null");
        }

        mOutputOrder = order;

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
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder outputTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mOutputTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder runBy(@Nonnull final Runner runner) {

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mRunner = runner;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public RoutineConfigurationBuilder syncRunner(@Nonnull final SyncRunnerType type) {

        if (type == null) {

            throw new NullPointerException("the synchronous runner type must not be null");
        }

        mRunnerType = type;

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
                                        mAvailTimeout, mInputMaxSize, mInputTimeout, mInputOrder,
                                        mOutputMaxSize, mOutputTimeout, mOutputOrder, mLog,
                                        mLogLevel);
    }
}
