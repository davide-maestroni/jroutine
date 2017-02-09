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

package com.github.dm.jrt.rx2;

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
import com.github.dm.jrt.rx2.builder.AbstractFlowableBuilder;
import com.github.dm.jrt.rx2.builder.FlowableBuilder;
import com.github.dm.jrt.rx2.config.FlowableConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;

import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Flowable instance from a channel:
 * <pre><code>
 * JRoutineFlowable.from(myChannel).buildFlowable().subscribe(getAction());
 * </code></pre>
 * <p>
 * In a dual way, a channel can be created from an Flowable:
 * <pre><code>
 * JRoutineFlowable.with(myFlowable).buildChannel().bind(getConsumer());
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineFlowable {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineFlowable() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of Flowables fed by the specified channel.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the Flowable builder.
   */
  @NotNull
  public static <OUT> FlowableBuilder<?, OUT> from(@NotNull final Channel<?, OUT> channel) {
    return new ChannelFlowableBuilder<OUT>(channel);
  }

  /**
   * Returns a builder of Flowables fed by an invocation of the specified routine.
   *
   * @param routine the routine instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Flowable builder.
   */
  @NotNull
  public static <IN, OUT> FlowableBuilder<IN, OUT> from(@NotNull final Routine<IN, OUT> routine) {
    return new RoutineFlowableBuilder<IN, OUT>(routine);
  }

  /**
   * Returns a builder of Flowables fed by an invocation of a routine created through the
   * specified builder.
   *
   * @param builder the routine builder.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Flowable builder.
   */
  @NotNull
  public static <IN, OUT> FlowableBuilder<IN, OUT> from(
      @NotNull final RoutineBuilder<IN, OUT> builder) {
    return from(builder.buildRoutine());
  }

  /**
   * Returns a builder of channels fed by the specified Flowable.
   *
   * @param flowable the Flowable instance.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> with(@NotNull final Flowable<OUT> flowable) {
    return new FlowableChannelBuilder<OUT>(flowable);
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
   * Builder of Flowables emitting a channel output data.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelFlowableBuilder<OUT> extends AbstractFlowableBuilder<Object, OUT> {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    private ChannelFlowableBuilder(@NotNull final Channel<?, OUT> channel) {
      mChannel = ConstantConditions.notNull("channel instance", channel);
    }

    @NotNull
    public Flowable<OUT> buildFlowable() {
      return Flowable.create(new ChannelOnSubscribe<OUT>(mChannel),
          getConfiguration().getBackpressureOrElse(BackpressureStrategy.BUFFER));
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

    public void onSubscribe(final Disposable d) {
    }

    public void onNext(final OUT out) {
      mChannel.pass(out);
    }

    public void onError(final Throwable t) {
      mChannel.abort(InvocationException.wrapIfNeeded(t));
    }

    public void onComplete() {
      mChannel.close();
    }
  }

  /**
   * Function binding a channel to an emitter.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelOnSubscribe<OUT> implements FlowableOnSubscribe<OUT>, Cancellable {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    private ChannelOnSubscribe(@NotNull final Channel<?, OUT> channel) {
      mChannel = channel;
    }

    public void cancel() {
      mChannel.after(noTime()).abort();
    }

    public void subscribe(final FlowableEmitter<OUT> emitter) throws Exception {
      emitter.setCancellable(this);
      mChannel.bind(new EmitterConsumer<OUT>(emitter));
    }
  }

  /**
   * Subscriber feeding a channel.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelSubscriber<OUT> implements Subscriber<OUT> {

    private final Channel<OUT, ?> mChannel;

    /**
     * The channel instance.
     *
     * @param channel the channel.
     */
    private ChannelSubscriber(@NotNull final Channel<OUT, ?> channel) {
      mChannel = channel;
    }

    public void onSubscribe(final Subscription s) {
      s.request(Long.MAX_VALUE);
    }

    public void onNext(final OUT out) {
      mChannel.pass(out);
    }

    public void onError(final Throwable t) {
      mChannel.abort(InvocationException.wrapIfNeeded(t));
    }

    public void onComplete() {
      mChannel.close();
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
    private EmitterConsumer(@NotNull final FlowableEmitter<OUT> emitter) {
      mEmitter = emitter;
    }

    public void onComplete() {
      mEmitter.onComplete();
    }

    public void onError(@NotNull final RoutineException error) {
      mEmitter.onError(error);
    }

    public void onOutput(final OUT output) {
      mEmitter.onNext(output);
    }
  }

  /**
   * Builder of channels fed by an Flowable.
   *
   * @param <OUT> the output data type.
   */
  private static class FlowableChannelBuilder<OUT> implements ChannelBuilder<OUT, OUT> {

    private final Flowable<OUT> mFlowable;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param flowable the feeding Flowable.
     */
    private FlowableChannelBuilder(@NotNull final Flowable<OUT> flowable) {
      mFlowable = ConstantConditions.notNull("Flowable instance", flowable);
    }

    @NotNull
    public ChannelBuilder<OUT, OUT> apply(@NotNull final ChannelConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
      return this;
    }

    @NotNull
    public Channel<OUT, OUT> buildChannel() {
      final Channel<OUT, OUT> channel = JRoutineCore.<OUT>ofInputs().buildChannel();
      mFlowable.subscribe(new ChannelSubscriber<OUT>(channel));
      return channel;
    }

    @NotNull
    public Builder<? extends ChannelBuilder<OUT, OUT>> channelConfiguration() {
      return new Builder<ChannelBuilder<OUT, OUT>>(this, mConfiguration);
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
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineFlowableBuilder<IN, OUT> extends AbstractFlowableBuilder<IN, OUT> {

    private final Routine<IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     */
    private RoutineFlowableBuilder(@NotNull final Routine<IN, OUT> routine) {
      mRoutine = ConstantConditions.notNull("routine instance", routine);
    }

    @NotNull
    public Flowable<OUT> buildFlowable() {
      final FlowableConfiguration<IN> configuration = getConfiguration();
      return Flowable.create(
          new RoutineOnSubscribe<IN, OUT>(mRoutine, configuration.getInputsOrElse(null)),
          configuration.getBackpressureOrElse(BackpressureStrategy.BUFFER));
    }
  }

  /**
   * Function binding an invocation channel to an emitter.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineOnSubscribe<IN, OUT> implements FlowableOnSubscribe<OUT> {

    private final Iterable<IN> mInputs;

    private final Routine<? super IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     * @param inputs  an iterable returning the invocation inputs.
     */
    private RoutineOnSubscribe(@NotNull final Routine<? super IN, OUT> routine,
        @Nullable final Iterable<IN> inputs) {
      mRoutine = routine;
      mInputs = inputs;
    }

    public void subscribe(final FlowableEmitter<OUT> emitter) {
      final Channel<? super IN, OUT> channel = mRoutine.call();
      emitter.setCancellable(new ChannelOnSubscribe<OUT>(channel));
      channel.bind(new EmitterConsumer<OUT>(emitter)).pass(mInputs).close();
    }
  }
}
