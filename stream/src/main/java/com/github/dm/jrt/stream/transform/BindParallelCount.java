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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Parallel by count binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindParallelCount<IN, OUT> extends BindMap<IN, OUT> {

  private final ChannelConfiguration mConfiguration;

  private final int mCount;

  /**
   * Constructor.
   *
   * @param configuration  the channel configuration.
   * @param count          the channel count.
   * @param routine        the routine instance.
   * @param invocationMode the invocation mode.
   * @throws java.lang.IllegalArgumentException if the channel count is not positive.
   */
  BindParallelCount(@NotNull final ChannelConfiguration configuration, final int count,
      @NotNull final Routine<? super IN, ? extends OUT> routine,
      @NotNull final InvocationMode invocationMode) {
    super(routine, invocationMode);
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    mCount = ConstantConditions.positive("channel count", count);
  }

  public Channel<?, OUT> apply(final Channel<?, IN> channel) {
    final int count = mCount;
    final Channel<OUT, OUT> outputChannel = JRoutineCore.io().apply(mConfiguration).buildChannel();
    final HashMap<Channel<IN, IN>, Channel<?, OUT>> channels =
        new HashMap<Channel<IN, IN>, Channel<?, OUT>>(count);
    for (int i = 0; i < count; ++i) {
      final Channel<IN, IN> inputChannel = JRoutineCore.io().buildChannel();
      final Channel<?, OUT> invocationChannel = super.apply(inputChannel);
      outputChannel.pass(invocationChannel);
      channels.put(inputChannel, invocationChannel);
    }

    channel.bind(new ParallelCountChannelConsumer<IN>(channels));
    return outputChannel.close();
  }
}
