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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Appending invocation used to call a supplier a specific number of times.
 * <p>
 * Created by davide-maestroni on 07/06/2016.
 *
 * @param <OUT> the output data type.
 */
class AppendSupplierInvocation<OUT> extends GenerateInvocation<OUT, OUT> {

  private final long mCount;

  private final Supplier<? extends OUT> mOutputSupplier;

  /**
   * Constructor.
   *
   * @param count          the loop count.
   * @param outputSupplier the supplier instance.
   */
  AppendSupplierInvocation(final long count,
      @NotNull final Supplier<? extends OUT> outputSupplier) {
    super(asArgs(ConstantConditions.positive("count number", count), wrapSupplier(outputSupplier)));
    mCount = count;
    mOutputSupplier = outputSupplier;
  }

  public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
    final long count = mCount;
    final Supplier<? extends OUT> supplier = mOutputSupplier;
    for (long i = 0; i < count; ++i) {
      result.pass(supplier.get());
    }
  }

  public void onInput(final OUT input, @NotNull final Channel<OUT, ?> result) {
    result.pass(input);
  }
}
