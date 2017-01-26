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
import static com.github.dm.jrt.operator.math.Numbers.addSafe;
import static com.github.dm.jrt.operator.math.Numbers.getOperationSafe;

/**
 * Factory of invocations computing the average of the input numbers in the specified class
 * precision.
 * <p>
 * Created by davide-maestroni on 01/25/2017.
 *
 * @param <N> the number type.
 */
class AverageOutputPrecisionInvocationFactory<N extends Number>
    extends InvocationFactory<Number, N> {

  private final Operation<?> mOperation;

  /**
   * Constructor.
   *
   * @param type the number type.
   */
  AverageOutputPrecisionInvocationFactory(@NotNull final Class<N> type) {
    super(asArgs(type));
    mOperation = getOperationSafe(type);
  }

  @NotNull
  public Invocation<Number, N> newInvocation() {
    return new AverageInvocation<N>(mOperation);
  }

  /**
   * Invocation computing the average of the input numbers in the specified class precision.
   *
   * @param <N> the number type.
   */
  private static class AverageInvocation<N extends Number> extends TemplateInvocation<Number, N> {

    private final Operation<?> mOperation;

    private int mCount;

    private Number mSum;

    /**
     * Constructor.
     *
     * @param operation the operation instance.
     */
    private AverageInvocation(@NotNull final Operation<?> operation) {
      mOperation = operation;
    }

    @Override
    public void onComplete(@NotNull final Channel<N, ?> result) {
      final int count = mCount;
      if (count == 0) {
        result.pass((N) mOperation.convert(0));

      } else {
        result.pass((N) mOperation.divide(mSum, count));
      }
    }

    @Override
    public void onInput(final Number input, @NotNull final Channel<N, ?> result) {
      mSum = mOperation.convert(addSafe(mSum, input));
      ++mCount;
    }

    @Override
    public void onRestart() {
      mSum = mOperation.convert(0);
      mCount = 0;
    }
  }
}
