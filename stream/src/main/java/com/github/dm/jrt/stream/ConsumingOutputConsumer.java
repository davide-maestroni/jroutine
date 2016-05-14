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

import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.ConsumerWrapper;

import org.jetbrains.annotations.NotNull;

/**
 * Output consumer passing outputs to a consumer instance.
 * <p>
 * Created by davide-maestroni on 05/14/2016.
 *
 * @param <OUT> the output data type.
 */
class ConsumingOutputConsumer<OUT> implements OutputConsumer<OUT> {

    private final ConsumerWrapper<? super OUT> mConsumer;

    private final IOChannel<?> mOutputChannel;

    /**
     * Constructor.
     *
     * @param consumer      the consumer instance.
     * @param outputChannel the output channel.
     */
    ConsumingOutputConsumer(@NotNull final ConsumerWrapper<? super OUT> consumer,
            @NotNull final IOChannel<?> outputChannel) {

        mConsumer = ConstantConditions.notNull("consumer instance", consumer);
        mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
    }

    public void onComplete() {

        mOutputChannel.close();
    }

    public void onError(@NotNull final RoutineException error) {

        mOutputChannel.abort(error);
    }

    public void onOutput(final OUT output) throws Exception {

        mConsumer.accept(output);
    }
}
