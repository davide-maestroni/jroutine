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
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.lambda.PredicateDecorator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Mapping invocation replacing all the data satisfying the specified predicate.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 *
 * @param <DATA> the data type.
 */
class ReplaceInvocation<DATA> extends MappingInvocation<DATA, DATA> {

  private final PredicateDecorator<? super DATA> mPredicate;

  private final DATA mReplacement;

  /**
   * Constructor.
   *
   * @param predicate   the predicate instance.
   * @param replacement the replacement instance.
   */
  ReplaceInvocation(@NotNull final PredicateDecorator<? super DATA> predicate,
      @Nullable final DATA replacement) {
    super(asArgs(ConstantConditions.notNull("predicate instance", predicate), replacement));
    mPredicate = predicate;
    mReplacement = replacement;
  }

  public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) throws Exception {
    if (mPredicate.test(input)) {
      result.pass(mReplacement);

    } else {
      result.pass(input);
    }
  }
}
