package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Channel decorator making the wrapped one read-only.
 * <p>
 * Created by davide-maestroni on 05/05/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class ReadOnlyChannel<IN, OUT> extends DeepEqualObject implements Channel<IN, OUT> {

  private final Channel<IN, OUT> mChannel;

  /**
   * Constructor.
   *
   * @param channel the wrapped channel.
   */
  ReadOnlyChannel(@NotNull final Channel<IN, OUT> channel) {
    super(asArgs(channel));
    mChannel = ConstantConditions.notNull("channel instance", channel);
  }

  @NotNull
  private static IllegalStateException illegalInput() {
    return new IllegalStateException("cannot pass inputs to read-only channel");
  }

  public boolean abort() {
    return mChannel.abort();
  }

  public boolean abort(@Nullable final Throwable reason) {
    return mChannel.abort(reason);
  }

  @NotNull
  public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
    mChannel.after(delay, timeUnit);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> after(@NotNull final DurationMeasure delay) {
    mChannel.after(delay);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> afterNoDelay() {
    mChannel.afterNoDelay();
    return this;
  }

  @NotNull
  public List<OUT> all() {
    return mChannel.all();
  }

  @NotNull
  public Channel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
    mChannel.allInto(results);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> close() {
    return this;
  }

  @NotNull
  public Channel<IN, OUT> consume(@NotNull final ChannelConsumer<? super OUT> consumer) {
    mChannel.consume(consumer);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyAbort() {
    mChannel.eventuallyAbort();
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
    mChannel.eventuallyAbort(reason);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyContinue() {
    mChannel.eventuallyContinue();
    return this;
  }

  @NotNull
  public Channel<IN, OUT> eventuallyFail() {
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

  @NotNull
  public Channel<IN, OUT> in(final long timeout, @NotNull final TimeUnit timeUnit) {
    mChannel.in(timeout, timeUnit);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> in(@NotNull final DurationMeasure timeout) {
    mChannel.in(timeout);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> inNoTime() {
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
  public List<OUT> next(final int count) {
    return mChannel.next(count);
  }

  public OUT nextOrElse(final OUT output) {
    return mChannel.nextOrElse(output);
  }

  public int outputSize() {
    return mChannel.outputSize();
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
    throw illegalInput();
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
    throw illegalInput();
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final IN input) {
    throw illegalInput();
  }

  @NotNull
  public Channel<IN, OUT> pass(@Nullable final IN... inputs) {
    throw illegalInput();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <AFTER> Channel<IN, AFTER> pipe(@NotNull final Channel<? super OUT, AFTER> channel) {
    ((Channel<OUT, AFTER>) channel).pass(mChannel);
    return new FlatChannel<IN, AFTER>(this, channel);
  }

  public int size() {
    return mChannel.size();
  }

  @NotNull
  public Channel<IN, OUT> skipNext(final int count) {
    mChannel.skipNext(count);
    return this;
  }

  @NotNull
  public Channel<IN, OUT> sorted() {
    mChannel.sorted();
    return this;
  }

  public void throwError() {
    mChannel.throwError();
  }

  @NotNull
  public Channel<IN, OUT> unsorted() {
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
