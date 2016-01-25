/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Interface defining the basic communication channel with the routine invocation.
 * <p/>
 * Channel instances are used to transfer data to and from the code executed inside the routine
 * invocation.
 * <p/>
 * Created by davide-maestroni on 09/09/2014.
 */
public interface Channel {

    /**
     * Closes the channel and abort the transfer of data, thus aborting the routine invocation.
     * <p/>
     * An instance of {@link com.github.dm.jrt.channel.AbortException AbortException} will be passed
     * as the abortion reason.
     * <p/>
     * Note that, in case the channel was already closed, the call to this method has no effect.
     *
     * @return whether the channel status changed as a result of the call.
     */
    boolean abort();

    /**
     * Closes the channel and abort the transfer of data, thus aborting the routine invocation and
     * causing the specified throwable to be passed as the abortion reason.<br/>
     * The throwable, unless it extends the base {@link com.github.dm.jrt.channel.RoutineException
     * RoutineException}, will be wrapped as the cause of an
     * {@link com.github.dm.jrt.channel.AbortException AbortException} instance.
     * <p/>
     * Note that, in case the channel was already closed, the call to this method has no effect.
     *
     * @param reason the throwable object identifying the reason of the invocation abortion.
     * @return whether the channel status changed as a result of the call.
     */
    boolean abort(@Nullable Throwable reason);

    /**
     * Checks if the channel is empty, that is, no data is stored in it.
     *
     * @return whether the channel is empty.
     */
    boolean isEmpty();

    /**
     * Checks if the channel is open, that is, more data are expected to be passed to it.
     *
     * @return whether the channel is open.
     */
    boolean isOpen();

    /**
     * Interface defining an input channel.
     * <p/>
     * Note that the delivery order of the input data might not be guaranteed.
     *
     * @param <IN> the input data type.
     */
    interface InputChannel<IN> extends Channel {

        /**
         * Tells the channel to delay the transfer of data of the specified time duration.<br/>
         * Note that an abortion command will be delayed as well. Note, however, that a delayed
         * abortion will not prevent the invocation from completing, as input data do.
         *
         * @param delay the delay.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> after(@NotNull TimeDuration delay);

        /**
         * Tells the channel to delay the transfer of data of the specified time duration.<br/>
         * Note that an abortion command will be delayed as well. Note, however, that a delayed
         * abortion will not prevent the invocation from completing, as input data do.
         *
         * @param delay    the delay value.
         * @param timeUnit the delay time unit.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalArgumentException         if the specified delay is negative.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> after(long delay, @NotNull TimeUnit timeUnit);

        /**
         * Tells the channel to not delay the transfer of data.
         *
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> now();

        /**
         * Tells the channel to sort the passed input data based on the order of the calls to the
         * pass methods.
         * <p/>
         * By default no particular order is applied.
         *
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         * @see #orderByDelay()
         */
        @NotNull
        InputChannel<IN> orderByCall();

        /**
         * Tells the channel to sort the passed input data based on the specific delay.<br/>
         * Note that only the inputs passed with a 0 delay will be delivered in the same order as
         * they are passed to the channel, while the others will be delivered as soon as the
         * dedicated runner handles the specific execution.
         * <p/>
         * By default no particular order is applied.
         *
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         * @see #orderByCall()
         */
        @NotNull
        InputChannel<IN> orderByDelay();

        /**
         * Passes the data returned by the specified channel to this one.
         * <p/>
         * Note that the output channel will be bound as a result of the call, thus effectively
         * preventing any other consumer from getting data from it.
         *
         * @param channel the output channel.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> pass(@Nullable OutputChannel<? extends IN> channel);

        /**
         * Passes the data returned by the specified iterable to this channel.
         *
         * @param inputs the iterable returning the input data.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> pass(@Nullable Iterable<? extends IN> inputs);

        /**
         * Passes the specified input to this channel.
         *
         * @param input the input.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> pass(@Nullable IN input);

        /**
         * Passes the specified input data to this channel.
         *
         * @param inputs the input data.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException            if this channel is already closed.
         */
        @NotNull
        InputChannel<IN> pass(@Nullable IN... inputs);
    }

    /**
     * Interface defining an output channel, that is the channel used to read the routine invocation
     * output data.
     * <p/>
     * Note that the delivery order of the output data might not be guaranteed.
     *
     * @param <OUT> the output data type.
     */
    interface OutputChannel<OUT> extends Channel, Iterator<OUT>, Iterable<OUT> {

        /**
         * Tells the channel to wait at the maximum the specified time duration for the next result
         * to be available.
         * <p/>
         * By default the timeout is set to 0 to avoid unexpected deadlocks.
         *
         * @param timeout the maximum timeout.
         * @return this channel.
         */
        @NotNull
        OutputChannel<OUT> afterMax(@NotNull TimeDuration timeout);

