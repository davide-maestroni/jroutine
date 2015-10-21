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
package com.github.dm.jrt.functional;

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class wrapping a consumer instance.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN> the input data type.
 */
public class ConsumerWrapper<IN> implements Consumer<IN> {

    private final List<Consumer<?>> mConsumers;

    /**
     * Constructor.
     *
     * @param consumers the list of wrapped consumers.
     */
    ConsumerWrapper(@NotNull final List<Consumer<?>> consumers) {

        if (consumers.isEmpty()) {

            throw new IllegalArgumentException("the list of consumers must not be empty");
        }

        mConsumers = consumers;
    }

    @SuppressWarnings("unchecked")
    public void accept(final IN in) {

        for (final Consumer<?> consumer : mConsumers) {

            ((Consumer<Object>) consumer).accept(in);
        }
    }

    /**
     * Returns a composed consumer chain that performs, in sequence, this operation followed by
     * the after operation.
     *
     * @param after the operation to perform after this operation.
     * @return the composed consumer.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public ConsumerWrapper<IN> andThen(@NotNull final Consumer<? super IN> after) {

        final Class<? extends Consumer> consumerClass = after.getClass();
        final List<Consumer<?>> consumers = mConsumers;
        final ArrayList<Consumer<?>> newConsumers =
                new ArrayList<Consumer<?>>(consumers.size() + 1);
        newConsumers.addAll(consumers);

        if (consumerClass == ConsumerWrapper.class) {

            newConsumers.addAll(((ConsumerWrapper<?>) after).mConsumers);

        } else {

            newConsumers.add(after);
        }

        return new ConsumerWrapper<IN>(newConsumers);
    }

    /**
     * Checks if this consumer chain has a static context.
     *
     * @return whether this instance has a static context.
     */
    public boolean hasStaticContext() {

        for (final Consumer<?> consumer : mConsumers) {

            if (!Reflection.hasStaticContext(consumer.getClass())) {

                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = 0;

        for (final Consumer<?> consumer : mConsumers) {

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

        final ConsumerWrapper<?> that = (ConsumerWrapper<?>) o;
        final List<Consumer<?>> thisConsumers = mConsumers;
        final List<Consumer<?>> thatConsumers = that.mConsumers;
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
