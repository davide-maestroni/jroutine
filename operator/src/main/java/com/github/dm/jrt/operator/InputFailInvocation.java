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
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.PredicateDecorator.wrapPredicate;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Mapping invocation failing when a specific condition is met.
 * <p>
 * Created by davide-maestroni on 04/30/2017.
 *
 * @param <IN> the input data type.
 */
class InputFailInvocation<IN> extends MappingInvocation<IN, IN> {

  private final Predicate<? super IN> mFailPredicate;

  private final Supplier<? extends Throwable> mReasonSupplier;

  /**
   * Constructor.
   *
   * @param failPredicate  the predicate instance.
   * @param reasonSupplier the supplier of the reason of the failure.
   */
  InputFailInvocation(@NotNull final Predicate<? super IN> failPredicate,
      @NotNull final Supplier<? extends Throwable> reasonSupplier) {
    super(asArgs(wrapPredicate(failPredicate), wrapSupplier(reasonSupplier)));
    mFailPredicate = failPredicate;
    mReasonSupplier = reasonSupplier;
  }

  public void onInput(final IN input, @NotNull final Channel<IN, ?> result) throws Exception {
    if (mFailPredicate.test(input)) {
      result.abort(mReasonSupplier.get());

    } else {
      result.pass(input);
    }
  }
}
