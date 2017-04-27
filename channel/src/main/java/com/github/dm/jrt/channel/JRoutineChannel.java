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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.channel.builder.ChannelHandler;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * This utility class acts as a factory of handlers providing several methods to split and merge
 * channels together, making also possible to transfer data in multiple flows.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 */
public class JRoutineChannel {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineChannel() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static ChannelHandler channelHandler() {
    return channelHandlerOn(defaultExecutor());
  }

  @NotNull
  public static ChannelHandler channelHandlerOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultChannelHandler(executor);
  }
}
