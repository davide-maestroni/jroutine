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

package com.github.dm.jrt.android.v4.function;

import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.function.builder.AbstractStatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of a stateless Loader routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStatelessLoaderRoutineBuilderCompat<IN, OUT>
    extends AbstractStatelessLoaderRoutineBuilder<IN, OUT, StatelessLoaderRoutineBuilder<IN, OUT>> {

  private final LoaderSourceCompat mLoaderSource;

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultStatelessLoaderRoutineBuilderCompat(@NotNull final LoaderSourceCompat loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
  }

  @NotNull
  @Override
  public LoaderRoutine<IN, OUT> create() {
    final ContextInvocationFactory<IN, OUT> factory =
        new DefaultStatelessContextFactoryBuilderCompat<IN, OUT>().onNext(getOnNext())
                                                                  .onError(getOnError())
                                                                  .onComplete(getOnComplete())
                                                                  .create();
    return JRoutineLoaderCompat.routineOn(mLoaderSource)
                               .withConfiguration(getInvocationConfiguration())
                               .withConfiguration(getLoaderConfiguration())
                               .of(factory);
  }
}
