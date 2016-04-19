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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.ConsumerWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Generate invocation used to call a consumer a specific number of times.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class LoopConsumerInvocation<OUT> extends GenerateInvocation<OUT> {

    private final ConsumerWrapper<? super ResultChannel<OUT>> mConsumer;

    private final long mCount;

    /**
     * Constructor.
     *
     * @param count    the loop count.
     * @param consumer the consumer instance.
     */
    LoopConsumerInvocation(final long count,
            @NotNull final ConsumerWrapper<? super ResultChannel<OUT>> consumer) {

        super(asArgs(count, consumer));
        ConstantConditions.notNull("consumer wrapper", consumer);
        ConstantConditions.positive("count number", count);
        mCount = count;
        mConsumer = consumer;
    }

    public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {

        final long count = mCount;
        final ConsumerWrapper<? super ResultChannel<OUT>> consumer = mConsumer;
        for (long i = 0; i < count; i++) {
            consumer.accept(result);
        }
    }
}
