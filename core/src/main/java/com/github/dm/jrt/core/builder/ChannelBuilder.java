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

package com.github.dm.jrt.core.builder;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfigurable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface defining a builder of channel objects.
 * <p>
 * Note that the passed inputs might be delivered through the configured executor.
 * <p>
 * Created by davide-maestroni on 03/07/2015.
 */
public interface ChannelBuilder extends ChannelConfigurable<ChannelBuilder> {

  /**
   * Creates a channel producing the specified outputs.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param outputs the output data.
   * @param <OUT>   the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of(@Nullable OUT... outputs);

  /**
   * Creates a channel producing the specified outputs.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param outputs the iterable returning the output data.
   * @param <OUT>   the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of(@Nullable Iterable<OUT> outputs);

  /**
   * Creates a channel producing no data.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param <OUT> the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of();

  /**
   * Creates a channel producing the specified output.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param output the output.
   * @param <OUT>  the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> of(@Nullable OUT output);

  /**
   * Creates a new channel instance.
   *
   * @param <DATA> the data type.
   * @return the channel instance.
   */
  @NotNull
  <DATA> Channel<DATA, DATA> ofType();
}
