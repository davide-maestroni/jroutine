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

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.InvocationDeadlockException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.DurationMeasure.Condition;
import com.github.dm.jrt.core.util.LocalFence;
import com.github.dm.jrt.core.util.LocalField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.common.Backoff.NO_DELAY;
import static com.github.dm.jrt.core.util.DurationMeasure.fromUnit;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Class handling the invocation output.
 * <p>
 * This class centralizes the managing of data passing through the routine output and result
 * channels, since, logically, the two objects are part of the same entity. In fact, on one end the
 * result channel puts data into the output queue and, on the other end, the output channel reads
 * them from the same queue.
 * <p>
 * Created by davide-maestroni on 06/12/2015.
 *
 * @param <OUT> the output data type.
 */
class ResultChannel<OUT> implements Channel<OUT, OUT> {

  private static final String INVOCATION_DEADLOCK_MESSAGE =
      "cannot wait while no invocation instance is available"
          + "\nTry increasing the max number of instances";

  private static final LocalFence sInvocationFence = new LocalFence();

  private final ArrayList<Channel<?, ? extends OUT>> mBoundChannels =
      new ArrayList<Channel<?, ? extends OUT>>();

  private final ScheduledExecutor mExecutor;

  private final Flusher<OUT> mFlusher;

  private final AbortHandler mHandler;

  private final Condition mHasOutputs;

  private final Logger mLogger;

  private final int mMaxOutput;

  private final Object mMutex = new Object();

  private final Backoff mOutputBackoff;

  private final NestedQueue<Object> mOutputQueue;

  private final LocalField<DurationMeasure> mOutputTimeout;

  private final LocalField<DurationMeasure> mResultDelay;

  private final LocalField<OrderType> mResultOrder;

  private final LocalField<TimeoutActionType> mTimeoutActionType;

  private final LocalField<Throwable> mTimeoutException = new LocalField<Throwable>(null);

  private RoutineException mAbortException;

  private BindingHandler<OUT> mBindingHandler;

  private volatile FlushCommand mFlushCommand;

  private volatile FlushCommand mForcedFlushCommand;

  private boolean mIWaitingOutput;

  private Condition mIsComplete;

  private Condition mIsError;

  private boolean mIsWaitingInvocation;

  private int mOutputCount;

  private Condition mOutputHasNext;

  private Condition mOutputNotEmpty;

  private int mPendingOutputCount;

  private OutputChannelState mState;

  /**
   * Constructor.
   *
   * @param executor      the executor instance.
   * @param configuration the channel configuration.
   * @param handler       the abort handler.
   * @param logger        the logger instance.
   */
  ResultChannel(@NotNull final ScheduledExecutor executor,
      @NotNull final ChannelConfiguration configuration, @NotNull final AbortHandler handler,
      @NotNull final Logger logger) {
    this(executor, configuration.getOrderTypeOrElse(OrderType.UNSORTED),
        configuration.getMaxSizeOrElse(Integer.MAX_VALUE), configuration.getBackoffOrElse(null),
        configuration.getOutputTimeoutOrElse(noTime()),
        configuration.getOutputTimeoutActionOrElse(TimeoutActionType.FAIL), handler, logger);
  }

  /**
   * Constructor.
   *
   * @param executor      the executor instance.
   * @param configuration the invocation configuration.
   * @param handler       the abort handler.
   * @param logger        the logger instance.
   */
  ResultChannel(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration, @NotNull final AbortHandler handler,
      @NotNull final Logger logger) {
    this(executor, configuration.getOutputOrderTypeOrElse(OrderType.UNSORTED),
        configuration.getOutputMaxSizeOrElse(Integer.MAX_VALUE),
        configuration.getOutputBackoffOrElse(null), configuration.getOutputTimeoutOrElse(noTime()),
        configuration.getOutputTimeoutActionOrElse(TimeoutActionType.FAIL), handler, logger);
  }

