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

package com.github.dm.jrt.core.routine;

import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Empty abstract implementation of a routine.
 * <p>
 * This class is useful to avoid the need of implementing some of the methods defined in the
 * interface.
 * <p>
 * Created by davide-maestroni on 10/17/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class TemplateRoutine<IN, OUT> implements Routine<IN, OUT> {

  @NotNull
  public Channel<IN, OUT> call(@Nullable final IN input) {
    return invoke().pass(input).close();
  }

  @NotNull
  public Channel<IN, OUT> call(@Nullable final IN... inputs) {
    return invoke().pass(inputs).close();
  }

  @NotNull
  public Channel<IN, OUT> call(@Nullable final Iterable<? extends IN> inputs) {
    return invoke().pass(inputs).close();
  }

  @NotNull
  public Channel<IN, OUT> call(@Nullable final Channel<?, ? extends IN> inputs) {
    return invoke().pass(inputs).close();
  }

  @NotNull
  public Channel<IN, OUT> call() {
    return invoke().close();
  }

  @NotNull
  public Channel<IN, OUT> callParallel(@Nullable final IN input) {
    return invokeParallel().pass(input).close();
  }

  @NotNull
  public Channel<IN, OUT> callParallel(@Nullable final IN... inputs) {
    return invokeParallel().pass(inputs).close();
  }

  @NotNull
  public Channel<IN, OUT> callParallel(@Nullable final Iterable<? extends IN> inputs) {
    return invokeParallel().pass(inputs).close();
  }

  @NotNull
  public Channel<IN, OUT> callParallel(@Nullable final Channel<?, ? extends IN> inputs) {
    return invokeParallel().pass(inputs).close();
  }

  public void clear() {
  }
}
