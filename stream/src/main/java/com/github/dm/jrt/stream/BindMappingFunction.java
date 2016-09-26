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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Mapping binding function.
 * <p>
 * Created by davide-maestroni on 09/11/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindMappingFunction<IN, OUT> implements Function<Channel<?, IN>, Channel<?, OUT>> {

  private final ChannelConfiguration mConfiguration;

  private final Function<? super IN, ? extends OUT> mMappingFunction;

  /**
   * Constructor.
   *
   * @param configuration   the channel configuration.
   * @param mappingFunction the mapping function.
   */
  BindMappingFunction(@NotNull final ChannelConfiguration configuration,
      @NotNull final Function<? super IN, ? extends OUT> mappingFunction) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    mMappingFunction = ConstantConditions.notNull("function instance", mappingFunction);
  }

  public Channel<?, OUT> apply(final Channel<?, IN> channel) {
    final Channel<OUT, OUT> outputChannel = JRoutineCore.io().apply(mConfiguration).buildChannel();
    channel.bind(new MappingFunctionConsumer<IN, OUT>(mMappingFunction, outputChannel));
    return outputChannel;
  }

  /**
   * Channel consumer implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class MappingFunctionConsumer<IN, OUT> implements ChannelConsumer<IN> {

    private final Function<? super IN, ? extends OUT> mMappingFunction;

    private final Channel<OUT, ?> mOutputChannel;

    /**
     * Constructor.
     *
     * @param mappingFunction the mapping function.
     * @param outputChannel   the output channel.
     */
    private MappingFunctionConsumer(
        @NotNull final Function<? super IN, ? extends OUT> mappingFunction,
        @NotNull final Channel<OUT, ?> outputChannel) {
      mMappingFunction = mappingFunction;
      mOutputChannel = outputChannel;
    }

    public void onComplete() {
      mOutputChannel.close();
    }

    public void onError(@NotNull final RoutineException error) {
      mOutputChannel.abort(error);
    }

    public void onOutput(final IN output) throws Exception {
      mOutputChannel.pass(mMappingFunction.apply(output));
    }
  }
}
