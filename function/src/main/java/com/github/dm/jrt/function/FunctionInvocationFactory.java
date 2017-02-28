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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.lambda.FunctionDecorator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of call invocations based on a function instance.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class FunctionInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

  private final CallInvocation<IN, OUT> mInvocation;

  /**
   * Constructor.
   *
   * @param function the function instance.
   */
  FunctionInvocationFactory(
      @NotNull final FunctionDecorator<? super List<IN>, ? extends OUT> function) {
    super(asArgs(ConstantConditions.notNull("function wrapper", function)));
    mInvocation = new FunctionInvocation<IN, OUT>(function);
  }

  @NotNull
  @Override
  public Invocation<IN, OUT> newInvocation() {
    return mInvocation;
  }

  /**
   * Invocation implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class FunctionInvocation<IN, OUT> extends CallInvocation<IN, OUT> {

    private final FunctionDecorator<? super List<IN>, ? extends OUT> mFunction;

    /**
     * Constructor.
     *
     * @param function the function instance.
     */
    private FunctionInvocation(
        @NotNull FunctionDecorator<? super List<IN>, ? extends OUT> function) {
      mFunction = function;
    }

    @Override
    protected void onCall(@NotNull final List<? extends IN> inputs,
        @NotNull final Channel<OUT, ?> result) throws Exception {
      result.pass(mFunction.apply(new ArrayList<IN>(inputs)));
    }
  }
}
