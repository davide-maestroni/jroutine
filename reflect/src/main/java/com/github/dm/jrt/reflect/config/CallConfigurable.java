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

package com.github.dm.jrt.reflect.config;

import com.github.dm.jrt.reflect.config.CallConfiguration.Builder;
import com.github.dm.jrt.reflect.config.CallConfiguration.Configurable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining an object that can be configured through a call configuration.
 * <p>
 * Created by davide-maestroni on 05/01/2015.
 *
 * @param <TYPE> the object type.
 */
public interface CallConfigurable<TYPE> extends Configurable<TYPE> {

  /**
   * Gets the call configuration builder related to the instance.
   * <br>
   * The configuration options not supported by the specific implementation might be ignored.
   * <p>
   * Note that the configuration builder must be initialized with the current configuration.
   *
   * @return the call configuration builder.
   */
  @NotNull
  Builder<? extends TYPE> callConfiguration();
}
