package com.github.dm.jrt.rx.builder;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfigurable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Interface defining a builder of channels fed by an RxJava object.
 * <p>
 * Created by davide-maestroni on 05/04/2017.
 */
public interface RxChannelBuilder extends ChannelConfigurable<RxChannelBuilder> {

  /**
   * Returns a channel fed by the specified Completable.
   *
   * @param completable the Completable instance.
   * @return the channel instance.
   */
  @NotNull
  Channel<?, ?> of(@NotNull Completable completable);

  /**
   * Returns a channel fed by the specified Flowable.
   *
   * @param flowable the Flowable instance.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of(@NotNull Flowable<OUT> flowable);

  /**
   * Returns a channel fed by the specified Observable.
   *
   * @param observable the Observable instance.
   * @param <OUT>      the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of(@NotNull Observable<OUT> observable);

  /**
   * Returns a channel fed by the specified Single.
   *
   * @param single the Single instance.
   * @param <OUT>  the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of(@NotNull Single<OUT> single);
}
