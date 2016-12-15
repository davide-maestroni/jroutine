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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Observable instance from a channel:
 * <pre>
 *   <code>
 *
 *     JRoutineRx.observableFrom(myChannel).subscribe(getAction());
 *   </code>
 * </pre>
 * <p>
 * In a dual way, a channel can be created from an Observable:
 * <pre>
 *   <code>
 *
 *     JRoutineRx.from(myObservable).buildChannel().bind(getConsumer());
 *   </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 12/09/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineRx {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineRx() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels fed by the specified observable.
   *
   * @param observable the observable instance.
   * @param <OUT>      the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> from(@NotNull final Observable<OUT> observable) {
    return new ObservableChannelBuilder<OUT>(observable);
  }

  /**
   * Creates a new observable from the specified channel.
   * <br>
   * Note that the channel will be bound only when an observer subscribes to the observable.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the observable instance.
   */
  @NotNull
  public static <OUT> Observable<OUT> observableFrom(@NotNull final Channel<?, OUT> channel) {
    return Observable.create(new OnSubscribeChannel<OUT>(channel));
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

    public void onError(final Throwable e) {
      mChannel.abort(InvocationException.wrapIfNeeded(e));
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
    public Builder<? extends ChannelBuilder<OUT, OUT>> applyChannelConfiguration() {
      return new Builder<ChannelBuilder<OUT, OUT>>(this, mConfiguration);
    }

    @NotNull
    public Channel<OUT, OUT> buildChannel() {
      final Channel<OUT, OUT> channel = JRoutineCore.<OUT>ofInputs().buildChannel();
      mObservable.subscribe(new ChannelObserver<OUT>(channel));
      return channel;
    }
  }

  /**
   * Subscription listener binding the subscriber to a channel.
   *
   * @param <OUT> the output data type.
   */
  private static class OnSubscribeChannel<OUT> implements OnSubscribe<OUT> {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    private OnSubscribeChannel(@NotNull final Channel<?, OUT> channel) {
      mChannel = ConstantConditions.notNull("channel", channel);
    }

    public void call(final Subscriber<? super OUT> subscriber) {
      mChannel.bind(new SubscriberChannelConsumer<OUT>(subscriber));
    }
  }

  /**
   * Channel consumer feeding output data to a subscriber.
   *
   * @param <OUT> the output data type.
   */
  private static class SubscriberChannelConsumer<OUT> implements ChannelConsumer<OUT> {

    private final Subscriber<? super OUT> mSubscriber;

    /**
     * Constructor.
     *
     * @param subscriber the subscriber instance.
     */
    private SubscriberChannelConsumer(@NotNull final Subscriber<? super OUT> subscriber) {
      mSubscriber = subscriber;
    }

    public void onComplete() {
      final Subscriber<? super OUT> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onCompleted();
      }
    }

    public void onError(@NotNull final RoutineException error) {
      final Subscriber<? super OUT> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onError(error);
      }
    }

    public void onOutput(final OUT output) {
      final Subscriber<? super OUT> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onNext(output);
      }
    }
  }
}
