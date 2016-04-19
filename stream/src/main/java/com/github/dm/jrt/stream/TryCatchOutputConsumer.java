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

import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

/**
 * Try/catch output consumer implementation.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class TryCatchOutputConsumer<OUT> implements OutputConsumer<OUT> {

    private final BiConsumer<? super RoutineException, ? super InputChannel<OUT>> mConsumer;

    private final IOChannel<OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param consumer      the consumer instance.
     * @param outputChannel the output channel.
     */
    TryCatchOutputConsumer(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer,
            @NotNull final IOChannel<OUT> outputChannel) {

        ConstantConditions.notNull("bi-consumer instance", consumer);
        ConstantConditions.notNull("output channel", outputChannel);
        mConsumer = consumer;
        mOutputChannel = outputChannel;
    }

    public void onComplete() {

        mOutputChannel.close();
    }

    public void onError(@NotNull final RoutineException error) {

        final IOChannel<OUT> channel = mOutputChannel;
        try {
            mConsumer.accept(error, channel);
            channel.close();

        } catch (final Throwable t) {
            InvocationInterruptedException.throwIfInterrupt(t);
            channel.abort(t);
        }
    }

    public void onOutput(final OUT output) {

        mOutputChannel.pass(output);
    }
}