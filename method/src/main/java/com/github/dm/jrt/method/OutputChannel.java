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

package com.github.dm.jrt.method;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Channel implementation acting as output of a routine method.
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 *
 * @see com.github.dm.jrt.method.RoutineMethod RoutineMethod
 */
public final class OutputChannel<OUT> implements Channel<OUT, OUT> {

  private final Channel<OUT, OUT> mChannel;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped channel.
   */
  OutputChannel(@NotNull final Channel<OUT, OUT> wrapped) {
    mChannel = ConstantConditions.notNull("wrapped channel", wrapped);
  }

  public boolean abort() {
    return mChannel.abort();
  }

  public boolean abort(@Nullable final Throwable reason) {
    return mChannel.abort(reason);
  }

  @NotNull
  public OutputChannel<OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
    mChannel.after(delay, timeUnit);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> after(@NotNull final UnitDuration delay) {
    mChannel.after(delay);
    return this;
  }

  @NotNull
  public List<OUT> all() {
    return mChannel.all();
  }

  @NotNull
  public OutputChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {
    mChannel.allInto(results);
    return this;
  }

  @NotNull
  public <AFTER> Channel<? super OUT, AFTER> bind(
      @NotNull final Channel<? super OUT, AFTER> channel) {
    return mChannel.bind(channel);
  }

  @NotNull
  public OutputChannel<OUT> bind(@NotNull final ChannelConsumer<? super OUT> consumer) {
    mChannel.bind(consumer);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> close() {
    mChannel.close();
    return this;
  }

  @NotNull
  public OutputChannel<OUT> eventuallyAbort() {
    mChannel.eventuallyAbort();
    return this;
  }

  @NotNull
  public OutputChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {
    mChannel.eventuallyAbort(reason);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> eventuallyContinue() {
    mChannel.eventuallyContinue();
    return this;
  }

  @NotNull
  public OutputChannel<OUT> eventuallyFail() {
    mChannel.eventuallyFail();
    return this;
  }

  @NotNull
  public Iterator<OUT> expiringIterator() {
    return mChannel.expiringIterator();
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

  public OUT next() {
    return mChannel.next();
  }

  public int inputCount() {
    return mChannel.inputCount();
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
  public List<OUT> next(final int count) {
    return mChannel.next(count);
  }

  public OUT nextOrElse(final OUT output) {
    return mChannel.nextOrElse(output);
  }

  @NotNull
  public OutputChannel<OUT> now() {
    mChannel.now();
    return this;
  }

  public int outputCount() {
    return mChannel.outputCount();
  }

  @NotNull
  public OutputChannel<OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
    mChannel.pass(channel);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> pass(@Nullable final Iterable<? extends OUT> inputs) {
    mChannel.pass(inputs);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> pass(@Nullable final OUT input) {
    mChannel.pass(input);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> pass(@Nullable final OUT... inputs) {
    mChannel.pass(inputs);
    return this;
  }

  public int size() {
    return mChannel.size();
  }

  @NotNull
  public OutputChannel<OUT> skipNext(final int count) {
    mChannel.skipNext(count);
    return this;
  }

  @NotNull
  public OutputChannel<OUT> sorted() {
    mChannel.sorted();
    return this;
  }

  public void throwError() {
    mChannel.throwError();
  }

  @NotNull
  public OutputChannel<OUT> unsorted() {
    mChannel.unsorted();
    return this;
  }

  public Iterator<OUT> iterator() {
    return mChannel.iterator();
  }

  public void remove() {
    mChannel.remove();
  }
}
