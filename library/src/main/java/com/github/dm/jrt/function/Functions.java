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

import com.github.dm.jrt.util.WeakIdentityHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Utility class supporting functional programming.
 * <p/>
 * Created by davide-maestroni on 09/21/2015.
 */
public class Functions {

    private static final WeakIdentityHashMap<Object, Object> mMutexes =
            new WeakIdentityHashMap<Object, Object>();

    private static final BiConsumer<?, ?> sBiSink =
            biConsumer(new com.github.dm.jrt.function.BiConsumer<Object, Object>() {

                public void accept(final Object input1, final Object input2) {

                }
            });

    private static final Function<?, ?> sIdentity =
            function(new com.github.dm.jrt.function.Function<Object, Object>() {

                public Object apply(final Object input) {

                    return input;
                }
            });

    private static final Consumer<?> sSink =
            consumer(new com.github.dm.jrt.function.Consumer<Object>() {

                public void accept(final Object input) {

                }
            });

    /**
     * Avoid direct instantiation.
     */
    protected Functions() {

    }

    /**
     * Wraps the specified consumer function so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param consumer the consumer instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @return the wrapped consumer.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static <IN1, IN2> BiConsumer<IN1, IN2> biConsumer(
            @Nonnull final com.github.dm.jrt.function.BiConsumer<IN1, IN2> consumer) {

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        return new BiConsumer<IN1, IN2>(
                Collections.<com.github.dm.jrt.function.BiConsumer<?, ?>>singletonList(consumer));
    }

    /**
     * Returns a consumer just discarding the passed inputs.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the consumer.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiConsumer<IN1, IN2> biSink() {

        return (BiConsumer<IN1, IN2>) sBiSink;
    }

    /**
     * Wraps the specified consumer function so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param consumer the consumer instance.
     * @param <IN>     the input data type.
     * @return the wrapped consumer.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static <IN> Consumer<IN> consumer(
            @Nonnull final com.github.dm.jrt.function.Consumer<IN> consumer) {

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        return new Consumer<IN>(
                Collections.<com.github.dm.jrt.function.Consumer<?>>singletonList(consumer));
    }

    /**
     * Wraps the specified function so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the wrapped function.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static <IN, OUT> Function<IN, OUT> function(
            @Nonnull final com.github.dm.jrt.function.Function<IN, OUT> function) {

        if (function == null) {

            throw new NullPointerException("the function instance must not be null");
        }

        return new Function<IN, OUT>(
                Collections.<com.github.dm.jrt.function.Function<?, ?>>singletonList(function));
    }

    /**
     * Returns a function returning the very same input as output.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param <IN> the input data type.
     * @return the wrapped function.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <IN> Function<IN, ? super IN> identity() {

        return (Function<IN, ? super IN>) sIdentity;
    }

    /**
     * Returns a consumer just discarding the passed inputs.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param <IN> the input data type.
     * @return the consumer.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <IN> Consumer<IN> sink() {

        return (Consumer<IN>) sSink;
    }

    @Nonnull
    private static Object getMutex(@Nonnull final Object function) {

        synchronized (mMutexes) {

            final WeakIdentityHashMap<Object, Object> mutexes = mMutexes;
            Object mutex = mutexes.get(function);

            if (mutex == null) {

                mutex = new Object();
                mutexes.put(function, mutex);
            }

            return mutex;
        }
    }

    public static class BiConsumer<IN1, IN2>
            implements com.github.dm.jrt.function.BiConsumer<IN1, IN2> {

        private final List<com.github.dm.jrt.function.BiConsumer<?, ?>> mConsumers;

        private BiConsumer(
                @Nonnull final List<com.github.dm.jrt.function.BiConsumer<?, ?>> consumers) {

            mConsumers = consumers;
        }

        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public BiConsumer<IN1, IN2> andThen(
                @Nonnull final com.github.dm.jrt.function.BiConsumer<? super IN1, ? super IN2>
                        after) {

            if (after == null) {

                throw new NullPointerException("the after consumer must not be null");
            }

            final List<com.github.dm.jrt.function.BiConsumer<?, ?>> consumers = mConsumers;
            final ArrayList<com.github.dm.jrt.function.BiConsumer<?, ?>> newConsumers =
                    new ArrayList<com.github.dm.jrt.function.BiConsumer<?, ?>>(
                            consumers.size() + 1);
            newConsumers.addAll(consumers);
            newConsumers.add(after);
            return new BiConsumer<IN1, IN2>(newConsumers);
        }

        @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
        public void accept(final IN1 input1, final IN2 input2) {

            for (final com.github.dm.jrt.function.BiConsumer<?, ?> consumer : mConsumers) {

                final Object mutex = getMutex(consumer);

                synchronized (mutex) {

                    ((com.github.dm.jrt.function.BiConsumer<Object, Object>) consumer).accept(
                            input1, input2);
                }
            }
        }
    }

    public static class Consumer<IN> implements com.github.dm.jrt.function.Consumer<IN> {

        private final List<com.github.dm.jrt.function.Consumer<?>> mConsumers;

        private Consumer(@Nonnull final List<com.github.dm.jrt.function.Consumer<?>> consumers) {

            mConsumers = consumers;
        }

        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public Consumer<IN> andThen(
                @Nonnull final com.github.dm.jrt.function.Consumer<? super IN> after) {

            if (after == null) {

                throw new NullPointerException("the after consumer must not be null");
            }

            final List<com.github.dm.jrt.function.Consumer<?>> consumers = mConsumers;
            final ArrayList<com.github.dm.jrt.function.Consumer<?>> newConsumers =
                    new ArrayList<com.github.dm.jrt.function.Consumer<?>>(consumers.size() + 1);
            newConsumers.addAll(consumers);
            newConsumers.add(after);
            return new Consumer<IN>(newConsumers);
        }

        @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
        public void accept(final IN input) {

            for (final com.github.dm.jrt.function.Consumer<?> consumer : mConsumers) {

                final Object mutex = getMutex(consumer);

                synchronized (mutex) {

                    ((com.github.dm.jrt.function.Consumer<Object>) consumer).accept(input);
                }
            }
        }
    }

    public static class Function<IN, OUT> implements com.github.dm.jrt.function.Function<IN, OUT> {

        private final List<com.github.dm.jrt.function.Function<?, ?>> mFunctions;

        private Function(@Nonnull final List<com.github.dm.jrt.function.Function<?, ?>> functions) {

            mFunctions = functions;
        }

        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public <AFTER> Function<IN, AFTER> andThen(
                @Nonnull final com.github.dm.jrt.function.Function<? super OUT, AFTER> after) {

            if (after == null) {

                throw new NullPointerException("the after function must not be null");
            }

            final List<com.github.dm.jrt.function.Function<?, ?>> functions = mFunctions;
            final ArrayList<com.github.dm.jrt.function.Function<?, ?>> newFunctions =
                    new ArrayList<com.github.dm.jrt.function.Function<?, ?>>(functions.size() + 1);
            newFunctions.addAll(functions);
            newFunctions.add(after);
            return new Function<IN, AFTER>(newFunctions);
        }

        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public <BEFORE> Function<BEFORE, OUT> compose(
                @Nonnull final com.github.dm.jrt.function.Function<BEFORE, ? extends IN> before) {

            if (before == null) {

                throw new NullPointerException("the before function must not be null");
            }

            final List<com.github.dm.jrt.function.Function<?, ?>> functions = mFunctions;
            final ArrayList<com.github.dm.jrt.function.Function<?, ?>> newFunctions =
                    new ArrayList<com.github.dm.jrt.function.Function<?, ?>>(functions.size() + 1);
            newFunctions.add(before);
            newFunctions.addAll(functions);
            return new Function<BEFORE, OUT>(newFunctions);
        }

        @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
        public OUT apply(final IN input) {

            Object result = input;

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                final Object mutex = getMutex(function);

                synchronized (mutex) {

                    result = ((com.github.dm.jrt.function.Function<Object, Object>) function).apply(
                            result);
                }
            }

            return (OUT) result;
        }
    }
}
