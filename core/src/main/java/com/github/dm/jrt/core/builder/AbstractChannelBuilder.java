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

import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Base abstract implementation of a channel builder.
 * <p>
 * Created by davide-maestroni on 12/11/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractChannelBuilder<IN, OUT> implements ChannelBuilder<IN, OUT> {

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  @NotNull
  public ChannelBuilder<IN, OUT> apply(@NotNull final ChannelConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
  }

  @NotNull
  public Builder<? extends ChannelBuilder<IN, OUT>> channelConfiguration() {
    return new Builder<ChannelBuilder<IN, OUT>>(this, mConfiguration);
  }

  /**
   * Returns the current configuration.
   *
   * @return the channel configuration.
   */
  @NotNull
  protected ChannelConfiguration getConfiguration() {
    return mConfiguration;
  }
}
