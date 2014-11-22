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

import com.bmd.jrt.builder.RoutineBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.builder.RoutineBuilder.DEFAULT;

/**
 * Class storing the routine configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * Created by davide on 11/15/14.
 */
public class RoutineConfiguration {

    private final TimeDuration mAvailTimeout;

    private final int mInputMaxSize;

    private final DataOrder mInputOrder;

    private final TimeDuration mInputTimeout;

    private final Log mLog;

    private final LogLevel mLogLevel;

    private final int mMaxRetained;

    private final int mMaxRunning;

    private final int mOutputMaxSize;

    private final DataOrder mOutputOrder;

    private final TimeDuration mOutputTimeout;

    private final Runner mRunner;

    private final RunnerType mRunnerType;

    /**
     * Constructor.
     *
     * @param runner        the runner used for asynchronous invocations.
     * @param runnerType    the type of the runner used for synchronous invocations.
     * @param maxRunning    the maximum number of parallel running invocations. Must be positive.
     * @param maxRetained   the maximum number of retained invocation instances. Must be 0 or a
     *                      positive number.
     * @param availTimeout  the maximum timeout while waiting for an invocation instance to be
     *                      available.
     * @param inputMaxSize  the maximum number of buffered input data. Must be positive.
     * @param inputTimeout  the maximum timeout while waiting for an input to be passed to the
     *                      input channel.
     * @param inputOrder    whether the input data are forced to be delivered in insertion order.
     * @param outputMaxSize the maximum number of buffered output data. Must be positive.
     * @param outputTimeout the maximum timeout while waiting for an output to be passed to the
     *                      result channel.
     * @param outputOrder   whether the output data are forced to be delivered in insertion order.
     * @param log           the log instance.
     * @param logLevel      the log level.
     */
    @SuppressWarnings("ConstantConditions")
    RoutineConfiguration(@Nullable final Runner runner, @Nonnull final RunnerType runnerType,
            final int maxRunning, final int maxRetained, @Nullable final TimeDuration availTimeout,
            final int inputMaxSize, @Nullable final TimeDuration inputTimeout,
            final @Nonnull DataOrder inputOrder, final int outputMaxSize,
            @Nullable final TimeDuration outputTimeout, @Nonnull final DataOrder outputOrder,
            @Nullable final Log log, @Nonnull final LogLevel logLevel) {

        mRunner = runner;
        mRunnerType = runnerType;
        mMaxRunning = maxRunning;
        mMaxRetained = maxRetained;
        mAvailTimeout = availTimeout;
        mInputMaxSize = inputMaxSize;
        mInputTimeout = inputTimeout;
        mInputOrder = inputOrder;
        mOutputMaxSize = outputMaxSize;
        mOutputTimeout = outputTimeout;
        mOutputOrder = outputOrder;
        mLog = log;
        mLogLevel = logLevel;
    }

    /**
     * Returns the maximum timeout while waiting for an invocation instance to be available (null
     * by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getAvailTimeout(final TimeDuration valueIfNotSet) {

        final TimeDuration availTimeout = mAvailTimeout;

        return (availTimeout != null) ? availTimeout : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered input data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getInputMaxSize(final int valueIfNotSet) {

        final int inputMaxSize = mInputMaxSize;

        return (inputMaxSize != DEFAULT) ? inputMaxSize : valueIfNotSet;
    }

    /**
     * Returns the input data order (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public DataOrder getInputOrder(final DataOrder valueIfNotSet) {

        final DataOrder orderedInput = mInputOrder;

        return (orderedInput != DataOrder.DEFAULT) ? orderedInput : valueIfNotSet;
    }

    /**
     * Returns the maximum timeout while waiting for an input to be passed to the input channel
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getInputTimeout(final TimeDuration valueIfNotSet) {

        final TimeDuration inputTimeout = mInputTimeout;

        return (inputTimeout != null) ? inputTimeout : valueIfNotSet;
    }

    /**
     * Returns the log instance (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the log instance.
     */
    public Log getLog(final Log valueIfNotSet) {

        final Log log = mLog;

        return (log != null) ? log : valueIfNotSet;
    }

