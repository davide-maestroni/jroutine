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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

/**
 * Try/catch channel consumer implementation.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class TryCatchChannelConsumer<OUT> implements ChannelConsumer<OUT> {

  private final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> mCatchConsumer;

  private final Channel<OUT, ?> mOutputChannel;

  /**
   * Constructor.
   *
   * @param catchConsumer the consumer instance.
   * @param outputChannel the output channel.
   */
  TryCatchChannelConsumer(
      @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> catchConsumer,
      @NotNull final Channel<OUT, ?> outputChannel) {
    mCatchConsumer = ConstantConditions.notNull("bi-consumer instance", catchConsumer);
    mOutputChannel = ConstantConditions.notNull("channel instance", outputChannel);
  }

  public void onComplete() {
    mOutputChannel.close();
  }

  public void onError(@NotNull final RoutineException error) {
    final Channel<OUT, ?> channel = mOutputChannel;
    try {
      mCatchConsumer.accept(error, channel);
      channel.close();

    } catch (final Throwable t) {
      channel.abort(t);
      InvocationInterruptedException.throwIfInterrupt(t);
    }
  }

  public void onOutput(final OUT output) {
    mOutputChannel.pass(output);
  }
}
