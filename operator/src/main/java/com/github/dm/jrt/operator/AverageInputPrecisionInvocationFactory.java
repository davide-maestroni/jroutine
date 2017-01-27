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
import static com.github.dm.jrt.operator.math.Numbers.divideSafe;
import static com.github.dm.jrt.operator.math.Numbers.getOperationSafe;

/**
 * Factory of invocations computing the average of the input numbers in the specified class
 * precision.
 * <p>
 * Created by davide-maestroni on 01/26/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class AverageInputPrecisionInvocationFactory<IN extends Number, OUT extends Number>
    extends InvocationFactory<Number, OUT> {

  private final Operation<?> mInputOperation;

  private final Operation<?> mOutputOperation;

  /**
   * Constructor.
   *
   * @param inType  the input type.
   * @param outType the output type.
   */
  AverageInputPrecisionInvocationFactory(@NotNull final Class<IN> inType,
      @NotNull final Class<OUT> outType) {
    super(asArgs(inType, outType));
    mInputOperation = getOperationSafe(inType);
    mOutputOperation = getOperationSafe(outType);
  }

  @NotNull
  public Invocation<Number, OUT> newInvocation() {
    return new AverageInvocation<OUT>(mInputOperation, mOutputOperation);
  }

  /**
   * Invocation computing the average of the input numbers in the specified class precision.
   *
   * @param <N> the number type.
   */
  private static class AverageInvocation<N extends Number> extends TemplateInvocation<Number, N> {

    private final Operation<?> mInputOperation;

    private final Operation<?> mOutputOperation;

    private int mCount;

    private Number mSum;

    /**
     * Constructor.
     *
     * @param inputOperation  the input operation instance.
     * @param outputOperation the output operation instance.
     */
    private AverageInvocation(@NotNull final Operation<?> inputOperation,
        @NotNull final Operation<?> outputOperation) {
      mInputOperation = inputOperation;
      mOutputOperation = outputOperation;
    }

    @Override
    public void onComplete(@NotNull final Channel<N, ?> result) {
      final Operation<?> outputOperation = mOutputOperation;
      final int count = mCount;
      if (count == 0) {
        result.pass((N) outputOperation.convert(0));

      } else {
        result.pass((N) outputOperation.convert(divideSafe(outputOperation.convert(mSum), count)));
      }
    }

    @Override
    public void onInput(final Number input, @NotNull final Channel<N, ?> result) {
      mSum = mInputOperation.add(mSum, input);
      ++mCount;
    }

    @Override
    public void onRestart() {
      mSum = mInputOperation.convert(0);
      mCount = 0;
    }
  }
}
