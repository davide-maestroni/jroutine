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

package com.github.dm.jrt.android.channel;

import com.github.dm.jrt.android.channel.builder.AndroidChannelHandler;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * Utility class for handling routine channels.
 * <p>
 * Created by davide-maestroni on 06/18/2015.
 */
public class JRoutineAndroidChannels {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineAndroidChannels() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a channel handler employing the default executor.
   *
   * @return the handler instance.
   */
  @NotNull
  public static AndroidChannelHandler channelHandler() {
    return channelHandlerOn(defaultExecutor());
  }

  /**
   * Returns a channel handler employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the handler instance.
   */
  @NotNull
  public static AndroidChannelHandler channelHandlerOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultAndroidChannelHandler(executor);
  }
}
