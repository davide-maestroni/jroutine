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
import com.github.dm.jrt.core.invocation.ConversionInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.ConsumerWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation implementation wrapping a consumer accepting output data.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <IN> the input data type.
 */
class ConsumerInvocation<IN> extends ConversionInvocation<IN, Void> {

    private final ConsumerWrapper<? super IN> mConsumer;

    /**
     * Constructor.
     *
     * @param consumer the consumer instance.
     */
    ConsumerInvocation(@NotNull final ConsumerWrapper<? super IN> consumer) {

        super(asArgs(ConstantConditions.notNull("consumer instance", consumer)));
        mConsumer = consumer;
    }

    public void onInput(final IN input, @NotNull final ResultChannel<Void> result) throws
            Exception {

        mConsumer.accept(input);
    }
}
