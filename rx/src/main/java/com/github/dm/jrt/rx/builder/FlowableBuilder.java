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

package com.github.dm.jrt.rx.builder;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.rx.config.FlowableConfigurable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;

/**
 * Interface defining a builder of Flowables.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
public interface FlowableBuilder extends FlowableConfigurable<FlowableBuilder> {

  /**
   * Builds and returns a Flowable fed by the specified channel.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the newly created Flowable instance.
   */
  @NotNull
  <OUT> Flowable<OUT> of(@NotNull Channel<?, OUT> channel);
}
