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

/**
 * Interface extending a builder of routine objects.
 * <p/>
 * In addition to a routine builder, the number of input and output data buffered in the
 * corresponding channel can be limited in order to avoid excessive memory consumption. In case the
 * maximum number is reached when passing an input or output, the call blocks until enough data are
 * consumed or the specified timeout elapses. In the latter case a
 * {@link com.bmd.jrt.common.DeadlockException} will be thrown.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.
 * <p/>
 * Finally, by default the order of input and output data is not guaranteed. Nevertheless, it is
 * possible to force data to be delivered in insertion order, at the cost of a slightly increased
 * memory usage and computation, by calling the proper methods.
 * <p/>
 * Created by davide on 11/11/14.
 */
public interface RoutineChannelBuilder extends RoutineBuilder {

    @Nonnull
    @Override
    public RoutineChannelBuilder apply(@Nonnull RoutineConfiguration configuration);

    @Nonnull
    @Override
    public RoutineChannelBuilder availableTimeout(long timeout, @Nonnull TimeUnit timeUnit);

    @Nonnull
    @Override
    public RoutineChannelBuilder availableTimeout(@Nullable TimeDuration timeout);

    @Nonnull
    @Override
    public RoutineChannelBuilder logLevel(@Nonnull LogLevel level);

    @Nonnull
    @Override
    public RoutineChannelBuilder loggedWith(@Nullable Log log);

    @Nonnull
    @Override
    public RoutineChannelBuilder maxRetained(int maxRetainedInstances);

    @Nonnull
    @Override
    public RoutineChannelBuilder maxRunning(int maxRunningInstances);

    @Nonnull
    @Override
    public RoutineChannelBuilder onResultTimeout(@Nonnull TimeoutAction action);

    @Nonnull
    @Override
    public RoutineChannelBuilder resultTimeout(long timeout, @Nonnull TimeUnit timeUnit);

    @Nonnull
    @Override
    public RoutineChannelBuilder resultTimeout(@Nullable TimeDuration timeout);

    @Nonnull
    @Override
    public RoutineChannelBuilder runBy(@Nullable Runner runner);

    @Nonnull
    @Override
    public RoutineChannelBuilder syncRunner(@Nonnull RunnerType type);

    /**
     * Sets the order in which input data are collected from the input channel.
     *
     * @param order the order type.
     * @return this builder.
     * @throws NullPointerException if the specified order type is null.
     */
    @Nonnull
    public RoutineChannelBuilder inputOrder(@Nonnull DataOrder order);

    /**
     * Sets the maximum number of data that the input channel can retain before they are consumed.
     * A {@link RoutineConfiguration#DEFAULT} value means that it is up to the framework to chose a
     * default size.
     *
     * @param inputMaxSize the maximum size.
     * @return this builder.
     * @throws IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public RoutineChannelBuilder inputSize(int inputMaxSize);

    /**
     * Sets the timeout for an input channel to have room for additional data. A null value means
     * that it is up to the framework to chose a default.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @Nonnull
    public RoutineChannelBuilder inputTimeout(@Nullable TimeDuration timeout);

    /**
     * Sets the timeout for an input channel to have room for additional data.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws NullPointerException     if the specified time unit is null.
     * @throws IllegalArgumentException if the specified timeout is negative.
     */
    @Nonnull
    public RoutineChannelBuilder inputTimeout(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * Sets the order in which output data are collected from the result channel.
     *
     * @param order the order type.
     * @return this builder.
     * @throws NullPointerException if the specified order type is null.
     */
    @Nonnull
    public RoutineChannelBuilder outputOrder(@Nonnull DataOrder order);

    /**
     * Sets the maximum number of data that the result channel can retain before they are consumed.
     * A {@link RoutineConfiguration#DEFAULT} value means that it is up to the framework to chose a
     * default size.
     *
     * @param outputMaxSize the maximum size.
     * @return this builder.
     * @throws IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public RoutineChannelBuilder outputSize(int outputMaxSize);

    /**
     * Sets the timeout for a result channel to have room for additional data.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws NullPointerException     if the specified time unit is null.
     * @throws IllegalArgumentException if the specified timeout is negative.
     */
    @Nonnull
    public RoutineChannelBuilder outputTimeout(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * Sets the timeout for a result channel to have room for additional data. A null value means
     * that it is up to the framework to chose a default.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @Nonnull
    public RoutineChannelBuilder outputTimeout(@Nullable TimeDuration timeout);

    /**
     * Enumeration defining how data are ordered inside a channel.
     */
    public enum DataOrder {

        /**
         * Insertion order.<br/>
         * Data are returned in the same order as they are passed to the channel, independently from
         * the specific delay.
         */
        INSERTION,
        /**
         * Delivery order.<br/>
         * Data are returned in the same order as they are delivered, taking also into consideration
         * the specific delay. Note that the delivery time might be different based on the specific
         * runner implementation, so there is no guarantee about the data order when, for example,
         * two objects are passed one immediately after the other with the same delay.
         */
        DELIVERY,
        /**
         * Default order.<br/>
         * This value is used to indicated that the choice of the order is left to the framework.
         */
        DEFAULT
    }
}
