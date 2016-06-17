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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiConsumerWrapper;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.function.SupplierWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Invocation implementation accumulating the result returned by a bi-function instance.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class AccumulateConsumerInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private final BiConsumerWrapper<? super OUT, ? super IN> mAccumulateConsumer;

    private final SupplierWrapper<? extends OUT> mSeedSupplier;

    private OUT mAccumulated;

    private boolean mIsFirst;

    /**
     * Constructor.
     *
     * @param seedSupplier       the supplier of initial accumulation values.
     * @param accumulateConsumer the accumulating bi-consumer instance.
     */
    private AccumulateConsumerInvocation(
            @Nullable final SupplierWrapper<? extends OUT> seedSupplier,
            @NotNull final BiConsumerWrapper<? super OUT, ? super IN> accumulateConsumer) {
        mSeedSupplier = seedSupplier;
        mAccumulateConsumer = accumulateConsumer;
    }

    /**
     * Builds and returns a new accumulating invocation factory backed by the specified bi-consumer
     * instance.
     *
     * @param accumulateConsumer the accumulating bi-consumer instance.
     * @param <IN>               the input data type.
     * @return the invocation factory.
     */
    @NotNull
    static <IN> InvocationFactory<IN, IN> consumerFactory(
            @NotNull final BiConsumer<? super IN, ? super IN> accumulateConsumer) {
        return new AccumulateInvocationFactory<IN, IN>(null, wrap(accumulateConsumer));
    }

    /**
     * Builds and returns a new accumulating invocation factory backed by the specified bi-consumer
     * instance.
     *
     * @param seedSupplier       the supplier of initial accumulation values.
     * @param accumulateConsumer the accumulating bi-consumer instance.
     * @param <IN>               the input data type.
     * @param <OUT>              the output data type.
     * @return the invocation factory.
     */
    @NotNull
    static <IN, OUT> InvocationFactory<IN, OUT> consumerFactory(
            @NotNull final Supplier<? extends OUT> seedSupplier,
            @NotNull final BiConsumer<? super OUT, ? super IN> accumulateConsumer) {
        return new AccumulateInvocationFactory<IN, OUT>(wrap(seedSupplier),
                wrap(accumulateConsumer));
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
        mAccumulated = null;
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
        if (!mIsFirst) {
            result.pass(mAccumulated);
            mAccumulated = null;

        } else {
            final SupplierWrapper<? extends OUT> supplier = mSeedSupplier;
            if (supplier != null) {
                result.pass(supplier.get());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
        if (mIsFirst) {
            mIsFirst = false;
            final SupplierWrapper<? extends OUT> supplier = mSeedSupplier;
            if (supplier != null) {
                mAccumulated = supplier.get();
                mAccumulateConsumer.accept(mAccumulated, input);

            } else {
                mAccumulated = (OUT) input;
            }

        } else {
            mAccumulateConsumer.accept(mAccumulated, input);
        }
    }

    @Override
    public void onRecycle() {
        mIsFirst = true;
    }

    /**
     * Class implementing an accumulating invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AccumulateInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final BiConsumerWrapper<? super OUT, ? super IN> mAccumulateConsumer;

        private final SupplierWrapper<? extends OUT> mSeedSupplier;

        /**
         * Constructor.
         *
         * @param seedSupplier       the supplier of initial accumulation values.
         * @param accumulateConsumer the accumulating bi-consumer instance.
         */
        private AccumulateInvocationFactory(
                @Nullable final SupplierWrapper<? extends OUT> seedSupplier,
                @NotNull final BiConsumerWrapper<? super OUT, ? super IN> accumulateConsumer) {
            super(asArgs(seedSupplier, accumulateConsumer));
            mSeedSupplier = seedSupplier;
            mAccumulateConsumer = accumulateConsumer;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {
            return new AccumulateConsumerInvocation<IN, OUT>(mSeedSupplier, mAccumulateConsumer);
        }
    }
}
