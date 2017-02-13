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

import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Emitter;
import io.reactivex.FlowableEmitter;

/**
 * Channel consumer feeding an Emitter.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <OUT> the output data type.
 */
class EmitterConsumer<OUT> implements ChannelConsumer<OUT> {

  private final Emitter<OUT> mEmitter;

  /**
   * Constructor.
   *
   * @param emitter the Emitter instance.
   */
  EmitterConsumer(@NotNull final FlowableEmitter<OUT> emitter) {
    mEmitter = ConstantConditions.notNull("emitter instance", emitter);
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
