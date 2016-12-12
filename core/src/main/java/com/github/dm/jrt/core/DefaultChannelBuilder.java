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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Class implementing a builder of channel objects.
 * <p>
 * Created by davide-maestroni on 10/25/2014.
 *
 * @param <DATA> the data type.
 */
class DefaultChannelBuilder<DATA> extends AbstractChannelBuilder<DATA, DATA> {

  private final Iterable<DATA> mData;

  /**
   * Constructor.
   */
  DefaultChannelBuilder() {
    mData = null;
  }

  /**
   * Constructor.
   *
   * @param data the output data.
   */
  DefaultChannelBuilder(@NotNull Iterable<DATA> data) {
    mData = ConstantConditions.notNull("data", data);
  }

  @NotNull
  public Channel<DATA, DATA> buildChannel() {
    final DefaultChannel<DATA> channel = new DefaultChannel<DATA>(getConfiguration());
    final Iterable<DATA> data = mData;
    if (data != null) {
      channel.pass(data).close();
    }

    return channel;
  }
}
