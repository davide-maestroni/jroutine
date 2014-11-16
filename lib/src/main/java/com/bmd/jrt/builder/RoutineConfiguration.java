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

import javax.annotation.Nullable;

/**
 * Class storing the routine configuration.
 * <p/>
 * Each instance is immutable, thus, in order to modify a configuration parameter, a new builder
 * must be created starting from the specific configuration instance.
 * <p/>
 * Created by davide on 11/15/14.
 */
public class RoutineConfiguration {

    public static final int NOT_SET = Integer.MIN_VALUE;

    private final TimeDuration mAvailTimeout;

    private final TimeDuration mInputTimeout;

    private final Boolean mIsSequential;

    private final Log mLog;

    private final LogLevel mLogLevel;

    private final int mMaxInputSize;

    private final int mMaxOutputSize;

    private final int mMaxRetained;

    private final int mMaxRunning;

    private final Boolean mOrderedInput;

    private final Boolean mOrderedOutput;

    private final TimeDuration mOutputTimeout;

    private final Runner mRunner;

    /**
     * Constructor.
     *
     * @param runner        the runner used for asynchronous invocations.
     * @param isSequential  whether the sequential runner is used for synchronous invocations.
     * @param maxRunning    the maximum number of parallel running invocations. Must be positive.
     * @param maxRetained   the maximum number of retained invocation instances. Must be 0 or a
     *                      positive number.
     * @param availTimeout  the maximum timeout while waiting for an invocation instance to be
     *                      available.
     * @param maxInputSize  the maximum number of buffered input data. Must be positive.
     * @param inputTimeout  the maximum timeout while waiting for an input to be passed to the
     *                      input channel.
     * @param orderedInput  whether the input data are forced to be delivered in insertion order.
     * @param maxOutputSize the maximum number of buffered output data. Must be positive.
     * @param outputTimeout the maximum timeout while waiting for an output to be passed to the
     *                      result channel.
     * @param orderedOutput whether the output data are forced to be delivered in insertion order.
     * @param log           the log instance.
     * @param logLevel      the log level.
     */
    RoutineConfiguration(@Nullable final Runner runner, @Nullable final Boolean isSequential,
            final int maxRunning, final int maxRetained, @Nullable final TimeDuration availTimeout,
            final int maxInputSize, @Nullable final TimeDuration inputTimeout,
            final @Nullable Boolean orderedInput, final int maxOutputSize,
            @Nullable final TimeDuration outputTimeout, @Nullable final Boolean orderedOutput,
            @Nullable final Log log, @Nullable final LogLevel logLevel) {

        mRunner = runner;
        mIsSequential = isSequential;
        mMaxRunning = maxRunning;
        mMaxRetained = maxRetained;
        mAvailTimeout = availTimeout;
        mMaxInputSize = maxInputSize;
        mInputTimeout = inputTimeout;
        mOrderedInput = orderedInput;
        mMaxOutputSize = maxOutputSize;
        mOutputTimeout = outputTimeout;
        mOrderedOutput = orderedOutput;
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

    public TimeDuration getInputTimeout(final TimeDuration valueIfNotSet) {

        final TimeDuration inputTimeout = mInputTimeout;

        return (inputTimeout != null) ? inputTimeout : valueIfNotSet;
    }

    public Boolean getIsSequential(final Boolean valueIfNotSet) {

        final Boolean isSequential = mIsSequential;

        return (isSequential != null) ? isSequential : valueIfNotSet;
    }

    public Log getLog(final Log valueIfNotSet) {

        final Log log = mLog;

        return (log != null) ? log : valueIfNotSet;
    }

    public LogLevel getLogLevel(final LogLevel valueIfNotSet) {

        final LogLevel logLevel = mLogLevel;

        return (logLevel != null) ? logLevel : valueIfNotSet;
    }

    public int getMaxInputSize(final int valueIfNotSet) {

        final int maxInputSize = mMaxInputSize;

        return (maxInputSize != NOT_SET) ? maxInputSize : valueIfNotSet;
    }

    public int getMaxOutputSize(final int valueIfNotSet) {

        final int maxOutputSize = mMaxOutputSize;

        return (maxOutputSize != NOT_SET) ? maxOutputSize : valueIfNotSet;
    }

    public int getMaxRetained(final int valueIfNotSet) {

        final int maxRetained = mMaxRetained;

        return (maxRetained != NOT_SET) ? maxRetained : valueIfNotSet;
    }

    public int getMaxRunning(final int valueIfNotSet) {

        final int maxRunning = mMaxRunning;

        return (maxRunning != NOT_SET) ? maxRunning : valueIfNotSet;
    }

    public Boolean getOrderedInput(final Boolean valueIfNotSet) {

        final Boolean orderedInput = mOrderedInput;

        return (orderedInput != null) ? orderedInput : valueIfNotSet;
    }

    public Boolean getOrderedOutput(final Boolean valueIfNotSet) {

        final Boolean orderedOutput = mOrderedOutput;

        return (orderedOutput != null) ? orderedOutput : valueIfNotSet;
    }

    public TimeDuration getOutputTimeout(final TimeDuration valueIfNotSet) {

        final TimeDuration outputTimeout = mOutputTimeout;

        return (outputTimeout != null) ? outputTimeout : valueIfNotSet;
    }

    public Runner getRunner(final Runner valueIfNotSet) {

        final Runner runner = mRunner;

        return (runner != null) ? runner : valueIfNotSet;
    }

    @Override
    public int hashCode() {

        // auto-generated code
        int result = mAvailTimeout != null ? mAvailTimeout.hashCode() : 0;
        result = 31 * result + (mInputTimeout != null ? mInputTimeout.hashCode() : 0);
        result = 31 * result + (mIsSequential != null ? mIsSequential.hashCode() : 0);
        result = 31 * result + (mLog != null ? mLog.hashCode() : 0);
        result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
        result = 31 * result + mMaxInputSize;
        result = 31 * result + mMaxOutputSize;
        result = 31 * result + mMaxRetained;
        result = 31 * result + mMaxRunning;
        result = 31 * result + (mOrderedInput != null ? mOrderedInput.hashCode() : 0);
        result = 31 * result + (mOrderedOutput != null ? mOrderedOutput.hashCode() : 0);
        result = 31 * result + (mOutputTimeout != null ? mOutputTimeout.hashCode() : 0);
        result = 31 * result + (mRunner != null ? mRunner.hashCode() : 0);
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

        return mMaxInputSize == that.mMaxInputSize && mMaxOutputSize == that.mMaxOutputSize
                && mMaxRetained == that.mMaxRetained && mMaxRunning == that.mMaxRunning && !(
                mAvailTimeout != null ? !mAvailTimeout.equals(that.mAvailTimeout)
                        : that.mAvailTimeout != null) && !(mInputTimeout != null
                ? !mInputTimeout.equals(that.mInputTimeout) : that.mInputTimeout != null) && !(
                mIsSequential != null ? !mIsSequential.equals(that.mIsSequential)
                        : that.mIsSequential != null) && !(mLog != null ? !mLog.equals(that.mLog)
                : that.mLog != null) && mLogLevel == that.mLogLevel && !(mOrderedInput != null
                ? !mOrderedInput.equals(that.mOrderedInput) : that.mOrderedInput != null) && !(
                mOrderedOutput != null ? !mOrderedOutput.equals(that.mOrderedOutput)
                        : that.mOrderedOutput != null) && !(mOutputTimeout != null ? !mOutputTimeout
                .equals(that.mOutputTimeout) : that.mOutputTimeout != null) && !(mRunner != null
                ? !mRunner.equals(that.mRunner) : that.mRunner != null);
    }
}
