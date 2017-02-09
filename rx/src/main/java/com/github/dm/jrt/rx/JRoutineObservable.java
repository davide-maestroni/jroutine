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
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.AbstractObservableBuilder;
import com.github.dm.jrt.rx.builder.ObservableBuilder;
import com.github.dm.jrt.rx.config.ObservableConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Cancellable;

import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Observable instance from a channel:
 * <pre><code>
 * JRoutineObservable.from(myChannel).buildObservable().subscribe(getAction());
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

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineObservable() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of Observables fed by the specified channel.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the Observable builder.
   */
  @NotNull
  public static <OUT> ObservableBuilder<?, OUT> from(@NotNull final Channel<?, OUT> channel) {
    return new ChannelObservableBuilder<OUT>(channel);
  }

  /**
   * Returns a builder of Observables fed by an invocation of the specified routine.
   *
   * @param routine the routine instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Observable builder.
   */
  @NotNull
  public static <IN, OUT> ObservableBuilder<IN, OUT> from(@NotNull final Routine<IN, OUT> routine) {
    return new RoutineObservableBuilder<IN, OUT>(routine);
  }

  /**
   * Returns a builder of Observables fed by an invocation of a routine created through the
   * specified builder.
   *
   * @param builder the routine builder.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Observable builder.
   */
  @NotNull
  public static <IN, OUT> ObservableBuilder<IN, OUT> from(
      @NotNull final RoutineBuilder<IN, OUT> builder) {
    return from(builder.buildRoutine());
  }

  /**
   * Returns a builder of channels fed by the specified Observable.
   *
   * @param observable the Observable instance.
   * @param <OUT>      the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> with(@NotNull final Observable<OUT> observable) {
    return new ObservableChannelBuilder<OUT>(observable);
  }

  /**
   * Function binding a channel to an emitter.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelEmitter<OUT> implements Action1<Emitter<OUT>>, Cancellable {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    private ChannelEmitter(@NotNull final Channel<?, OUT> channel) {
      mChannel = channel;
    }

    public void call(final Emitter<OUT> outEmitter) {
      outEmitter.setCancellation(this);
      mChannel.bind(new EmitterConsumer<OUT>(outEmitter));
    }

    public void cancel() {
      mChannel.after(noTime()).abort();
    }
  }

  /**
   * Builder of Observables emitting a channel output data.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelObservableBuilder<OUT>
      extends AbstractObservableBuilder<Object, OUT> {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    private ChannelObservableBuilder(@NotNull final Channel<?, OUT> channel) {
      mChannel = ConstantConditions.notNull("channel instance", channel);
    }

    @NotNull
    public Observable<OUT> buildObservable() {
      return Observable.fromEmitter(new ChannelEmitter<OUT>(mChannel),
          getConfiguration().getBackpressureOrElse(BackpressureMode.BUFFER));
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
   * Channel consumer feeding an emitter.
   *
   * @param <OUT> the output data type.
   */
  private static class EmitterConsumer<OUT> implements ChannelConsumer<OUT> {

    private final Emitter<OUT> mEmitter;

    /**
     * Constructor.
     *
     * @param emitter the emitter instance.
     */
    private EmitterConsumer(@NotNull final Emitter<OUT> emitter) {
      mEmitter = emitter;
    }

    public void onComplete() {
      mEmitter.onCompleted();
    }

    public void onError(@NotNull final RoutineException error) {
      mEmitter.onError(error);
    }

    public void onOutput(final OUT output) {
      mEmitter.onNext(output);
    }
  }

  /**
   * Builder of channels fed by an Observable.
   *
   * @param <OUT> the output data type.
   */
  private static class ObservableChannelBuilder<OUT> implements ChannelBuilder<OUT, OUT> {

    private final Observable<OUT> mObservable;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param observable the feeding Observable.
     */
    private ObservableChannelBuilder(@NotNull final Observable<OUT> observable) {
      mObservable = ConstantConditions.notNull("Observable instance", observable);
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
   * Function binding an invocation channel to an emitter.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineEmitter<IN, OUT> implements Action1<Emitter<OUT>> {

    private final Iterable<? extends IN> mInputs;

    private final Routine<? super IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     * @param inputs  an iterable returning the invocation inputs.
     */
    private RoutineEmitter(@NotNull final Routine<? super IN, OUT> routine,
        @Nullable final Iterable<? extends IN> inputs) {
      mRoutine = routine;
      mInputs = inputs;
    }

    public void call(final Emitter<OUT> outEmitter) {
      final Channel<? super IN, OUT> channel = mRoutine.call();
      outEmitter.setCancellation(new ChannelEmitter<OUT>(channel));
      channel.bind(new EmitterConsumer<OUT>(outEmitter)).pass(mInputs).close();
    }
  }

  /**
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineObservableBuilder<IN, OUT>
      extends AbstractObservableBuilder<IN, OUT> {

    private final Routine<IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     */
    private RoutineObservableBuilder(@NotNull final Routine<IN, OUT> routine) {
      mRoutine = ConstantConditions.notNull("routine instance", routine);
    }

    @NotNull
    public Observable<OUT> buildObservable() {
      final ObservableConfiguration<IN> configuration = getConfiguration();
      return Observable.fromEmitter(
          new RoutineEmitter<IN, OUT>(mRoutine, configuration.getInputsOrElse(null)),
          configuration.getBackpressureOrElse(BackpressureMode.BUFFER));
    }
  }
}
