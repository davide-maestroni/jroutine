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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx2.builder.AbstractFlowableBuilder;

import org.jetbrains.annotations.NotNull;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

/**
 * Builder of Flowables emitting a channel output data.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <OUT> the output data type.
 */
class ChannelFlowableBuilder<OUT> extends AbstractFlowableBuilder<Object, OUT> {

  private final Channel<?, OUT> mChannel;

  /**
   * Constructor.
   *
   * @param channel the channel instance.
   */
  ChannelFlowableBuilder(@NotNull final Channel<?, OUT> channel) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
  }

  @NotNull
  public Flowable<OUT> buildFlowable() {
    return Flowable.create(new ChannelOnSubscribe<OUT>(mChannel),
        getConfiguration().getBackpressureOrElse(BackpressureStrategy.BUFFER));
  }
}
