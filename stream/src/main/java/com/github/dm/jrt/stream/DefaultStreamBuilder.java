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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.stream.builder.StreamConfiguration;

import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of a stream routine builder.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStreamBuilder<IN, OUT> extends AbstractStreamBuilder<IN, OUT> {

  private static final StreamConfiguration sDefaultConfiguration =
      new StreamConfiguration(InvocationConfiguration.defaultConfiguration(),
          InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC);

  /**
   * Constructor.
   */
  DefaultStreamBuilder() {
    this(sDefaultConfiguration);
  }

  /**
   * Constructor.
   *
   * @param streamConfiguration the stream configuration.
   */
  private DefaultStreamBuilder(@NotNull final StreamConfiguration streamConfiguration) {
    super(streamConfiguration);
  }

  @NotNull
  @Override
  protected <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
      @NotNull final StreamConfiguration streamConfiguration,
      @NotNull final InvocationFactory<? super BEFORE, ? extends AFTER> factory) {
    return newRoutine(streamConfiguration, JRoutineCore.with(factory));
  }
}
