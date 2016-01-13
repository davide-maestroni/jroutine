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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class wrapping a bi-consumer instance.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 */
public class BiConsumerWrapper<IN1, IN2> implements BiConsumer<IN1, IN2> {

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
    @SuppressWarnings("ConstantConditions")
    BiConsumerWrapper(@NotNull final BiConsumer<?, ?> consumer) {

        this(Collections.<BiConsumer<?, ?>>singletonList(consumer));

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }
    }

    /**
     * Constructor.
     *
     * @param consumers the list of wrapped consumers.
     */
    private BiConsumerWrapper(@NotNull final List<BiConsumer<?, ?>> consumers) {

        mConsumers = consumers;
    }

    /**
     * Returns a bi-consumer wrapper just discarding the passed inputs.<br/>
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
     * Returns a composed bi-consumer wrapper that performs, in sequence, this operation followed
     * by the after operation.
     *
     * @param after the operation to perform after this operation.
     * @return the composed bi-consumer.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public BiConsumerWrapper<IN1, IN2> andThen(
            @NotNull final BiConsumer<? super IN1, ? super IN2> after) {

        final List<BiConsumer<?, ?>> consumers = mConsumers;
        final ArrayList<BiConsumer<?, ?>> newConsumers =
                new ArrayList<BiConsumer<?, ?>>(consumers.size() + 1);
        newConsumers.addAll(consumers);

        if (after instanceof BiConsumerWrapper) {

            newConsumers.addAll(((BiConsumerWrapper<?, ?>) after).mConsumers);

        } else if (after == null) {

            throw new NullPointerException("the consumer must not be null");

        } else {

            newConsumers.add(after);
        }

        return new BiConsumerWrapper<IN1, IN2>(newConsumers);
    }

    @Override
    public int hashCode() {

        return mConsumers.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof BiConsumerWrapper)) {

            return false;
        }

        final BiConsumerWrapper<?, ?> that = (BiConsumerWrapper<?, ?>) o;
        return mConsumers.equals(that.mConsumers);
    }

    /**
     * Extra implementation of {@code equals()} checking for wrapped consumer classes rather than
     * instances equality.<br/>
     * In most cases the wrapped consumers are instances of anonymous classes, as a consequence the
     * standard equality test will always fail.
     *
     * @param o the reference object with which to compare.
     * @return whether the wrapped consumers share the same classes in the same order.
     */
    public boolean typeEquals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof BiConsumerWrapper)) {

            return false;
        }

        final BiConsumerWrapper<?, ?> that = (BiConsumerWrapper<?, ?>) o;
        final List<BiConsumer<?, ?>> thisConsumers = mConsumers;
        final List<BiConsumer<?, ?>> thatConsumers = that.mConsumers;
        final int size = thisConsumers.size();

        if (size != thatConsumers.size()) {

            return false;
        }

        for (int i = 0; i < size; ++i) {

            if (!thisConsumers.get(i).getClass().equals(thatConsumers.get(i).getClass())) {

                return false;
            }
        }

        return true;
    }

    /**
     * Extra implementation of {@code hashCode()} employing wrapped consumer class rather than
     * instance hash codes.
     *
     * @return the cumulative hash code of the wrapped consumers.
     * @see #typeEquals(Object)
     */
    public int typeHashCode() {

        int result = 0;

        for (final BiConsumer<?, ?> consumer : mConsumers) {

            result += result * 31 + consumer.getClass().hashCode();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public void accept(final IN1 in1, final IN2 in2) {

        for (final BiConsumer<?, ?> consumer : mConsumers) {

            ((BiConsumer<Object, Object>) consumer).accept(in1, in2);
        }
    }
}
