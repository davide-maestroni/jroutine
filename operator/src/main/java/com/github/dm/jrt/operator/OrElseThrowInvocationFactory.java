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
 * Factory of invocations aborting the execution when no output has been received.
 * <p>
 * Created by davide-maestroni on 07/02/2016.
 *
 * @param <DATA> the data type.
 */
class OrElseThrowInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

  private final Throwable mError;

  /**
   * Constructor.
   *
   * @param error the error.
   */
  OrElseThrowInvocationFactory(@Nullable final Throwable error) {
    super(asArgs(error));
    mError = error;
  }

  @NotNull
  @Override
  public Invocation<DATA, DATA> newInvocation() {
    return new OrElseThrowInvocation<DATA>(mError);
  }

  /**
   * Invocation aborting the execution when no output has been received.
   *
   * @param <DATA> the data type.
   */
  private static class OrElseThrowInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

    private final Throwable mError;

    private boolean mHasOutputs;

    /**
     * Constructor.
     *
     * @param error the error.
     */
    OrElseThrowInvocation(@Nullable final Throwable error) {
      mError = error;
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) {
      if (!mHasOutputs) {
        result.abort(mError);
      }
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
      mHasOutputs = true;
      result.pass(input);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }

    @Override
    public void onRestart() {
      mHasOutputs = false;
    }
  }
}
