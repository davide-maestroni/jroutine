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

import org.jetbrains.annotations.NotNull;

import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Cancellable;

import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Function binding a channel to an Emitter.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <OUT> the output data type.
 */
class ChannelOnSubscribe<OUT> implements FlowableOnSubscribe<OUT>, Cancellable {

  private final Channel<?, OUT> mChannel;

  /**
   * Constructor.
   *
   * @param channel the channel instance.
   */
  ChannelOnSubscribe(@NotNull final Channel<?, OUT> channel) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
  }

  public void cancel() {
    mChannel.after(noTime()).abort();
  }

  public void subscribe(final FlowableEmitter<OUT> emitter) {
    emitter.setCancellable(this);
    mChannel.bind(new EmitterConsumer<OUT>(emitter));
  }
}
