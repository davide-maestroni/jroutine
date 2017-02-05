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

package com.github.dm.jrt.channel.builder;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfigurable;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface defining a builder of lists of channels.
 * <p>
 * Created by davide-maestroni on 02/05/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface ChannelListBuilder<IN, OUT>
    extends ChannelConfigurable<ChannelListBuilder<IN, OUT>> {

  /**
   * Builds and return a new channel list.
   *
   * @return the newly created list of channels.
   */
  @NotNull
  List<? extends Channel<IN, OUT>> buildChannels();
}
