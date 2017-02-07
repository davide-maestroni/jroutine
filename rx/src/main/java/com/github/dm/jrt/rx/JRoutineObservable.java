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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Action3;
import rx.functions.Func0;
import rx.observables.AsyncOnSubscribe;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Observable instance from a channel:
 * <pre><code>
 * JRoutineObservable.create(myChannel.in(seconds(3))).subscribe(getAction());
 * </code></pre>
 * <p>
 * In a dual way, a channel can be created from an Observable:
 * <pre><code>
 * JRoutineObservable.with(myObservable).buildChannel().bind(getConsumer());
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 12/09/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineObservable {

  private static final ChannelNext<?> sChannelNext = new ChannelNext<Object>();

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineObservable() {
    ConstantConditions.avoid();
  }

  /**
   * Creates a new observable from the specified channel.
   * <br>
   * Note that, unless properly configure, the channel might timeout when polled for outputs.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.ChannelConfiguration ChannelConfiguration
   */
  @NotNull
  public static <OUT> Observable<OUT> create(@NotNull final Channel<?, OUT> channel) {
    @SuppressWarnings("unchecked") final ChannelNext<OUT> channelNext =
        (ChannelNext<OUT>) sChannelNext;
    return Observable.create(
        AsyncOnSubscribe.createSingleState(new ChannelGenerator<OUT>(channel), channelNext,
            channelNext));
  }

  /**
   * Creates a new observable by invoking the specified routine.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param routine the routine instance.
   * @param input   the invocation input.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <IN, OUT> Observable<OUT> create(@NotNull final Routine<? super IN, OUT> routine,
      @Nullable final IN input) {
    return create(routine, Collections.singletonList(input));
  }

  /**
   * Creates a new observable by invoking the specified routine.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param routine the routine instance.
   * @param inputs  the invocation inputs.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <IN, OUT> Observable<OUT> create(@NotNull final Routine<? super IN, OUT> routine,
      @Nullable final IN... inputs) {
    if (inputs == null) {
      return create(routine);
    }

    return create(routine, Arrays.asList(inputs));
  }

  /**
   * Creates a new observable by invoking the specified routine.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param routine the routine instance.
   * @param inputs  an iterable returning the invocation inputs.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <IN, OUT> Observable<OUT> create(@NotNull final Routine<? super IN, OUT> routine,
      @Nullable final Iterable<IN> inputs) {
    if (inputs == null) {
      return create(routine);
    }

    @SuppressWarnings("unchecked") final ChannelNext<OUT> channelNext =
        (ChannelNext<OUT>) sChannelNext;
    return Observable.create(
        AsyncOnSubscribe.createSingleState(new RoutineGenerator<IN, OUT>(routine, inputs),
            channelNext, channelNext));
  }

  /**
   * Creates a new observable by invoking the specified routine.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param routine the routine instance.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <OUT> Observable<OUT> create(@NotNull final Routine<?, OUT> routine) {
    final ChannelNext<OUT> channelNext = (ChannelNext<OUT>) sChannelNext;
    return Observable.create(AsyncOnSubscribe.createSingleState(
        new RoutineGenerator<Object, OUT>((Routine<Object, OUT>) routine, null), channelNext,
        channelNext));
  }

  /**
   * Creates a new observable by invoking a routine instantiated through the specified builder.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param builder the routine builder.
   * @param input   the invocation input.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <IN, OUT> Observable<OUT> create(
      @NotNull final RoutineBuilder<? super IN, OUT> builder, @Nullable final IN input) {
    return create(builder.buildRoutine(), input);
  }

  /**
   * Creates a new observable by invoking a routine instantiated through the specified builder.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param builder the routine builder.
   * @param inputs  the invocation inputs.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <IN, OUT> Observable<OUT> create(
      @NotNull final RoutineBuilder<? super IN, OUT> builder, @Nullable final IN... inputs) {
    return create(builder.buildRoutine(), inputs);
  }

  /**
   * Creates a new observable by invoking a routine instantiated through the specified builder.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param builder the routine builder.
   * @param inputs  an iterable returning the invocation inputs.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <IN, OUT> Observable<OUT> create(
      @NotNull final RoutineBuilder<? super IN, OUT> builder, @Nullable final Iterable<IN> inputs) {
    return create(builder.buildRoutine(), inputs);
  }

  /**
   * Creates a new observable by invoking a routine instantiated through the specified builder.
   * <br>
   * Note that, unless properly configure, the invocation might timeout when polled for outputs.
   *
   * @param builder the routine builder.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   * @see com.github.dm.jrt.core.config.InvocationConfiguration InvocationConfiguration
   */
  @NotNull
  public static <OUT> Observable<OUT> create(@NotNull final RoutineBuilder<?, OUT> builder) {
    return create(builder.buildRoutine());
  }

  /**
   * Returns a builder of channels fed by the specified observable.
   *
   * @param observable the observable instance.
   * @param <OUT>      the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> with(@NotNull final Observable<OUT> observable) {
    return new ObservableChannelBuilder<OUT>(observable);
  }

  /**
   * Function returning a channel instance.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelGenerator<OUT> implements Func0<Channel<?, OUT>> {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    private ChannelGenerator(@NotNull final Channel<?, OUT> channel) {
      mChannel = ConstantConditions.notNull("channel instance", channel);
    }

    public Channel<?, OUT> call() {
      return mChannel;
    }
  }

  /**
   * Subscription listener reading channel data.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelNext<OUT>
      implements Action3<Channel<?, OUT>, Long, Observer<Observable<? extends OUT>>>,
      Action1<Channel<?, OUT>> {

    public void call(final Channel<?, OUT> channel, final Long requested,
        final Observer<Observable<? extends OUT>> observer) {
      try {
        final int count = (int) Math.min(Integer.MAX_VALUE, requested);
        final List<OUT> outputs = channel.next(count);
        if (!outputs.isEmpty()) {
          observer.onNext(Observable.from(outputs));

        } else if (channel.getComplete()) {
          observer.onCompleted();

        } else {
          observer.onNext(Observable.<OUT>empty());
        }

      } catch (final RoutineException e) {
        observer.onError(e);
        InterruptedInvocationException.throwIfInterrupt(e);
      }
    }

    public void call(final Channel<?, OUT> channel) {
      channel.abort();
    }
  }

  /**
   * Observer feeding a channel.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelObserver<OUT> implements Observer<OUT> {

    private final Channel<OUT, ?> mChannel;

    /**
     * The channel instance.
     *
     * @param channel the channel.
     */
    private ChannelObserver(@NotNull final Channel<OUT, ?> channel) {
      mChannel = channel;
    }

    public void onCompleted() {
      mChannel.close();
    }

    public void onError(final Throwable t) {
      mChannel.abort(InvocationException.wrapIfNeeded(t));
    }

    public void onNext(final OUT out) {
      mChannel.pass(out);
    }
  }

  /**
   * Builder of channels fed by an observable.
   *
   * @param <OUT> the output data type.
   */
  private static class ObservableChannelBuilder<OUT> implements ChannelBuilder<OUT, OUT> {

    private final Observable<OUT> mObservable;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param observable the feeding observable.
     */
    private ObservableChannelBuilder(@NotNull final Observable<OUT> observable) {
      mObservable = ConstantConditions.notNull("observable instance", observable);
    }

    @NotNull
    public ChannelBuilder<OUT, OUT> apply(@NotNull final ChannelConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
      return this;
    }

    @NotNull
    public Channel<OUT, OUT> buildChannel() {
      final Channel<OUT, OUT> channel = JRoutineCore.<OUT>ofInputs().buildChannel();
      mObservable.subscribe(new ChannelObserver<OUT>(channel));
      return channel;
    }

    @NotNull
    public Builder<? extends ChannelBuilder<OUT, OUT>> channelConfiguration() {
      return new Builder<ChannelBuilder<OUT, OUT>>(this, mConfiguration);
    }
  }

  /**
   * Function returning a routine invocation channel.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineGenerator<IN, OUT> implements Func0<Channel<?, OUT>> {

    private final Iterable<IN> mInputs;

    private final Routine<? super IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     * @param inputs  an iterable returning the invocation inputs.
     */
    private RoutineGenerator(@NotNull final Routine<? super IN, OUT> routine,
        @Nullable final Iterable<IN> inputs) {
      mRoutine = ConstantConditions.notNull("routine instance", routine);
      mInputs = inputs;
    }

    public Channel<?, OUT> call() {
      return mRoutine.call(mInputs);
    }
  }
}
