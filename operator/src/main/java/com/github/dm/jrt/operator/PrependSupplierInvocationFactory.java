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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of prepending invocation used to call a supplier a specific number of times.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 *
 * @param <OUT> the output data type.
 */
class PrependSupplierInvocationFactory<OUT> extends InvocationFactory<OUT, OUT> {

  private final long mCount;

  private final SupplierDecorator<? extends OUT> mOutputSupplier;

  /**
   * Constructor.
   *
   * @param count          the loop count.
   * @param outputSupplier the supplier instance.
   */
  PrependSupplierInvocationFactory(final long count,
      @NotNull final SupplierDecorator<? extends OUT> outputSupplier) {
    super(asArgs(ConstantConditions.positive("count number", count),
        ConstantConditions.notNull("supplier instance", outputSupplier)));
    mCount = count;
    mOutputSupplier = outputSupplier;
  }

  @NotNull
  @Override
  public Invocation<OUT, OUT> newInvocation() {
    return new PrependSupplierInvocation<OUT>(mCount, mOutputSupplier);
  }

  /**
   * Prepending invocation used to call a supplier a specific number of times.
   *
   * @param <OUT> the output data type.
   */
  private static class PrependSupplierInvocation<OUT> extends TemplateInvocation<OUT, OUT> {

    private final long mCount;

    private final SupplierDecorator<? extends OUT> mOutputSupplier;

    private boolean mIsCalled;

    /**
     * Constructor.
     *
     * @param count          the loop count.
     * @param outputSupplier the supplier instance.
     */
    private PrependSupplierInvocation(final long count,
        @NotNull final SupplierDecorator<? extends OUT> outputSupplier) {
      mCount = count;
      mOutputSupplier = outputSupplier;
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      onResult(result);
    }

    @Override
    public void onInput(final OUT input, @NotNull final Channel<OUT, ?> result) throws Exception {
      onResult(result);
      result.pass(input);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }

    @Override
    public void onRestart() {
      mIsCalled = false;
    }

    private void onResult(@NotNull final Channel<OUT, ?> result) throws Exception {
      if (!mIsCalled) {
        mIsCalled = true;
        final long count = mCount;
        final SupplierDecorator<? extends OUT> supplier = mOutputSupplier;
        for (long i = 0; i < count; ++i) {
          result.pass(supplier.get());
        }
      }
    }
  }
}
