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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.PredicateWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations verifying that all the inputs satisfy a specific conditions.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 *
 * @param <IN> the input data type.
 */
class AllMatchInvocationFactory<IN> extends InvocationFactory<IN, Boolean> {

    private final PredicateWrapper<? super IN> mPredicate;

    /**
     * Constructor.
     *
     * @param predicate the predicate defining the condition.
     */
    AllMatchInvocationFactory(@NotNull final PredicateWrapper<? super IN> predicate) {

        super(asArgs(ConstantConditions.notNull("predicate instance", predicate)));
        mPredicate = predicate;
    }

    @NotNull
    @Override
    public Invocation<IN, Boolean> newInvocation() {

        return new AllMatchInvocation<IN>(mPredicate);
    }

    /**
     * Invocation verifying that all the inputs satisfy a specific conditions.
     *
     * @param <IN> the input data type.
     */
    private static class AllMatchInvocation<IN> extends TemplateInvocation<IN, Boolean> {

        private final PredicateWrapper<? super IN> mPredicate;

        private boolean mIsMatch;

        /**
         * Constructor.
         *
         * @param predicate the predicate defining the condition.
         */
        private AllMatchInvocation(@NotNull final PredicateWrapper<? super IN> predicate) {

            mPredicate = predicate;
        }

        @Override
        public void onInitialize() {

            mIsMatch = true;
        }

        @Override
        public void onInput(final IN input, @NotNull final ResultChannel<Boolean> result) throws
                Exception {

            if (mIsMatch && !mPredicate.test(input)) {
                mIsMatch = false;
                result.pass(false);
            }
        }

        @Override
        public void onResult(@NotNull final ResultChannel<Boolean> result) {

            if (mIsMatch) {
                result.pass(true);
            }
        }
    }
}
