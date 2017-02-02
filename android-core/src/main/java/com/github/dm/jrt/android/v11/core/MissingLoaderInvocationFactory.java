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

package com.github.dm.jrt.android.v11.core;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation factory used to know whether a Loader with a specific ID is present or not.
 * <p>
 * Created by davide-maestroni on 01/14/2015.
 *
 * @param <OUT> the output data type.
 */
final class MissingLoaderInvocationFactory<OUT> extends ContextInvocationFactory<Void, OUT> {

  private final int mId;

  /**
   * Constructor.
   *
   * @param id the Loader ID.
   */
  MissingLoaderInvocationFactory(final int id) {
    super(asArgs(id));
    mId = id;
  }

  @NotNull
  @Override
  public ContextInvocation<Void, OUT> newInvocation() {
    return new MissingLoaderInvocation<OUT>(mId);
  }

  /**
   * Call Context invocation implementation.
   *
   * @param <OUT> the output data type.
   */
  private static class MissingLoaderInvocation<OUT> extends TemplateContextInvocation<Void, OUT> {

    private final int mId;

    /**
     * Constructor.
     *
     * @param id the Loader ID.
     */
    private MissingLoaderInvocation(final int id) {
      mId = id;
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      result.abort(new MissingLoaderException(mId));
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }
}
