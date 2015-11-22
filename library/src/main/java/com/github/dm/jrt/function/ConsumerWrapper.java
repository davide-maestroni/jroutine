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
import java.util.Collections;
import java.util.List;

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
     * @param consumer the wrapped consumer.
     */
    @SuppressWarnings("ConstantConditions")
    ConsumerWrapper(@NotNull final Consumer<?> consumer) {

        this(Collections.<Consumer<?>>singletonList(consumer));

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }
    }

    /**
     * Constructor.
     *
     * @param consumers the list of wrapped consumers.
     */
    private ConsumerWrapper(@NotNull final List<Consumer<?>> consumers) {

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
     * Returns a composed consumer wrapper that performs, in sequence, this operation followed by
     * the after operation.
     *
     * @param after the operation to perform after this operation.
     * @return the composed consumer.
     */
    @NotNull
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
     * Checks if the consumers wrapped by this instance have a static context.
     *
     * @return whether the consumers have a static context.
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

        return mConsumers.hashCode();
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
        return mConsumers.equals(that.mConsumers);
    }
}
