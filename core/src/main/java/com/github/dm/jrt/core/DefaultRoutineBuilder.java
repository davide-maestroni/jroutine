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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.builder.AbstractRoutineBuilder;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDecorator;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class implementing a builder of routine objects based on an invocation factory.
 * <p>
 * Created by davide-maestroni on 09/21/2014.
 */
class DefaultRoutineBuilder extends AbstractRoutineBuilder {

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultRoutineBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  public <IN, OUT> Routine<IN, OUT> of(@NotNull final InvocationFactory<IN, OUT> factory) {
    return new DefaultRoutine<IN, OUT>(getConfiguration(), mExecutor, factory);
  }

  @NotNull
  public <IN, OUT> Routine<IN, OUT> ofSingleton(@NotNull final Invocation<IN, OUT> invocation) {
    return new DefaultRoutine<IN, OUT>(
        getConfiguration().builderFrom().withMaxInvocations(1).configured(), mExecutor,
        new SingletonInvocationFactory<IN, OUT>(invocation));
  }

  /**
   * Invocation decorator ensuring that a destroyed instance will not be reused.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SingletonInvocation<IN, OUT> extends InvocationDecorator<IN, OUT> {

    private boolean mIsDestroyed;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    private SingletonInvocation(@NotNull final Invocation<IN, OUT> wrapped) {
      super(wrapped);
    }

    @Override
    public void onDestroy() throws Exception {
      mIsDestroyed = true;
      super.onDestroy();
    }

    @Override
    public void onStart() throws Exception {
      if (mIsDestroyed) {
        throw new IllegalStateException("the invocation has been destroyed");
      }

      super.onStart();
    }
  }

  /**
   * Factory wrapping an invocation instance so that, once destroyed, it will not be reused.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SingletonInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final SingletonInvocation<IN, OUT> mInvocation;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    private SingletonInvocationFactory(@NotNull final Invocation<IN, OUT> wrapped) {
      super(asArgs(wrapped));
      mInvocation = new SingletonInvocation<IN, OUT>(wrapped);
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return mInvocation;
    }
  }
}
