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
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;

import org.jetbrains.annotations.NotNull;

/**
 * Bi-consumer implementation wrapping a try/catch consumer.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class TryCatchBiConsumerConsumer<OUT> implements BiConsumer<RoutineException, InputChannel<OUT>> {

    private final Consumer<? super RoutineException> mConsumer;

    /**
     * Constructor.
     *
     * @param consumer the consumer instance.
     */
    TryCatchBiConsumerConsumer(@NotNull final Consumer<? super RoutineException> consumer) {

        mConsumer = ConstantConditions.notNull("consumer instance", consumer);
    }

    public void accept(final RoutineException error, final InputChannel<OUT> channel) {

        mConsumer.accept(error);
    }
}
