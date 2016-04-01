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
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class wrapping a consumer instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN> the input data type.
 */
public class ConsumerWrapper<IN> implements Consumer<IN>, Wrapper {

    private static final ConsumerWrapper<Object> sSink =
            new ConsumerWrapper<Object>(new Consumer<Object>() {

                public void accept(final Object in) {}
            });

    private final List<Consumer<?>> mConsumers;

    /**
     * Constructor.
     *
     * @param consumer the wrapped consumer.
     */
    ConsumerWrapper(@NotNull final Consumer<?> consumer) {

        this(Collections.<Consumer<?>>singletonList(
                ConstantConditions.notNull("consumer instance", consumer)));
    }

    /**
     * Constructor.
     *
     * @param consumers the list of wrapped consumers.
     */
    private ConsumerWrapper(@NotNull final List<Consumer<?>> consumers) {

        mConsumers = consumers;
    }

    /**
     * Returns a consumer wrapper just discarding the passed inputs.<br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the consumer wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> ConsumerWrapper<IN> sink() {

        return (ConsumerWrapper<IN>) sSink;
    }

    /**
     * Returns a composed consumer wrapper that performs, in sequence, this operation followed by
     * the after operation.
     *
     * @param after the operation to perform after this operation.
     * @return the composed consumer.
     */
    @NotNull
    public ConsumerWrapper<IN> andThen(@NotNull final Consumer<? super IN> after) {

        final List<Consumer<?>> consumers = mConsumers;
        final ArrayList<Consumer<?>> newConsumers =
                new ArrayList<Consumer<?>>(consumers.size() + 1);
        newConsumers.addAll(consumers);
        if (after instanceof ConsumerWrapper) {
            newConsumers.addAll(((ConsumerWrapper<?>) after).mConsumers);

        } else {
            newConsumers.add(ConstantConditions.notNull("consumer instance", after));
        }

        return new ConsumerWrapper<IN>(newConsumers);
    }

    public boolean hasStaticScope() {

        for (final Consumer<?> consumer : mConsumers) {
            if (!Reflection.hasStaticScope(consumer)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        return mConsumers.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        // AUTO-GENERATED CODE
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConsumerWrapper)) {
            return false;
        }

        final ConsumerWrapper<?> that = (ConsumerWrapper<?>) o;
        return mConsumers.equals(that.mConsumers);
    }

    @SuppressWarnings("unchecked")
    public void accept(final IN in) {

        for (final Consumer<?> consumer : mConsumers) {
            ((Consumer<Object>) consumer).accept(in);
        }
    }
}
