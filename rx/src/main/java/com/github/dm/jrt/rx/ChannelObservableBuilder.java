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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.AbstractObservableBuilder;

import org.jetbrains.annotations.NotNull;

import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Cancellable;

import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Builder of Observables emitting a channel output data.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <OUT> the output data type.
 */
class ChannelObservableBuilder<OUT> extends AbstractObservableBuilder<Object, OUT> {

  private final Channel<?, OUT> mChannel;

  /**
   * Constructor.
   *
   * @param channel the channel instance.
   */
  ChannelObservableBuilder(@NotNull final Channel<?, OUT> channel) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
  }

  @NotNull
  public Observable<OUT> buildObservable() {
    return Observable.fromEmitter(new ChannelEmitter<OUT>(mChannel),
        getConfiguration().getBackpressureOrElse(BackpressureMode.BUFFER));
  }

  /**
   * Function binding a channel to an Emitter.
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
}
