package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.RxChannelBuilder;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Default implementation of an RxJava channel builder.
 * <p>
 * Created by davide-maestroni on 05/04/2017.
 */
class DefaultRxChannelBuilder implements RxChannelBuilder {

  private final ScheduledExecutor mExecutor;

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultRxChannelBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  public Channel<?, ?> of(@NotNull final Completable completable) {
    return of(completable.toFlowable());
  }

  @NotNull
  public <OUT> Channel<?, OUT> of(@NotNull final Flowable<OUT> flowable) {
    final Channel<OUT, OUT> channel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    flowable.subscribe(new ChannelSubscriber<OUT>(channel));
    return channel;
  }

  @NotNull
  public <OUT> Channel<?, OUT> of(@NotNull final Observable<OUT> observable) {
    return of(observable.toFlowable(BackpressureStrategy.MISSING));
  }

  @NotNull
  public <OUT> Channel<?, OUT> of(@NotNull final Single<OUT> single) {
    return of(single.toFlowable());
  }

  @NotNull
  public Builder<? extends RxChannelBuilder> withChannel() {
    return new Builder<RxChannelBuilder>(this, mConfiguration);
  }

  @NotNull
  public RxChannelBuilder withConfiguration(@NotNull final ChannelConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
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
}
