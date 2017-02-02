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
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations prepending outputs.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 *
 * @param <OUT> the output data type.
 */
class PrependOutputInvocationFactory<OUT> extends InvocationFactory<OUT, OUT> {

  private final Channel<?, ? extends OUT> mChannel;

  /**
   * Constructor.
   *
   * @param channel the output channel.
   */
  PrependOutputInvocationFactory(@Nullable final Channel<?, ? extends OUT> channel) {
    super(asArgs(channel));
    mChannel = channel;
  }

  @NotNull
  @Override
  public Invocation<OUT, OUT> newInvocation() {
    return new PrependOutputInvocation<OUT>(mChannel);
  }

  /**
   * Invocation prepending outputs.
   *
   * @param <OUT> the output data type.
   */
  private static class PrependOutputInvocation<OUT> extends TemplateInvocation<OUT, OUT> {

    private final Channel<?, ? extends OUT> mChannel;

    private boolean mIsCalled;

    /**
     * Constructor.
     *
     * @param channel the output channel.
     */
    private PrependOutputInvocation(@Nullable final Channel<?, ? extends OUT> channel) {
      mChannel = channel;
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      onResult(result);
    }

    @Override
    public void onInput(final OUT input, @NotNull final Channel<OUT, ?> result) {
      onResult(result);
      result.pass(input);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }

    @Override
    public void onRestart() {
      mIsCalled = false;
    }

    private void onResult(@NotNull final Channel<OUT, ?> result) {
      if (!mIsCalled) {
        mIsCalled = true;
        result.pass(mChannel);
      }
    }
  }
}
