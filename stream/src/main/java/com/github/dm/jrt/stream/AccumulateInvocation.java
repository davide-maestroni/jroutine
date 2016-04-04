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
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionWrapper;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.function.SupplierWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Invocation implementation accumulating the result returned by a bi-function instance.
 * <p>
 * Created by davide-maestroni on 10/18/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class AccumulateInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private final BiFunction<? super OUT, ? super IN, ? extends OUT> mFunction;

    private final SupplierWrapper<? extends OUT> mSupplier;

    private OUT mAccumulated;

    private boolean mIsFirst;

    /**
     * Constructor.
     *
     * @param supplier the supplier of initial accumulation values.
     * @param function the accumulating bi-function instance.
     */
    private AccumulateInvocation(@Nullable final SupplierWrapper<? extends OUT> supplier,
            @NotNull final BiFunction<? super OUT, ? super IN, ? extends OUT> function) {

        mSupplier = supplier;
        mFunction = function;
    }

    /**
     * Builds and returns a new accumulating invocation factory backed by the specified bi-function
     * instance.
     *
     * @param function the accumulating bi-function instance.
     * @param <IN>     the input data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN> InvocationFactory<IN, IN> functionFactory(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return new AccumulateInvocationFactory<IN, IN>(null, wrap(function));
    }

    /**
     * Builds and returns a new accumulating invocation factory backed by the specified bi-function
     * instance.
     *
     * @param supplier the supplier of initial accumulation values.
     * @param function the accumulating bi-function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN, OUT> InvocationFactory<IN, OUT> functionFactory(
            @NotNull final Supplier<? extends OUT> supplier,
            @NotNull final BiFunction<? super OUT, ? super IN, ? extends OUT> function) {

        return new AccumulateInvocationFactory<IN, OUT>(wrap(supplier), wrap(function));
    }

    @Override
    public void onInitialize() {

        mIsFirst = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

        if (mIsFirst) {
            mIsFirst = false;
            final SupplierWrapper<? extends OUT> supplier = mSupplier;
            if (supplier != null) {
                mAccumulated = mFunction.apply(supplier.get(), input);

            } else {
                mAccumulated = (OUT) input;
            }

        } else {
            mAccumulated = mFunction.apply(mAccumulated, input);
        }
    }

    @Override
    public void onResult(@NotNull final ResultChannel<OUT> result) {

        result.pass(mAccumulated);
    }

    @Override
    public void onTerminate() {

        mAccumulated = null;
    }

    /**
     * Class implementing an accumulating invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AccumulateInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final BiFunctionWrapper<? super OUT, ? super IN, ? extends OUT> mFunction;

        private final SupplierWrapper<? extends OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier of initial accumulation values.
         * @param function the accumulating bi-function instance.
         */
        private AccumulateInvocationFactory(@Nullable final SupplierWrapper<? extends OUT> supplier,
                @NotNull final BiFunctionWrapper<? super OUT, ? super IN, ? extends OUT> function) {

            super(asArgs(supplier, function));
            mSupplier = supplier;
            mFunction = function;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new AccumulateInvocation<IN, OUT>(mSupplier, mFunction);
        }
    }
}
