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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations collecting inputs into arrays.
 * <p>
 * Created by davide-maestroni on 08/19/2016.
 *
 * @param <IN> the input data type.
 */
class ToArrayInvocationFactory<IN> extends InvocationFactory<IN, IN[]> {

  private final Class<? extends IN> mComponentType;

  /**
   * Constructor.
   *
   * @param componentType the array component type.
   */
  ToArrayInvocationFactory(@NotNull final Class<? extends IN> componentType) {
    super(asArgs(componentType));
    mComponentType = ConstantConditions.notNull("element type", componentType);
  }

  @NotNull
  @Override
  public Invocation<IN, IN[]> newInvocation() {
    return new ToArrayInvocation<IN>(mComponentType);
  }

  /**
   * Implementation of an invocation collecting inputs into arrays.
   *
   * @param <IN> the input data type.
   */
  private static class ToArrayInvocation<IN> extends CallInvocation<IN, IN[]> {

    private final Class<? extends IN> mElementType;

    /**
     * Constructor.
     *
     * @param elementType the array element type.
     */
    private ToArrayInvocation(@NotNull final Class<? extends IN> elementType) {
      mElementType = elementType;
    }

    @Override
    protected void onCall(@NotNull final List<? extends IN> inputs,
        @NotNull final Channel<IN[], ?> result) throws Exception {
      @SuppressWarnings("unchecked") final IN[] array =
          (IN[]) Array.newInstance(mElementType, inputs.size());
      result.pass(inputs.toArray(array));
    }
  }
}
