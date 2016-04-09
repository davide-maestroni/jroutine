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

import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class used to build output consumer based on consumer functions.
 * <p>
 * Created by davide-maestroni on 09/21/2015.
 *
 * @param <OUT> the output data type.
 */
public class OutputConsumerBuilder<OUT> implements OutputConsumer<OUT> {

    private final ConsumerWrapper<Void> mOnComplete;

    private final ConsumerWrapper<RoutineException> mOnError;

    private final ConsumerWrapper<OUT> mOnOutput;

    /**
     * Constructor.
     *
     * @param onComplete the complete consumer.
     * @param onError    the error consumer.
     * @param onOutput   the output consumer.
     */
    OutputConsumerBuilder(@NotNull final ConsumerWrapper<Void> onComplete,
            @NotNull final ConsumerWrapper<RoutineException> onError,
            @NotNull final ConsumerWrapper<OUT> onOutput) {

        mOnOutput = ConstantConditions.notNull("output consumer", onOutput);
        mOnError = ConstantConditions.notNull("error consumer", onError);
        mOnComplete = ConstantConditions.notNull("complete consumer", onComplete);
    }

    public void onComplete() {

        mOnComplete.accept(null);
    }

    public void onError(@NotNull final RoutineException error) {

        mOnError.accept(error);
    }

    public void onOutput(final OUT output) {

        mOnOutput.accept(output);
    }

    /**
     * Returns a new output consumer builder employing also the specified consumer function to
     * handle the invocation completion.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public OutputConsumerBuilder<OUT> thenComplete(@NotNull final Consumer<Void> consumer) {

        return new OutputConsumerBuilder<OUT>(mOnComplete.andThen(consumer), mOnError, mOnOutput);
    }

    /**
     * Returns a new output consumer builder employing also the specified consumer function to
     * handle the invocation errors.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public OutputConsumerBuilder<OUT> thenError(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return new OutputConsumerBuilder<OUT>(mOnComplete, mOnError.andThen(consumer), mOnOutput);
    }

    /**
     * Returns a new output consumer builder employing also the specified consumer function to
     * handle the invocation outputs.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public OutputConsumerBuilder<OUT> thenOutput(@NotNull final Consumer<? super OUT> consumer) {

        return new OutputConsumerBuilder<OUT>(mOnComplete, mOnError, mOnOutput.andThen(consumer));
    }
}
