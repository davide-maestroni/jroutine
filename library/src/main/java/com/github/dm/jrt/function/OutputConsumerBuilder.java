/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.function;

import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.function.Functions.wrapConsumer;

/**
 * Utility class used to build output consumer based on consumer functions.
 * <p/>
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
     * @param onOutput   the output consumer.
     * @param onError    the error consumer.
     * @param onComplete the complete consumer.
     */
    private OutputConsumerBuilder(@NotNull final ConsumerWrapper<OUT> onOutput,
            @NotNull final ConsumerWrapper<RoutineException> onError,
            @NotNull final ConsumerWrapper<Void> onComplete) {

        mOnOutput = onOutput;
        mOnError = onError;
        mOnComplete = onComplete;
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation completion.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public static OutputConsumerBuilder<Object> onComplete(@NotNull final Consumer<Void> consumer) {

        return new OutputConsumerBuilder<Object>(Functions.sink(),
                                                 Functions.<RoutineException>sink(),
                                                 wrapConsumer(consumer));
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation errors.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public static OutputConsumerBuilder<Object> onError(
            @NotNull final Consumer<RoutineException> consumer) {

        return new OutputConsumerBuilder<Object>(Functions.sink(), wrapConsumer(consumer),
                                                 Functions.<Void>sink());
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation outputs.
     *
     * @param consumer the consumer function.
     * @param <OUT>    the output data type.
     * @return the builder instance.
     */
    @NotNull
    public static <OUT> OutputConsumerBuilder<OUT> onOutput(@NotNull final Consumer<OUT> consumer) {

        return new OutputConsumerBuilder<OUT>(wrapConsumer(consumer),
                                              Functions.<RoutineException>sink(),
                                              Functions.<Void>sink());
    }

    /**
     * Returns a new output consumer builder employing also the specified consumer function to
     * handle the invocation completion.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public OutputConsumerBuilder<OUT> andThenComplete(@NotNull final Consumer<Void> consumer) {

        return new OutputConsumerBuilder<OUT>(mOnOutput, mOnError, mOnComplete.andThen(consumer));
    }

    /**
     * Returns a new output consumer builder employing also the specified consumer function to
     * handle the invocation errors.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public OutputConsumerBuilder<OUT> andThenError(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return new OutputConsumerBuilder<OUT>(mOnOutput, mOnError.andThen(consumer), mOnComplete);
    }

    /**
     * Returns a new output consumer builder employing also the specified consumer function to
     * handle the invocation outputs.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public OutputConsumerBuilder<OUT> andThenOutput(@NotNull final Consumer<? super OUT> consumer) {

        return new OutputConsumerBuilder<OUT>(mOnOutput.andThen(consumer), mOnError, mOnComplete);
    }

    public void onComplete() {

        mOnComplete.accept(null);
    }

    public void onError(@Nullable final RoutineException error) {

        mOnError.accept(error);
    }

    public void onOutput(final OUT output) {

        mOnOutput.accept(output);
    }
}
