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

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Command invocation based on a consumer instance.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <OUT> the output data type.
 */
class ConsumerCommandInvocation<OUT> extends CommandInvocation<OUT> {

    private final ConsumerWrapper<? super ResultChannel<OUT>> mConsumer;

    /**
     * Constructor.
     *
     * @param consumer the consumer instance.
     */
    ConsumerCommandInvocation(@NotNull final ConsumerWrapper<? super ResultChannel<OUT>> consumer) {
        super(asArgs(ConstantConditions.notNull("consumer wrapper", consumer)));
        mConsumer = consumer;
    }

    public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {
        mConsumer.accept(result);
    }
}
