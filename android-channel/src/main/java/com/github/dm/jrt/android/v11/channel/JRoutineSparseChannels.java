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

package com.github.dm.jrt.android.v11.channel;

import com.github.dm.jrt.android.channel.JRoutineAndroidChannels;
import com.github.dm.jrt.android.v11.channel.builder.SparseChannelHandler;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * Utility class for handling routine channels.
 * <p>
 * See
 * {@link com.github.dm.jrt.android.v4.channel.JRoutineSparseChannelsCompat JRoutineSparseChannelsCompat}
 * for support of API levels lower than {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
public class JRoutineSparseChannels extends JRoutineAndroidChannels {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineSparseChannels() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a channel handler employing the default executor.
   *
   * @return the handler instance.
   */
  @NotNull
  public static SparseChannelHandler channelHandler() {
    return channelHandlerOn(defaultExecutor());
  }

  /**
   * Returns a channel handler employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the handler instance.
   */
  @NotNull
  public static SparseChannelHandler channelHandlerOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultSparseChannelHandler(executor);
  }
}
