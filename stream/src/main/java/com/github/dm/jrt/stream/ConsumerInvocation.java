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
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.ConsumerWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation implementation wrapping a consumer accepting output data.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class ConsumerInvocation<OUT> extends FilterInvocation<OUT, Void> {

    private final ConsumerWrapper<? super OUT> mConsumer;

    /**
     * Constructor.
     *
     * @param consumer the consumer instance.
     */
    ConsumerInvocation(@NotNull final ConsumerWrapper<? super OUT> consumer) {

        super(asArgs(consumer));
        ConstantConditions.notNull("consumer wrapper", consumer);
        mConsumer = consumer;
    }

    public void onInput(final OUT input, @NotNull final ResultChannel<Void> result) {

        mConsumer.accept(input);
    }
}