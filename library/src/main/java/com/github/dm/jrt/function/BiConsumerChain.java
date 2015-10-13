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

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class wrapping a bi-consumer instance.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 */
public class BiConsumerChain<IN1, IN2> implements BiConsumer<IN1, IN2> {

    private final List<BiConsumer<?, ?>> mConsumers;

    /**
     * Constructor.
     *
     * @param consumers the list of wrapped consumers.
     */
    BiConsumerChain(@NotNull final List<BiConsumer<?, ?>> consumers) {

        if (consumers.isEmpty()) {

            throw new IllegalArgumentException("the list of consumer must not be empty");
        }

        mConsumers = consumers;
    }

    /**
     * Performs this operation on the given arguments.
     *
     * @param in1 the first input argument.
     * @param in2 the second input argument.
     */
    @SuppressWarnings("unchecked")
    public void accept(final IN1 in1, final IN2 in2) {

        for (final BiConsumer<?, ?> consumer : mConsumers) {

            ((BiConsumer<Object, Object>) consumer).accept(in1, in2);
        }
    }

    /**
     * Returns a composed bi-consumer chain that performs, in sequence, this operation followed
     * by the after operation.
     *
     * @param after the operation to perform after this operation.
     * @return the composed bi-consumer.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public BiConsumerChain<IN1, IN2> andThen(
            @NotNull final BiConsumer<? super IN1, ? super IN2> after) {

        final Class<? extends BiConsumer> consumerClass = after.getClass();
        final List<BiConsumer<?, ?>> consumers = mConsumers;
        final ArrayList<BiConsumer<?, ?>> newConsumers =
                new ArrayList<BiConsumer<?, ?>>(consumers.size() + 1);
        newConsumers.addAll(consumers);

        if (consumerClass == BiConsumerChain.class) {

            newConsumers.addAll(((BiConsumerChain<?, ?>) after).mConsumers);

        } else {

            newConsumers.add(after);
        }

        return new BiConsumerChain<IN1, IN2>(newConsumers);
    }

    /**
     * Checks if this bi-consumer chain has a static context.
     *
     * @return whether this instance has a static context.
     */
    public boolean hasStaticContext() {

        for (final BiConsumer<?, ?> consumer : mConsumers) {

            if (!Reflection.hasStaticContext(consumer.getClass())) {

                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = 0;

        for (final BiConsumer<?, ?> consumer : mConsumers) {

            result = 31 * result + consumer.getClass().hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {

            return false;
        }

        final BiConsumerChain<?, ?> that = (BiConsumerChain<?, ?>) o;
        final List<BiConsumer<?, ?>> thisConsumers = mConsumers;
        final List<BiConsumer<?, ?>> thatConsumers = that.mConsumers;
        final int size = thisConsumers.size();

        if (size != thatConsumers.size()) {

            return false;
        }

        for (int i = 0; i < size; ++i) {

            if (thisConsumers.get(i).getClass() != thatConsumers.get(i).getClass()) {

                return false;
            }
        }

        return true;
    }
}
