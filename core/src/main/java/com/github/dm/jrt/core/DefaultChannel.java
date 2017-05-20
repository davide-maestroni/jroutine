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

import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutorDecorator;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of a channel.
 * <p>
 * Created by davide-maestroni on 10/24/2014.
 *
 * @param <DATA> the data type.
 */
class DefaultChannel<DATA> implements Channel<DATA, DATA> {

  private static final WeakIdentityHashMap<ScheduledExecutor, WeakReference<ChannelExecutor>>
      sExecutors = new WeakIdentityHashMap<ScheduledExecutor, WeakReference<ChannelExecutor>>();

  private static final ScheduledExecutor sSyncExecutor = ScheduledExecutors.syncExecutor();

  private final ResultChannel<DATA> mChannel;

  /**
   * Constructor.
   *
   * @param configuration the channel configuration.
   * @param executor      the executor instance.
   */
  DefaultChannel(@NotNull final ChannelConfiguration configuration,
      @NotNull final ScheduledExecutor executor) {
    final Logger logger = configuration.newLogger(this);
    ChannelExecutor channelExecutor;
    synchronized (sExecutors) {
      final WeakIdentityHashMap<ScheduledExecutor, WeakReference<ChannelExecutor>> executors =
          sExecutors;
      final WeakReference<ChannelExecutor> executorReference = executors.get(executor);
      channelExecutor = (executorReference != null) ? executorReference.get() : null;
      if (channelExecutor == null) {
        channelExecutor = new ChannelExecutor(executor);
        executors.put(executor, new WeakReference<ChannelExecutor>(channelExecutor));
      }
    }

    final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
    final ResultChannel<DATA> channel =
        new ResultChannel<DATA>(configuration, channelExecutor, abortHandler, logger);
    abortHandler.setChannel(channel);
    mChannel = channel;
    logger.dbg("building channel with configuration: %s", configuration);
  }

  public boolean abort() {
    return mChannel.abort();
  }

  public boolean abort(@Nullable final Throwable reason) {
    return mChannel.abort(reason);
  }

  @NotNull
  public Channel<DATA, DATA> after(final long delay, @NotNull final TimeUnit timeUnit) {
    mChannel.after(delay, timeUnit);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> after(@NotNull final DurationMeasure delay) {
    mChannel.after(delay);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> afterNoDelay() {
    mChannel.afterNoDelay();
    return this;
  }

  @NotNull
  public List<DATA> all() {
    return mChannel.all();
  }

  @NotNull
  public Channel<DATA, DATA> allInto(@NotNull final Collection<? super DATA> results) {
    mChannel.allInto(results);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> close() {
    mChannel.close();
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> consume(@NotNull final ChannelConsumer<? super DATA> consumer) {
    mChannel.consume(consumer);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> eventuallyAbort() {
    mChannel.eventuallyAbort();
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> eventuallyAbort(@Nullable final Throwable reason) {
    mChannel.eventuallyAbort(reason);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> eventuallyContinue() {
    mChannel.eventuallyContinue();
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> eventuallyFail() {
    mChannel.eventuallyFail();
    return this;
  }

  @NotNull
  public Iterator<DATA> expiringIterator() {
    return mChannel.expiringIterator();
  }

  public DATA get() {
    return mChannel.get();
  }

  public boolean getComplete() {
    return mChannel.getComplete();
  }

  @Nullable
  public RoutineException getError() {
    return mChannel.getError();
  }

  public boolean hasNext() {
    return mChannel.hasNext();
  }

  public DATA next() {
    return mChannel.next();
  }

  @NotNull
  public Channel<DATA, DATA> in(final long timeout, @NotNull final TimeUnit timeUnit) {
    mChannel.in(timeout, timeUnit);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> in(@NotNull final DurationMeasure timeout) {
    mChannel.in(timeout);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> inNoTime() {
    mChannel.inNoTime();
    return this;
  }

  public int inputSize() {
    return mChannel.inputSize();
  }

  public boolean isBound() {
    return mChannel.isBound();
  }

  public boolean isEmpty() {
    return mChannel.isEmpty();
  }

  public boolean isOpen() {
    return mChannel.isOpen();
  }

  @NotNull
  public List<DATA> next(final int count) {
    return mChannel.next(count);
  }

  public DATA nextOrElse(final DATA output) {
    return mChannel.nextOrElse(output);
  }

  public int outputSize() {
    return mChannel.outputSize();
  }

  @NotNull
  public Channel<DATA, DATA> pass(@Nullable final Channel<?, ? extends DATA> channel) {
    mChannel.pass(channel);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> pass(@Nullable final Iterable<? extends DATA> inputs) {
    mChannel.pass(inputs);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> pass(@Nullable final DATA input) {
    mChannel.pass(input);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> pass(@Nullable final DATA... inputs) {
    mChannel.pass(inputs);
    return this;
  }

  @NotNull
  public <AFTER> Channel<DATA, AFTER> pipe(@NotNull final Channel<? super DATA, AFTER> channel) {
    return mChannel.pipe(channel);
  }

  public int size() {
    return mChannel.size();
  }

  @NotNull
  public Channel<DATA, DATA> skipNext(final int count) {
    mChannel.skipNext(count);
    return this;
  }

  @NotNull
  public Channel<DATA, DATA> sorted() {
    mChannel.sorted();
    return this;
  }

  public void throwError() {
    mChannel.throwError();
  }

  @NotNull
  public Channel<DATA, DATA> unsorted() {
    mChannel.unsorted();
    return this;
  }

  public Iterator<DATA> iterator() {
    return mChannel.iterator();
  }

  public void remove() {
    mChannel.remove();
  }

  /**
   * Abort handler used to close the channel on abort.
   */
  private static class ChannelAbortHandler implements AbortHandler {

    private ResultChannel<?> mChannel;

    public void onAbort(@NotNull final RoutineException reason, final long delay,
        @NotNull final TimeUnit timeUnit) {
      mChannel.close(reason);
    }

    private void setChannel(@NotNull final ResultChannel<?> channel) {
      mChannel = channel;
    }
  }

  /**
   * Executor decorator running executions synchronously if delay is 0.
   */
  private static class ChannelExecutor extends ScheduledExecutorDecorator {

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    private ChannelExecutor(@NotNull final ScheduledExecutor wrapped) {
      super(wrapped);
    }

    @Override
    public void execute(@NotNull final Runnable command) {
      sSyncExecutor.execute(command);
    }

    @Override
    public void execute(@NotNull final Runnable command, final long delay,
        @NotNull final TimeUnit timeUnit) {
      if (delay == 0) {
        execute(command);

      } else {
        super.execute(command, delay, timeUnit);
      }
    }

    @Override
    public boolean isExecutionThread() {
      return true;
    }
  }
}
