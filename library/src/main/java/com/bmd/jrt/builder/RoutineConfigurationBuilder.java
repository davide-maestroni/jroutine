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

    private int mCoreInvocations = RoutineConfiguration.DEFAULT;

    private int mInputMaxSize = RoutineConfiguration.DEFAULT;

    private OrderBy mInputOrder = null;

    private TimeDuration mInputTimeout = null;

    private Log mLog = null;

    private LogLevel mLogLevel = null;

    private int mMaxInvocations = RoutineConfiguration.DEFAULT;

    private int mOutputMaxSize = RoutineConfiguration.DEFAULT;

    private OrderBy mOutputOrder = null;

    private TimeDuration mOutputTimeout = null;

    private TimeDuration mReadTimeout = null;

    private Runner mRunner = null;

    private RunnerType mRunnerType = null;

    private TimeoutAction mTimeoutAction = null;

    /**
     * Constructor.
     */
    public RoutineConfigurationBuilder() {

    }

    /**
     * Constructor.
     *
     * @param initialConfiguration the initial configuration.
     * @throws java.lang.NullPointerException if the specified configuration instance is null.
     */
    public RoutineConfigurationBuilder(@Nonnull final RoutineConfiguration initialConfiguration) {

        mRunner = initialConfiguration.getRunnerOr(mRunner);
        mRunnerType = initialConfiguration.getSyncRunnerOr(mRunnerType);
        mMaxInvocations = initialConfiguration.getMaxInvocationsOr(mMaxInvocations);
        mCoreInvocations = initialConfiguration.getCoreInvocationsOr(mCoreInvocations);
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

            withRunner(runner);
        }

        final RunnerType syncRunner = configuration.getSyncRunnerOr(null);

        if (syncRunner != null) {

            withSyncRunner(syncRunner);
        }

        final int maxInvocations = configuration.getMaxInvocationsOr(RoutineConfiguration.DEFAULT);

        if (maxInvocations != RoutineConfiguration.DEFAULT) {

            withMaxInvocations(maxInvocations);
        }

        final int coreInvocations =
                configuration.getCoreInvocationsOr(RoutineConfiguration.DEFAULT);

        if (coreInvocations != RoutineConfiguration.DEFAULT) {

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

        final TimeoutAction timeoutAction = configuration.getReadTimeoutActionOr(null);

        if (timeoutAction != null) {

            onReadTimeout(timeoutAction);
        }

        final OrderBy inputOrder = configuration.getInputOrderOr(null);

        if (inputOrder != null) {

            withInputOrder(inputOrder);
        }

        final int inputSize = configuration.getInputSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            withInputSize(inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            withInputTimeout(inputTimeout);
        }

        final OrderBy outputOrder = configuration.getOutputOrderOr(null);

        if (outputOrder != null) {

            withOutputOrder(outputOrder);
        }

        final int outputSize = configuration.getOutputSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            withOutputSize(outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            withOutputTimeout(outputTimeout);
        }

        final Log log = configuration.getLogOr(null);

        if (log != null) {

            withLog(log);
        }

        final LogLevel logLevel = configuration.getLogLevelOr(null);

        if (logLevel != null) {

            withLogLevel(logLevel);
        }

        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder onReadTimeout(@Nullable final TimeoutAction action) {

        mTimeoutAction = action;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withAvailableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return withAvailableTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withAvailableTimeout(@Nullable final TimeDuration timeout) {

        mAvailTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withCoreInvocations(final int coreInvocations) {

        if ((coreInvocations != RoutineConfiguration.DEFAULT) && (coreInvocations < 0)) {

            throw new IllegalArgumentException(
                    "the maximum number of retained instances cannot be negative");
        }

        mCoreInvocations = coreInvocations;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withLog(@Nullable final Log log) {

        mLog = log;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withLogLevel(@Nullable final LogLevel level) {

        mLogLevel = level;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withMaxInvocations(final int maxInvocations) {

        if ((maxInvocations != RoutineConfiguration.DEFAULT) && (maxInvocations < 1)) {

            throw new IllegalArgumentException(
                    "the maximum number of concurrently running instances cannot be less than 1");
        }

        mMaxInvocations = maxInvocations;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withReadTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return withReadTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withReadTimeout(@Nullable final TimeDuration timeout) {

        mReadTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withRunner(@Nullable final Runner runner) {

        mRunner = runner;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withSyncRunner(@Nullable final RunnerType type) {

        mRunnerType = type;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withInputOrder(@Nullable final OrderBy order) {

        mInputOrder = order;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withInputSize(final int inputMaxSize) {

        if ((inputMaxSize != RoutineConfiguration.DEFAULT) && (inputMaxSize <= 0)) {

            throw new IllegalArgumentException("the input buffer size cannot be 0 or negative");
        }

        mInputMaxSize = inputMaxSize;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withInputTimeout(@Nullable final TimeDuration timeout) {

        mInputTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withInputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return withInputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withOutputOrder(@Nullable final OrderBy order) {

        mOutputOrder = order;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withOutputSize(final int outputMaxSize) {

        if ((outputMaxSize != RoutineConfiguration.DEFAULT) && (outputMaxSize <= 0)) {

            throw new IllegalArgumentException("the output buffer size cannot be 0 or negative");
        }

        mOutputMaxSize = outputMaxSize;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withOutputTimeout(@Nullable final TimeDuration timeout) {

        mOutputTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfigurationBuilder withOutputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return withOutputTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Builds and return the configuration instance.
     *
     * @return the routine configuration instance.
     */
    @Nonnull
    public RoutineConfiguration buildConfiguration() {

        return new RoutineConfiguration(mRunner, mRunnerType, mMaxInvocations, mCoreInvocations,
                                        mAvailTimeout, mReadTimeout, mTimeoutAction, mInputMaxSize,
                                        mInputTimeout, mInputOrder, mOutputMaxSize, mOutputTimeout,
                                        mOutputOrder, mLog, mLogLevel);
    }
}
