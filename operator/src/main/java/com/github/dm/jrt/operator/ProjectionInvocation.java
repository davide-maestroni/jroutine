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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.function.util.BiConsumer;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.BiConsumerDecorator.wrapBiConsumer;

/**
 * Mapping invocation applying the specified projection function to each input.
 * <p>
 * Created by davide-maestroni on 05/06/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class ProjectionInvocation<IN, OUT> extends MappingInvocation<IN, OUT> {

  private final BiConsumer<? super IN, ? super Channel<OUT, ?>> mProjectionConsumer;

  /**
   * Constructor.
   *
   * @param projectionConsumer the consumer instance.
   */
  ProjectionInvocation(
      @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> projectionConsumer) {
    super(asArgs(wrapBiConsumer(projectionConsumer)));
    mProjectionConsumer = projectionConsumer;
  }

  public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
    mProjectionConsumer.accept(input, result);
  }
}
