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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining the basic communication channel with the routine invocation.
 * <p>
 * Channel instances are used to transfer data to and from the code executed inside the routine
 * invocation.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface Channel<IN, OUT> extends Iterator<OUT>, Iterable<OUT> {

  /**
   * Closes the channel and abort the transfer of data, thus aborting the routine invocation.
   * <br>
   * An instance of {@link com.github.dm.jrt.core.channel.AbortException AbortException} will be
   * passed as the abortion reason.
   * <br>
   * If a delay has been set through the dedicated methods, the abortion will be accordingly
   * postponed.
   * <p>
   * Note that, in case the channel is already closed, the method invocation will have no effect.
   *
   * @return whether the channel status changed as a result of the call.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  boolean abort();

  /**
   * Closes the channel and abort the transfer of data, thus aborting the routine invocation and
   * causing the specified throwable to be passed as the abortion reason.
   * <br>
   * The throwable, unless it extends the base
   * {@link com.github.dm.jrt.core.common.RoutineException RoutineException}, will be wrapped as
   * the cause of an {@link com.github.dm.jrt.core.channel.AbortException AbortException}
   * instance.
   * <br>
   * If a delay has been set through the dedicated methods, the abortion will be accordingly
   * postponed.
   * <p>
   * Note that, in case the channel is already closed, the method invocation will have no effect.
   *
   * @param reason the throwable object identifying the reason of the invocation abortion.
   * @return whether the channel status changed as a result of the call.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  boolean abort(@Nullable Throwable reason);

  /**
   * Tells the channel to delay the following operations of the specified time duration.
   * <br>
   * Read operations will not be affected.
   * <p>
   * By default the delay is set to 0.
   * <p>
   * Note that closing and abortion commands will be delayed as well. Note, however, that a
   * delayed abortion will not prevent the invocation from completing, as pending input data do.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @param delay    the delay value.
   * @param timeUnit the delay time unit.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @throws java.lang.IllegalArgumentException             if the specified delay is negative.
   */
  @NotNull
  Channel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

  /**
   * Tells the channel to delay the following operations of the specified time duration.
   * <br>
   * Read operations will not be affected.
   * <p>
   * By default the delay is set to 0.
   * <p>
   * Note that closing and abortion commands will be delayed as well. Note, however, that a
   * delayed abortion will not prevent the invocation from completing, as pending input data do.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @param delay the delay.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   */
  @NotNull
  Channel<IN, OUT> after(@NotNull DurationMeasure delay);

  /**
   * Tells the channel to not delay the following operations.
   * <br>
   * Read operations will not be affected.
   * <p>
   * By default the delay is set to 0.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   */
  @NotNull
  Channel<IN, OUT> afterNoDelay();

  /**
   * Consumes all the results by waiting for the routine to complete at the maximum for the set
   * delay.
   * <p>
   * Note that this method invocation will block the calling thread until the routine invocation
   * completes or is aborted, or the timeout elapses.
   *
   * @return the list of results.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  List<OUT> all();

  /**
   * Consumes all the results by waiting for the routine to complete at the maximum for the set
   * delay, and put them into the specified collection.
   * <p>
   * Note that this method invocation will block the calling thread until the routine invocation
   * completes or is aborted, or the timeout elapses.
   *
   * @param results the collection to fill.
   * @return this channel.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Channel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

  /**
   * Closes this channel and completes the invocation.
   * <br>
   * After channel is closed, attempting to pass additional input data through the dedicated
   * methods will cause an {@link java.lang.IllegalStateException} to be thrown.
   * <br>
   * If a delay has been set through the dedicated methods, the closing command will be
   * accordingly postponed.
   * <p>
   * Note that, even if calling this method is not strictly mandatory, some invocation
   * implementations may rely on the completion notification to produce their results. So, it's
   * always advisable to close the channel as soon as all the input data has been passed.
   *
   * @return this channel.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  Channel<IN, OUT> close();

  /**
   * Binds this channel to the specified consumer.
   * <br>
   * After method exits, all the output will be passed only to the consumer. Attempting to read
   * through the dedicated methods will cause an {@link java.lang.IllegalStateException} to be
   * thrown.
   * <br>
   * If a delay has been set through the dedicated methods, the transfer of data will be
   * accordingly postponed.
   * <p>
   * Note that the consumer methods may be called on the executor thread.
   *
   * @param consumer the consumer instance.
   * @return this channel.
   * @throws java.lang.IllegalStateException if this channel is already bound.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  Channel<IN, OUT> consume(@NotNull ChannelConsumer<? super OUT> consumer);

  /**
   * Tells the channel to abort the invocation execution in case, after a read method is invoked,
   * no result is available before the timeout has elapsed.
   * <p>
   * By default an
   * {@link com.github.dm.jrt.core.channel.OutputTimeoutException OutputTimeoutException}
   * exception will be thrown.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Channel<IN, OUT> eventuallyAbort();

  /**
   * Tells the channel to abort the invocation execution in case, after a read method is invoked,
   * no result is available before the timeout has elapsed.
   * <p>
   * By default an
   * {@link com.github.dm.jrt.core.channel.OutputTimeoutException OutputTimeoutException}
   * exception will be thrown.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @param reason the throwable object identifying the reason of the invocation abortion.
   * @return this channel.
   * @see #eventuallyAbort()
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Channel<IN, OUT> eventuallyAbort(@Nullable Throwable reason);

  /**
   * Tells the channel to break the invocation execution in case, after a read method is
   * invoked, no result is available before the timeout has elapsed.
   * <p>
   * By default an
   * {@link com.github.dm.jrt.core.channel.OutputTimeoutException OutputTimeoutException}
   * exception will be thrown.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Channel<IN, OUT> eventuallyContinue();

  /**
   * Tells the channel to throw an
   * {@link com.github.dm.jrt.core.channel.OutputTimeoutException OutputTimeoutException} in case,
   * after a read method is invoked, no result is available before the timeout has elapsed.
   * <p>
   * This is the default behavior.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Channel<IN, OUT> eventuallyFail();

  /**
   * Returns an iterator whose lifetime cannot exceed the set delay.
   *
   * @return the iterator instance.
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Iterator<OUT> expiringIterator();

  /**
   * Checks if the invocation has completed, waiting at the maximum for the set delay.
   * <p>
   * Note that this method invocation will block the calling thread until the routine invocation
   * completes or is aborted, or the timeout elapses.
   *
   * @return whether the routine execution has completed.
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  boolean getComplete();

  /**
   * Gets the invocation error or abort exception, if the invocation is aborted, waiting at the
   * maximum for the set delay.
   * <p>
   * Note that this method invocation will block the calling thread until the routine invocation
   * completes or is aborted, or the timeout elapses.
   *
   * @return the invocation error or null.
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @Nullable
  RoutineException getError();

  /**
   * Checks if more results are available by waiting at the maximum for the set timeout.
   * <p>
   * Note that this method invocation will block the calling thread until a new output is
   * available, the routine invocation completes or is aborted, or the timeout elapses.
   *
   * @return whether at least one result is available.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  boolean hasNext();

  /**
   * Consumes the first available result by waiting at the maximum for the set timeout.
   * <p>
   * Note that this method invocation will block the calling thread until a new output is
   * available, the routine invocation completes or is aborted, or the timeout elapses.
   *
   * @return the first available result.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @throws java.util.NoSuchElementException                      if no output is available (it
   *                                                               might be thrown also in case
   *                                                               the read timeout elapses and no
   *                                                               timeout exception is set to be
   *                                                               thrown).
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  OUT next();

  /**
   * Tells the channel to wait at maximum for the specified time duration for the following read
   * operations to complete.
   * <p>
   * By default the timeout is set to 0 to avoid unexpected deadlocks.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @param timeout  the timeout value.
   * @param timeUnit the timeout time unit.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @throws java.lang.IllegalArgumentException             if the specified delay is negative.
   */
  @NotNull
  Channel<IN, OUT> in(long timeout, @NotNull TimeUnit timeUnit);

  /**
   * Tells the channel to wait at maximum for the specified time duration for the following read
   * operations to complete.
   * <p>
   * By default the timeout is set to 0 to avoid unexpected deadlocks.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @param timeout the timeout.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   */
  @NotNull
  Channel<IN, OUT> in(@NotNull DurationMeasure timeout);

  /**
   * Tells the channel to not wait any time for the following read operations to complete.
   * <p>
   * By default the timeout is set to 0 to avoid unexpected deadlocks.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   */
  @NotNull
  Channel<IN, OUT> inNoTime();

  /**
   * Returns the number of input data stored in the channel.
   *
   * @return the input data size.
   */
  int inputSize();

  /**
   * Checks if this channel is bound to a consumer or another channel.
   *
   * @return whether the channel is bound.
   * @see #consume(ChannelConsumer)
   * @see #pipe(Channel)
   */
  boolean isBound();

  /**
   * Checks if the channel is empty, that is, no data are stored in it.
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
   * Consumes the first {@code count} available results by waiting at the maximum for the set delay.
   * <p>
   * Note that this method invocation will block the calling thread until {@code count} new
   * outputs are available, the routine invocation completes or is aborted, or the timeout elapses.
   *
   * @param count the number of outputs to read.
   * @return the first {@code count} available results.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  List<OUT> next(int count);

  /**
   * Consumes the first available result by waiting at the maximum for the set delay.
   * <br>
   * If the timeout elapses and the channel is not configured to throw an exception or abort the
   * invocation, the specified alternative output is returned.
   * <p>
   * Note that this method invocation will block the calling thread until a new output is
   * available, the routine invocation completes or is aborted, or the timeout elapses.
   *
   * @param output the default output to return.
   * @return the first available result.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @throws java.util.NoSuchElementException                      if no output is available (it
   *                                                               might be thrown also in case
   *                                                               the read timeout elapses and no
   *                                                               timeout exception is set to be
   *                                                               thrown).
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  OUT nextOrElse(OUT output);

  /**
   * Returns the number of output data stored in the channel.
   *
   * @return the output data size.
   */
  int outputSize();

  /**
   * Passes the data returned by the specified channel to this one.
   * <br>
   * If a delay has been set through the dedicated methods, the transfer of data will be
   * accordingly postponed.
   * <p>
   * Note that the passed channel will be bound as a result of the call, thus effectively
   * preventing any other consumer from getting data from it.
   *
   * @param channel the channel.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @throws java.lang.IllegalStateException                if this channel is already closed.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  Channel<IN, OUT> pass(@Nullable Channel<?, ? extends IN> channel);

  /**
   * Passes the data returned by the specified iterable to this channel.
   * <br>
   * If a delay has been set through the dedicated methods, the transfer of data will be
   * accordingly postponed.
   *
   * @param inputs the iterable returning the input data.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @throws java.lang.IllegalStateException                if this channel is already closed.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  Channel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

  /**
   * Passes the specified input to this channel.
   * <br>
   * If a delay has been set through the dedicated methods, the transfer of data will be
   * accordingly postponed.
   *
   * @param input the input.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @throws java.lang.IllegalStateException                if this channel is already closed.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  Channel<IN, OUT> pass(@Nullable IN input);

  /**
   * Passes the specified input data to this channel.
   * <br>
   * If a delay has been set through the dedicated methods, the transfer of data will be
   * accordingly postponed.
   *
   * @param inputs the input data.
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @throws java.lang.IllegalStateException                if this channel is already closed.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  Channel<IN, OUT> pass(@Nullable IN... inputs);

  /**
   * Creates a new channel piping the output data into the specified one.
   * <br>
   * After method exits, all the output will be passed only to the specified input channel.
   * Attempting to read through the dedicated methods will cause an
   * {@link java.lang.IllegalStateException} to be thrown.
   * <br>
   * If a delay has been set through the dedicated methods, the transfer of data will be
   * accordingly postponed.
   *
   * @param channel the input channel
   * @param <AFTER> the channel output type.
   * @return the newly created pipe channel.
   * @throws java.lang.IllegalStateException if this channel is already bound.
   * @see #after(DurationMeasure)
   * @see #after(long, TimeUnit)
   * @see #afterNoDelay()
   */
  @NotNull
  <AFTER> Channel<IN, AFTER> pipe(@NotNull Channel<? super OUT, AFTER> channel);

  /**
   * Returns the total number of data stored in the channel.
   *
   * @return the data size.
   */
  int size();

  /**
   * Skips the first {@code count} available results by waiting at the maximum for the set delay.
   * <p>
   * Note that this method invocation will block the calling thread until {@code count} new
   * outputs are available, the routine invocation completes or is aborted, or the timeout elapses.
   *
   * @param count the number of outputs to skip.
   * @return this channel.
   * @throws com.github.dm.jrt.core.channel.OutputTimeoutException if the channel is set to throw
   *                                                               an exception when the timeout
   *                                                               elapses.
   * @throws com.github.dm.jrt.core.common.RoutineException        if the execution has been
   *                                                               aborted.
   * @throws java.lang.IllegalStateException                       if this channel is already
   *                                                               bound to a consumer or another
   *                                                               channel.
   * @see #eventuallyAbort()
   * @see #eventuallyAbort(Throwable)
   * @see #eventuallyContinue()
   * @see #eventuallyFail()
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  @NotNull
  Channel<IN, OUT> skipNext(int count);

  /**
   * Tells the channel to sort the passed input data based in the same order as they are passed
   * to the channel.
   * <p>
   * By default no particular order is applied.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @see #unsorted()
   */
  @NotNull
  Channel<IN, OUT> sorted();

  /**
   * Throws the invocation error or abort exception, if the invocation is aborted, waiting at the
   * maximum for the set delay.
   * <p>
   * Note that this method invocation will block the calling thread until the routine invocation
   * completes or is aborted, or the timeout elapses.
   *
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @see #in(DurationMeasure)
   * @see #in(long, TimeUnit)
   * @see #inNoTime()
   */
  void throwError();

  /**
   * Tells the channel to not sort the passed input data.
   * <p>
   * Note that only the inputs passed with a 0 delay will be delivered in the same order as they
   * are passed to the channel, while the others will be delivered as soon as the dedicated executor
   * handles the specific execution.
   * <p>
   * This is the default behavior.
   * <p>
   * Note that the implementing class should ensure that calls of this method from different
   * threads will not interfere with each others.
   *
   * @return this channel.
   * @throws com.github.dm.jrt.core.common.RoutineException if the execution has been aborted.
   * @see #sorted()
   */
  @NotNull
  Channel<IN, OUT> unsorted();
}
