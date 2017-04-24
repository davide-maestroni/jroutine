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

package com.github.dm.jrt.core.builder;

import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routine objects.
 * <p>
 * Created by davide-maestroni on 11/11/2014.
 */
public interface RoutineBuilder extends InvocationConfigurable<RoutineBuilder> {

  /**
   * Builds a new routine instance based on the specified invocation factory.
   * <br>
   * In order to prevent undesired leaks, the class of the specified factory should have a static
   * scope.
   *
   * @param factory the invocation factory.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the routine instance.
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> of(@NotNull InvocationFactory<IN, OUT> factory);

  /**
   * Builds a new routine instance based on the specified invocation.
   * <br>
   * Only that specific invocation instance will be employed (and possibly recycled) by the routine.
   * <br>
   * In case the instance is destroyed, a successive routine invocation will fail with an exception.
   *
   * @param invocation the invocation insatnce.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine instance.
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> ofSingleton(@NotNull Invocation<IN, OUT> invocation);
}
