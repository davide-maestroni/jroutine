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

import com.github.dm.jrt.channel.builder.BufferStreamConfiguration.Builder;
import com.github.dm.jrt.channel.builder.BufferStreamConfiguration.Configurable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining an object that can be configured through an output stream configuration.
 * <p>
 * Created by davide-maestroni on 01/01/2017.
 *
 * @param <TYPE> the object type.
 */
public interface BufferStreamConfigurable<TYPE> extends Configurable<TYPE> {

  /**
   * Gets the output stream configuration builder related to the instance.
   * <br>
   * The configuration options not supported by the specific implementation might be ignored.
   * <p>
   * Note that the configuration builder must be initialized with the current configuration.
   *
   * @return the output stream configuration builder.
   */
  @NotNull
  Builder<? extends TYPE> applyBufferStreamConfiguration();
}