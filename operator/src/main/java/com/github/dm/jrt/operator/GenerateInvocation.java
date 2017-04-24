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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base abstract implementation of an invocation generating output data.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
abstract class GenerateInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
    implements Invocation<IN, OUT> {

  /**
   * Constructor.
   *
   * @param args the constructor arguments.
   */
  GenerateInvocation(@Nullable final Object[] args) {
    super(args);
  }

  @NotNull
  @Override
  public final Invocation<IN, OUT> newInvocation() {
    return this;
  }

  public final void onAbort(@NotNull final RoutineException reason) {
  }

  public final void onDestroy() {
  }

  public final boolean onRecycle() {
    return true;
  }

  public final void onStart() {
  }
}
