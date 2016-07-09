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
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionDecorator;
import com.github.dm.jrt.function.Functions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation implementation computing the result by applying a bi-function instance to the inputs.
 * <p>
 * Created by davide-maestroni on 06/15/2016.
 *
 * @param <DATA> the data type.
 */
class BinaryOperatorInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

    private final BiFunction<DATA, DATA, DATA> mBinaryFunction;

    private boolean mIsFirst;

    private DATA mResult;

    /**
     * Constructor.
     *
     * @param binaryFunction the operator bi-function instance.
     */
    private BinaryOperatorInvocation(@NotNull final BiFunction<DATA, DATA, DATA> binaryFunction) {
        mBinaryFunction = binaryFunction;
    }

    /**
     * Builds and returns a new binary operator invocation factory backed by the specified
     * bi-function instance.
     *
     * @param binaryFunction the operator bi-function instance.
     * @param <DATA>         the data type.
     * @return the invocation factory.
     */
    @NotNull
    static <DATA> InvocationFactory<DATA, DATA> functionFactory(
            @NotNull final BiFunction<DATA, DATA, DATA> binaryFunction) {
        return new BinaryOperatorInvocationFactory<DATA>(Functions.decorate(binaryFunction));
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) {
        if (!mIsFirst) {
            result.pass(mResult);
        }
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) throws Exception {
        if (mIsFirst) {
            mIsFirst = false;
            mResult = input;

        } else {
            mResult = mBinaryFunction.apply(mResult, input);
        }
    }

    @Override
    public void onRecycle(final boolean isReused) {
        mResult = null;
    }

    @Override
    public void onRestart() {
        mIsFirst = true;
    }

    /**
     * Class implementing a binary operator invocation factory.
     *
     * @param <DATA> the data type.
     */
    private static class BinaryOperatorInvocationFactory<DATA>
            extends InvocationFactory<DATA, DATA> {

        private final BiFunctionDecorator<DATA, DATA, DATA> mBinaryFunction;

        /**
         * Constructor.
         *
         * @param binaryFunction the operator bi-function instance.
         */
        private BinaryOperatorInvocationFactory(
                @NotNull final BiFunctionDecorator<DATA, DATA, DATA> binaryFunction) {
            super(asArgs(binaryFunction));
            mBinaryFunction = binaryFunction;
        }

        @NotNull
        @Override
        public Invocation<DATA, DATA> newInvocation() {
            return new BinaryOperatorInvocation<DATA>(mBinaryFunction);
        }
    }
}
