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
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.operator.math.Numbers.add;

/**
 * Invocation computing the sum of the input numbers.
 * <br>
 * The result will have the type matching the input with the highest precision.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
class SumInvocation extends TemplateInvocation<Number, Number> {

  private static final InvocationFactory<Number, Number> sFactory =
      new InvocationFactory<Number, Number>(null) {

        @NotNull
        @Override
        public Invocation<Number, Number> newInvocation() {
          return new SumInvocation();
        }
      };

  private Number mSum;

  /**
   * Constructor.
   */
  private SumInvocation() {
  }

  /**
   * Returns a factory of invocations computing the sum of the input numbers.
   *
   * @return the factory instance.
   */
  @NotNull
  static InvocationFactory<Number, Number> factoryOf() {
    return sFactory;
  }

  @Override
  public void onComplete(@NotNull final Channel<Number, ?> result) {
    result.pass(mSum);
  }

  @Override
  public void onInput(final Number input, @NotNull final Channel<Number, ?> result) {
    mSum = add(mSum, input);
  }

  @Override
  public boolean onRecycle(final boolean isReused) {
    return true;
  }

  @Override
  public void onRestart() {
    mSum = 0;
  }
}
