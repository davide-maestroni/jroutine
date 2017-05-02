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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * TODO javadoc
 * <p><
 * Created by davide-maestroni on 05/02/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineTransformations {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineTransformations() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static ChannelTransformer channelTransformer() {
    return channelTransformerOn(defaultExecutor());
  }

  @NotNull
  public static ChannelTransformer channelTransformerOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultChannelTransformer(executor);
  }
}
