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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Builder of channels fed by an Observable.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <OUT> the output data type.
 */
class ObservableChannelBuilder<OUT> implements ChannelBuilder<OUT, OUT> {

  private final Observable<OUT> mObservable;

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param observable the feeding Observable.
   */
  ObservableChannelBuilder(@NotNull final Observable<OUT> observable) {
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
}
