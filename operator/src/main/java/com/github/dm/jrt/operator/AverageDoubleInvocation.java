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

import static com.github.dm.jrt.operator.math.Numbers.addOptimistic;

/**
 * Invocation computing the average of the input numbers in double precision.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
class AverageDoubleInvocation extends TemplateInvocation<Number, Double> {

  private static final InvocationFactory<Number, Double> sFactory =
      new InvocationFactory<Number, Double>(null) {

        @NotNull
        @Override
        public Invocation<Number, Double> newInvocation() {
          return new AverageDoubleInvocation();
        }
      };

  private int mCount;

  private Number mSum;

  /**
   * Constructor.
   */
  private AverageDoubleInvocation() {
  }

  /**
   * Returns a factory of invocations computing the average of the input numbers in double
   * precision.
   *
   * @return the factory instance.
   */
  @NotNull
  static InvocationFactory<Number, Double> factoryOf() {
    return sFactory;
  }

  @Override
  public void onComplete(@NotNull final Channel<Double, ?> result) {
    if (mCount == 0) {
      result.pass(0d);

    } else {
      result.pass(mSum.doubleValue() / mCount);
    }
  }

  @Override
  public void onInput(final Number input, @NotNull final Channel<Double, ?> result) {
    mSum = addOptimistic(mSum, input).doubleValue();
    ++mCount;
  }

  @Override
  public void onRestart() {
    mSum = (byte) 0;
    mCount = 0;
  }
}
