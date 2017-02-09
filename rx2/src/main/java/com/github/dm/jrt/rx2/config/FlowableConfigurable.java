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

package com.github.dm.jrt.rx2.config;

import com.github.dm.jrt.rx2.config.FlowableConfiguration.Builder;
import com.github.dm.jrt.rx2.config.FlowableConfiguration.Configurable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining an object that can be configured through an Flowable configuration.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 *
 * @param <IN>   the input data type.
 * @param <TYPE> the configurable object type.
 */
public interface FlowableConfigurable<IN, TYPE> extends Configurable<IN, TYPE> {

  /**
   * Gets the Flowable configuration builder related to the instance.
   * <br>
   * The configuration options not supported by the specific implementation might be ignored.
   * <p>
   * Note that the configuration builder must be initialized with the current configuration.
   *
   * @return the Flowable configuration builder.
   */
  @NotNull
  Builder<IN, TYPE> flowableConfiguration();
}
