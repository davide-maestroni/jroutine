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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.LocalField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.DurationMeasure.fromUnit;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Implementation of a channel backed by a Future instance.
 * <p>
 * Created by davide-maestroni on 08/30/2016.
 *
 * @param <OUT> the output data type.
 */
class FutureChannel<OUT> implements Channel<OUT, OUT> {

  private final ScheduledExecutor mExecutor;

  private final Future<OUT> mFuture;

  private final boolean mInterruptIfRunning;

  private final AtomicBoolean mIsBound = new AtomicBoolean();

  private final AtomicBoolean mIsRead = new AtomicBoolean();

  private final Logger mLogger;

  private final LocalField<DurationMeasure> mOutputTimeout;

  private final LocalField<DurationMeasure> mResultDelay;

  private final LocalField<TimeoutActionType> mTimeoutActionType;

  private final ThreadLocal<Throwable> mTimeoutException = new ThreadLocal<Throwable>();

  private Throwable mAbortException;

  /**
   * Constructor.
   *
   * @param executor              the executor instance.
   * @param configuration         the channel configuration.
   * @param future                the Future instance.
   * @param mayInterruptIfRunning if the thread executing the task should be interrupted.
   */
  FutureChannel(@NotNull final ScheduledExecutor executor,
      @NotNull final ChannelConfiguration configuration, @NotNull final Future<OUT> future,
      final boolean mayInterruptIfRunning) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mFuture = ConstantConditions.notNull("future instance", future);
    mOutputTimeout =
        new LocalField<DurationMeasure>(configuration.getOutputTimeoutOrElse(noTime()));
    mTimeoutActionType = new LocalField<TimeoutActionType>(
        configuration.getOutputTimeoutActionOrElse(TimeoutActionType.FAIL));
    mInterruptIfRunning = mayInterruptIfRunning;
    mResultDelay = new LocalField<DurationMeasure>(noTime());
    mLogger = configuration.newLogger(this);
  }

  public boolean abort() {
    return abort(null);
  }

  public boolean abort(@Nullable final Throwable reason) {
    final Future<OUT> future = mFuture;
    final DurationMeasure delay = mResultDelay.get();
    if (delay.isZero()) {
      final boolean isCancelled = future.cancel(mInterruptIfRunning);
      if (isCancelled) {
        mAbortException = reason;
      }

      return isCancelled;
    }

    if (!future.isCancelled()) {
      mExecutor.execute(new Runnable() {

        public void run() {
          future.cancel(mInterruptIfRunning);
        }
      }, delay.value, delay.unit);
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
    mResultDelay.set(ConstantConditions.notNull("delay", delay));
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> afterNoDelay() {
    return after(noTime());
  }

  @NotNull
  public List<OUT> all() {
    final ArrayList<OUT> outputs = new ArrayList<OUT>();
    allInto(outputs);
    return outputs;
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
    return this;
  }

  @NotNull
  public Channel<OUT, OUT> consume(@NotNull final ChannelConsumer<? super OUT> consumer) {
    if (mIsBound.getAndSet(true)) {
      mLogger.err("invalid call on bound channel");
      throw new IllegalStateException("the channel is already bound");
    }

    if (mIsRead.get()) {

      try {
        if (mFuture.isCancelled()) {
          consumer.onError(AbortException.wrapIfNeeded(mAbortException));

        } else {
          consumer.onComplete();
        }

      } catch (final Throwable t) {
        InterruptedInvocationException.throwIfInterrupt(t);
        mLogger.wrn(t, "consumer exception (%s)", consumer);
      }

    } else {
      final DurationMeasure delay = mResultDelay.get();
      mExecutor.execute(new Runnable() {

        public void run() {
          try {
            try {
              consumer.onOutput(mFuture.get());

            } catch (final CancellationException e) {
              consumer.onError(AbortException.wrapIfNeeded(mAbortException));
              return;

            } catch (final InterruptedException e) {
              consumer.onError(new InterruptedInvocationException(e));
              return;

            } catch (final Throwable t) {
              consumer.onError(InvocationException.wrapIfNeeded(t));
              return;
            }

            consumer.onComplete();

          } catch (final Throwable t) {
            InterruptedInvocationException.throwIfInterrupt(t);
            mLogger.wrn(t, "consumer exception (%s)", consumer);
          }
        }
      }, delay.value, delay.unit);
    }

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
    return new ExpiringFutureIterator(mOutputTimeout.get(), mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  public boolean getComplete() {
    final DurationMeasure timeout = mOutputTimeout.get();
    try {
      mFuture.get(timeout.value, timeout.unit);
      return true;

    } catch (final Exception ignored) {
    }

    return false;
  }

  @Nullable
  public RoutineException getError() {
    final DurationMeasure timeout = mOutputTimeout.get();
    try {
      mFuture.get(timeout.value, timeout.unit);

    } catch (final TimeoutException ignored) {

    } catch (final CancellationException e) {
      return AbortException.wrapIfNeeded(mAbortException);

    } catch (final InterruptedException e) {
      return new InterruptedInvocationException(e);

    } catch (final Throwable t) {
      return InvocationException.wrapIfNeeded(t);
    }

    return null;
  }

  public boolean hasNext() {
    final DurationMeasure timeout = mOutputTimeout.get();
    return isNextAvailable(timeout.value, timeout.unit, mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  public OUT next() {
    final DurationMeasure timeout = mOutputTimeout.get();
    return readNext(timeout.value, timeout.unit, mTimeoutActionType.get(), mTimeoutException.get());
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

  public int inputSize() {
    return outputSize();
  }

  public boolean isBound() {
    return mIsBound.get();
  }

  public boolean isEmpty() {
    return (outputSize() == 0);
  }

  public boolean isOpen() {
    return false;
  }

  @NotNull
  public List<OUT> next(final int count) {
    if (count <= 0) {
      return new ArrayList<OUT>(0);
    }

    return all();
  }

  public OUT nextOrElse(final OUT output) {
    try {
      return next();

    } catch (final NoSuchElementException ignored) {
    }

    return output;
  }

  public int outputSize() {
    if (mFuture.isDone()) {
      return mIsRead.get() ? 0 : 1;
    }

    return 0;
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
    return failPass();
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final Iterable<? extends OUT> inputs) {
    return failPass();
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final OUT input) {
    return failPass();
  }

  @NotNull
  public Channel<OUT, OUT> pass(@Nullable final OUT... inputs) {
    return failPass();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <AFTER> Channel<OUT, AFTER> pipe(@NotNull final Channel<? super OUT, AFTER> channel) {
    ((Channel<OUT, AFTER>) channel).pass(this);
    return JRoutineCore.flattenChannels(this, channel);
  }

  public int size() {
    return outputSize();
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
        final DurationMeasure timeout = mOutputTimeout.get();
        final TimeoutActionType timeoutAction = mTimeoutActionType.get();
        mLogger.wrn("skipping output timeout: %s => [%s]", timeout, timeoutAction);
        if (timeoutAction == TimeoutActionType.FAIL) {
          throw new OutputTimeoutException(
              "timeout while waiting to know if more outputs are coming " + timeout);

        } else if (timeoutAction == TimeoutActionType.ABORT) {
          final Throwable timeoutException = mTimeoutException.get();
          abort(timeoutException);
          throw AbortException.wrapIfNeeded(timeoutException);
        }
      }
    }

    return this;
  }

  @NotNull
  public Channel<OUT, OUT> sorted() {
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
    return this;
  }

  @NotNull
  public Iterator<OUT> iterator() {
    verifyBound();
    return new FutureIterator(mOutputTimeout.get(), mTimeoutActionType.get(),
        mTimeoutException.get());
  }

  @NotNull
  private Channel<OUT, OUT> failPass() {
    if (mFuture.isCancelled()) {
      throw AbortException.wrapIfNeeded(mAbortException);
    }

    throw new IllegalStateException("the channel is closed");
  }

  private boolean isNextAvailable(final long timeout, @NotNull final TimeUnit timeUnit,
      @NotNull final TimeoutActionType timeoutAction, @Nullable final Throwable timeoutException) {
    verifyBound();
    final Logger logger = mLogger;
    try {
      if (mIsRead.get()) {
        return false;
      }

      mFuture.get(timeout, timeUnit);
      verifyBound();
      logger.dbg("has output: %s [%d %s]", true, timeout, timeUnit);
      return true;

    } catch (final TimeoutException e) {
      logger.wrn("has output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
      if (timeoutAction == TimeoutActionType.FAIL) {
        throw new OutputTimeoutException(
            "timeout while waiting to know if more outputs are coming [" + timeout + " " + timeUnit
                + "]");

      } else if (timeoutAction == TimeoutActionType.ABORT) {
        abort(timeoutException);
        throw AbortException.wrapIfNeeded(timeoutException);
      }

    } catch (final CancellationException e) {
      throw AbortException.wrapIfNeeded(mAbortException);

    } catch (final IllegalStateException e) {
      throw e;

    } catch (final Throwable t) {
      InterruptedInvocationException.throwIfInterrupt(t);
      throw InvocationException.wrapIfNeeded(t);
    }

    return false;
  }

  @Nullable
  private OUT readNext(final long timeout, @NotNull final TimeUnit timeUnit,
      @NotNull final TimeoutActionType timeoutAction, @Nullable final Throwable timeoutException) {
    verifyBound();
    try {
      if (mIsRead.get()) {
        throw new NoSuchElementException();
      }

      final OUT output = mFuture.get(timeout, timeUnit);
      if (mIsRead.getAndSet(true)) {
        throw new NoSuchElementException();
      }

      verifyBound();
      return output;

    } catch (final NoSuchElementException e) {
      throw e;

    } catch (final TimeoutException e) {
      mLogger.wrn("reading output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
      if (timeoutAction == TimeoutActionType.FAIL) {
        throw new OutputTimeoutException(
            "timeout while waiting to know if more outputs are coming [" + timeout + " " + timeUnit
                + "]");

      } else if (timeoutAction == TimeoutActionType.ABORT) {
        abort(timeoutException);
        throw AbortException.wrapIfNeeded(timeoutException);
      }

      throw new NoSuchElementException();

    } catch (final CancellationException e) {
      throw AbortException.wrapIfNeeded(mAbortException);

    } catch (final IllegalStateException e) {
      throw e;

    } catch (final Throwable t) {
      InterruptedInvocationException.throwIfInterrupt(t);
      throw InvocationException.wrapIfNeeded(t);
    }
  }

  private void verifyBound() {
    if (mIsBound.get()) {
      mLogger.err("invalid call on bound channel");
      throw new IllegalStateException("the channel is already bound");
    }
  }

  /**
   * Default implementation of a Future channel expiring iterator.
   */
  private class ExpiringFutureIterator implements Iterator<OUT> {

    private final TimeoutActionType mAction;

    private final Throwable mException;

    private final Object mMutex = new Object();

    private final long mTimeoutMillis;

    private long mEndTime = Long.MIN_VALUE;

    /**
     * Constructor.
     *
     * @param timeout   the output timeout.
     * @param action    the timeout action.
     * @param exception the timeout exception.
     */
    private ExpiringFutureIterator(@NotNull final DurationMeasure timeout,
        @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {
      mTimeoutMillis = timeout.toMillis();
      mAction = action;
      mException = exception;
    }

    public boolean hasNext() {
      return isNextAvailable(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
    }

    private long getTimeoutMillis() {
      synchronized (mMutex) {
        if (mEndTime == Long.MIN_VALUE) {
          mEndTime = System.currentTimeMillis() + mTimeoutMillis;
        }

        return Math.max(0, mEndTime - System.currentTimeMillis());
      }
    }

    public OUT next() {
      return readNext(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
    }

    public void remove() {
      ConstantConditions.unsupported();
    }
  }

  /**
   * Default implementation of a Future channel iterator.
   */
  private class FutureIterator implements Iterator<OUT> {

    private final TimeoutActionType mAction;

    private final Throwable mException;

    private final TimeUnit mTimeUnit;

    private final long mTimeout;

    /**
     * Constructor.
     *
     * @param timeout   the output timeout.
     * @param action    the timeout action.
     * @param exception the timeout exception.
     */
    private FutureIterator(@NotNull final DurationMeasure timeout,
        @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {
      mTimeout = timeout.value;
      mTimeUnit = timeout.unit;
      mAction = action;
      mException = exception;
    }

    public boolean hasNext() {
      return isNextAvailable(mTimeout, mTimeUnit, mAction, mException);
    }

    public OUT next() {
      return readNext(mTimeout, mTimeUnit, mAction, mException);
    }

    public void remove() {
      ConstantConditions.unsupported();
    }
  }

  public void remove() {
    ConstantConditions.unsupported();
  }
}
