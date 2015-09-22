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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

                public void accept(final Object in1, final Object in2) {

                }
            });

    private static final Function<?, ?> sIdentity =
            function(new com.github.dm.jrt.function.Function<Object, Object>() {

                public Object apply(final Object in) {

                    return in;
                }
            });

    private static final Consumer<?> sSink =
            consumer(new com.github.dm.jrt.function.Consumer<Object>() {

                public void accept(final Object in) {

                }
            });

    /**
     * Avoid direct instantiation.
     */
    protected Functions() {

    }

    /**
     * Wraps the specified bi-consumer instance so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @return the wrapped bi-consumer.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN1, IN2> BiConsumer<IN1, IN2> biConsumer(
            @NotNull final com.github.dm.jrt.function.BiConsumer<IN1, IN2> consumer) {

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        return new BiConsumer<IN1, IN2>(
                Collections.<com.github.dm.jrt.function.BiConsumer<?, ?>>singletonList(consumer));
    }

    /**
     * Returns a bi-consumer just discarding the passed inputs.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the wrapped bi-consumer.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiConsumer<IN1, IN2> biSink() {

        return (BiConsumer<IN1, IN2>) sBiSink;
    }

    /**
     * Wraps the specified consumer instance so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param consumer the consumer instance.
     * @param <IN>     the input data type.
     * @return the wrapped consumer.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN> Consumer<IN> consumer(
            @NotNull final com.github.dm.jrt.function.Consumer<IN> consumer) {

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        return new Consumer<IN>(
                Collections.<com.github.dm.jrt.function.Consumer<?>>singletonList(consumer));
    }

    /**
     * Wraps the specified function instance so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the wrapped function.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN, OUT> Function<IN, OUT> function(
            @NotNull final com.github.dm.jrt.function.Function<IN, OUT> function) {

        if (function == null) {

            throw new NullPointerException("the function instance must not be null");
        }

        return new Function<IN, OUT>(
                Collections.<com.github.dm.jrt.function.Function<?, ?>>singletonList(function));
    }

    /**
     * Returns the identity function.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param <IN> the input data type.
     * @return the wrapped function.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> Function<IN, ? super IN> identity() {

        return (Function<IN, ? super IN>) sIdentity;
    }

    /**
     * Returns a consumer just discarding the passed inputs.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param <IN> the input data type.
     * @return the wrapped consumer.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> Consumer<IN> sink() {

        return (Consumer<IN>) sSink;
    }

    @NotNull
    private static Object getMutex(@NotNull final Object function) {

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

    /**
     * Class wrapping a bi-consumer instance.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     */
    public static class BiConsumer<IN1, IN2>
            implements com.github.dm.jrt.function.BiConsumer<IN1, IN2> {

        private final List<com.github.dm.jrt.function.BiConsumer<?, ?>> mConsumers;

        /**
         * Constructor.
         *
         * @param consumers the list of wrapped consumers.
         */
        private BiConsumer(
                @NotNull final List<com.github.dm.jrt.function.BiConsumer<?, ?>> consumers) {

            mConsumers = consumers;
        }

        /**
         * Returns a composed bi-consumer that performs, in sequence, this operation followed by the
         * after operation.
         *
         * @param after the operation to perform after this operation.
         * @return the composed bi-consumer.
         */
        @NotNull
        @SuppressWarnings("ConstantConditions")
        public BiConsumer<IN1, IN2> andThen(
                @NotNull final com.github.dm.jrt.function.BiConsumer<? super IN1, ? super IN2>
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

        /**
         * Performs this operation on the given arguments.
         *
         * @param in1 the first input argument.
         * @param in2 the second input argument.
         */
        @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
        public void accept(final IN1 in1, final IN2 in2) {

            for (final com.github.dm.jrt.function.BiConsumer<?, ?> consumer : mConsumers) {

                final Object mutex = getMutex(consumer);

                synchronized (mutex) {

                    ((com.github.dm.jrt.function.BiConsumer<Object, Object>) consumer).accept(in1,
                                                                                              in2);
                }
            }
        }
    }

    /**
     * Class wrapping a consumer instance.
     *
     * @param <IN> the input data type.
     */
    public static class Consumer<IN> implements com.github.dm.jrt.function.Consumer<IN> {

        private final List<com.github.dm.jrt.function.Consumer<?>> mConsumers;

        /**
         * Constructor.
         *
         * @param consumers the list of wrapped consumers.
         */
        private Consumer(@NotNull final List<com.github.dm.jrt.function.Consumer<?>> consumers) {

            mConsumers = consumers;
        }

        /**
         * Returns a composed consumer that performs, in sequence, this operation followed by the
         * after operation.
         *
         * @param after the operation to perform after this operation.
         * @return the composed consumer.
         */
        @NotNull
        @SuppressWarnings("ConstantConditions")
        public Consumer<IN> andThen(
                @NotNull final com.github.dm.jrt.function.Consumer<? super IN> after) {

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

        /**
         * Performs this operation on the given argument.
         *
         * @param in the input argument.
         */
        @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
        public void accept(final IN in) {

            for (final com.github.dm.jrt.function.Consumer<?> consumer : mConsumers) {

                final Object mutex = getMutex(consumer);

                synchronized (mutex) {

                    ((com.github.dm.jrt.function.Consumer<Object>) consumer).accept(in);
                }
            }
        }
    }

    /**
     * Class wrapping a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    public static class Function<IN, OUT> implements com.github.dm.jrt.function.Function<IN, OUT> {

        private final List<com.github.dm.jrt.function.Function<?, ?>> mFunctions;

        /**
         * Constructor.
         *
         * @param functions the list of wrapped functions.
         */
        private Function(@NotNull final List<com.github.dm.jrt.function.Function<?, ?>> functions) {

            mFunctions = functions;
        }

        /**
         * Returns a composed function that first applies this function to its input, and then
         * applies the after function to the result.
         *
         * @param after   the function to apply after this function is applied.
         * @param <AFTER> the type of output of the after function.
         * @return the composed function.
         */
        @NotNull
        @SuppressWarnings("ConstantConditions")
        public <AFTER> Function<IN, AFTER> andThen(
                @NotNull final com.github.dm.jrt.function.Function<? super OUT, AFTER> after) {

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

        /**
         * Returns a composed function that first applies this function to its input, and then
         * applies the after function to the result.
         *
         * @param before   the function to apply before this function is applied.
         * @param <BEFORE> the type of input to the before function.
         * @return the composed function.
         */
        @NotNull
        @SuppressWarnings("ConstantConditions")
        public <BEFORE> Function<BEFORE, OUT> compose(
                @NotNull final com.github.dm.jrt.function.Function<BEFORE, ? extends IN> before) {

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

        /**
         * Applies this function to the given argument.
         *
         * @param in the input argument.
         * @return the function result.
         */
        @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
        public OUT apply(final IN in) {

            Object result = in;

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
