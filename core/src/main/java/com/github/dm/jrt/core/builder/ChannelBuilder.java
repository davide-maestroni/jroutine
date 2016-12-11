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

/**
 * Interface defining a builder of channel objects.
 * <p>
 * Note that the passed inputs might be delivered through the configured runner.
 * <p>
 * Created by davide-maestroni on 03/07/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface ChannelBuilder<IN, OUT> extends ChannelConfigurable<ChannelBuilder<IN, OUT>> {

  /**
   * Builds and returns a channel instance.
   *
   * @return the newly created channel.
   */
  @NotNull
  Channel<IN, OUT> buildChannel();
}
