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

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.config.ObservableConfiguration;
import com.github.dm.jrt.rx.config.ObservableConfiguration.Builder;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of an Observable builder.
 * <p>
 * Created by davide-maestroni on 02/08/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractObservableBuilder<IN, OUT> implements ObservableBuilder<IN, OUT> {

  private ObservableConfiguration<IN> mConfiguration =
      ObservableConfiguration.defaultConfiguration();

  @NotNull
  public ObservableBuilder<IN, OUT> apply(
      @NotNull final ObservableConfiguration<IN> configuration) {
    mConfiguration = ConstantConditions.notNull("Observable configuration", configuration);
    return this;
  }

  @NotNull
  public Builder<IN, ObservableBuilder<IN, OUT>> observableConfiguration() {
    return new Builder<IN, ObservableBuilder<IN, OUT>>(this, mConfiguration);
  }

  @NotNull
  protected ObservableConfiguration<IN> getConfiguration() {
    return mConfiguration;
  }
}
