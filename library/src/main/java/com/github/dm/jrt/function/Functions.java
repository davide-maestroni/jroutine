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
import com.github.dm.jrt.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class supporting functional programming.
 * <p/>
 * Created by davide-maestroni on 09/21/2015.
 */
public class Functions {

    private static final WeakIdentityHashMap<Object, Object> mMutexes =
            new WeakIdentityHashMap<Object, Object>();

    private static final BiConsumer<?, ?> sBiSink =
            newBiConsumer(new com.github.dm.jrt.function.BiConsumer<Object, Object>() {

                public void accept(final Object in1, final Object in2) {

                }
            });

    private static final Function<?, ?> sIdentity =
            newFunction(new com.github.dm.jrt.function.Function<Object, Object>() {

                public Object apply(final Object in) {

                    return in;
                }
            });

    private static final Consumer<?> sSink =
            newConsumer(new com.github.dm.jrt.function.Consumer<Object>() {

                public void accept(final Object in) {

                }
            });

    /**
     * Avoid direct instantiation.
     */
    protected Functions() {

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
     * Returns a supplier always returning the same result.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param result the result.
     * @param <OUT>  the output data type.
     * @return the wrapped supplier.
     */
    @NotNull
    public static <OUT> Supplier<OUT> constant(final OUT result) {

        return newSupplier(new com.github.dm.jrt.function.Supplier<OUT>() {

            public OUT get() {

                return result;
            }
        });
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
     * Wraps the specified bi-consumer instance so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @return the wrapped bi-consumer.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <IN1, IN2> BiConsumer<IN1, IN2> newBiConsumer(
            @NotNull final com.github.dm.jrt.function.BiConsumer<IN1, IN2> consumer) {

        if (consumer.getClass() == BiConsumer.class) {

            return (BiConsumer<IN1, IN2>) consumer;
        }

        return new BiConsumer<IN1, IN2>(
                Collections.<com.github.dm.jrt.function.BiConsumer<?, ?>>singletonList(consumer));
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
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <IN> Consumer<IN> newConsumer(
            @NotNull final com.github.dm.jrt.function.Consumer<IN> consumer) {

        if (consumer.getClass() == Consumer.class) {

            return (Consumer<IN>) consumer;
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
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <IN, OUT> Function<IN, OUT> newFunction(
            @NotNull final com.github.dm.jrt.function.Function<IN, OUT> function) {

        if (function.getClass() == Function.class) {

            return (Function<IN, OUT>) function;
        }

        return new Function<IN, OUT>(
                Collections.<com.github.dm.jrt.function.Function<?, ?>>singletonList(function));
    }

    /**
     * Wraps the specified supplier instance so to provide additional features.<br/>
     * The returned object will support concatenation and synchronization.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the wrapped supplier.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <OUT> Supplier<OUT> newSupplier(
            @NotNull final com.github.dm.jrt.function.Supplier<OUT> supplier) {

        if (supplier.getClass() == Supplier.class) {

            return (Supplier<OUT>) supplier;
        }

        return new Supplier<OUT>(supplier,
                                 Collections.<com.github.dm.jrt.function.Function<?, ?>>emptyList
                                         ());
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
         * Checks if this bi-consumer instance has a static context.
         *
         * @return whether this instance has a static context.
         */
        public boolean hasStaticContext() {

            for (final com.github.dm.jrt.function.BiConsumer<?, ?> consumer : mConsumers) {

                final boolean isStaticContext;
                final Class<? extends com.github.dm.jrt.function.BiConsumer> consumerClass =
                        consumer.getClass();

                if (consumerClass == BiConsumer.class) {

                    isStaticContext = ((BiConsumer) consumer).hasStaticContext();

                } else {

                    isStaticContext = Reflection.hasStaticContext(consumerClass);
                }

                if (!isStaticContext) {

                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {

            int result = 0;

            for (final com.github.dm.jrt.function.BiConsumer<?, ?> consumer : mConsumers) {

                final Class<? extends com.github.dm.jrt.function.BiConsumer> consumerClass =
                        consumer.getClass();

                if (consumerClass == BiConsumer.class) {

                    result = 31 * result + consumer.hashCode();

                } else {

                    result = 31 * result + consumerClass.hashCode();
                }
            }

            return result;
        }

        @Override
        @SuppressFBWarnings(value = "EQ_GETCLASS_AND_CLASS_CONSTANT",
                justification = "comparing class of the internal list objects")
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final BiConsumer<?, ?> that = (BiConsumer<?, ?>) o;
            final List<com.github.dm.jrt.function.BiConsumer<?, ?>> thisConsumers = mConsumers;
            final List<com.github.dm.jrt.function.BiConsumer<?, ?>> thatConsumers = that.mConsumers;
            final int size = thisConsumers.size();

            if (size != thatConsumers.size()) {

                return false;
            }

            for (int i = 0; i < size; i++) {

                final com.github.dm.jrt.function.BiConsumer<?, ?> thisConsumer =
                        thisConsumers.get(i);
                final com.github.dm.jrt.function.BiConsumer<?, ?> thatConsumer =
                        thatConsumers.get(i);
                final Class<? extends com.github.dm.jrt.function.BiConsumer> thisClass =
                        thisConsumer.getClass();
                final Class<? extends com.github.dm.jrt.function.BiConsumer> thatClass =
                        thatConsumer.getClass();

                if (thisClass == BiConsumer.class) {

                    if (thatClass == BiConsumer.class) {

                        if (!thisConsumer.equals(thatConsumer)) {

                            return false;
                        }

                    } else {

                        final BiConsumer<?, ?> thisInstance = (BiConsumer<?, ?>) thisConsumer;

                        if ((thisInstance.mConsumers.size() != 1) || (
                                thisInstance.mConsumers.get(0).getClass() != thatClass)) {

                            return false;
                        }
                    }

                } else if (thatClass == BiConsumer.class) {

                    final BiConsumer<?, ?> thatInstance = (BiConsumer<?, ?>) thatConsumer;

                    if ((thatInstance.mConsumers.size() != 1) || (
                            thatInstance.mConsumers.get(0).getClass() != thisClass)) {

                        return false;
                    }

                } else if (thisClass != thatClass) {

                    return false;
                }
            }

            return true;
        }

        /**
         * Performs this operation on the given arguments.
         *
         * @param in1 the first input argument.
         * @param in2 the second input argument.
         */
        @SuppressWarnings("unchecked")
        public void accept(final IN1 in1, final IN2 in2) {

            for (final com.github.dm.jrt.function.BiConsumer<?, ?> consumer : mConsumers) {

                synchronized (getMutex(consumer)) {

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
         * Checks if this consumer instance has a static context.
         *
         * @return whether this instance has a static context.
         */
        public boolean hasStaticContext() {

            for (final com.github.dm.jrt.function.Consumer<?> consumer : mConsumers) {

                final boolean isStaticContext;
                final Class<? extends com.github.dm.jrt.function.Consumer> consumerClass =
                        consumer.getClass();

                if (consumerClass == Consumer.class) {

                    isStaticContext = ((Consumer) consumer).hasStaticContext();

                } else {

                    isStaticContext = Reflection.hasStaticContext(consumerClass);
                }

                if (!isStaticContext) {

                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {

            int result = 0;

            for (final com.github.dm.jrt.function.Consumer<?> consumer : mConsumers) {

                final Class<? extends com.github.dm.jrt.function.Consumer> consumerClass =
                        consumer.getClass();

                if (consumerClass == Consumer.class) {

                    result = 31 * result + consumer.hashCode();

                } else {

                    result = 31 * result + consumerClass.hashCode();
                }
            }

            return result;
        }

        @Override
        @SuppressFBWarnings(value = "EQ_GETCLASS_AND_CLASS_CONSTANT",
                justification = "comparing class of the internal list objects")
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final Consumer<?> that = (Consumer<?>) o;
            final List<com.github.dm.jrt.function.Consumer<?>> thisConsumers = mConsumers;
            final List<com.github.dm.jrt.function.Consumer<?>> thatConsumers = that.mConsumers;
            final int size = thisConsumers.size();

            if (size != thatConsumers.size()) {

                return false;
            }

            for (int i = 0; i < size; i++) {

                final com.github.dm.jrt.function.Consumer<?> thisConsumer = thisConsumers.get(i);
                final com.github.dm.jrt.function.Consumer<?> thatConsumer = thatConsumers.get(i);
                final Class<? extends com.github.dm.jrt.function.Consumer> thisClass =
                        thisConsumer.getClass();
                final Class<? extends com.github.dm.jrt.function.Consumer> thatClass =
                        thatConsumer.getClass();

                if (thisClass == Consumer.class) {

                    if (thatClass == Consumer.class) {

                        if (!thisConsumer.equals(thatConsumer)) {

                            return false;
                        }

                    } else {

                        final Consumer<?> thisInstance = (Consumer<?>) thisConsumer;

                        if ((thisInstance.mConsumers.size() != 1) || (
                                thisInstance.mConsumers.get(0).getClass() != thatClass)) {

                            return false;
                        }
                    }

                } else if (thatClass == Consumer.class) {

                    final Consumer<?> thatInstance = (Consumer<?>) thatConsumer;

                    if ((thatInstance.mConsumers.size() != 1) || (
                            thatInstance.mConsumers.get(0).getClass() != thisClass)) {

                        return false;
                    }

                } else if (thisClass != thatClass) {

                    return false;
                }
            }

            return true;
        }

        /**
         * Performs this operation on the given argument.
         *
         * @param in the input argument.
         */
        @SuppressWarnings("unchecked")
        public void accept(final IN in) {

            for (final com.github.dm.jrt.function.Consumer<?> consumer : mConsumers) {

                synchronized (getMutex(consumer)) {

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
         * Returns a composed function that first applies the before function to its input, and then
         * applies this function to the result.
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
         * Checks if this function instance has a static context.
         *
         * @return whether this instance has a static context.
         */
        public boolean hasStaticContext() {

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                final boolean isStaticContext;
                final Class<? extends com.github.dm.jrt.function.Function> functionClass =
                        function.getClass();

                if (functionClass == Function.class) {

                    isStaticContext = ((Function) function).hasStaticContext();

                } else {

                    isStaticContext = Reflection.hasStaticContext(functionClass);
                }

                if (!isStaticContext) {

                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {

            int result = 0;

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                final Class<? extends com.github.dm.jrt.function.Function> functionClass =
                        function.getClass();

                if (functionClass == Function.class) {

                    result = 31 * result + function.hashCode();

                } else {

                    result = 31 * result + functionClass.hashCode();
                }
            }

            return result;
        }

        @Override
        @SuppressFBWarnings(value = "EQ_GETCLASS_AND_CLASS_CONSTANT",
                justification = "comparing class of the internal list objects")
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final Function<?, ?> that = (Function<?, ?>) o;
            final List<com.github.dm.jrt.function.Function<?, ?>> thisFunctions = mFunctions;
            final List<com.github.dm.jrt.function.Function<?, ?>> thatFunctions = that.mFunctions;
            final int size = thisFunctions.size();

            if (size != thatFunctions.size()) {

                return false;
            }

            for (int i = 0; i < size; i++) {

                final com.github.dm.jrt.function.Function<?, ?> thisFunction = thisFunctions.get(i);
                final com.github.dm.jrt.function.Function<?, ?> thatFunction = thatFunctions.get(i);
                final Class<? extends com.github.dm.jrt.function.Function> thisClass =
                        thisFunction.getClass();
                final Class<? extends com.github.dm.jrt.function.Function> thatClass =
                        thatFunction.getClass();

                if (thisClass == Function.class) {

                    if (thatClass == Function.class) {

                        if (!thisFunction.equals(thatFunction)) {

                            return false;
                        }

                    } else {

                        final Function<?, ?> thisInstance = (Function<?, ?>) thisFunction;

                        if ((thisInstance.mFunctions.size() != 1) || (
                                thisInstance.mFunctions.get(0).getClass() != thatClass)) {

                            return false;
                        }
                    }

                } else if (thatClass == Function.class) {

                    final Function<?, ?> thatInstance = (Function<?, ?>) thatFunction;

                    if ((thatInstance.mFunctions.size() != 1) || (
                            thatInstance.mFunctions.get(0).getClass() != thisClass)) {

                        return false;
                    }

                } else if (thisClass != thatClass) {

                    return false;
                }
            }

            return true;
        }

        /**
         * Applies this function to the given argument.
         *
         * @param in the input argument.
         * @return the function result.
         */
        @SuppressWarnings("unchecked")
        public OUT apply(final IN in) {

            Object result = in;

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                synchronized (getMutex(function)) {

                    result = ((com.github.dm.jrt.function.Function<Object, Object>) function).apply(
                            result);
                }
            }

            return (OUT) result;
        }
    }

    /**
     * Class wrapping a supplier instance.
     *
     * @param <OUT> the output data type.
     */
    public static class Supplier<OUT> implements com.github.dm.jrt.function.Supplier<OUT> {

        private final List<com.github.dm.jrt.function.Function<?, ?>> mFunctions;

        private final com.github.dm.jrt.function.Supplier<?> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier  the initial wrapped supplier.
         * @param functions the list of wrapped functions.
         */
        private Supplier(@NotNull final com.github.dm.jrt.function.Supplier<?> supplier,
                @NotNull final List<com.github.dm.jrt.function.Function<?, ?>> functions) {

            mSupplier = supplier;
            mFunctions = functions;
        }

        /**
         * Returns a composed supplier that first gets this supplier result, and then
         * applies the after function to it.
         *
         * @param after   the function to apply after this function is applied.
         * @param <AFTER> the type of output of the after function.
         * @return the composed function.
         */
        @NotNull
        @SuppressWarnings("ConstantConditions")
        public <AFTER> Supplier<AFTER> andThen(
                @NotNull final com.github.dm.jrt.function.Function<? super OUT, AFTER> after) {

            if (after == null) {

                throw new NullPointerException("the after function must not be null");
            }

            final List<com.github.dm.jrt.function.Function<?, ?>> functions = mFunctions;
            final ArrayList<com.github.dm.jrt.function.Function<?, ?>> newFunctions =
                    new ArrayList<com.github.dm.jrt.function.Function<?, ?>>(functions.size() + 1);
            newFunctions.addAll(functions);
            newFunctions.add(after);
            return new Supplier<AFTER>(mSupplier, newFunctions);
        }

        /**
         * Checks if this supplier instance has a static context.
         *
         * @return whether this instance has a static context.
         */
        public boolean hasStaticContext() {

            final com.github.dm.jrt.function.Supplier<?> supplier = mSupplier;
            final Class<? extends com.github.dm.jrt.function.Supplier> supplierClass =
                    supplier.getClass();

            if (((supplierClass == Supplier.class) && !((Supplier) supplier).hasStaticContext())
                    || !Reflection.hasStaticContext(supplierClass)) {

                return false;
            }

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                final boolean isStaticContext;
                final Class<? extends com.github.dm.jrt.function.Function> functionClass =
                        function.getClass();

                if (functionClass == Function.class) {

                    isStaticContext = ((Function) function).hasStaticContext();

                } else {

                    isStaticContext = Reflection.hasStaticContext(functionClass);
                }

                if (!isStaticContext) {

                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {

            int result = mSupplier.getClass().hashCode();

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                final Class<? extends com.github.dm.jrt.function.Function> functionClass =
                        function.getClass();

                if (functionClass == Function.class) {

                    result = 31 * result + function.hashCode();

                } else {

                    result = 31 * result + functionClass.hashCode();
                }
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

            final Supplier<?> that = (Supplier<?>) o;

            if (mSupplier.getClass() != that.mSupplier.getClass()) {

                return false;
            }

            final List<com.github.dm.jrt.function.Function<?, ?>> thisFunctions = mFunctions;
            final List<com.github.dm.jrt.function.Function<?, ?>> thatFunctions = that.mFunctions;
            final int size = thisFunctions.size();

            if (size != thatFunctions.size()) {

                return false;
            }

            for (int i = 0; i < size; i++) {

                final com.github.dm.jrt.function.Function<?, ?> thisFunction = thisFunctions.get(i);
                final com.github.dm.jrt.function.Function<?, ?> thatFunction = thatFunctions.get(i);
                final Class<? extends com.github.dm.jrt.function.Function> thisClass =
                        thisFunction.getClass();
                final Class<? extends com.github.dm.jrt.function.Function> thatClass =
                        thatFunction.getClass();

                if (thisClass == Function.class) {

                    if (thatClass == Function.class) {

                        if (!thisFunction.equals(thatFunction)) {

                            return false;
                        }

                    } else {

                        final Function<?, ?> thisInstance = (Function<?, ?>) thisFunction;

                        if ((thisInstance.mFunctions.size() != 1) || (
                                thisInstance.mFunctions.get(0).getClass() != thatClass)) {

                            return false;
                        }
                    }

                } else if (thatClass == Function.class) {

                    final Function<?, ?> thatInstance = (Function<?, ?>) thatFunction;

                    if ((thatInstance.mFunctions.size() != 1) || (
                            thatInstance.mFunctions.get(0).getClass() != thisClass)) {

                        return false;
                    }

                } else if (thisClass != thatClass) {

                    return false;
                }
            }

            return true;
        }

        /**
         * Gets a result.
         *
         * @return a result.
         */
        @SuppressWarnings("unchecked")
        public OUT get() {

            Object result;
            final com.github.dm.jrt.function.Supplier<?> supplier = mSupplier;

            synchronized (getMutex(supplier)) {

                result = supplier.get();
            }

            for (final com.github.dm.jrt.function.Function<?, ?> function : mFunctions) {

                synchronized (getMutex(function)) {

                    result = ((com.github.dm.jrt.function.Function<Object, Object>) function).apply(
                            result);
                }
            }

            return (OUT) result;
        }
    }
}
