/*
 * Copyright 2017 Davide Maestroni
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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Channel implementation pushing inputs to an input one and collecting outputs from an output one.
 * <p>
 * Created by davide-maestroni on 02/21/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class FlatChannel<IN, OUT> implements Channel<IN, OUT> {

  private final ArrayList<Channel<?, ?>> mChannels = new ArrayList<Channel<?, ?>>();

  private final Channel<IN, ?> mInputChannel;

  private final Channel<?, OUT> mOutputChannel;

  /**
   * Constructor.
   *
   * @param inputChannel  the input channel.
   * @param outputChannel the output channel.
   */
  @SuppressWarnings("unchecked")
  FlatChannel(@NotNull final Channel<IN, ?> inputChannel,
      @NotNull final Channel<?, OUT> outputChannel) {
    if (outputChannel instanceof FlatChannel) {
      final FlatChannel<?, OUT> flatChannel = (FlatChannel<?, OUT>) outputChannel;
      mChannels.addAll(flatChannel.mChannels);
      mOutputChannel = flatChannel.mOutputChannel;

    } else {
      mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
      mChannels.add(outputChannel);
    }

    if (inputChannel instanceof FlatChannel) {
      final FlatChannel<IN, ?> flatChannel = (FlatChannel<IN, ?>) inputChannel;
      mChannels.addAll(flatChannel.mChannels);
      mInputChannel = flatChannel.mInputChannel;

    } else {
      mInputChannel = ConstantConditions.notNull("input channel", inputChannel);
      mChannels.add(inputChannel);
    }
  }

  public boolean abort() {
    return mInputChannel.abort();
  }

  public boolean abort(@Nullable final Throwable reason) {
    return mInputChannel.abort(reason);
  }

  @NotNull
  public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
    mInputChannel.after(delay, timeUnit);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> after(@NotNull final DurationMeasure delay) {
    mInputChannel.after(delay);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> afterNoDelay() {
    mInputChannel.afterNoDelay();
    return this;
  }

  @NotNull
  public List<OUT> all() {
    return mOutputChannel.all();
  }

  @NotNull
  public Channel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
    mOutputChannel.allInto(results);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> close() {
    for (final Channel<?, ?> channel : mChannels) {
      channel.close();
    }

    return this;
  }

  @NotNull
  public Channel<IN, OUT> consume(@NotNull final ChannelConsumer<? super OUT> consumer) {
    mOutputChannel.consume(consumer);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyAbort() {
    mOutputChannel.eventuallyAbort();
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
    mOutputChannel.eventuallyAbort(reason);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyContinue() {
    mOutputChannel.eventuallyContinue();
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyFail() {
    mOutputChannel.eventuallyFail();
    return this;
  }

  @NotNull
  public Iterator<OUT> expiringIterator() {
    return mOutputChannel.expiringIterator();
  }

  public OUT get() {
    return mOutputChannel.get();
  }

  public boolean getComplete() {
    return mOutputChannel.getComplete();
  }

  @Nullable
  public RoutineException getError() {
    return mOutputChannel.getError();
  }

  public boolean hasNext() {
    return mOutputChannel.hasNext();
  }

  public OUT next() {
    return mOutputChannel.next();
  }

  @NotNull
  public Channel<IN, OUT> in(final long timeout, @NotNull final TimeUnit timeUnit) {
    mOutputChannel.in(timeout, timeUnit);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> in(@NotNull final DurationMeasure timeout) {
    mOutputChannel.in(timeout);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> inNoTime() {
    mOutputChannel.inNoTime();
    return this;
  }

  public boolean isBound() {
    return mOutputChannel.isBound();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean isOpen() {
    return mInputChannel.isOpen();
  }

  @NotNull
  public List<OUT> next(final int count) {
    return mOutputChannel.next(count);
  }

  public OUT nextOrElse(final OUT output) {
    return mOutputChannel.nextOrElse(output);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
    mInputChannel.pass(channel);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
    mInputChannel.pass(inputs);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final IN input) {
    mInputChannel.pass(input);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final IN... inputs) {
    mInputChannel.pass(inputs);
    return this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <AFTER> Channel<IN, AFTER> pipe(@NotNull final Channel<? super OUT, AFTER> channel) {
    ((Channel<OUT, AFTER>) channel).pass(this);
    return new FlatChannel<IN, AFTER>(this, channel);
  }

  public int size() {
    return mInputChannel.size() + mOutputChannel.size();
  }

  @NotNull
  public Channel<IN, OUT> skipNext(final int count) {
    mOutputChannel.skipNext(count);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> sorted() {
    mInputChannel.sorted();
    return this;
  }

  public void throwError() {
    mOutputChannel.throwError();
  }

  @NotNull
  public Channel<IN, OUT> unsorted() {
    mInputChannel.unsorted();
    return this;
  }

  @Override
  public int hashCode() {
    return mChannels.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final FlatChannel<?, ?> that = (FlatChannel<?, ?>) o;
    return mChannels.equals(that.mChannels);
  }

  public Iterator<OUT> iterator() {
    return mOutputChannel.iterator();
  }

  public void remove() {
    mOutputChannel.remove();
  }
}
