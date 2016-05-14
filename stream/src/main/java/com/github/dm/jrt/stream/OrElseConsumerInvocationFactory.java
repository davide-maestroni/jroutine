/*
 * Copyright (c) 2016. Davide Maestroni
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
import com.github.dm.jrt.function.ConsumerWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation factory employing a consumer to generate outputs when no one has been received.
 * <p>
 * Created by davide-maestroni on 04/29/2016.
 *
 * @param <DATA> the data type.
 */
class OrElseConsumerInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

    private final ConsumerWrapper<? super ResultChannel<DATA>> mConsumer;

    private final long mCount;

    /**
     * Constructor.
     *
     * @param count    the loop count.
     * @param consumer the consumer instance.
     */
    OrElseConsumerInvocationFactory(final long count,
            @NotNull final ConsumerWrapper<? super ResultChannel<DATA>> consumer) {

        super(asArgs(ConstantConditions.positive("count number", count),
                ConstantConditions.notNull("consumer instance", consumer)));
        mCount = count;
        mConsumer = consumer;
    }

    @NotNull
    @Override
    public Invocation<DATA, DATA> newInvocation() {

        return new OrElseConsumerInvocation<DATA>(mCount, mConsumer);
    }

    /**
     * Invocation employing a consumer to generate outputs when no one has been received.
     *
     * @param <DATA> the data type.
     */
    private static class OrElseConsumerInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final ConsumerWrapper<? super ResultChannel<DATA>> mConsumer;

        private final long mCount;

        private boolean mHasOutputs;

        /**
         * Constructor.
         *
         * @param count    the loop count.
         * @param consumer the consumer instance.
         */
        OrElseConsumerInvocation(final long count,
                @NotNull final ConsumerWrapper<? super ResultChannel<DATA>> consumer) {

            mCount = count;
            mConsumer = consumer;
        }

        @Override
        public void onInitialize() {

            mHasOutputs = false;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

            mHasOutputs = true;
            result.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<DATA> result) throws Exception {

            if (!mHasOutputs) {
                final long count = mCount;
                final ConsumerWrapper<? super ResultChannel<DATA>> consumer = mConsumer;
                for (long i = 0; i < count; ++i) {
                    consumer.accept(result);
                }
            }
        }
    }
}
