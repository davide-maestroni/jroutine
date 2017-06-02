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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.InvocationExecution.ExecutionObserver;
import com.github.dm.jrt.core.InvocationExecution.InputData;
import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.DurationMeasure.Condition;
import com.github.dm.jrt.core.util.LocalField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.common.Backoff.NO_DELAY;
import static com.github.dm.jrt.core.util.DurationMeasure.fromUnit;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Default implementation of an invocation channel.
 * <p>
 * Created by davide-maestroni on 06/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class InvocationChannel<IN, OUT> implements Channel<IN, OUT> {

  private static final ScheduledExecutor sSyncExecutor = ScheduledExecutors.syncExecutor();

  private final IdentityHashMap<Channel<?, ? extends IN>, Void> mBoundChannels =
      new IdentityHashMap<Channel<?, ? extends IN>, Void>();

  private final InvocationExecution<IN, OUT> mExecution;

  private final ScheduledExecutor mExecutor;

  private final Condition mHasInputs;

  private final Backoff mInputBackoff;

  private final LocalField<DurationMeasure> mInputDelay;

  private final LocalField<OrderType> mInputOrder;

  private final NestedQueue<IN> mInputQueue;

  private final Logger mLogger;

  private final int mMaxInput;

  private final Object mMutex = new Object();

  private final ResultChannel<OUT> mResultChanel;

  private RoutineException mAbortException;

  private int mInputCount;

  private boolean mIsPendingExecution;

  private boolean mIsWaitingInput;

  private int mPendingExecutionCount;

  private InputChannelState mState;

  /**
   * Constructor.
   *
   * @param configuration the invocation configuration.
   * @param manager       the invocation manager.
   * @param executor      the executor instance.
   * @param logger        the logger instance.
   */
  InvocationChannel(@NotNull final InvocationConfiguration configuration,
      @NotNull final InvocationManager<IN, OUT> manager, @NotNull final ConcurrentExecutor executor,
      @NotNull final Logger logger) {
    mLogger = logger.subContextLogger(this);
    mExecutor = ConstantConditions.notNull("invocation executor", executor);
    mInputOrder =
        new LocalField<OrderType>(configuration.getInputOrderTypeOrElse(OrderType.UNSORTED));
    mInputBackoff = configuration.getInputBackoffOrElse(BackoffBuilder.noDelay());
    mMaxInput = configuration.getInputMaxSizeOrElse(Integer.MAX_VALUE);
    mInputDelay = new LocalField<DurationMeasure>(noTime());
    mInputQueue = new NestedQueue<IN>() {

      @Override
      void close() {
        // Preventing closing
      }
    };
    mHasInputs = (configuration.getInputBackoffOrElse(null) != null) ? new Condition() {

      public boolean isTrue() {
        return (mInputBackoff.getDelay(mInputCount) == NO_DELAY) || (mAbortException != null);
      }
    } : new Condition() {

      public boolean isTrue() {
        return true;
      }
    };
    mResultChanel = new ResultChannel<OUT>(executor.decorated(), configuration, new AbortHandler() {

      public void onAbort(@NotNull final RoutineException reason, final long delay,
          @NotNull final TimeUnit timeUnit) {
        final Runnable command;
        synchronized (mMutex) {
          command = mState.onHandlerAbort(reason, delay, timeUnit);
        }

        if (command != null) {
          mExecutor.execute(command, delay, timeUnit);

        } else {
          // Make sure the invocation is properly recycled
          mExecutor.execute(new Runnable() {

            public void run() {
              mExecution.recycle(reason);
              mResultChanel.close(reason);
            }
          });
        }
      }
    }, logger);
    mExecution =
        new InvocationExecution<IN, OUT>(manager, new DefaultExecutionObserver(), mResultChanel,
            logger);
    mState = new InputChannelState();
  }

  public boolean abort() {
    return abort(null);
  }

  public boolean abort(@Nullable final Throwable reason) {
    final DurationMeasure delay = mInputDelay.get();
    final boolean isAbort;
    final Runnable command;
    synchronized (mMutex) {
      final InputChannelState state = mState;
      isAbort = state.abortOutput();
      command = state.abortInvocation(delay, reason);
    }

    final boolean needsAbort = (command != null);
    if (needsAbort) {
      mExecutor.execute(command, delay.value, delay.unit);
    }

    if (isAbort) {
      return mResultChanel.abort(reason);
    }

    return needsAbort && delay.isZero();
  }

  @NotNull
  public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
    return after(fromUnit(delay, timeUnit));
  }

  @NotNull
  public Channel<IN, OUT> after(@NotNull final DurationMeasure delay) {
    mInputDelay.set(ConstantConditions.notNull("input delay", delay));
    return this;
  }

  @NotNull
  public Channel<IN, OUT> afterNoDelay() {
    return after(noTime());
  }

  @NotNull
  public List<OUT> all() {
    return mResultChanel.all();
  }

  @NotNull
  public Channel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
    mResultChanel.allInto(results);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> close() {
    final DurationMeasure delay = mInputDelay.get();
    final Runnable command;
    synchronized (mMutex) {
      command = mState.onClose(delay);
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);
    }

    return this;
  }

  @NotNull
  public Channel<IN, OUT> consume(@NotNull final ChannelConsumer<? super OUT> consumer) {
    mResultChanel.consume(consumer);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyAbort() {
    mResultChanel.eventuallyAbort();
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
    mResultChanel.eventuallyAbort(reason);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyContinue() {
    mResultChanel.eventuallyContinue();
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyFail() {
    mResultChanel.eventuallyFail();
    return this;
  }

  @NotNull
  public Iterator<OUT> expiringIterator() {
    return mResultChanel.expiringIterator();
  }

  public OUT get() {
    return mResultChanel.get();
  }

  public boolean getComplete() {
    return mResultChanel.getComplete();
  }

  @Nullable
  public RoutineException getError() {
    return mResultChanel.getError();
  }

  public boolean hasNext() {
    return mResultChanel.hasNext();
  }

  public OUT next() {
    return mResultChanel.next();
  }

  @NotNull
  public Channel<IN, OUT> in(final long timeout, @NotNull final TimeUnit timeUnit) {
    mResultChanel.in(timeout, timeUnit);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> in(@NotNull final DurationMeasure timeout) {
    mResultChanel.in(timeout);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> inNoTime() {
    mResultChanel.inNoTime();
    return this;
  }

  public boolean isBound() {
    return mResultChanel.isBound();
  }

  public boolean isEmpty() {
    return (size() == 0);
  }

  public boolean isOpen() {
    synchronized (mMutex) {
      return mState.isChannelOpen();
    }
  }

  @NotNull
  public List<OUT> next(final int count) {
    return mResultChanel.next(count);
  }

  public OUT nextOrElse(final OUT output) {
    return mResultChanel.nextOrElse(output);
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
    final ChannelConsumer<IN> consumer;
    synchronized (mMutex) {
      consumer = mState.pass(channel);
    }

    if ((consumer != null) && (channel != null)) {
      try {
        channel.consume(consumer);

      } catch (final IllegalStateException e) {
        synchronized (mMutex) {
          mState.onBindFailure();
        }

        throw e;
      }
    }

    return this;
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
    final DurationMeasure delay = mInputDelay.get();
    final Runnable command;
    synchronized (mMutex) {
      command = mState.pass(inputs, delay);
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);
    }

    synchronized (mMutex) {
      if (!mHasInputs.isTrue()) {
        waitInputs();
      }
    }

    return this;
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final IN input) {
    final DurationMeasure delay = mInputDelay.get();
    final Runnable command;
    synchronized (mMutex) {
      command = mState.pass(input, delay);
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);
    }

    synchronized (mMutex) {
      if (!mHasInputs.isTrue()) {
        waitInputs();
      }
    }

    return this;
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final IN... inputs) {
    final DurationMeasure delay = mInputDelay.get();
    final Runnable command;
    synchronized (mMutex) {
      command = mState.pass(inputs, delay);
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);
    }

    synchronized (mMutex) {
      if (!mHasInputs.isTrue()) {
        waitInputs();
      }
    }

    return this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <AFTER> Channel<IN, AFTER> pipe(@NotNull final Channel<? super OUT, AFTER> channel) {
    ((Channel<OUT, AFTER>) channel).pass(this);
    return new FlatChannel<IN, AFTER>(this, channel);
  }

  public int size() {
    final int inputSize;
    synchronized (mMutex) {
      inputSize = mState.inputCount();
    }

    return inputSize + mResultChanel.size();
  }

  @NotNull
  public Channel<IN, OUT> skipNext(final int count) {
    mResultChanel.skipNext(count);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> sorted() {
    mInputOrder.set(OrderType.SORTED);
    return this;
  }

  public void throwError() {
    mResultChanel.throwError();
  }

  @NotNull
  public Channel<IN, OUT> unsorted() {
    mInputOrder.set(OrderType.UNSORTED);
    return this;
  }

  public Iterator<OUT> iterator() {
    return mResultChanel.iterator();
  }

  public void remove() {
    mResultChanel.remove();
  }

  private void checkMaxSize() {
    if (mInputCount > mMaxInput) {
      throw new InputDeadlockException(
          "maximum input channel size has been exceeded: " + mMaxInput);
    }
  }

  private void internalAbort(@NotNull final RoutineException abortException) {
    mAbortException = abortException;
    mExecutor.cancel(mExecution);
    mInputCount = 0;
    mInputQueue.clear();
  }

  private void waitInputs() {
    final long delay = mInputBackoff.getDelay(mInputCount);
    if ((delay > 0) && mExecutor.isExecutionThread()) {
      throw new InputDeadlockException(
          "cannot wait on the invocation executor thread: " + Thread.currentThread()
              + "\nTry employing a different executor than: " + mExecutor);
    }

    try {
      mIsWaitingInput = true;
      if (!DurationMeasure.waitUntil(mMutex, mHasInputs, delay, TimeUnit.MILLISECONDS)) {
        mLogger.dbg("timeout while waiting for room in the input channel [%s %s]", delay,
            TimeUnit.MILLISECONDS);
      }

    } catch (final InterruptedException e) {
      throw new InterruptedInvocationException(e);

    } finally {
      mIsWaitingInput = false;
    }
  }

  /**
   * The invocation execution has been aborted.
   */
  private class AbortChannelState extends CompleteChannelState {

    @Override
    boolean abortOutput() {
      mLogger.dbg("avoiding aborting result channel since invocation is aborting");
      return false;
    }

    @Nullable
    @Override
    Runnable delayedConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue) {
      mLogger.dbg("avoiding closing result channel after delay since invocation is aborted");
      return null;
    }

    @Nullable
    @Override
    Runnable onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue, final long delay, @NotNull final TimeUnit timeUnit) {
      throw consumerException();
    }

    @Nullable
    @Override
    Runnable onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
        @NotNull final OrderType orderType, final long delay, @NotNull final TimeUnit timeUnit) {
      throw consumerException();
    }

    @Nullable
    @Override
    Runnable onHandlerAbort(@NotNull final RoutineException reason, final long delay,
        @NotNull final TimeUnit timeUnit) {
      mLogger.dbg(reason, "avoiding aborting result channel since invocation is aborted");
      return null;
    }

    @NotNull
    private RoutineException consumerException() {
      final RoutineException abortException = mAbortException;
      mLogger.dbg(abortException, "consumer abort exception");
      return abortException;
    }

    @NotNull
    private RoutineException exception() {
      final RoutineException abortException = mAbortException;
      mLogger.dbg(abortException, "abort exception");
      throw abortException;
    }

    @Override
    public int inputCount() {
      return 0;
    }

    @Nullable
    @Override
    ChannelConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final Iterable<? extends IN> inputs,
        @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final IN input, @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final IN[] inputs, @NotNull final DurationMeasure delay) {
      throw exception();
    }
  }

  /**
   * Implementation of a runnable handling the abortion of the result channel.
   */
  private class AbortResultCommand implements Runnable {

    private final Throwable mAbortException;

    /**
     * Constructor.
     *
     * @param reason the reason of the abortion.
     */
    private AbortResultCommand(@NotNull final Throwable reason) {
      mAbortException = reason;
    }

    public void run() {
      mResultChanel.close(mAbortException);
    }
  }

  /**
   * The channel is closed and no more inputs are expected.
   */
  private class ClosedChannelState extends InputChannelState {

    @Nullable
    @Override
    Runnable abortInvocation(@NotNull final DurationMeasure delay,
        @Nullable final Throwable reason) {
      mLogger.dbg(reason, "avoiding aborting since channel is closed");
      return null;
    }

    @Override
    boolean abortOutput() {
      mLogger.dbg("aborting result channel");
      return true;
    }

    @Nullable
    @Override
    Runnable delayedAbortInvocation(@NotNull final RoutineException reason) {
      if (mPendingExecutionCount <= 0) {
        mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
        return null;
      }

      return super.delayedAbortInvocation(reason);
    }

    @Nullable
    @Override
    Runnable delayedClose() {
      mLogger.dbg("avoiding closing result channel after delay since already closed");
      return null;
    }

    @NotNull
    private IllegalStateException exception() {
      mLogger.err("invalid call on closed channel");
      return new IllegalStateException("the input channel is closed");
    }

    @Nullable
    @Override
    Runnable onClose(@NotNull final DurationMeasure delay) {
      return null;
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final Iterable<? extends IN> inputs,
        @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Override
    boolean isChannelOpen() {
      return false;
    }

    @Nullable
    @Override
    ChannelConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final IN input, @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final IN[] inputs, @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Override
    public boolean onConsumeComplete() {
      return (mPendingExecutionCount <= 0);
    }
  }

  /**
   * The invocation is complete.
   */
  private class CompleteChannelState extends ClosedChannelState {

    @NotNull
    private IllegalStateException exception() {
      mLogger.err("consumer invalid call on closed channel");
      return new IllegalStateException("the input channel is closed");
    }

    @Nullable
    @Override
    Runnable delayedAbortInvocation(@NotNull final RoutineException reason) {
      mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
      return null;
    }

    @Nullable
    @Override
    Runnable delayedConsumerError(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final RoutineException error) {
      mLogger.dbg(error, "avoiding aborting consumer after delay since invocation has completed");
      return null;
    }

    @Nullable
    @Override
    Runnable delayedConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue) {
      mLogger.dbg("avoiding closing result channel after delay since invocation has completed");
      return null;
    }

    @Nullable
    @Override
    Runnable onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue, final long delay, @NotNull final TimeUnit timeUnit) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable onConsumerError(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final RoutineException error, final long delay, @NotNull final TimeUnit timeUnit) {
      mLogger.wrn(error, "avoiding aborting consumer since invocation has completed");
      return null;
    }

    @Nullable
    @Override
    Runnable onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
        @NotNull final OrderType orderType, final long delay, @NotNull final TimeUnit timeUnit) {
      throw exception();
    }

    @Override
    public boolean onConsumeComplete() {
      return false;
    }

    @Override
    public boolean onFirstInput(@NotNull final InputData<IN> inputData) {
      return onNextInput(inputData);
    }

    @Override
    public void onInvocationComplete() {
    }

    @Override
    public boolean onNextInput(@NotNull final InputData<IN> inputData) {
      mLogger.wrn("avoiding consuming input since invocation has completed [#%d]",
          mPendingExecutionCount);
      return false;
    }

    @Nullable
    @Override
    Runnable delayedInput(@NotNull final NestedQueue<IN> queue, @Nullable final IN input) {
      mLogger.wrn("avoiding delayed input command since invocation has completed: %s", input);
      return null;
    }

    @Nullable
    @Override
    Runnable delayedInputs(@NotNull final NestedQueue<IN> queue, final List<IN> inputs) {
      mLogger.wrn("avoiding delayed input command since invocation has completed: %s", inputs);
      return null;
    }

    @Nullable
    @Override
    Runnable onHandlerAbort(@NotNull final RoutineException reason, final long delay,
        @NotNull final TimeUnit timeUnit) {
      mLogger.dbg(reason, "aborting result channel");
      return new AbortResultCommand(reason);
    }
  }

  /**
   * Default implementation of an channel consumer pushing the data into the input queue.
   */
  private class DefaultChannelConsumer implements ChannelConsumer<IN> {

    private final Channel<?, ? extends IN> mChannel;

    private final long mDelay;

    private final TimeUnit mDelayUnit;

    private final OrderType mOrderType;

    private final NestedQueue<IN> mQueue;

    /**
     * Constructor.
     *
     * @param channel the bound channel.
     */
    private DefaultChannelConsumer(@NotNull final Channel<?, ? extends IN> channel) {
      final DurationMeasure delay = mInputDelay.get();
      mDelay = delay.value;
      mDelayUnit = delay.unit;
      final OrderType order = (mOrderType = mInputOrder.get());
      mQueue = (order == OrderType.SORTED) ? mInputQueue.addNested() : mInputQueue;
      mChannel = channel;
    }

    public void onComplete() {
      final long delay = mDelay;
      final TimeUnit timeUnit = mDelayUnit;
      final Runnable command;
      synchronized (mMutex) {
        command = mState.onConsumerComplete(mChannel, mQueue, delay, timeUnit);
      }

      if (command != null) {
        mExecutor.execute(command, delay, timeUnit);
      }
    }

    public void onError(@NotNull final RoutineException error) {
      final long delay = mDelay;
      final TimeUnit timeUnit = mDelayUnit;
      final Runnable command;
      synchronized (mMutex) {
        command = mState.onConsumerError(mChannel, error, delay, timeUnit);
      }

      if (command != null) {
        mExecutor.execute(command, delay, timeUnit);
      }
    }

    public void onOutput(final IN output) {
      final long delay = mDelay;
      final TimeUnit timeUnit = mDelayUnit;
      final Runnable command;
      synchronized (mMutex) {
        command = mState.onConsumerOutput(output, mQueue, mOrderType, delay, timeUnit);
      }

      if (command != null) {
        mExecutor.execute(command, delay, timeUnit);
      }

      synchronized (mMutex) {
        if (!mHasInputs.isTrue()) {
          waitInputs();
        }
      }
    }
  }

  /**
   * Default implementation of an input iterator.
   */
  private class DefaultExecutionObserver implements ExecutionObserver<IN> {

    @NotNull
    public RoutineException getAbortException() {
      synchronized (mMutex) {
        return mState.getAbortException();
      }
    }

    public void onAbortComplete() {
      final Throwable abortException;
      final List<Channel<?, ? extends IN>> channels;
      synchronized (mMutex) {
        abortException = mAbortException;
        final IdentityHashMap<Channel<?, ? extends IN>, Void> boundChannels = mBoundChannels;
        mLogger.dbg(abortException, "aborting bound channels [%d]", boundChannels.size());
        channels = new ArrayList<Channel<?, ? extends IN>>(boundChannels.keySet());
        boundChannels.clear();
      }

      for (final Channel<?, ? extends IN> channel : channels) {
        channel.afterNoDelay().abort(abortException);
      }
    }

    public boolean onConsumeComplete() {
      synchronized (mMutex) {
        return mState.onConsumeComplete();
      }
    }

    public boolean onFirstInput(@NotNull final InputData<IN> inputData) {
      synchronized (mMutex) {
        return mState.onFirstInput(inputData);
      }
    }

    public void onInvocationComplete() {
      synchronized (mMutex) {
        mState.onInvocationComplete();
      }
    }

    public boolean onNextInput(@NotNull final InputData<IN> inputData) {
      synchronized (mMutex) {
        return mState.onNextInput(inputData);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed abortion.
   */
  private class DelayedAbortCommand implements Runnable {

    private final RoutineException mAbortException;

    /**
     * Constructor.
     *
     * @param reason the reason of the abortion.
     */
    private DelayedAbortCommand(@NotNull final RoutineException reason) {
      mAbortException = reason;
    }

    public void run() {
      final RoutineException abortException = mAbortException;
      final Runnable command;
      synchronized (mMutex) {
        command = mState.delayedAbortInvocation(abortException);
      }

      if (command != null) {
        sSyncExecutor.execute(command);
      }

      mResultChanel.abort(abortException);
    }
  }

  /**
   * Implementation of a runnable handling a delayed close.
   */
  private class DelayedCloseCommand implements Runnable {

    /**
     * Constructor.
     */
    private DelayedCloseCommand() {
    }

    public void run() {
      final Runnable command;
      synchronized (mMutex) {
        command = mState.delayedClose();
      }

      if (command != null) {
        sSyncExecutor.execute(command);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed consumer completion.
   */
  private class DelayedConsumerCompleteCommand implements Runnable {

    private final Channel<?, ? extends IN> mChannel;

    private final NestedQueue<IN> mQueue;

    /**
     * Constructor.
     *
     * @param channel the channel bound to this one.
     * @param queue   the output queue.
     */
    private DelayedConsumerCompleteCommand(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue) {
      mChannel = channel;
      mQueue = queue;
    }

    public void run() {
      final Runnable command;
      synchronized (mMutex) {
        command = mState.delayedConsumerComplete(mChannel, mQueue);
      }

      if (command != null) {
        sSyncExecutor.execute(command);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed consumer error.
   */
  private class DelayedConsumerErrorCommand implements Runnable {

    private final RoutineException mAbortException;

    private final Channel<?, ? extends IN> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel bound to this one.
     * @param error   the abortion error.
     */
    private DelayedConsumerErrorCommand(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final RoutineException error) {
      mChannel = channel;
      mAbortException = error;
    }

    public void run() {
      final Runnable command;
      synchronized (mMutex) {
        command = mState.delayedConsumerError(mChannel, mAbortException);
      }

      if (command != null) {
        sSyncExecutor.execute(command);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed input.
   */
  private class DelayedInputCommand implements Runnable {

    private final IN mInput;

    private final NestedQueue<IN> mQueue;

    /**
     * Constructor.
     *
     * @param queue the input queue.
     * @param input the input.
     */
    private DelayedInputCommand(@NotNull final NestedQueue<IN> queue, @Nullable final IN input) {
      mQueue = queue;
      mInput = input;
    }

    public void run() {
      final Runnable command;
      synchronized (mMutex) {
        command = mState.delayedInput(mQueue, mInput);
      }

      if (command != null) {
        sSyncExecutor.execute(command);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed input of a list of data.
   */
  private class DelayedListInputCommand implements Runnable {

    private final ArrayList<IN> mInputs;

    private final NestedQueue<IN> mQueue;

    /**
     * Constructor.
     *
     * @param queue  the input queue.
     * @param inputs the list of input data.
     */
    private DelayedListInputCommand(@NotNull final NestedQueue<IN> queue,
        final ArrayList<IN> inputs) {
      mInputs = inputs;
      mQueue = queue;
    }

    public void run() {
      final Runnable command;
      synchronized (mMutex) {
        command = mState.delayedInputs(mQueue, mInputs);
      }

      if (command != null) {
        sSyncExecutor.execute(command);
      }
    }
  }

  /**
   * Invocation channel internal state (using "state" design pattern).
   */
  private class InputChannelState implements ExecutionObserver<IN> {

    /**
     * Called when the invocation is aborted.
     *
     * @param delay  the input delay.
     * @param reason the reason of the abortion.
     * @return the command to run or null.
     */
    @Nullable
    Runnable abortInvocation(@NotNull final DurationMeasure delay,
        @Nullable final Throwable reason) {
      final RoutineException abortException = AbortException.wrapIfNeeded(reason);
      if (delay.isZero()) {
        mLogger.dbg(reason, "aborting channel");
        internalAbort(abortException);
        mState = new AbortChannelState();
        mMutex.notifyAll();
        return mExecution.abort();
      }

      return new DelayedAbortCommand(abortException);
    }

    /**
     * Called when the invocation is aborted.
     *
     * @return whether the output channel has to be aborted.
     */
    boolean abortOutput() {
      return false;
    }

    /**
     * Called when the invocation is aborted after a delay.
     *
     * @param reason the reason of the abortion.
     * @return the command to run or null.
     */
    @Nullable
    Runnable delayedAbortInvocation(@NotNull final RoutineException reason) {
      mLogger.dbg(reason, "aborting channel after delay");
      internalAbort(reason);
      mState = new AbortChannelState();
      mMutex.notifyAll();
      return mExecution.abort();
    }

    /**
     * Called when the channel is closed after a delay.
     *
     * @return the command to run or null.
     */
    @Nullable
    Runnable delayedClose() {
      mLogger.dbg("closing input channel after delay");
      mState = new ClosedChannelState();
      return mExecution;
    }

    /**
     * Called when the feeding consumer completes after a delay.
     *
     * @param channel the bound channel.
     * @param queue   the input queue.
     * @return the command to run or null.
     */
    @Nullable
    Runnable delayedConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue) {
      mLogger.dbg("closing consumer after delay");
      mBoundChannels.remove(channel);
      queue.close();
      if (!mIsPendingExecution) {
        mIsPendingExecution = true;
        return mExecution;
      }

      --mPendingExecutionCount;
      return null;
    }

    /**
     * Called when the feeding consumer receives an error after a delay.
     *
     * @param channel the bound channel.
     * @param error   the error.
     * @return the command to run or null.
     */
    @Nullable
    Runnable delayedConsumerError(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final RoutineException error) {
      mLogger.dbg(error, "aborting consumer after delay");
      mBoundChannels.remove(channel);
      internalAbort(error);
      mState = new AbortChannelState();
      return mExecution.abort();
    }

    /**
     * Called when an input is passed to the invocation after a delay.
     *
     * @param queue the input queue.
     * @param input the input.
     * @return the command to run or null.
     */
    @Nullable
    Runnable delayedInput(@NotNull final NestedQueue<IN> queue, @Nullable final IN input) {
      mLogger.dbg("delayed input command: %s", input);
      queue.add(input);
      queue.close();
      return mExecution;
    }

    /**
     * Called when some inputs are passed to the invocation after a delay.
     *
     * @param queue  the input queue.
     * @param inputs the inputs.
     * @return the command to run or null.
     */
    @Nullable
    Runnable delayedInputs(@NotNull final NestedQueue<IN> queue, final List<IN> inputs) {
      mLogger.dbg("delayed input command: %s", inputs);
      queue.addAll(inputs);
      queue.close();
      return mExecution;
    }

    /**
     * Returns the number of inputs still to be processed.
     *
     * @return the input count.
     */
    int inputCount() {
      return mInputCount;
    }

    /**
     * Called to know if the channel is open.
     *
     * @return whether the channel is open.
     */
    boolean isChannelOpen() {
      return true;
    }

    /**
     * Called when the binding of channel fails.
     */
    void onBindFailure() {
      --mPendingExecutionCount;
    }

    /**
     * Called when the inputs are complete.
     *
     * @param delay the input delay value.
     * @return the command to run or null.
     */
    @Nullable
    Runnable onClose(@NotNull final DurationMeasure delay) {
      if (delay.isZero()) {
        mLogger.dbg("closing input channel");
        mState = new ClosedChannelState();
        if (!mIsPendingExecution) {
          ++mPendingExecutionCount;
          mIsPendingExecution = true;
          return mExecution;
        }

        return null;
      }

      ++mPendingExecutionCount;
      return new DelayedCloseCommand();
    }

    /**
     * Called when the feeding consumer completes.
     *
     * @param channel  the bound channel.
     * @param queue    the input queue.
     * @param delay    the input delay value.
     * @param timeUnit the input delay unit.
     * @return the command to run or null.
     */
    @Nullable
    Runnable onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final NestedQueue<IN> queue, final long delay, @NotNull final TimeUnit timeUnit) {
      if (delay == 0) {
        mLogger.dbg("closing consumer");
        mBoundChannels.remove(channel);
        queue.close();
        if (!mIsPendingExecution) {
          mIsPendingExecution = true;
          return mExecution;
        }

        --mPendingExecutionCount;
        return null;
      }

      return new DelayedConsumerCompleteCommand(channel, queue);
    }

    /**
     * Called when the feeding consumer receives an error.
     *
     * @param channel  the bound channel.
     * @param error    the error.
     * @param delay    the input delay value.
     * @param timeUnit the input delay unit.
     * @return the command to run or null.
     */
    @Nullable
    Runnable onConsumerError(@NotNull final Channel<?, ? extends IN> channel,
        @NotNull final RoutineException error, final long delay, @NotNull final TimeUnit timeUnit) {
      if (delay == 0) {
        mLogger.dbg(error, "aborting consumer");
        mBoundChannels.remove(channel);
        internalAbort(error);
        mState = new AbortChannelState();
        return mExecution.abort();
      }

      return new DelayedConsumerErrorCommand(channel, error);
    }

    /**
     * Called when the feeding consumer receives an output.
     *
     * @param input     the input.
     * @param queue     the input queue.
     * @param orderType the input order type.
     * @param delay     the input delay value.
     * @param timeUnit  the input delay unit.
     * @return the command to run or null.
     */
    @Nullable
    Runnable onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
        @NotNull final OrderType orderType, final long delay, @NotNull final TimeUnit timeUnit) {
      mLogger.dbg("consumer input [#%d+1]: %s [%d %s]", mInputCount, input, delay, timeUnit);
      ++mInputCount;
      checkMaxSize();
      if (delay == 0) {
        queue.add(input);
        if (!mIsPendingExecution) {
          ++mPendingExecutionCount;
          mIsPendingExecution = true;
          return mExecution;
        }

        return null;
      }

      ++mPendingExecutionCount;
      return new DelayedInputCommand((orderType != OrderType.UNSORTED) ? queue.addNested() : queue,
          input);
    }

    /**
     * Called when the invocation is aborted through the registered handler.
     *
     * @param reason   the reason of the abortion.
     * @param delay    the input delay value.
     * @param timeUnit the input delay unit.
     * @return the command to run or null.
     */
    @Nullable
    Runnable onHandlerAbort(@NotNull final RoutineException reason, final long delay,
        @NotNull final TimeUnit timeUnit) {
      final RoutineException abortException = AbortException.wrapIfNeeded(reason);
      if (delay == 0) {
        mLogger.dbg(reason, "aborting channel");
        internalAbort(abortException);
        mState = new AbortChannelState();
        mMutex.notifyAll();
        return mExecution.abort();
      }

      return new DelayedAbortCommand(abortException);
    }

    /**
     * Called when a channel is passed to the invocation.
     *
     * @param channel the channel instance.
     * @return the channel consumer to bind or null.
     */
    @Nullable
    ChannelConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
      if (channel == null) {
        mLogger.wrn("passing null channel");
        return null;
      }

      ++mPendingExecutionCount;
      mBoundChannels.put(channel, null);
      mLogger.dbg("passing channel: %s", channel);
      return new DefaultChannelConsumer(channel);
    }

    /**
     * Called when some inputs are passed to the invocation.
     *
     * @param inputs the inputs.
     * @param delay  the input delay.
     * @return the command to run or null.
     */
    @Nullable
    Runnable pass(@Nullable final Iterable<? extends IN> inputs,
        @NotNull final DurationMeasure delay) {
      if (inputs == null) {
        mLogger.wrn("passing null iterable");
        return null;
      }

      final ArrayList<IN> list = new ArrayList<IN>();
      for (final IN input : inputs) {
        list.add(input);
      }

      final int size = list.size();
      mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mInputCount, size, inputs, delay);
      mInputCount += size;
      checkMaxSize();
      if (delay.isZero()) {
        mInputQueue.addAll(list);
        if (!mIsPendingExecution) {
          ++mPendingExecutionCount;
          mIsPendingExecution = true;
          return mExecution;
        }

        return null;
      }

      ++mPendingExecutionCount;
      return new DelayedListInputCommand(
          (mInputOrder.get() != OrderType.UNSORTED) ? mInputQueue.addNested() : mInputQueue, list);
    }

    /**
     * Called when an input is passed to the invocation.
     *
     * @param input the input.
     * @param delay the input delay.
     * @return the command to run or null.
     */
    @Nullable
    Runnable pass(@Nullable final IN input, @NotNull final DurationMeasure delay) {
      mLogger.dbg("passing input [#%d+1]: %s [%s]", mInputCount, input, delay);
      ++mInputCount;
      checkMaxSize();
      if (delay.isZero()) {
        mInputQueue.add(input);
        if (!mIsPendingExecution) {
          ++mPendingExecutionCount;
          mIsPendingExecution = true;
          return mExecution;
        }

        return null;
      }

      ++mPendingExecutionCount;
      return new DelayedInputCommand(
          (mInputOrder.get() != OrderType.UNSORTED) ? mInputQueue.addNested() : mInputQueue, input);
    }

    /**
     * Called when some inputs are passed to the invocation.
     *
     * @param inputs the inputs.
     * @param delay  the input delay.
     * @return the command to run or null.
     */
    @Nullable
    Runnable pass(@Nullable final IN[] inputs, @NotNull final DurationMeasure delay) {
      if (inputs == null) {
        mLogger.wrn("passing null input array");
        return null;
      }

      final int size = inputs.length;
      mLogger.dbg("passing array [#%d+%d]: %s [%s]", mInputCount, size, Arrays.toString(inputs),
          delay);
      mInputCount += size;
      checkMaxSize();
      final ArrayList<IN> list = new ArrayList<IN>(size);
      Collections.addAll(list, inputs);
      if (delay.isZero()) {
        mInputQueue.addAll(list);
        if (!mIsPendingExecution) {
          ++mPendingExecutionCount;
          mIsPendingExecution = true;
          return mExecution;
        }

        return null;
      }

      ++mPendingExecutionCount;
      return new DelayedListInputCommand(
          (mInputOrder.get() != OrderType.UNSORTED) ? mInputQueue.addNested() : mInputQueue, list);
    }

    @NotNull
    public RoutineException getAbortException() {
      return InvocationException.wrapIfNeeded(mAbortException);
    }

    public void onAbortComplete() {
    }

    public boolean onConsumeComplete() {
      return false;
    }

    public boolean onFirstInput(@NotNull final InputData<IN> inputData) {
      --mPendingExecutionCount;
      mIsPendingExecution = false;
      return onNextInput(inputData);
    }

    public void onInvocationComplete() {
      mState = new CompleteChannelState();
    }

    public boolean onNextInput(@NotNull final InputData<IN> inputData) {
      final NestedQueue<IN> inputQueue = mInputQueue;
      if (!inputQueue.isEmpty()) {
        final IN input = inputQueue.removeFirst();
        mLogger.dbg("consuming input [#%d]: %s", mInputCount, input);
        final int inputCount = --mInputCount;
        if (mIsWaitingInput && (mInputBackoff.getDelay(inputCount) == NO_DELAY)) {
          mMutex.notifyAll();
        }

        inputData.data = input;
        return true;
      }

      return false;
    }
  }
}
