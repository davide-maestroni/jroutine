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

import java.math.BigDecimal;
import java.math.MathContext;

import static com.github.dm.jrt.operator.math.Numbers.toBigDecimalOptimistic;

/**
 * Invocation computing the average of the input numbers by employing a {@code BigDecimal}.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
class AverageBigInvocation extends TemplateInvocation<Number, BigDecimal> {

  private static final InvocationFactory<Number, BigDecimal> sFactory =
      new InvocationFactory<Number, BigDecimal>(null) {

        @NotNull
        @Override
        public Invocation<Number, BigDecimal> newInvocation() {
          return new AverageBigInvocation();
        }
      };

  private int mCount;

  private BigDecimal mSum;

  /**
   * Constructor.
   */
  private AverageBigInvocation() {
  }

  /**
   * Returns a factory of invocations computing the average of the input numbers by employing a
   * {@code BigDecimal}.
   *
   * @return the factory instance.
   */
  @NotNull
  static InvocationFactory<Number, BigDecimal> factoryOf() {
    return sFactory;
  }

  @Override
  public void onComplete(@NotNull final Channel<BigDecimal, ?> result) {
    result.pass(mSum.divide(new BigDecimal(mCount), MathContext.UNLIMITED));
  }

  @Override
  public void onInput(final Number input, @NotNull final Channel<BigDecimal, ?> result) {
    mSum = mSum.add(toBigDecimalOptimistic(input));
    ++mCount;
  }

  @Override
  public void onRestart() {
    mSum = BigDecimal.ZERO;
    mCount = 0;
  }
}
