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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.builder.AbstractStatelessRoutineBuilder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of a stateless routine builder.
 * <p>
 * Created by davide-maestroni on 02/27/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStatelessRoutineBuilder<IN, OUT>
    extends AbstractStatelessRoutineBuilder<IN, OUT, StatelessRoutineBuilder<IN, OUT>> {

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultStatelessRoutineBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  public Routine<IN, OUT> create() {
    final InvocationFactory<IN, OUT> factory =
        new DefaultStatelessFactoryBuilder<IN, OUT>().onNext(getOnNext())
                                                     .onError(getOnError())
                                                     .onComplete(getOnComplete())
                                                     .create();
    return JRoutineCore.routineOn(mExecutor).withConfiguration(getConfiguration()).of(factory);
  }
}
