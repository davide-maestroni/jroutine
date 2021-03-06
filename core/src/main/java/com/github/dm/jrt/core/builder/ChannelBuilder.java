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
 * Note that the passed inputs might be delivered through the configured runner.
 * <p>
 * Created by davide-maestroni on 03/07/2015.
 */
public interface ChannelBuilder extends ChannelConfigurable<ChannelBuilder> {

  /**
   * Builds and returns a channel instance.
   *
   * @param <DATA> the data type.
   * @return the newly created channel.
   */
  @NotNull
  <DATA> Channel<DATA, DATA> buildChannel();

  /**
   * Builds and returns a channel producing no data.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param <DATA> the data type.
   * @return the newly created channel.
   */
  @NotNull
  <DATA> Channel<DATA, DATA> of();

  /**
   * Builds and returns a channel producing the specified input.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param input  the input.
   * @param <DATA> the data type.
   * @return the newly created channel.
   */
  @NotNull
  <DATA> Channel<DATA, DATA> of(@Nullable DATA input);

  /**
   * Builds and returns a channel producing the specified inputs.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param inputs the input data.
   * @param <DATA> the data type.
   * @return the newly created channel.
   */
  @NotNull
  <DATA> Channel<DATA, DATA> of(@Nullable DATA... inputs);

  /**
   * Builds and returns a channel producing the specified inputs.
   * <p>
   * Note that the returned channel will be already closed.
   *
   * @param inputs the iterable returning the input data.
   * @param <DATA> the data type.
   * @return the newly created channel.
   */
  @NotNull
  <DATA> Channel<DATA, DATA> of(@Nullable Iterable<DATA> inputs);
}
