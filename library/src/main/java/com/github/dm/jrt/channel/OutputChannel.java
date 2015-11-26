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
package com.github.dm.jrt.channel;

import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining an output channel, that is the channel used to read the routine invocation
 * output data.
 * <p/>
 * Note that the delivery order of the output data might not be guaranteed.
 * <p/>
 * Created by davide-maestroni on 09/04/2014.
 *
 * @param <OUT> the output data type.
 */
public interface OutputChannel<OUT> extends Channel, Iterator<OUT>, Iterable<OUT> {

    /**
     * Tells the channel to wait at the max the specified time duration for the next result to be
     * available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout the maximum timeout.
     * @return this channel.
     */
    @NotNull
    OutputChannel<OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * Tells the channel to wait at the max the specified time duration for the next result to be
     * available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the maximum timeout value.
     * @param timeUnit the timeout time unit.
     * @return this channel.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     */
    @NotNull
    OutputChannel<OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * Consumes all the results by waiting for the routine to complete at the maximum for the set
     * timeout.
     *
     * @return this channel.
     * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to throw an
     *                                                             exception when the timeout
     *                                                             elapses.
     * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
     *                                                             aborted.
     * @throws java.lang.IllegalStateException                     if this channel is already bound
     *                                                             to a consumer.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    @NotNull
    List<OUT> all();

    /**
     * Consumes all the results by waiting for the routine to complete at the maximum for the set
     * timeout, and put them into the specified collection.
     *
     * @param results the collection to fill.
     * @return this channel.
     * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to throw an
     *                                                             exception when the timeout
     *                                                             elapses.
     * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
     *                                                             aborted.
     * @throws java.lang.IllegalStateException                     if this channel is already bound
     *                                                             to a consumer.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    @NotNull
    OutputChannel<OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * Checks if the routine is complete, waiting at the maximum for the set timeout.
     *
     * @return whether the routine execution has complete.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    boolean checkComplete();

    /**
     * Tells the channel to abort the invocation execution in case no result is available before
     * the timeout has elapsed.
     * <p/>
     * By default an {@link com.github.dm.jrt.channel.ExecutionTimeoutException
     * ExecutionTimeoutException} exception will be thrown.
     *
     * @return this channel.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    @NotNull
    OutputChannel<OUT> eventuallyAbort();

    /**
     * Tells the channel to abort the invocation execution in case no result is available before
     * the timeout has elapsed.
     * <p/>
     * By default an {@link com.github.dm.jrt.channel.ExecutionTimeoutException
     * ExecutionTimeoutException} exception will be thrown.
     *
     * @param reason the throwable object identifying the reason of the invocation abortion.
     * @return this channel.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    @NotNull
    OutputChannel<OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * Tells the channel to break execution in case no result is available before the timeout has
     * elapsed.
     * <p/>
     * By default an {@link com.github.dm.jrt.channel.ExecutionTimeoutException
     * ExecutionTimeoutException} exception will be thrown.
     *
     * @return this channel.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyThrow()
     */
    @NotNull
    OutputChannel<OUT> eventuallyExit();

    /**
     * Tells the channel to throw an {@link com.github.dm.jrt.channel.ExecutionTimeoutException
     * ExecutionTimeoutException} in case no result is available before the timeout has elapsed.
     * <p/>
     * This is the default behavior.
     *
     * @return this channel.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     */
    @NotNull
    OutputChannel<OUT> eventuallyThrow();

    /**
     * Checks if more results are available by waiting at the maximum for the set timeout.
     *
     * @return whether at least one result is available.
     * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to throw an
     *                                                             exception when the timeout
     *                                                             elapses.
     * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
     *                                                             aborted.
     * @throws java.lang.IllegalStateException                     if this channel is already bound
     *                                                             to a consumer.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    boolean hasNext();

    /**
     * Consumes the first available result by waiting at the maximum for the set timeout.
     *
     * @return the first available result.
     * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to throw an
     *                                                             exception when the timeout
     *                                                             elapses.
     * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
     *                                                             aborted.
     * @throws java.lang.IllegalStateException                     if this channel is already bound
     *                                                             to a consumer.
     * @throws java.util.NoSuchElementException                    if no output is available (it
     *                                                             might be thrown also in the case
     *                                                             the read timeout elapses and no
     *                                                             timeout exception is set to be
     *                                                             thrown).
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    OUT next();

    /**
     * Tells the channel to not wait for results to be available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @return this channel.
     */
    @NotNull
    OutputChannel<OUT> immediately();

    /**
     * Checks if this channel is bound to a consumer or another channel.
     *
     * @return whether the channel is bound.
     * @see #passTo(InputChannel)
     * @see #passTo(OutputConsumer)
     */
    boolean isBound();

    /**
     * Consumes the first {@code count} available results by waiting at the maximum for the set
     * timeout.
     *
     * @return the first available results.
     * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to throw an
     *                                                             exception when the timeout
     *                                                             elapses.
     * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
     *                                                             aborted.
     * @throws java.lang.IllegalStateException                     if this channel is already bound
     *                                                             to a consumer.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    @NotNull
    List<OUT> next(int count);

    /**
     * Binds this channel to the specified one. After the call, all the output will be passed
     * only to the specified input channel. Attempting to read through the dedicated methods will
     * cause an {@link java.lang.IllegalStateException} to be thrown.
     *
     * @param channel   the input channel
     * @param <CHANNEL> the input channel type.
     * @return this channel.
     * @throws java.lang.IllegalStateException if this channel is already bound.
     */
    @NotNull
    <CHANNEL extends InputChannel<? super OUT>> CHANNEL passTo(@NotNull CHANNEL channel);

    /**
     * Binds this channel to the specified consumer. After the call, all the output will be passed
     * only to the consumer. Attempting to read through the dedicated methods will cause an
     * {@link java.lang.IllegalStateException} to be thrown.<br/>
     * Note that the consumer methods will be called on the runner thread.
     *
     * @param consumer the consumer instance.
     * @return this channel.
     * @throws java.lang.IllegalStateException if this channel is already bound.
     */
    @NotNull
    OutputChannel<OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * Skips the first {@code count} available results by waiting at the maximum for the set
     * timeout.
     *
     * @return the first available results.
     * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to throw an
     *                                                             exception when the timeout
     *                                                             elapses.
     * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
     *                                                             aborted.
     * @throws java.lang.IllegalStateException                     if this channel is already bound
     *                                                             to a consumer.
     * @see #afterMax(TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyAbort()
     * @see #eventuallyAbort(Throwable)
     * @see #eventuallyExit()
     * @see #eventuallyThrow()
     */
    @NotNull
    OutputChannel<OUT> skip(int count);
}
