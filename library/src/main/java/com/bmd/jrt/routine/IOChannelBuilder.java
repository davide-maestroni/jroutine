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

import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Class implementing a builder of I/O channel objects.
 * <p/>
 * Created by davide on 10/25/14.
 */
public class IOChannelBuilder {

    private final DefaultConfigurationBuilder mBuilder;

    /**
     * Avoid direct instantiation.
     */
    IOChannelBuilder() {

        mBuilder = new DefaultConfigurationBuilder();
    }

    /**
     * Applies the specified configuration to this builder.
     *
     * @param configuration the configuration.
     * @return this builder.
     * @throws NullPointerException if the specified configuration is null.
     */
    @Nonnull
    public IOChannelBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        mBuilder.apply(configuration);

        return this;
    }

    /**
     * Sets the timeout for the channel to have room for additional data.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws NullPointerException     if the specified time unit is null.
     * @throws IllegalArgumentException if the specified timeout is negative.
     */
    @Nonnull
    public IOChannelBuilder bufferTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        mBuilder.outputTimeout(timeout, timeUnit);

        return this;
    }

    /**
     * Sets the timeout for the channel to have room for additional data.
     *
     * @param timeout the timeout.
     * @return this builder.
     * @throws NullPointerException if the specified timeout is null.
     */
    @Nonnull
    public IOChannelBuilder bufferTimeout(@Nonnull final TimeDuration timeout) {

        mBuilder.outputTimeout(timeout);

        return this;
    }

    /**
     * Builds and returns the channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    public <T> IOChannel<T> buildChannel() {

        return new DefaultIOChannel<T>(mBuilder.buildConfiguration());
    }

    /**
     * Sets the order in which data are collected from the channel.
     *
     * @param order the order type.
     * @return this builder.
     * @throws NullPointerException if the specified order type is null.
     */
    @Nonnull
    public IOChannelBuilder dataOrder(@Nonnull final DataOrder order) {

        mBuilder.outputOrder(order);

        return this;
    }

    /**
     * Sets the runner instance used to schedule delayed inputs.
     *
     * @param runner the runner instance.
     * @return this builder.
     * @throws NullPointerException if the specified runner is null.
     */
    @Nonnull
    public IOChannelBuilder delayRunner(@Nonnull final Runner runner) {

        mBuilder.runBy(runner);

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
    public IOChannelBuilder logLevel(@Nonnull final LogLevel level) {

        mBuilder.logLevel(level);

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
    public IOChannelBuilder loggedWith(@Nonnull final Log log) {

        mBuilder.loggedWith(log);

        return this;
    }

    /**
     * Sets the maximum number of data that the channel can retain before they are consumed.
     *
     * @param maxBufferSize the maximum size.
     * @return this builder.
     * @throws IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public IOChannelBuilder maxSize(final int maxBufferSize) {

        mBuilder.outputSize(maxBufferSize);

        return this;
    }
}
