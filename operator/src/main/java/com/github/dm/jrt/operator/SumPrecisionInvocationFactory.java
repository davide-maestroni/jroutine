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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.operator.math.Operation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.operator.math.Numbers.add;
import static com.github.dm.jrt.operator.math.Numbers.getOperation;

/**
 * Factory of invocations computing the sum of the input numbers in the specified class precision.
 * <p>
 * Created by davide-maestroni on 01/25/2017.
 *
 * @param <N> the number type.
 */
class SumPrecisionInvocationFactory<N extends Number> extends InvocationFactory<Number, N> {

  private final Operation<?> mOperation;

  /**
   * Constructor.
   *
   * @param type the number type.
   */
  SumPrecisionInvocationFactory(@NotNull final Class<N> type) {
    super(asArgs(type));
    mOperation = getOperation(type);
  }

  @NotNull
  public Invocation<Number, N> newInvocation() throws Exception {
    return new SumPrecisionInvocation<N>(mOperation);
  }

  /**
   * Invocation computing the sum of the input numbers in the specified class precision.
   *
   * @param <N> the number type.
   */
  private static class SumPrecisionInvocation<N extends Number>
      extends TemplateInvocation<Number, N> {

    private final Operation<?> mOperation;

    private Number mSum;

    /**
     * Constructor.
     *
     * @param operation the operation instance.
     */
    private SumPrecisionInvocation(@NotNull final Operation<?> operation) {
      mOperation = operation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComplete(@NotNull final Channel<N, ?> result) {
      result.pass((N) mSum);
    }

    @Override
    public void onInput(final Number input, @NotNull final Channel<N, ?> result) {
      mSum = mOperation.convert(add(mSum, input));
    }

    @Override
    public void onRestart() {
      mSum = mOperation.convert(0);
    }
  }
}
