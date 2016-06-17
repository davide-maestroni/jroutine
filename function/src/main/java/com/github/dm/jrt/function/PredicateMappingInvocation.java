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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Mapping invocation based on a predicate instance.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <IN> the input data type.
 */
class PredicateMappingInvocation<IN> extends MappingInvocation<IN, IN> {

    private final PredicateWrapper<? super IN> mPredicate;

    /**
     * Constructor.
     *
     * @param predicate the predicate instance.
     */
    PredicateMappingInvocation(@NotNull final PredicateWrapper<? super IN> predicate) {
        super(asArgs(ConstantConditions.notNull("predicate wrapper", predicate)));
        mPredicate = predicate;
    }

    public void onInput(final IN input, @NotNull final Channel<IN, ?> result) throws Exception {
        if (mPredicate.test(input)) {
            result.pass(input);
        }
    }
}
