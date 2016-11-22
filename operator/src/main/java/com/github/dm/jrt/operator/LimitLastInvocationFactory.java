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
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations limiting data to last inputs.
 * <p>
 * Created by davide-maestroni on 07/26/2016.
 *
 * @param <DATA> the data type.
 */
class LimitLastInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

  private final int mCount;

  /**
   * Constructor.
   *
   * @param count the number of data to pass.
   * @throws java.lang.IllegalArgumentException if the count is negative.
   */
  LimitLastInvocationFactory(final int count) {
    super(asArgs(ConstantConditions.notNegative("count", count)));
    mCount = count;
  }

  @NotNull
  @Override
  public Invocation<DATA, DATA> newInvocation() {
    return new LimitInvocation<DATA>(mCount);
  }

  /**
   * Routine invocation passing only the last {@code count} input data.
   *
   * @param <DATA> the data type.
   */
  private static class LimitInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

    private final int mCount;

    private final SimpleQueue<DATA> mData = new SimpleQueue<DATA>();

    /**
     * Constructor.
     *
     * @param count the number of data to pass.
     */
    private LimitInvocation(final int count) {
      mCount = count;
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) throws Exception {
      result.pass(mData);
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
      final SimpleQueue<DATA> data = mData;
      data.add(input);
      if (data.size() > mCount) {
        data.removeFirst();
      }
    }

    @Override
    public void onRecycle(final boolean isReused) throws Exception {
      mData.clear();
    }
  }
}
