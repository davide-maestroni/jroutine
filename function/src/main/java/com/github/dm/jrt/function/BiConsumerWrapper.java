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

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class wrapping a bi-consumer instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 */
public class BiConsumerWrapper<IN1, IN2> extends DeepEqualObject
        implements BiConsumer<IN1, IN2>, Wrapper {

    private static final BiConsumerWrapper<Object, Object> sBiSink =
            new BiConsumerWrapper<Object, Object>(new BiConsumer<Object, Object>() {

                public void accept(final Object in1, final Object in2) {}
            });

    private final List<BiConsumer<?, ?>> mConsumers;

    /**
     * Constructor.
     *
     * @param consumer the wrapped consumer.
     */
    private BiConsumerWrapper(@NotNull final BiConsumer<?, ?> consumer) {
        this(Collections.<BiConsumer<?, ?>>singletonList(
                ConstantConditions.notNull("consumer instance", consumer)));
    }

    /**
     * Constructor.
     *
     * @param consumers the list of wrapped consumers.
     */
    private BiConsumerWrapper(@NotNull final List<BiConsumer<?, ?>> consumers) {
        super(asArgs(consumers));
        mConsumers = consumers;
    }

    /**
     * Returns a bi-consumer wrapper just discarding the passed inputs.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-consumer wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiConsumerWrapper<IN1, IN2> biSink() {
        return (BiConsumerWrapper<IN1, IN2>) sBiSink;
    }

    /**
     * Wraps the specified bi-consumer instance so to provide additional features.
     * <br>
     * The returned object will support concatenation and comparison.
     * <p>
     * Note that the passed object is expected to have a functional behavior, that is, it must not
     * retain a mutable internal state.
     * <br>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @return the wrapped bi-consumer.
     */
    @NotNull
    public static <IN1, IN2> BiConsumerWrapper<IN1, IN2> wrap(
            @NotNull final BiConsumer<IN1, IN2> consumer) {
        if (consumer instanceof BiConsumerWrapper) {
            return (BiConsumerWrapper<IN1, IN2>) consumer;
        }

        return new BiConsumerWrapper<IN1, IN2>(consumer);
    }

    /**
     * Returns a composed bi-consumer wrapper that performs, in sequence, this operation followed
     * by the after operation.
     *
     * @param after the operation to perform after this operation.
     * @return the composed bi-consumer.
     */
    @NotNull
    public BiConsumerWrapper<IN1, IN2> andThen(
            @NotNull final BiConsumer<? super IN1, ? super IN2> after) {
        ConstantConditions.notNull("consumer instance", after);
        final List<BiConsumer<?, ?>> consumers = mConsumers;
        final ArrayList<BiConsumer<?, ?>> newConsumers =
                new ArrayList<BiConsumer<?, ?>>(consumers.size() + 1);
        newConsumers.addAll(consumers);
        if (after instanceof BiConsumerWrapper) {
            newConsumers.addAll(((BiConsumerWrapper<?, ?>) after).mConsumers);

        } else {
            newConsumers.add(after);
        }

        return new BiConsumerWrapper<IN1, IN2>(newConsumers);
    }

    public boolean hasStaticScope() {
        for (final BiConsumer<?, ?> consumer : mConsumers) {
            if (!Reflection.hasStaticScope(consumer)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public void accept(final IN1 in1, final IN2 in2) throws Exception {
        for (final BiConsumer<?, ?> consumer : mConsumers) {
            ((BiConsumer<Object, Object>) consumer).accept(in1, in2);
        }
    }
}