    /**
     * Returns the log level (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the log level.
     */
    public LogLevel getLogLevel(final LogLevel valueIfNotSet) {

        final LogLevel logLevel = mLogLevel;

        return (logLevel != LogLevel.DEFAULT) ? logLevel : valueIfNotSet;
    }

    /**
     * Returns the maximum number of retained invocation instances (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum number.
     */
    public int getMaxRetained(final int valueIfNotSet) {

        final int maxRetained = mMaxRetained;

        return (maxRetained != DEFAULT) ? maxRetained : valueIfNotSet;
    }

    /**
     * Returns the maximum number of parallel running invocations (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum number.
     */
    public int getMaxRunning(final int valueIfNotSet) {

        final int maxRunning = mMaxRunning;

        return (maxRunning != DEFAULT) ? maxRunning : valueIfNotSet;
    }

    /**
     * Returns the maximum number of buffered output data (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the maximum size.
     */
    public int getOutputMaxSize(final int valueIfNotSet) {

        final int outputMaxSize = mOutputMaxSize;

        return (outputMaxSize != DEFAULT) ? outputMaxSize : valueIfNotSet;
    }

    /**
     * Returns the output data order (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the order type.
     */
    public DataOrder getOutputOrder(final DataOrder valueIfNotSet) {

        final DataOrder orderedOutput = mOutputOrder;

        return (orderedOutput != DataOrder.DEFAULT) ? orderedOutput : valueIfNotSet;
    }

    /**
     * Returns the maximum timeout while waiting for an output to be passed to the result channel
     * (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the timeout.
     */
    public TimeDuration getOutputTimeout(final TimeDuration valueIfNotSet) {

        final TimeDuration outputTimeout = mOutputTimeout;

        return (outputTimeout != null) ? outputTimeout : valueIfNotSet;
    }

    /**
     * Returns the runner used for asynchronous invocations (null by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the runner instance.
     */
    public Runner getRunner(final Runner valueIfNotSet) {

        final Runner runner = mRunner;

        return (runner != null) ? runner : valueIfNotSet;
    }

    /**
     * Returns the type of the runner used for synchronous invocations (DEFAULT by default).
     *
     * @param valueIfNotSet the default value if none was set.
     * @return the runner type.
     */
    public RunnerType getSyncRunner(final RunnerType valueIfNotSet) {

        final RunnerType runnerType = mRunnerType;

        return (runnerType != RunnerType.DEFAULT) ? runnerType : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mAvailTimeout != null ? mAvailTimeout.hashCode() : 0;
        result = 31 * result + mInputMaxSize;
        result = 31 * result + mInputOrder.hashCode();
        result = 31 * result + (mInputTimeout != null ? mInputTimeout.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + mLogLevel.hashCode();
        result = 31 * result + mMaxRetained;
        result = 31 * result + mMaxRunning;
        result = 31 * result + mOutputMaxSize;
        result = 31 * result + mOutputOrder.hashCode();
        result = 31 * result + (mOutputTimeout != null ? mOutputTimeout.hashCode() : 0);
        result = 31 * result + (mRunner != null ? mRunner.hashCode() : 0);
        result = 31 * result + mRunnerType.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {

        // auto-generated code
        if (this == o) {

            return true;
        }

        if (!(o instanceof RoutineConfiguration)) {

            return false;
        }

        final RoutineConfiguration that = (RoutineConfiguration) o;

        return mInputMaxSize == that.mInputMaxSize && mMaxRetained == that.mMaxRetained
                && mMaxRunning == that.mMaxRunning && mOutputMaxSize == that.mOutputMaxSize && !(
                mAvailTimeout != null ? !mAvailTimeout.equals(that.mAvailTimeout)
                        : that.mAvailTimeout != null) && mInputOrder == that.mInputOrder && !(
                mInputTimeout != null ? !mInputTimeout.equals(that.mInputTimeout)
                        : that.mInputTimeout != null) && !(mLog != null ? !mLog.equals(that.mLog)
                : that.mLog != null) && mLogLevel == that.mLogLevel
                && mOutputOrder == that.mOutputOrder && !(mOutputTimeout != null
                ? !mOutputTimeout.equals(that.mOutputTimeout) : that.mOutputTimeout != null) && !(
                mRunner != null ? !mRunner.equals(that.mRunner) : that.mRunner != null)
                && mRunnerType == that.mRunnerType;
    }
}