        /**
         * Tells the channel to wait at the maximum the specified time duration for the next result
         * to be available.
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
         * Consumes all the results by waiting for the routine to complete at the maximum for the
         * set timeout.<br/>
         * Note that this method invocation will block the calling thread until the routine
         * invocation completes or the timeout elapses.
         *
         * @return the list of results.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        @NotNull
        List<OUT> all();

        /**
         * Consumes all the results by waiting for the routine to complete at the maximum for the
         * set timeout, and put them into the specified collection.<br/>
         * Note that this method invocation will block the calling thread until the routine
         * invocation completes or the timeout elapses.
         *
         * @param results the collection to fill.
         * @return this channel.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        @NotNull
        OutputChannel<OUT> allInto(@NotNull Collection<? super OUT> results);

        /**
         * Checks if the routine is complete, waiting at the maximum for the set timeout.<br/>
         * Note that this method invocation will block the calling thread until the routine
         * invocation completes or the timeout elapses.
         *
         * @return whether the routine execution has complete.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
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
         * @see #afterMax(long, TimeUnit)
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
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        @NotNull
        OutputChannel<OUT> eventuallyAbort(@Nullable Throwable reason);

        /**
         * Tells the channel to break execution in case no result is available before the timeout
         * has elapsed.
         * <p/>
         * By default an {@link com.github.dm.jrt.channel.ExecutionTimeoutException
         * ExecutionTimeoutException} exception will be thrown.
         *
         * @return this channel.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
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
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         */
        @NotNull
        OutputChannel<OUT> eventuallyThrow();

        /**
         * Checks if more results are available by waiting at the maximum for the set timeout.<br/>
         * Note that this method invocation will block the calling thread until a new output is
         * available, the routine invocation completes or the timeout elapses.
         *
         * @return whether at least one result is available.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        boolean hasNext();

        /**
         * Consumes the first available result by waiting at the maximum for the set timeout.<br/>
         * Note that this method invocation will block the calling thread until a new output is
         * available, the routine invocation completes or the timeout elapses.
         *
         * @return the first available result.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @throws java.util.NoSuchElementException                    if no output is available (it
         *                                                             might be thrown also in the
         *                                                             case the read timeout elapses
         *                                                             and no timeout exception is
         *                                                             set to be thrown).
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
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
         * @see #passTo passTo(InputChannel)
         * @see #passTo(OutputConsumer)
         */
        boolean isBound();

        /**
         * Consumes the first {@code count} available results by waiting at the maximum for the set
         * timeout.<br/>
         * Note that this method invocation will block the calling thread until {@code count} new
         * outputs are available, the routine invocation completes or the timeout elapses.
         *
         * @return the first {@code count} available results.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        @NotNull
        List<OUT> next(int count);

        /**
         * Consumes the first available result by waiting at the maximum for the set timeout.<br/>
         * If the timeout elapses and the channel is not configured to throw an exception, the
         * specified alternative output is returned.<br/>
         * Note that this method invocation will block the calling thread until a new output is
         * available, the routine invocation completes or the timeout elapses.
         *
         * @return the first available result.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @throws java.util.NoSuchElementException                    if no output is available (it
         *                                                             might be thrown also in the
         *                                                             case the read timeout elapses
         *                                                             and no timeout exception is
         *                                                             set to be thrown).
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        OUT nextOr(OUT output);

        /**
         * Binds this channel to the specified one. After the call, all the output will be passed
         * only to the specified input channel. Attempting to read through the dedicated methods
         * will cause an {@link IllegalStateException} to be thrown.
         *
         * @param channel   the input channel
         * @param <CHANNEL> the input channel type.
         * @return the passed channel.
         * @throws java.lang.IllegalStateException if this channel is already bound.
         */
        @NotNull
        <CHANNEL extends InputChannel<? super OUT>> CHANNEL passTo(@NotNull CHANNEL channel);

        /**
         * Binds this channel to the specified consumer. After the call, all the output will be
         * passed only to the consumer. Attempting to read through the dedicated methods will cause
         * an {@link IllegalStateException} to be thrown.<br/>
         * Note that the consumer methods may be called on the runner thread.
         *
         * @param consumer the consumer instance.
         * @return this channel.
         * @throws java.lang.IllegalStateException if this channel is already bound.
         */
        @NotNull
        OutputChannel<OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

        /**
         * Skips the first {@code count} available results by waiting at the maximum for the set
         * timeout.<br/>
         * Note that this method invocation will block the calling thread until {@code count} new
         * outputs are available, the routine invocation completes or the timeout elapses.
         *
         * @return this channel.
         * @throws com.github.dm.jrt.channel.ExecutionTimeoutException if the channel is set to
         *                                                             throw an exception when the
         *                                                             timeout elapses.
         * @throws com.github.dm.jrt.channel.RoutineException          if the execution has been
         *                                                             aborted.
         * @throws java.lang.IllegalStateException                     if this channel is already
         *                                                             bound to a consumer or
         *                                                             another channel.
         * @see #afterMax(TimeDuration)
         * @see #afterMax(long, TimeUnit)
         * @see #immediately()
         * @see #eventuallyAbort()
         * @see #eventuallyAbort(Throwable)
         * @see #eventuallyExit()
         * @see #eventuallyThrow()
         */
        @NotNull
        OutputChannel<OUT> skip(int count);
    }
}