  /**
   * Constructor.
   *
   * @param executor          the executor instance.
   * @param orderType         the channel order type.
   * @param maxOutput         the maximum output size.
   * @param backoff           the channel backoff or null.
   * @param outputTimeout     the output timeout.
   * @param timeoutActionType the timeout action type.
   * @param handler           the abort handler.
   * @param logger            the logger instance.
   */
  private ResultChannel(@NotNull final ScheduledExecutor executor,
      @NotNull final OrderType orderType, final int maxOutput, @Nullable final Backoff backoff,
      @NotNull final DurationMeasure outputTimeout,
      @NotNull final TimeoutActionType timeoutActionType, @NotNull final AbortHandler handler,
      @NotNull final Logger logger) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mHandler = ConstantConditions.notNull("abort handler", handler);
    mLogger = logger.subContextLogger(this);
    mFlusher = executor.isSynchronous() ? new SyncFlusher() : new AsyncFlusher();
    mResultOrder = new LocalField<OrderType>(orderType);
    mResultDelay = new LocalField<DurationMeasure>(noTime());
    mOutputTimeout = new LocalField<DurationMeasure>(outputTimeout);
    mTimeoutActionType = new LocalField<TimeoutActionType>(timeoutActionType);
    mMaxOutput = maxOutput;
    mOutputQueue = new NestedQueue<Object>() {

      @Override
      void close() {
        // Preventing closing
      }
    };
    final Backoff outputBackoff =
        mOutputBackoff = (backoff != null) ? backoff : BackoffBuilder.noDelay();
    mHasOutputs = (backoff != null) ? new Condition() {

      public boolean isTrue() {
        return (outputBackoff.getDelay(mOutputCount) == NO_DELAY) || mIsWaitingInvocation || (
            mAbortException != null);
      }
    } : new Condition() {

      public boolean isTrue() {
        return true;
      }
    };
    mBindingHandler = new OutputHandler();
    mState = new OutputChannelState();
  }

  public boolean abort() {
    return abort(null);
  }

  public boolean abort(@Nullable final Throwable reason) {
    final DurationMeasure delay = mResultDelay.get();
    final RoutineException abortException;
    synchronized (mMutex) {
      abortException = mState.abortInvocation(reason, delay);
    }

    if (abortException != null) {
      if (delay.isZero()) {
        mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);

      } else {
        mExecutor.execute(new DelayedAbortCommand(abortException), delay.value, delay.unit);
      }

      return true;
    }

    return false;
  }

  @NotNull
  public Channel<OUT, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
    return after(fromUnit(delay, timeUnit));
  }

  @NotNull
  public Channel<OUT, OUT> after(@NotNull final DurationMeasure delay) {
    mResultDelay.set(ConstantConditions.notNull("result delay", delay));
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> afterNoDelay() {
    return after(noTime());
  }

  @NotNull
  public List<OUT> all() {
    final ArrayList<OUT> results = new ArrayList<OUT>();
    allInto(results);
    return results;
  }

  @NotNull
  public Channel<OUT, OUT> allInto(@NotNull final Collection<? super OUT> results) {
    ConstantConditions.notNull("results collection", results);
    final Iterator<OUT> iterator = expiringIterator();
    while (iterator.hasNext()) {
      results.add(iterator.next());
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> close() {
    final DurationMeasure delay = mResultDelay.get();
    final boolean isOpen;
    final Runnable command;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      final OutputChannelState state = mState;
      isOpen = state.isOpen();
      command = state.closeResultChannel(delay);
      handler = mBindingHandler;
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);

    } else if (isOpen) {
      mFlusher.run(handler, false);
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> consume(@NotNull final ChannelConsumer<? super OUT> consumer) {
    final boolean forceClose;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      verifyBound();
      forceClose = mState.isDone();
      handler = (mBindingHandler =
          new ConsumerHandler(ConstantConditions.notNull("channel consumer", consumer)));
      mMutex.notifyAll();
    }

    mFlusher.run(handler, forceClose);
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> eventuallyAbort() {
    return eventuallyAbort(null);
  }

  @NotNull
  public Channel<OUT, OUT> eventuallyAbort(@Nullable final Throwable reason) {
    mTimeoutActionType.set(TimeoutActionType.ABORT);
    mTimeoutException.set(reason);
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> eventuallyContinue() {
    mTimeoutActionType.set(TimeoutActionType.CONTINUE);
    mTimeoutException.set(null);
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> eventuallyFail() {
    mTimeoutActionType.set(TimeoutActionType.FAIL);
    mTimeoutException.set(null);
    return this;
  }

  @NotNull
  public Iterator<OUT> expiringIterator() {
    verifyBound();
    final DurationMeasure outputTimeout = mOutputTimeout.get();
    return new ExpiringIterator(outputTimeout.value, outputTimeout.unit, mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  public OUT get() {
    OUT output = null;
    boolean hasOutputs = false;
    if (getComplete()) {
      for (final OUT out : this) {
        hasOutputs = true;
        output = out;
      }

    } else {
      final TimeoutActionType actionType = mTimeoutActionType.get();
      if (actionType == TimeoutActionType.ABORT) {
        abort(mTimeoutException.get());

      } else if (actionType == TimeoutActionType.FAIL) {
        throw new OutputTimeoutException(
            "timeout while waiting for outputs [" + mOutputTimeout.get() + "]");
      }
    }

    if (!hasOutputs) {
      throw new NoSuchElementException("no available output");
    }

    return output;
  }

  public boolean getComplete() {
    synchronized (mMutex) {
      if (mState.isDone()) {
        return true;
      }

      final DurationMeasure outputTimeout = mOutputTimeout.get();
      final long timeout = outputTimeout.value;
      if (timeout > 0) {
        checkCanWait();
      }

      if (mIsComplete == null) {
        mIsComplete = new Condition() {

          public boolean isTrue() {
            return mState.isDone() || mIsWaitingInvocation;
          }
        };
      }

      final TimeUnit timeoutUnit = outputTimeout.unit;
      final boolean isDone;
      try {
        isDone = DurationMeasure.waitUntil(mMutex, mIsComplete, timeout, timeoutUnit);

      } catch (final InterruptedException e) {
        throw new InterruptedInvocationException(e);
      }

      if (isDone) {
        if (mIsWaitingInvocation && !mState.isDone()) {
          throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
        }

      } else {
        mLogger.wrn("waiting done timeout: [%d %s]", timeout, timeoutUnit);
      }

      return isDone;
    }
  }

  @Nullable
  public RoutineException getError() {
    synchronized (mMutex) {
      if (mState.isDone()) {
        return mAbortException;
      }

      final DurationMeasure outputTimeout = mOutputTimeout.get();
      final long timeout = outputTimeout.value;
      if (timeout > 0) {
        checkCanWait();
      }

      if (mIsError == null) {
        mIsError = new Condition() {

          public boolean isTrue() {
            return mState.isDone() || (mAbortException != null) || mIsWaitingInvocation;
          }
        };
      }

      final TimeUnit timeoutUnit = outputTimeout.unit;
      final boolean isDone;
      try {
        isDone = DurationMeasure.waitUntil(mMutex, mIsError, timeout, timeoutUnit);

      } catch (final InterruptedException e) {
        throw new InterruptedInvocationException(e);
      }

      if (isDone) {
        if (mIsWaitingInvocation && !mState.isDone() && (mAbortException == null)) {
          throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
        }

      } else {
        mLogger.wrn("waiting error timeout: [%d %s]", timeout, timeoutUnit);
      }

      return mAbortException;
    }
  }

  public boolean hasNext() {
    final DurationMeasure outputTimeout = mOutputTimeout.get();
    return isNextAvailable(outputTimeout.value, outputTimeout.unit, mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  public OUT next() {
    final DurationMeasure outputTimeout = mOutputTimeout.get();
    return readNext(outputTimeout.value, outputTimeout.unit, mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  @NotNull
  public Channel<OUT, OUT> in(final long timeout, @NotNull final TimeUnit timeUnit) {
    return in(fromUnit(timeout, timeUnit));
  }

  @NotNull
  public Channel<OUT, OUT> in(@NotNull final DurationMeasure timeout) {
    mOutputTimeout.set(ConstantConditions.notNull("output timeout", timeout));
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> inNoTime() {
    return in(noTime());
  }

  public boolean isBound() {
    return getBindingHandler().isBound();
  }

  public boolean isEmpty() {
    return (size() == 0);
  }

  public boolean isOpen() {
    synchronized (mMutex) {
      return mState.isOpen();
    }
  }

  @NotNull
  public List<OUT> next(final int count) {
    if (count <= 0) {
      return new ArrayList<OUT>(0);
    }

    final ArrayList<OUT> results = new ArrayList<OUT>();
    final Iterator<OUT> iterator = expiringIterator();
    for (int i = 0; i < count && iterator.hasNext(); ++i) {
      results.add(iterator.next());
    }

    return results;
  }

  public OUT nextOrElse(final OUT output) {
    try {
      return next();

    } catch (final NoSuchElementException ignored) {
    }

    return output;
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
    final ChannelConsumer<OUT> consumer;
    synchronized (mMutex) {
      consumer = mState.pass(channel);
    }

    if ((consumer != null) && (channel != null)) {
      channel.consume(consumer);
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final Iterable<? extends OUT> outputs) {
    final DurationMeasure delay = mResultDelay.get();
    final Runnable command;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      command = mState.pass(outputs, delay);
      handler = mBindingHandler;
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);

    } else {
      mFlusher.run(handler, false);
    }

    synchronized (mMutex) {
      if (!mHasOutputs.isTrue()) {
        waitOutputs();
      }
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final OUT output) {
    final DurationMeasure delay = mResultDelay.get();
    final Runnable command;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      command = mState.pass(output, delay);
      handler = mBindingHandler;
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);

    } else {
      mFlusher.run(handler, false);
    }

    synchronized (mMutex) {
      if (!mHasOutputs.isTrue()) {
        waitOutputs();
      }
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final OUT... outputs) {
    final DurationMeasure delay = mResultDelay.get();
    final Runnable command;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      command = mState.pass(outputs, delay);
      handler = mBindingHandler;
    }

    if (command != null) {
      mExecutor.execute(command, delay.value, delay.unit);

    } else {
      mFlusher.run(handler, false);
    }

    synchronized (mMutex) {
      if (!mHasOutputs.isTrue()) {
        waitOutputs();
      }
    }

    return this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <AFTER> Channel<OUT, AFTER> pipe(@NotNull final Channel<? super OUT, AFTER> channel) {
    ((Channel<OUT, AFTER>) channel).pass(this);
    return new FlatChannel<OUT, AFTER>(this, channel);
  }

  public int size() {
    synchronized (mMutex) {
      return mOutputCount;
    }
  }

  @NotNull
  public Channel<OUT, OUT> skipNext(final int count) {
    if (count > 0) {
      final Iterator<OUT> iterator = expiringIterator();
      try {
        for (int i = 0; i < count; ++i) {
          iterator.next();
        }

      } catch (final NoSuchElementException ignored) {
        final DurationMeasure outputTimeout = mOutputTimeout.get();
        final long timeout = outputTimeout.value;
        final TimeUnit timeoutUnit = outputTimeout.unit;
        final TimeoutActionType timeoutAction = mTimeoutActionType.get();
        mLogger.wrn("skipping output timeout: [%d %s] => [%s]", timeout, timeoutUnit,
            timeoutAction);
        if (timeoutAction == TimeoutActionType.FAIL) {
          throw new OutputTimeoutException(
              "timeout while waiting for outputs [" + timeout + " " + timeoutUnit + "]");

        } else if (timeoutAction == TimeoutActionType.ABORT) {
          final Throwable timeoutException = mTimeoutException.get();
          final RoutineException abortException = AbortException.wrapIfNeeded(timeoutException);
          abortImmediately(abortException);
          throw abortException;
        }
      }
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> sorted() {
    synchronized (mMutex) {
      mState.orderBy(OrderType.SORTED);
    }

    return this;
  }

  public void throwError() {
    final RoutineException error = getError();
    if (error != null) {
      throw error;
    }
  }

  @NotNull
  public Channel<OUT, OUT> unsorted() {
    synchronized (mMutex) {
      mState.orderBy(OrderType.UNSORTED);
    }

    return this;
  }

  @NotNull
  public Iterator<OUT> iterator() {
    verifyBound();
    final DurationMeasure outputTimeout = mOutputTimeout.get();
    return new DefaultIterator(outputTimeout.value, outputTimeout.unit, mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  /**
   * Aborts immediately the execution.
   *
   * @param reason the reason of the abortion.
   * @see com.github.dm.jrt.core.channel.Channel#abort(Throwable) Channel.abort(Throwable)
   */
  boolean abortImmediately(@Nullable final Throwable reason) {
    RoutineException abortException = InvocationException.wrapIfNeeded(reason);
    synchronized (mMutex) {
      abortException = mState.abortInvocation(abortException, noTime());
    }

    if (abortException != null) {
      mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
      return true;
    }

    return false;
  }

  /**
   * Closes this channel with the specified exception.
   *
   * @param throwable the exception.
   */
  void close(@Nullable final Throwable throwable) {
    final ArrayList<Channel<?, ? extends OUT>> channels =
        new ArrayList<Channel<?, ? extends OUT>>();
    final RoutineException abortException;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      abortException = mState.closeInvocation(throwable, channels);
      handler = mBindingHandler;
    }

    if (abortException != null) {
      for (final Channel<?, ? extends OUT> channel : channels) {
        channel.afterNoDelay().abort(abortException);
      }

      mFlusher.run(handler, false);
    }
  }

  /**
   * Closes immediately the channel.
   */
  void closeImmediately() {
    final boolean needsFlush;
    final BindingHandler<OUT> handler;
    synchronized (mMutex) {
      needsFlush = mState.delayedCloseResultChannel();
      handler = mBindingHandler;
    }

    if (needsFlush) {
      mFlusher.run(handler, false);
    }
  }

  /**
   * Notifies to this channel that the invocation execution is entering.
   */
  void enterInvocation() {
    sInvocationFence.enter();
  }

  /**
   * Notifies to this channel that the invocation execution is exiting.
   */
  void exitInvocation() {
    sInvocationFence.exit();
  }

  /**
   * Tells the channel that the invocation instance is not available.
   */
  void startWaitingInvocation() {
    synchronized (mMutex) {
      mIsWaitingInvocation = true;
      mMutex.notifyAll();
    }
  }

  /**
   * Tells the channel that the invocation instance became available.
   */
  void stopWaitingInvocation() {
    synchronized (mMutex) {
      mIsWaitingInvocation = false;
    }
  }

  private void checkCanWait() {
    if (sInvocationFence.isInside()) {
      throw new ExecutionDeadlockException(
          "cannot wait inside an invocation: " + Thread.currentThread()
              + "\nTry binding the output channel to another channel or an output consumer");
    }

    if (ScheduledExecutor.isManagedThread()) {
      throw new ExecutionDeadlockException(
          "cannot wait on a executor thread: " + Thread.currentThread()
              + "\nTry binding the output channel to another channel or an output consumer");
    }

    if (mIsWaitingInvocation) {
      throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
    }
  }

  private void checkMaxSize() {
    if (mOutputCount > mMaxOutput) {
      throw new OutputDeadlockException(
          "maximum output channel size has been exceeded: " + mMaxOutput);
    }
  }

  private void closeConsumer(@NotNull final OutputChannelState state,
      @NotNull final ChannelConsumer<? super OUT> consumer) {
    state.closeConsumer(consumer);
    synchronized (mMutex) {
      final OutputChannelState currentState = mState;
      if (currentState.isReadyToComplete()) {
        mState = currentState.toDoneState();
        mMutex.notifyAll();
      }
    }
  }

  @NotNull
  private BindingHandler<OUT> getBindingHandler() {
    synchronized (mMutex) {
      return mBindingHandler;
    }
  }

  @NotNull
  private Runnable getFlushCommand(final boolean forceClose) {
    final FlushCommand command;
    if (forceClose) {
      if (mForcedFlushCommand == null) {
        mForcedFlushCommand = new FlushCommand(true);
      }

      command = mForcedFlushCommand;

    } else {
      if (mFlushCommand == null) {
        mFlushCommand = new FlushCommand(false);
      }

      command = mFlushCommand;
    }

    return command;
  }

  private void internalAbort(@NotNull final RoutineException abortException) {
    if (mAbortException == null) {
      mAbortException = abortException;
    }

    mState = new ExceptionChannelState();
    mMutex.notifyAll();
  }

  private boolean isNextAvailable(final long timeout, @NotNull final TimeUnit timeUnit,
      @NotNull final TimeoutActionType timeoutAction, @Nullable final Throwable timeoutException) {
    boolean isAbort = false;
    synchronized (mMutex) {
      verifyBound();
      final Logger logger = mLogger;
      final NestedQueue<Object> outputQueue = mOutputQueue;
      final boolean isDone = mState.isDone();
      final boolean hasOutputs = !outputQueue.isEmpty();
      if (isDone || hasOutputs) {
        logger.dbg("has output: %s [%d %s]", hasOutputs, timeout, timeUnit);
        return hasOutputs;

      } else if (timeout == 0) {
        logger.wrn("has output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
        if (timeoutAction == TimeoutActionType.FAIL) {
          throw new OutputTimeoutException(
              "timeout while waiting to know if more outputs are coming [" + timeout + " "
                  + timeUnit + "]");

        } else {
          isAbort = (timeoutAction == TimeoutActionType.ABORT);
        }

      } else {
        checkCanWait();
        if (mOutputHasNext == null) {
          mOutputHasNext = new Condition() {

            public boolean isTrue() {
              verifyBound();
              return !outputQueue.isEmpty() || mState.isDone() || mIsWaitingInvocation;
            }
          };
        }

        final boolean isTimeout;
        try {
          isTimeout = !DurationMeasure.waitUntil(mMutex, mOutputHasNext, timeout, timeUnit);

        } catch (final InterruptedException e) {
          throw new InterruptedInvocationException(e);
        }

        if (!isTimeout) {
          if (mIsWaitingInvocation && outputQueue.isEmpty() && !mState.isDone()) {
            throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
          }

        } else {
          logger.wrn("has output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
          if (timeoutAction == TimeoutActionType.FAIL) {
            throw new OutputTimeoutException(
                "timeout while waiting to know if more outputs are coming [" + timeout + " "
                    + timeUnit + "]");

          } else {
            isAbort = (timeoutAction == TimeoutActionType.ABORT);
          }
        }
      }

      if (!isAbort) {
        final boolean hasNext = !outputQueue.isEmpty();
        logger.dbg("has output: %s [%d %s]", hasNext, timeout, timeUnit);
        return hasNext;
      }
    }

    final RoutineException abortException = AbortException.wrapIfNeeded(timeoutException);
    abortImmediately(abortException);
    throw abortException;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private OUT nextOutput(final long timeout, @NotNull final TimeUnit timeUnit) {
    final NestedQueue<Object> outputQueue = mOutputQueue;
    final Object result = outputQueue.removeFirst();
    mLogger.dbg("reading output [#%d]: %s [%d %s]", mOutputCount, result, timeout, timeUnit);
    if (result instanceof RoutineExceptionWrapper) {
      outputQueue.clear();
      outputQueue.add(result);
      throw ((RoutineExceptionWrapper) result).raise();
    }

    final int outputCount = --mOutputCount;
    if (mIWaitingOutput && (mOutputBackoff.getDelay(outputCount) == NO_DELAY)) {
      mMutex.notifyAll();
    }

    return (OUT) result;
  }

  @Nullable
  private OUT readNext(final long timeout, @NotNull final TimeUnit timeUnit,
      @NotNull final TimeoutActionType timeoutAction, @Nullable final Throwable timeoutException) {
    final boolean isTimeout;
    synchronized (mMutex) {
      verifyBound();
      final Logger logger = mLogger;
      final NestedQueue<Object> outputQueue = mOutputQueue;
      boolean isAbort = false;
      if ((timeout == 0) || !outputQueue.isEmpty()) {
        if (outputQueue.isEmpty()) {
          logger.wrn("reading output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
          if (timeoutAction == TimeoutActionType.FAIL) {
            throw new OutputTimeoutException(
                "timeout while waiting for outputs [" + timeout + " " + timeUnit + "]");
          }

          isAbort = (timeoutAction == TimeoutActionType.ABORT);
        }

      } else {
        checkCanWait();
        if (mOutputNotEmpty == null) {
          mOutputNotEmpty = new Condition() {

            public boolean isTrue() {
              verifyBound();
              return !outputQueue.isEmpty() || mState.isDone() || mIsWaitingInvocation;
            }
          };
        }

        try {
          isTimeout = !DurationMeasure.waitUntil(mMutex, mOutputNotEmpty, timeout, timeUnit);

        } catch (final InterruptedException e) {
          throw new InterruptedInvocationException(e);
        }

        if (!isTimeout) {
          if (mIsWaitingInvocation && outputQueue.isEmpty() && !mState.isDone()) {
            throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
          }

        } else {
          logger.wrn("reading output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
          if (timeoutAction == TimeoutActionType.FAIL) {
            throw new OutputTimeoutException(
                "timeout while waiting for outputs [" + timeout + " " + timeUnit + "]");
          }

          isAbort = (timeoutAction == TimeoutActionType.ABORT);
        }
      }

      if (!isAbort) {
        return nextOutput(timeout, timeUnit);
      }
    }

    final RoutineException abortException = AbortException.wrapIfNeeded(timeoutException);
    abortImmediately(abortException);
    throw abortException;
  }

  private void verifyBound() {
    if (isBound()) {
      mLogger.err("invalid call on bound channel");
      throw new IllegalStateException("the channel is already bound");
    }
  }

  private void waitOutputs() {
    try {
      final long delay = mOutputBackoff.getDelay(mOutputCount);
      if (delay == NO_DELAY) {
        return;
      }

      mIWaitingOutput = true;
      if (!DurationMeasure.waitUntil(mMutex, mHasOutputs, delay, TimeUnit.MILLISECONDS)) {
        mLogger.wrn("timeout while waiting for room in the output channel [%s %s]", delay,
            TimeUnit.MILLISECONDS);
      }

    } catch (final InterruptedException e) {
      throw new InterruptedInvocationException(e);

    } finally {
      mIWaitingOutput = false;
    }
  }

  /**
   * Interface defining an abort handler.
   */
  interface AbortHandler {

    /**
     * Called on an abort.
     *
     * @param reason   the reason of the abortion.
     * @param delay    the abortion delay.
     * @param timeUnit the delay time unit.
     */
    void onAbort(@NotNull RoutineException reason, long delay, @NotNull TimeUnit timeUnit);
  }

  /**
   * Interface describing an handler of the channel binding state.
   *
   * @param <OUT> the output data type.
   */
  private interface BindingHandler<OUT> {

    /**
     * Flushes the outputs currently in the queue.
     *
     * @param forceClose whether to force the completion.
     */
    void flushOutput(boolean forceClose);

    /**
     * Returns the bound channel consumer.
     *
     * @return the consumer or null.
     */
    @Nullable
    ChannelConsumer<? super OUT> getConsumer();

    /**
     * Returns the consumer delay.
     *
     * @return the consumer delay.
     */
    @NotNull
    DurationMeasure getDelay();

    /**
     * Check if a consumer has been bound to the channel.
     *
     * @return whether the channel is bound.
     */
    boolean isBound();

    /**
     * Check if the consumer is delayed.
     *
     * @return whether the consumer is delayed.
     */
    boolean isDelayed();
  }

  /**
   * Interface describing an object responsible for the flushing of output data.
   *
   * @param <OUT> the output data type.
   */
  private interface Flusher<OUT> {

    /**
     * Runs the flushing process by employing the specified handler.
     *
     * @param handler    the binding handler.
     * @param forceClose whether to forcedly close the consumer.
     */
    void run(@NotNull BindingHandler<OUT> handler, boolean forceClose);
  }

  /**
   * The invocation has been aborted and the exception put into the output queue.
   */
  private class AbortChannelState extends ExceptionChannelState {

    @Nullable
    @Override
    RoutineException abortConsumer(@NotNull final Throwable reason) {
      final RoutineException abortException = InvocationException.wrapIfNeeded(reason);
      mLogger.wrn(reason, "aborting on consumer exception (%s)", mBindingHandler.getConsumer());
      internalAbort(abortException);
      return abortException;
    }

    @Override
    boolean isReadyToComplete() {
      return true;
    }

    @Nullable
    @Override
    RoutineException closeInvocation(@Nullable final Throwable throwable,
        @NotNull final ArrayList<Channel<?, ? extends OUT>> channels) {
      mLogger.dbg("avoid aborting result channel since already aborted");
      return null;
    }

    @Override
    void closeConsumer(@NotNull final ChannelConsumer<? super OUT> consumer) {
    }
  }

  /**
   * The invocation has completed with an abortion exception.
   */
  private class AbortedChannelState extends AbortChannelState {

    @Override
    boolean isReadyToComplete() {
      return false;
    }

    @Override
    boolean isDone() {
      return true;
    }

    @NotNull
    @Override
    OutputChannelState toDoneState() {
      return this;
    }
  }

  /**
   * Flusher implementation handling an asynchronous executor.
   */
  private class AsyncFlusher implements Flusher<OUT> {

    public void run(@NotNull final BindingHandler<OUT> handler, final boolean forceClose) {
      // Need to make sure to pass the outputs to the consumer in the executor thread, so to avoid
      // deadlock issues
      final ScheduledExecutor executor = mExecutor;
      if (handler.isDelayed()) {
        final DurationMeasure delay = handler.getDelay();
        executor.execute(getFlushCommand(forceClose), delay.value, delay.unit);

      } else if (executor.isExecutionThread()) {
        handler.flushOutput(forceClose);

      } else {
        executor.execute(getFlushCommand(forceClose));
      }
    }
  }

  /**
   * Class handling a consumer bound to the channel.
   */
  private class ConsumerHandler implements BindingHandler<OUT> {

    private final ChannelConsumer<? super OUT> mConsumer;

    private final DurationMeasure mDelay;

    private final boolean mIsDelayed;

    private boolean mIsForced;

    private boolean mIsRunning;

    private Object[] mOutputs = new Object[8];

    /**
     * Constructor.
     *
     * @param consumer the consumer instance.
     */
    private ConsumerHandler(@NotNull final ChannelConsumer<? super OUT> consumer) {
      final DurationMeasure delay = (mDelay = mResultDelay.get());
      mIsDelayed = !delay.isZero();
      mConsumer = consumer;
    }

    @SuppressWarnings("unchecked")
    public void flushOutput(final boolean forceClose) {
      int n;
      Object[] outputs = mOutputs;
      final boolean isFinal;
      synchronized (mMutex) {
        if (mIsRunning) {
          if (forceClose || mState.isReadyToComplete()) {
            mIsForced = true;
          }

          return;
        }

        mIsRunning = true;
        isFinal = forceClose || mState.isReadyToComplete();
        n = transferTo(outputs);
      }

      final Logger logger = mLogger;
      final ChannelConsumer<? super OUT> consumer = mConsumer;
      RoutineException abortException = null;
      try {
        while (n != 0) {
          final int length = (n < 0) ? outputs.length : n;
          for (int i = 0; i < length; ++i) {
            final Object output = outputs[i];
            if (output instanceof RoutineExceptionWrapper) {
              try {
                logger.dbg("aborting consumer (%s): %s", consumer, output);
                consumer.onError(((RoutineExceptionWrapper) output).raise());

              } catch (final RoutineException e) {
                InterruptedInvocationException.throwIfInterrupt(e);
                logger.wrn(e, "ignoring consumer exception (%s)", consumer);

              } catch (final Throwable t) {
                InterruptedInvocationException.throwIfInterrupt(t);
                logger.err(t, "ignoring consumer exception (%s)", consumer);
              }

              break;

            } else {
              logger.dbg("channel consumer (%s): %s", consumer, output);
              consumer.onOutput((OUT) output);
            }
          }

          if (n < -outputs.length) {
            outputs = new Object[outputs.length << 1];
          }

          n = getOutputs(outputs);
        }

        if (isFinal || isForced()) {
          closeConsumer(getState(), consumer);
        }

      } catch (final Throwable t) {
        synchronized (mMutex) {
          logger.wrn(t, "consumer exception (%s)", consumer);
          mIsRunning = false;
          mOutputQueue.clear();
          abortException = mState.abortConsumer(t);
        }

        InterruptedInvocationException.throwIfInterrupt(t);

      } finally {
        Arrays.fill(outputs, null);
      }

      if (abortException != null) {
        mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
      }
    }

    private int getOutputs(@NotNull final Object[] dst) {
      synchronized (mMutex) {
        return transferTo(dst);
      }
    }

    private OutputChannelState getState() {
      synchronized (mMutex) {
        return mState;
      }
    }

    private boolean isForced() {
      synchronized (mMutex) {
        return mIsForced;
      }
    }

    private int transferTo(@NotNull final Object[] dst) {
      final int result = mOutputQueue.transferTo(dst, 0);
      mOutputCount = 0;
      if (result == 0) {
        mIsRunning = false;
      }

      mMutex.notifyAll();
      return result;
    }

    @Nullable
    public ChannelConsumer<? super OUT> getConsumer() {
      return mConsumer;
    }

    @NotNull
    public DurationMeasure getDelay() {
      return mDelay;
    }

    public boolean isBound() {
      return true;
    }

    public boolean isDelayed() {
      return mIsDelayed;
    }
  }

  /**
   * Default implementation of an channel consumer pushing the data into the output queue.
   */
  private class DefaultChannelConsumer implements ChannelConsumer<OUT> {

    private final long mDelay;

    private final TimeUnit mDelayUnit;

    private final OrderType mOrderType;

    private final NestedQueue<Object> mQueue;

    /**
     * Constructor.
     */
    private DefaultChannelConsumer() {
      final DurationMeasure delay = mResultDelay.get();
      mDelay = delay.value;
      mDelayUnit = delay.unit;
      final OrderType order = (mOrderType = mResultOrder.get());
      mQueue = (order == OrderType.SORTED) ? mOutputQueue.addNested() : mOutputQueue;
    }

    public void onComplete() {
      final long delay = mDelay;
      final TimeUnit timeUnit = mDelayUnit;
      final boolean needsFlush;
      final BindingHandler<OUT> handler;
      synchronized (mMutex) {
        needsFlush = mState.onConsumerComplete(mQueue, delay, timeUnit);
        handler = mBindingHandler;
      }

      if (delay == 0) {
        if (needsFlush) {
          mFlusher.run(handler, false);
        }

      } else {
        mExecutor.execute(new DelayedConsumerCompleteCommand(mQueue), delay, timeUnit);
      }
    }

    public void onError(@NotNull final RoutineException error) {
      final long delay = mDelay;
      final TimeUnit timeUnit = mDelayUnit;
      final boolean needsAbort;
      synchronized (mMutex) {
        needsAbort = mState.onConsumerError(mQueue, error, delay, timeUnit);
      }

      if (needsAbort) {
        if (delay == 0) {
          mHandler.onAbort(error, 0, TimeUnit.MILLISECONDS);

        } else {
          mExecutor.execute(new DelayedConsumerErrorCommand(mQueue, error), delay, timeUnit);
        }
      }
    }

    public void onOutput(final OUT output) {
      final long delay = mDelay;
      final TimeUnit timeUnit = mDelayUnit;
      final Runnable command;
      final BindingHandler<OUT> handler;
      synchronized (mMutex) {
        command = mState.onConsumerOutput(mQueue, output, mOrderType, delay, timeUnit);
        handler = mBindingHandler;
      }

      if (command != null) {
        mExecutor.execute(command, delay, timeUnit);

      } else {
        mFlusher.run(handler, false);
      }

      synchronized (mMutex) {
        if (!mHasOutputs.isTrue()) {
          waitOutputs();
        }
      }
    }
  }

  /**
   * Default implementation of a channel iterator.
   */
  private class DefaultIterator implements Iterator<OUT> {

    private final TimeoutActionType mAction;

    private final Throwable mException;

    private final long mTimeout;

    private final TimeUnit mTimeoutUnit;

    /**
     * Constructor.
     *
     * @param timeout   the output timeout.
     * @param timeUnit  the output timeout unit.
     * @param action    the timeout action.
     * @param exception the timeout exception.
     */
    private DefaultIterator(final long timeout, @NotNull final TimeUnit timeUnit,
        @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {
      mTimeout = timeout;
      mTimeoutUnit = timeUnit;
      mAction = action;
      mException = exception;
    }

    public boolean hasNext() {
      return isNextAvailable(mTimeout, mTimeoutUnit, mAction, mException);
    }

    public OUT next() {
      return readNext(mTimeout, mTimeoutUnit, mAction, mException);
    }

    public void remove() {
      ConstantConditions.unsupported();
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
      final RoutineException abortException;
      synchronized (mMutex) {
        abortException = mState.delayedAbortInvocation(mAbortException);
      }

      if (abortException != null) {
        mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
      }
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
      mLogger.dbg("closing result channel after delay");
      final boolean needsFlush;
      final BindingHandler<OUT> handler;
      synchronized (mMutex) {
        needsFlush = mState.delayedCloseResultChannel();
        handler = mBindingHandler;
      }

      if (needsFlush) {
        handler.flushOutput(false);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed consumer completion.
   */
  private class DelayedConsumerCompleteCommand implements Runnable {

    private final NestedQueue<Object> mQueue;

    /**
     * Constructor.
     *
     * @param queue the output queue.
     */
    private DelayedConsumerCompleteCommand(@NotNull final NestedQueue<Object> queue) {
      mQueue = queue;
    }

    public void run() {
      final boolean needsFlush;
      final BindingHandler<OUT> handler;
      synchronized (mMutex) {
        needsFlush = mState.delayedConsumerComplete(mQueue);
        handler = mBindingHandler;
      }

      if (needsFlush) {
        handler.flushOutput(false);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed consumer error.
   */
  private class DelayedConsumerErrorCommand implements Runnable {

    private final RoutineException mAbortException;

    private final NestedQueue<Object> mQueue;

    /**
     * Constructor.
     *
     * @param queue the output queue.
     * @param error the abortion error.
     */
    private DelayedConsumerErrorCommand(@NotNull final NestedQueue<Object> queue,
        @NotNull final RoutineException error) {
      mQueue = queue;
      mAbortException = error;
    }

    public void run() {
      final RoutineException abortException = mAbortException;
      final boolean needsAbort;
      synchronized (mMutex) {
        needsAbort = mState.delayedConsumerError(mQueue, abortException);
      }

      if (needsAbort) {
        mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed output of a list of data.
   */
  private class DelayedListOutputCommand implements Runnable {

    private final ArrayList<OUT> mOutputs;

    private final NestedQueue<Object> mQueue;

    /**
     * Constructor.
     *
     * @param queue   the output queue.
     * @param outputs the list of output data.
     */
    private DelayedListOutputCommand(@NotNull final NestedQueue<Object> queue,
        @NotNull final ArrayList<OUT> outputs) {
      mOutputs = outputs;
      mQueue = queue;
    }

    public void run() {
      final boolean needsFlush;
      final BindingHandler<OUT> handler;
      synchronized (mMutex) {
        needsFlush = mState.delayedOutputs(mQueue, mOutputs);
        handler = mBindingHandler;
      }

      if (needsFlush) {
        handler.flushOutput(false);
      }
    }
  }

  /**
   * Implementation of a runnable handling a delayed output.
   */
  private class DelayedOutputCommand implements Runnable {

    private final OUT mOutput;

    private final NestedQueue<Object> mQueue;

    /**
     * Constructor.
     *
     * @param queue  the output queue.
     * @param output the output.
     */
    private DelayedOutputCommand(@NotNull final NestedQueue<Object> queue,
        @Nullable final OUT output) {
      mQueue = queue;
      mOutput = output;
    }

    public void run() {
      final boolean needsFlush;
      final BindingHandler<OUT> handler;
      synchronized (mMutex) {
        needsFlush = mState.delayedOutput(mQueue, mOutput);
        handler = mBindingHandler;
      }

      if (needsFlush) {
        handler.flushOutput(false);
      }
    }
  }

  /**
   * The invocation has successfully completed.
   */
  private class DoneChannelState extends FlushChannelState {

    @Nullable
    @Override
    RoutineException delayedAbortInvocation(@NotNull final RoutineException reason) {
      if (mOutputQueue.isEmpty()) {
        mLogger.dbg(reason, "avoiding aborting after delay since invocation has completed");
        return null;
      }

      return super.delayedAbortInvocation(reason);
    }

    @Override
    boolean isDone() {
      return true;
    }

    @Override
    boolean isReadyToComplete() {
      return false;
    }

    @NotNull
    @Override
    OutputChannelState toDoneState() {
      return this;
    }
  }

  /**
   * The invocation has been aborted with an exception.
   */
  private class ExceptionChannelState extends FlushChannelState {

    @Nullable
    @Override
    RoutineException abortConsumer(@NotNull final Throwable reason) {
      return null;
    }

    @Override
    boolean delayedConsumerComplete(@NotNull final NestedQueue<Object> queue) {
      mLogger.dbg("avoiding aborting after delay since channel is aborted");
      return false;
    }

    @Nullable
    @Override
    Runnable onConsumerOutput(@NotNull final NestedQueue<Object> queue, @Nullable final OUT output,
        @NotNull final OrderType orderType, final long delay, @NotNull final TimeUnit timeUnit) {
      throw abortException();
    }

    @Override
    boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue, final long delay,
        @NotNull final TimeUnit timeUnit) {
      throw abortException();
    }

    @NotNull
    private RoutineException abortException() {
      final RoutineException abortException = mAbortException;
      mLogger.dbg(abortException, "abort exception");
      return mAbortException;
    }

    @Nullable
    @Override
    RoutineException delayedAbortInvocation(@NotNull final RoutineException reason) {
      mLogger.dbg(reason, "avoiding aborting after delay since channel is aborted");
      return null;
    }

    @Override
    void orderBy(@NotNull final OrderType orderType) {
      throw abortException();
    }

    @Override
    boolean isReadyToComplete() {
      return false;
    }

    @Nullable
    @Override
    ChannelConsumer<OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
      throw abortException();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final Iterable<? extends OUT> outputs,
        @NotNull final DurationMeasure delay) {
      throw abortException();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final OUT output, @NotNull final DurationMeasure delay) {
      throw abortException();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final OUT[] outputs, @NotNull final DurationMeasure delay) {
      throw abortException();
    }

    @NotNull
    @Override
    OutputChannelState toDoneState() {
      return new AbortedChannelState();
    }
  }

  /**
   * Default implementation of a channel expiring iterator.
   */
  private class ExpiringIterator implements Iterator<OUT> {

    private final TimeoutActionType mAction;

    private final Throwable mException;

    private final Object mMutex = new Object();

    private final long mTimeoutMillis;

    private long mEndTime = Long.MIN_VALUE;

    /**
     * Constructor.
     *
     * @param timeout   the output timeout.
     * @param timeUnit  the output timeout unit.
     * @param action    the timeout action.
     * @param exception the timeout exception.
     */
    private ExpiringIterator(final long timeout, @NotNull final TimeUnit timeUnit,
        @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {
      mTimeoutMillis = timeUnit.toMillis(timeout);
      mAction = action;
      mException = exception;
    }

    private long getTimeoutMillis() {
      synchronized (mMutex) {
        if (mEndTime == Long.MIN_VALUE) {
          mEndTime = System.currentTimeMillis() + mTimeoutMillis;
        }

        return Math.max(0, mEndTime - System.currentTimeMillis());
      }
    }

    public boolean hasNext() {
      return isNextAvailable(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
    }

    public OUT next() {
      return readNext(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
    }

    public void remove() {
      ConstantConditions.unsupported();
    }
  }

  /**
   * Invocation has completed and no output is pending.
   */
  private class FlushChannelState extends ResultChannelState {

    @Override
    boolean delayedConsumerComplete(@NotNull final NestedQueue<Object> queue) {
      mLogger.dbg("avoiding aborting after delay since channel is closed");
      return false;
    }

    @Override
    boolean delayedOutput(@NotNull final NestedQueue<Object> queue, @Nullable final OUT output) {
      mLogger.wrn("avoiding delayed output command since channel is closed: %s", output);
      return false;
    }

    @Override
    boolean delayedOutputs(@NotNull final NestedQueue<Object> queue,
        @NotNull final List<OUT> outputs) {
      mLogger.wrn("avoiding delayed output command since channel is closed: %s", outputs);
      return false;
    }

    @NotNull
    private IllegalStateException exception() {
      mLogger.err("consumer invalid call on closed channel");
      return new IllegalStateException("the channel is closed");
    }

    @Nullable
    @Override
    RoutineException abortInvocation(@Nullable final Throwable reason,
        @NotNull final DurationMeasure delay) {
      mLogger.dbg(reason, "avoiding aborting since channel is closed");
      return null;
    }

    @Override
    boolean delayedConsumerError(@NotNull final NestedQueue<Object> queue,
        @NotNull final RoutineException error) {
      mLogger.dbg(error,
          "avoiding aborting on consumer exception after delay since result channel is "
              + "closed");
      return false;
    }

    @Nullable
    @Override
    Runnable onConsumerOutput(@NotNull final NestedQueue<Object> queue, @Nullable final OUT output,
        @NotNull final OrderType orderType, final long delay, @NotNull final TimeUnit timeUnit) {
      throw exception();
    }

    @Override
    boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue, final long delay,
        @NotNull final TimeUnit timeUnit) {
      throw exception();
    }

    @Override
    boolean onConsumerError(@NotNull final NestedQueue<Object> queue,
        @NotNull final RoutineException error, final long delay, @NotNull final TimeUnit timeUnit) {
      mLogger.dbg(error, "avoiding aborting on consumer exception since result channel is closed");
      return false;
    }

    @Override
    boolean isReadyToComplete() {
      return true;
    }
  }

  /**
   * Runnable flushing the output to the bound consumer.
   */
  private class FlushCommand implements Runnable {

    private final boolean mForceClose;

    /**
     * Constructor.
     *
     * @param forceClose whether to forcedly close the consumer.
     */
    private FlushCommand(final boolean forceClose) {
      mForceClose = forceClose;
    }

    public void run() {
      getBindingHandler().flushOutput(mForceClose);
    }
  }

  /**
   * Result channel internal state (using "state" design pattern).
   */
  private class OutputChannelState {

    /**
     * Called when a consumer cause the invocation to abort by throwing an exception.
     *
     * @param reason the reason of the abortion.
     * @return the abort exception or null.
     */
    @Nullable
    RoutineException abortConsumer(@NotNull final Throwable reason) {
      final RoutineException abortException = InvocationException.wrapIfNeeded(reason);
      mLogger.wrn(reason, "aborting on consumer exception (%s)", mBindingHandler.getConsumer());
      internalAbort(abortException);
      return abortException;
    }

    /**
     * Called when the invocation is aborted.
     *
     * @param reason the reason of the abortion.
     * @param delay  the abortion delay.
     * @return the abort exception or null.
     */
    @Nullable
    RoutineException abortInvocation(@Nullable final Throwable reason,
        @NotNull final DurationMeasure delay) {
      final RoutineException abortException = AbortException.wrapIfNeeded(reason);
      if (delay.isZero()) {
        mLogger.dbg(reason, "aborting channel");
        internalAbort(abortException);
      }

      return abortException;
    }

    /**
     * Called when the specified consumer is closed.
     *
     * @param consumer the consumer instance.
     */
    void closeConsumer(@NotNull final ChannelConsumer<? super OUT> consumer) {
      final Logger logger = mLogger;
      try {
        logger.dbg("closing consumer (%s)", consumer);
        consumer.onComplete();

      } catch (final RoutineException e) {
        InterruptedInvocationException.throwIfInterrupt(e);
        logger.wrn(e, "ignoring consumer exception (%s)", consumer);

      } catch (final Throwable t) {
        InterruptedInvocationException.throwIfInterrupt(t);
        logger.err(t, "ignoring consumer exception (%s)", consumer);
      }
    }

    /**
     * Called after invocation has been aborted.
     *
     * @param throwable the abortion error.
     * @param channels  the channels to close.
     * @return the abortion reason.
     */
    @Nullable
    RoutineException closeInvocation(@Nullable final Throwable throwable,
        @NotNull final ArrayList<Channel<?, ? extends OUT>> channels) {
      mLogger.dbg(throwable, "aborting result channel");
      final ArrayList<Channel<?, ? extends OUT>> boundChannels = mBoundChannels;
      channels.addAll(boundChannels);
      boundChannels.clear();
      final RoutineExceptionWrapper wrapper = RoutineExceptionWrapper.wrap(throwable);
      final NestedQueue<Object> outputQueue = mOutputQueue;
      if (mPendingOutputCount > 0) {
        final ArrayList<Object> outputs = new ArrayList<Object>();
        outputQueue.transferTo(outputs);
        outputQueue.clear();
        outputQueue.addAll(outputs);
      }

      outputQueue.add(wrapper);
      mPendingOutputCount = 0;
      final RoutineException abortException = InvocationException.wrapIfNeeded(throwable);
      if (mAbortException == null) {
        mAbortException = abortException;
      }

      mState = new AbortChannelState();
      mMutex.notifyAll();
      return abortException;
    }

    /**
     * Called when the result channel is closed.
     *
     * @param delay the result delay.
     * @return the command to run or null.
     */
    @Nullable
    Runnable closeResultChannel(@NotNull final DurationMeasure delay) {
      if (delay.isZero()) {
        mLogger.dbg("closing result channel [#%d]", mPendingOutputCount);
        if (mPendingOutputCount > 0) {
          mState = new ResultChannelState();

        } else {
          mState = new FlushChannelState();
        }

        return null;
      }

      return new DelayedCloseCommand();
    }

    /**
     * Called when the invocation is aborted after a delay.
     *
     * @param reason the reason of the abortion.
     * @return the abort exception or null.
     */
    @Nullable
    RoutineException delayedAbortInvocation(@NotNull final RoutineException reason) {
      mLogger.dbg(reason, "aborting channel after delay");
      internalAbort(reason);
      return reason;
    }

    /**
     * Called when the result channel is closed after a delay.
     *
     * @return whether the internal state has changed.
     */
    boolean delayedCloseResultChannel() {
      mLogger.dbg("closing result channel after delay [#%d]", mPendingOutputCount);
      if (mPendingOutputCount > 0) {
        mState = new ResultChannelState();

      } else {
        mState = new FlushChannelState();
      }

      return true;
    }

    /**
     * Called when the feeding consumer completes after a delay.
     *
     * @param queue the output queue.
     * @return whether the queue content has changed.
     */
    boolean delayedConsumerComplete(@NotNull final NestedQueue<Object> queue) {
      mLogger.dbg("on consumer complete after delay");
      queue.close();
      --mPendingOutputCount;
      mMutex.notifyAll();
      return false;
    }

    /**
     * Called when the feeding consumer receives an error after a delay.
     *
     * @param queue the output queue.
     * @param error the error.
     * @return whether the queue content has changed.
     */
    boolean delayedConsumerError(@NotNull final NestedQueue<Object> queue,
        @NotNull final RoutineException error) {
      mLogger.dbg(error, "aborting output on consumer exception after delay");
      queue.close();
      internalAbort(error);
      return true;
    }

    /**
     * Called when an output is passed to the invocation after a delay.
     *
     * @param queue  the output queue.
     * @param output the output.
     * @return whether the queue content has changed.
     */
    boolean delayedOutput(@NotNull final NestedQueue<Object> queue, @Nullable final OUT output) {
      mLogger.dbg("delayed output command: %s", output);
      --mPendingOutputCount;
      queue.add(output);
      queue.close();
      return true;
    }

    /**
     * Called when some outputs are passed to the invocation after a delay.
     *
     * @param queue   the output queue.
     * @param outputs the outputs.
     * @return whether the queue content has changed.
     */
    boolean delayedOutputs(@NotNull final NestedQueue<Object> queue,
        @NotNull final List<OUT> outputs) {
      mLogger.dbg("delayed output command: %s", outputs);
      --mPendingOutputCount;
      queue.addAll(outputs);
      queue.close();
      return true;
    }

    /**
     * Called to know if the invocation has completed.
     *
     * @return whether the invocation is complete.
     */
    boolean isDone() {
      return false;
    }

    /**
     * Called to know if the channel is open.
     *
     * @return whether the channel is open.
     */
    boolean isOpen() {
      return true;
    }

    /**
     * Called to know if this state is ready to complete.
     *
     * @return whether the state is ready to complete.
     */
    boolean isReadyToComplete() {
      return false;
    }

    /**
     * Called when the feeding consumer completes.
     *
     * @param queue    the output queue.
     * @param delay    the output delay.
     * @param timeUnit the output delay unit.
     * @return whether the queue content has changed.
     */
    boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue, final long delay,
        @NotNull final TimeUnit timeUnit) {
      if (delay == 0) {
        mLogger.dbg("on consumer complete");
        queue.close();
        --mPendingOutputCount;
        mMutex.notifyAll();
      }

      return false;
    }

    /**
     * Called when the feeding consumer receives an error.
     *
     * @param queue    the output queue.
     * @param error    the error.
     * @param delay    the output delay.
     * @param timeUnit the output delay unit.
     * @return whether the queue content has changed.
     */
    boolean onConsumerError(@NotNull final NestedQueue<Object> queue,
        @NotNull final RoutineException error, final long delay, @NotNull final TimeUnit timeUnit) {
      if (delay == 0) {
        mLogger.dbg(error, "aborting output on consumer exception");
        queue.close();
        internalAbort(error);
      }

      return true;
    }

    /**
     * Called when the feeding consumer receives an output.
     *
     * @param queue     the output queue.
     * @param output    the output.
     * @param orderType the output order type.
     * @param delay     the output delay.
     * @param timeUnit  the output delay unit.
     * @return the command to run or null.
     */
    @Nullable
    Runnable onConsumerOutput(@NotNull final NestedQueue<Object> queue, @Nullable final OUT output,
        @NotNull final OrderType orderType, final long delay, @NotNull final TimeUnit timeUnit) {
      mLogger.dbg("consumer output [#%d+1]: %s [%d %s]", mOutputCount, output, delay, timeUnit);
      ++mOutputCount;
      if (delay == 0) {
        queue.add(output);
        return null;
      }

      ++mPendingOutputCount;
      return new DelayedOutputCommand((orderType != OrderType.UNSORTED) ? queue.addNested() : queue,
          output);
    }

    /**
     * Called to set the result delivery order.
     *
     * @param orderType the result order type.
     */
    void orderBy(@NotNull final OrderType orderType) {
      mResultOrder.set(ConstantConditions.notNull("order type", orderType));
    }

    /**
     * Called when a channel is passed to the result channel.
     *
     * @param channel the channel instance.
     * @return the channel consumer to bind or null.
     */
    @Nullable
    ChannelConsumer<OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
      if (channel == null) {
        mLogger.wrn("passing null channel");
        return null;
      }

      mBoundChannels.add(channel);
      ++mPendingOutputCount;
      mLogger.dbg("passing channel: %s", channel);
      return new DefaultChannelConsumer();
    }

    /**
     * Called when some outputs are passed to the result channel.
     *
     * @param outputs the outputs.
     * @param delay   the result delay.
     * @return the command to run or null.
     */
    @Nullable
    Runnable pass(@Nullable final Iterable<? extends OUT> outputs,
        @NotNull final DurationMeasure delay) {
      if (outputs == null) {
        mLogger.wrn("passing null iterable");
        return null;
      }

      final ArrayList<OUT> list = new ArrayList<OUT>();
      for (final OUT output : outputs) {
        list.add(output);
      }

      final int size = list.size();
      mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mOutputCount, size, outputs, delay);
      mOutputCount += size;
      checkMaxSize();
      if (delay.isZero()) {
        mOutputQueue.addAll(list);
        return null;
      }

      ++mPendingOutputCount;
      return new DelayedListOutputCommand(
          (mResultOrder.get() != OrderType.UNSORTED) ? mOutputQueue.addNested() : mOutputQueue,
          list);
    }

    /**
     * Called when an output is passed to the result channel.
     *
     * @param output the output.
     * @param delay  the result delay;
     * @return the command to run or null.
     */
    @Nullable
    Runnable pass(@Nullable final OUT output, @NotNull final DurationMeasure delay) {
      mLogger.dbg("passing output [#%d+1]: %s [%s]", mOutputCount, output, delay);
      ++mOutputCount;
      checkMaxSize();
      if (delay.isZero()) {
        mOutputQueue.add(output);
        return null;
      }

      ++mPendingOutputCount;
      return new DelayedOutputCommand(
          (mResultOrder.get() != OrderType.UNSORTED) ? mOutputQueue.addNested() : mOutputQueue,
          output);
    }

    /**
     * Called when some outputs are passed to the result channel.
     *
     * @param outputs the outputs.
     * @param delay   the result delay;
     * @return the command to run or null.
     */
    @Nullable
    Runnable pass(@Nullable final OUT[] outputs, @NotNull final DurationMeasure delay) {
      if (outputs == null) {
        mLogger.wrn("passing null output array");
        return null;
      }

      final int size = outputs.length;
      mLogger.dbg("passing array [#%d+%d]: %s [%s]", mOutputCount, size, Arrays.toString(outputs),
          delay);
      mOutputCount += size;
      checkMaxSize();
      final ArrayList<OUT> list = new ArrayList<OUT>(size);
      Collections.addAll(list, outputs);
      if (delay.isZero()) {
        mOutputQueue.addAll(list);
        return null;
      }

      ++mPendingOutputCount;
      return new DelayedListOutputCommand(
          (mResultOrder.get() != OrderType.UNSORTED) ? mOutputQueue.addNested() : mOutputQueue,
          list);
    }

    /**
     * Converts this state to done.
     *
     * @return the done state.
     */
    @NotNull
    OutputChannelState toDoneState() {
      return new DoneChannelState();
    }
  }

  /**
   * Class handling the outputs in the queue.
   */
  private class OutputHandler implements BindingHandler<OUT> {

    public void flushOutput(final boolean forceClose) {
      synchronized (mMutex) {
        final OutputChannelState state = mState;
        mLogger.dbg("avoiding flushing output since channel is not bound");
        if (state.isReadyToComplete() && (mBindingHandler == this)) {
          mState = state.toDoneState();
        }

        mMutex.notifyAll();
      }
    }

    @Nullable
    public ChannelConsumer<? super OUT> getConsumer() {
      return null;
    }

    @NotNull
    public DurationMeasure getDelay() {
      return DurationMeasure.zero(TimeUnit.MILLISECONDS);
    }

    public boolean isBound() {
      return false;
    }

    public boolean isDelayed() {
      return false;
    }
  }

  /**
   * Invocation has completed but some outputs are still pending.
   */
  private class ResultChannelState extends OutputChannelState {

    @NotNull
    private IllegalStateException exception() {
      mLogger.err("invalid call on closed channel");
      return new IllegalStateException("the channel is closed");
    }

    @Nullable
    @Override
    Runnable closeResultChannel(@NotNull final DurationMeasure delay) {
      mLogger.dbg("avoiding closing result channel since already closed");
      return null;
    }

    @Override
    boolean delayedCloseResultChannel() {
      mLogger.dbg("avoiding closing result channel after delay since already closed");
      return false;
    }

    @Override
    boolean delayedConsumerComplete(@NotNull final NestedQueue<Object> queue) {
      queue.close();
      if (--mPendingOutputCount == 0) {
        mState = new FlushChannelState();
        return true;
      }

      mMutex.notifyAll();
      return false;
    }

    @Override
    boolean delayedOutput(@NotNull final NestedQueue<Object> queue, @Nullable final OUT output) {
      mLogger.dbg("delayed output command: %s", output);
      if (--mPendingOutputCount == 0) {
        mState = new FlushChannelState();
      }

      queue.add(output);
      queue.close();
      return true;
    }

    @Override
    boolean delayedOutputs(@NotNull final NestedQueue<Object> queue,
        @NotNull final List<OUT> outputs) {
      mLogger.dbg("delayed output command: %s", outputs);
      if (--mPendingOutputCount == 0) {
        mState = new FlushChannelState();
      }

      queue.addAll(outputs);
      queue.close();
      return true;
    }

    @Override
    void orderBy(@NotNull final OrderType orderType) {
      throw exception();
    }

    @Override
    boolean isOpen() {
      return false;
    }

    @Override
    boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue, final long delay,
        @NotNull final TimeUnit timeUnit) {
      if (delay == 0) {
        queue.close();
        if (--mPendingOutputCount == 0) {
          mState = new FlushChannelState();
          return true;
        }

        mMutex.notifyAll();
      }

      return false;
    }

    @Nullable
    @Override
    ChannelConsumer<OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final Iterable<? extends OUT> outputs,
        @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final OUT output, @NotNull final DurationMeasure delay) {
      throw exception();
    }

    @Nullable
    @Override
    Runnable pass(@Nullable final OUT[] outputs, @NotNull final DurationMeasure delay) {
      throw exception();
    }
  }

  /**
   * Flusher implementation handling a synchronous executor.
   */
  private class SyncFlusher implements Flusher<OUT> {

    public void run(@NotNull final BindingHandler<OUT> handler, final boolean forceClose) {
      if (handler.isDelayed()) {
        final DurationMeasure delay = handler.getDelay();
        mExecutor.execute(getFlushCommand(forceClose), delay.value, delay.unit);

      } else {
        handler.flushOutput(forceClose);
      }
    }
  }

  public void remove() {
    ConstantConditions.unsupported();
  }
}
