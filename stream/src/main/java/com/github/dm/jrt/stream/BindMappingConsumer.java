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
import com.github.dm.jrt.function.BiConsumer;
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
class BindMappingConsumer<IN, OUT> implements Function<Channel<?, IN>, Channel<?, OUT>> {

  private final ChannelConfiguration mConfiguration;

  private final BiConsumer<? super IN, ? super Channel<OUT, ?>> mMappingConsumer;

  /**
   * Constructor.
   *
   * @param configuration   the channel configuration.
   * @param mappingConsumer the mapping consumer.
   */
  BindMappingConsumer(@NotNull final ChannelConfiguration configuration,
      @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> mappingConsumer) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    mMappingConsumer = ConstantConditions.notNull("consumer instance", mappingConsumer);
  }

  public Channel<?, OUT> apply(final Channel<?, IN> channel) {
    final Channel<OUT, OUT> outputChannel = JRoutineCore.io().apply(mConfiguration).buildChannel();
    channel.bind(new MappingConsumerConsumer<IN, OUT>(mMappingConsumer, outputChannel));
    return outputChannel;
  }

  /**
   * Channel consumer implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class MappingConsumerConsumer<IN, OUT> implements ChannelConsumer<IN> {

    private final BiConsumer<? super IN, ? super Channel<OUT, ?>> mMappingConsumer;

    private final Channel<OUT, ?> mOutputChannel;

    /**
     * Constructor.
     *
     * @param mappingConsumer the mapping consumer.
     * @param outputChannel   the output channel.
     */
    private MappingConsumerConsumer(
        @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> mappingConsumer,
        @NotNull final Channel<OUT, ?> outputChannel) {
      mMappingConsumer = mappingConsumer;
      mOutputChannel = outputChannel;
    }

    public void onComplete() {
      mOutputChannel.close();
    }

    public void onError(@NotNull final RoutineException error) {
      mOutputChannel.abort(error);
    }

    public void onOutput(final IN output) throws Exception {
      mMappingConsumer.accept(output, mOutputChannel);
    }
  }
}
