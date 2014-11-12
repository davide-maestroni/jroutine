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
package com.bmd.jrt.routine;

import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of I/O channel objects.
 * <p/>
 * Created by davide on 10/25/14.
 */
public class IOChannelBuilder {

    private TimeDuration mInputTimeout;

    private boolean mIsOrdered;

    private Log mLog = Logger.getDefaultLog();

    private LogLevel mLogLevel = Logger.getDefaultLogLevel();

    private TimeDuration mOutputTimeout;

    /**
     * Avoid direct instantiation.
     */
    IOChannelBuilder() {

    }

    /**
     * Builds and returns the channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    public <T> IOChannel<T> buildChannel() {

        return new DefaultIOChannel<T>(mIsOrdered, mLog, mLogLevel);
    }

    /**
     * @param timeout
     * @param timeUnit
     * @return
     */
    @Nonnull
    public IOChannelBuilder inputBufferTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return inputBufferTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * @param timeout
     * @return
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public IOChannelBuilder inputBufferTimeout(final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mInputTimeout = timeout;

        return this;
    }

    /**
     * Sets the log level.
     *
     * @param level the log level.
     * @return this builder.
     * @throws NullPointerException if the log level is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public IOChannelBuilder logLevel(@Nonnull final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        mLogLevel = level;

        return this;
    }

    /**
     * Sets the log instance.
     *
     * @param log the log instance.
     * @return this builder.
     * @throws NullPointerException if the log is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public IOChannelBuilder loggedWith(@Nonnull final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;

        return this;
    }

    /**
     * @param maxInputBufferSize
     * @return
     */
    @Nonnull
    public IOChannelBuilder maxBufferedInput(final int maxInputBufferSize) {

        //TODO

        return this;
    }

    /**
     * @param maxOutputBufferSize
     * @return
     */
    @Nonnull
    public IOChannelBuilder maxBufferedOutput(final int maxOutputBufferSize) {

        //TODO

        return this;
    }

    /**
     * Forces the inputs to be ordered as they are passed to the input channel,
     * independently from the source or the input delay.
     *
     * @return this builder.
     */
    @Nonnull
    public IOChannelBuilder orderedInput() {

        mIsOrdered = true;

        return this;
    }

    /**
     * @param timeout
     * @param timeUnit
     * @return
     */
    @Nonnull
    public IOChannelBuilder outputBufferTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return outputBufferTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * @param timeout
     * @return
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public IOChannelBuilder outputBufferTimeout(final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mOutputTimeout = timeout;

        return this;
    }
}
