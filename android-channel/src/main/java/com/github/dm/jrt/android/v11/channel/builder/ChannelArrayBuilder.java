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

package com.github.dm.jrt.android.v11.channel.builder;

import android.util.SparseArray;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfigurable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of sparse arrays of channels.
 * <p>
 * Created by davide-maestroni on 12/12/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface ChannelArrayBuilder<IN, OUT>
    extends ChannelConfigurable<ChannelArrayBuilder<IN, OUT>> {

  /**
   * Builds and return a new channel sparse array.
   *
   * @return the newly created array of channels.
   */
  @NotNull
  SparseArray<? extends Channel<IN, OUT>> buildChannelArray();
}
