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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Channel implementation piping the data coming from an input into an output one.
 * <p>
 * Created by davide-maestroni on 02/21/2017.
 *
 * @param <IN>   the input data type.
 * @param <DATA> the intermediate data type.
 * @param <OUT>  the output data type.
 */
public class PipeChannel<IN, DATA, OUT> implements Channel<IN, OUT> {

  private final Channel<? super IN, ? extends DATA> mInputChannel;

  private final Channel<? super DATA, OUT> mOutputChannel;

  /**
   * Constructor.
   *
   * @param inputChannel  the input channel.
   * @param outputChannel the output channel.
   */
  @SuppressWarnings("unchecked")
  public PipeChannel(@NotNull final Channel<? super IN, ? extends DATA> inputChannel,
      @NotNull final Channel<? super DATA, OUT> outputChannel) {
    mInputChannel = ConstantConditions.notNull("input channel", inputChannel);
    mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
    ((Channel<DATA, OUT>) outputChannel).pass(inputChannel);
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
    mInputChannel.close();
    mOutputChannel.close();
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

  public int inputSize() {
    return mInputChannel.size();
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

  public int outputSize() {
    return mOutputChannel.size();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
    ((Channel<IN, DATA>) mInputChannel).pass(channel);
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
  public <AFTER> Channel<IN, AFTER> pipe(@NotNull final Channel<? super OUT, AFTER> channel) {
    return new PipeChannel<IN, OUT, AFTER>(this, channel);
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

  public Iterator<OUT> iterator() {
    return mOutputChannel.iterator();
  }

  public void remove() {
    mOutputChannel.remove();
  }
}
