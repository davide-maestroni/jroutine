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

package com.github.dm.jrt.function.builder;

import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.Consumer;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class used to build channel consumers based on functions.
 * <p>
 * Created by davide-maestroni on 09/21/2015.
 *
 * @param <OUT> the output data type.
 */
public interface FunctionalChannelConsumer<OUT> extends ChannelConsumer<OUT> {

  /**
   * Returns a new channel consumer builder employing also the specified consumer function to
   * handle the invocation completion.
   *
   * @param onComplete the action instance.
   * @return the builder instance.
   */
  @NotNull
  FunctionalChannelConsumer<OUT> andOnComplete(@NotNull Action onComplete);

  /**
   * Returns a new channel consumer builder employing also the specified consumer function to
   * handle the invocation errors.
   *
   * @param onError the consumer function.
   * @return the builder instance.
   */
  @NotNull
  FunctionalChannelConsumer<OUT> andOnError(@NotNull Consumer<? super RoutineException> onError);

  /**
   * Returns a new channel consumer builder employing also the specified consumer function to
   * handle the invocation outputs.
   *
   * @param onOutput the consumer function.
   * @return the builder instance.
   */
  @NotNull
  FunctionalChannelConsumer<OUT> andOnOutput(@NotNull Consumer<? super OUT> onOutput);
}
