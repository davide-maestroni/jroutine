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

package com.github.dm.jrt.stream.processor;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Bi-consumer implementation wrapping a try/catch function.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class TryCatchBiConsumerFunction<OUT> implements BiConsumer<RoutineException, Channel<OUT, ?>> {

    private final Function<? super RoutineException, ? extends OUT> mCatchFunction;

    /**
     * Constructor.
     *
     * @param catchFunction the function instance.
     */
    TryCatchBiConsumerFunction(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        mCatchFunction = ConstantConditions.notNull("function instance", catchFunction);
    }

    public void accept(final RoutineException error, final Channel<OUT, ?> channel) throws
            Exception {
        channel.pass(mCatchFunction.apply(error));
    }
}
