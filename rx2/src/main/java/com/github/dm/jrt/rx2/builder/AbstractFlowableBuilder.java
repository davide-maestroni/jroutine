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

package com.github.dm.jrt.rx2.builder;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx2.config.FlowableConfiguration;
import com.github.dm.jrt.rx2.config.FlowableConfiguration.Builder;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of an Flowable builder.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractFlowableBuilder<IN, OUT> implements FlowableBuilder<IN, OUT> {

  private FlowableConfiguration<IN> mConfiguration =
      FlowableConfiguration.defaultConfiguration();

  @NotNull
  public FlowableBuilder<IN, OUT> apply(@NotNull final FlowableConfiguration<IN> configuration) {
    mConfiguration = ConstantConditions.notNull("Flowable configuration", configuration);
    return this;
  }

  @NotNull
  public Builder<IN, FlowableBuilder<IN, OUT>> flowableConfiguration() {
    return new Builder<IN, FlowableBuilder<IN, OUT>>(this, mConfiguration);
  }

  @NotNull
  protected FlowableConfiguration<IN> getConfiguration() {
    return mConfiguration;
  }
}
