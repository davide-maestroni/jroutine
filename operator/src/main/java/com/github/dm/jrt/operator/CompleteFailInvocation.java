/*
 * Copyright 2017 Davide Maestroni
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
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Invocation failing on complete.
 * <p>
 * Created by davide-maestroni on 04/30/2017.
 *
 * @param <DATA> the data type.
 */
class CompleteFailInvocation<DATA> extends GenerateInvocation<DATA, DATA> {

  private final Supplier<? extends Throwable> mReasonSupplier;

  /**
   * Constructor.
   *
   * @param reasonSupplier the supplier of the reason of the failure.
   */
  CompleteFailInvocation(@NotNull final Supplier<? extends Throwable> reasonSupplier) {
    super(asArgs(wrapSupplier(reasonSupplier)));
    mReasonSupplier = reasonSupplier;
  }

  public void onComplete(@NotNull final Channel<DATA, ?> result) throws Exception {
    result.abort(mReasonSupplier.get());
  }

  public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
    result.pass(input);
  }
}
