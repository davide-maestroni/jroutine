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
public class ChannelBuilder {

    private TimeDuration mDuration = TimeDuration.INFINITY;

    private boolean mIsOrdered;

    private Log mLog = Logger.getDefaultLog();

    private LogLevel mLogLevel = Logger.getDefaultLogLevel();

    /**
     * Avoid direct instantiation.
     */
    ChannelBuilder() {

    }

    /**
     * Builds and returns the channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    public <T> IOChannel<T> buildChannel() {

        return new DefaultIOChannel<T>(mIsOrdered, mDuration, mLog, mLogLevel);
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
    public ChannelBuilder logLevel(@Nonnull final LogLevel level) {

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
    public ChannelBuilder loggedWith(@Nonnull final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;

        return this;
    }

    /**
     * Forces the inputs to be ordered as they are passed to the input channel,
     * independently from the source or the input delay.
     *
     * @return this builder.
     */
    @Nonnull
    public ChannelBuilder orderedInput() {

        mIsOrdered = true;

        return this;
    }

    /**
     * Sets the max age of the channel.<br/>
     * After the specified time elapsed, the channel will be automatically closed.
     *
     * @param duration the max age duration.
     * @param timeUnit the duration time unit.
     * @return this builder.
     */
    @Nonnull
    public ChannelBuilder withMaxAge(final long duration, @Nonnull final TimeUnit timeUnit) {

        return withMaxAge(fromUnit(duration, timeUnit));
    }

    /**
     * Sets the max age of the channel.<br/>
     * After the specified time elapsed, the channel will be automatically closed.
     *
     * @param duration the max age duration.
     * @return this builder.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ChannelBuilder withMaxAge(@Nonnull final TimeDuration duration) {

        if (duration == null) {

            throw new NullPointerException("the duration must not be null");
        }

        mDuration = duration;

        return this;
    }
}