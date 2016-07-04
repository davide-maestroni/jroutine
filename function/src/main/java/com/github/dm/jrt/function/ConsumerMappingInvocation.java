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
 * Mapping invocation based on a bi-consumer instance.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class ConsumerMappingInvocation<IN, OUT> extends MappingInvocation<IN, OUT> {

    private final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> mConsumer;

    /**
     * Constructor.
     *
     * @param consumer the consumer instance.
     */
    ConsumerMappingInvocation(
            @NotNull final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> consumer) {
        super(asArgs(ConstantConditions.notNull("bi-consumer wrapper", consumer)));
        mConsumer = consumer;
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
        mConsumer.accept(input, result);
    }
}
