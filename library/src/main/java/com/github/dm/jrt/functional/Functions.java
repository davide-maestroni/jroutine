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

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.FunctionInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class supporting functional programming.
 * <p/>
 * Created by davide-maestroni on 09/21/2015.
 */
public class Functions {

    private static final BiConsumerChain<?, ?> sBiSink =
            biConsumerChain(new BiConsumer<Object, Object>() {

                public void accept(final Object in1, final Object in2) {

                }
            });

    private static final FunctionChain<?, ?> sIdentity =
            functionChain(new Function<Object, Object>() {

                public Object apply(final Object in) {

                    return in;
                }
            });

    private static final ConsumerChain<?> sSink = consumerChain(new Consumer<Object>() {

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
     * The returned object will support concatenation and comparison.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @return the wrapped bi-consumer.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <IN1, IN2> BiConsumerChain<IN1, IN2> biConsumerChain(
            @NotNull final BiConsumer<IN1, IN2> consumer) {

        if (consumer.getClass() == BiConsumerChain.class) {

            return (BiConsumerChain<IN1, IN2>) consumer;
        }

        return new BiConsumerChain<IN1, IN2>(Collections.<BiConsumer<?, ?>>singletonList(consumer));
    }

    /**
     * Returns a bi-consumer chain just discarding the passed inputs.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the wrapped bi-consumer.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiConsumerChain<IN1, IN2> biSink() {

        return (BiConsumerChain<IN1, IN2>) sBiSink;
    }

    /**
     * Returns a supplier chain always returning the same result.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param result the result.
     * @param <OUT>  the output data type.
     * @return the wrapped supplier.
     */
    @NotNull
    public static <OUT> SupplierChain<OUT> constant(final OUT result) {

        return supplierChain(new Supplier<OUT>() {

            public OUT get() {

                return result;
            }
        });
    }

    /**
     * Wraps the specified consumer instance so to provide additional features.<br/>
     * The returned object will support concatenation and comparison.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param consumer the consumer instance.
     * @param <IN>     the input data type.
     * @return the wrapped consumer.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <IN> ConsumerChain<IN> consumerChain(@NotNull final Consumer<IN> consumer) {

        if (consumer.getClass() == ConsumerChain.class) {

            return (ConsumerChain<IN>) consumer;
        }

        return new ConsumerChain<IN>(Collections.<Consumer<?>>singletonList(consumer));
    }

    /**
     * Builds and returns a new command invocation based on the specified consumer instance.<br/>
     * In order to prevent undesired leaks, the class of the specified consumer must have a static
     * context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <OUT> CommandInvocation<OUT> consumerCommand(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return new ConsumerCommandInvocation<OUT>(consumerChain(consumer));
    }

    /**
     * Builds and returns a new invocation factory based on the specified bi-consumer instance.<br/>
     * In order to prevent undesired leaks, the class of the specified bi-consumer must have a
     * static context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> consumerFactory(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return new ConsumerInvocationFactory<IN, OUT>(biConsumerChain(consumer));
    }

    /**
     * Builds and returns a new filter invocation based on the specified bi-consumer instance.<br/>
     * In order to prevent undesired leaks, the class of the specified bi-consumer must have a
     * static context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FilterInvocation<IN, OUT> consumerFilter(
            @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

        return new ConsumerFilterInvocation<IN, OUT>(biConsumerChain(consumer));
    }

    /**
     * Wraps the specified function instance so to provide additional features.<br/>
     * The returned object will support concatenation and comparison.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the wrapped function.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <IN, OUT> FunctionChain<IN, OUT> functionChain(
            @NotNull final Function<IN, OUT> function) {

        if (function.getClass() == FunctionChain.class) {

            return (FunctionChain<IN, OUT>) function;
        }

        return new FunctionChain<IN, OUT>(Collections.<Function<?, ?>>singletonList(function));
    }

    /**
     * Builds and returns a new invocation factory based on the specified function instance.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> functionFactory(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return new FunctionInvocationFactory<IN, OUT>(functionChain(function));
    }

    /**
     * Builds and returns a new filter invocation based on the specified function instance.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FilterInvocation<IN, OUT> functionFilter(
            @NotNull final Function<? super IN, OUT> function) {

        return new FunctionFilterInvocation<IN, OUT>(functionChain(function));
    }

    /**
     * Returns the identity function.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the wrapped function.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> FunctionChain<IN, IN> identity() {

        return (FunctionChain<IN, IN>) sIdentity;
    }

    /**
     * Returns a consumer chain just discarding the passed inputs.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the wrapped consumer.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> ConsumerChain<IN> sink() {

        return (ConsumerChain<IN>) sSink;
    }

    /**
     * Wraps the specified supplier instance so to provide additional features.<br/>
     * The returned object will support concatenation and comparison.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the wrapped supplier.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public static <OUT> SupplierChain<OUT> supplierChain(@NotNull final Supplier<OUT> supplier) {

        if (supplier.getClass() == SupplierChain.class) {

            return (SupplierChain<OUT>) supplier;
        }

        return new SupplierChain<OUT>(supplier, Functions.<OUT>identity());
    }

    /**
     * Builds and returns a new command invocation based on the specified supplier instance.<br/>
     * In order to prevent undesired leaks, the class of the specified supplier must have a static
     * context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <OUT> CommandInvocation<OUT> supplierCommand(
            @NotNull final Supplier<OUT> supplier) {

        return new SupplierCommandInvocation<OUT>(supplierChain(supplier));
    }

    /**
     * Builds and returns a new invocation factory based on the specified supplier instance.<br/>
     * In order to prevent undesired leaks, the class of the specified supplier must have a static
     * context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param supplier the supplier instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> supplierFactory(
            @NotNull final Supplier<? extends Invocation<IN, OUT>> supplier) {

        return new SupplierInvocationFactory<IN, OUT>(supplierChain(supplier));
    }

    /**
     * Command invocation based on a consumer instance.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerCommandInvocation<OUT> extends CommandInvocation<OUT> {

        private final ConsumerChain<? super ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        public ConsumerCommandInvocation(
                @NotNull final ConsumerChain<? super ResultChannel<OUT>> consumer) {

            if (!consumer.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the consumer class must have a static context: " + consumer.getClass());
            }

            mConsumer = consumer;
        }

        @Override
        public int hashCode() {

            return mConsumer.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ConsumerCommandInvocation)) {

                return false;
            }

            final ConsumerCommandInvocation<?> that = (ConsumerCommandInvocation<?>) o;
            return mConsumer.equals(that.mConsumer);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(result);
        }
    }

    /**
     * Filter invocation based on a bi-consumer instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ConsumerFilterInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final BiConsumerChain<? super IN, ? super ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerFilterInvocation(
                @NotNull final BiConsumerChain<? super IN, ? super ResultChannel<OUT>> consumer) {

            if (!consumer.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the bi-consumer class must have a static context: " + consumer.getClass());
            }

            mConsumer = consumer;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(input, result);
        }

        @Override
        public int hashCode() {

            return mConsumer.hashCode();
        }


        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ConsumerFilterInvocation)) {

                return false;
            }

            final ConsumerFilterInvocation<?, ?> that = (ConsumerFilterInvocation<?, ?>) o;
            return mConsumer.equals(that.mConsumer);
        }
    }

    /**
     * Factory of function invocations based on a bi-consumer instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ConsumerInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final BiConsumerChain<? super List<? extends IN>, ? super ResultChannel<OUT>>
                mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerInvocationFactory(
                @NotNull final BiConsumerChain<? super List<? extends IN>, ? super
                        ResultChannel<OUT>> consumer) {

            if (!consumer.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the bi-consumer class must have a static context: " + consumer.getClass());
            }

            mConsumer = consumer;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new FunctionInvocation<IN, OUT>() {

                @Override
                protected void onCall(@NotNull final List<? extends IN> inputs,
                        @NotNull final ResultChannel<OUT> result) {

                    mConsumer.accept(inputs, result);
                }
            };
        }

        @Override
        public int hashCode() {

            return mConsumer.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ConsumerInvocationFactory)) {

                return false;
            }

            final ConsumerInvocationFactory<?, ?> that = (ConsumerInvocationFactory<?, ?>) o;
            return mConsumer.equals(that.mConsumer);
        }
    }

    /**
     * Filter invocation based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionFilterInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final FunctionChain<? super IN, OUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionFilterInvocation(@NotNull final FunctionChain<? super IN, OUT> function) {

            if (!function.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the function class must have a static context: " + function.getClass());
            }

            mFunction = function;
        }

        @Override
        public int hashCode() {

            return mFunction.hashCode();
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            result.pass(mFunction.apply(input));
        }


        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof FunctionFilterInvocation)) {

                return false;
            }

            final FunctionFilterInvocation<?, ?> that = (FunctionFilterInvocation<?, ?>) o;
            return mFunction.equals(that.mFunction);
        }
    }

    /**
     * Factory of function invocations based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final FunctionChain<? super List<? extends IN>, OUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionInvocationFactory(
                @NotNull final FunctionChain<? super List<? extends IN>, OUT> function) {

            if (!function.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the function class must have a static context: " + function.getClass());
            }

            mFunction = function;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new FunctionInvocation<IN, OUT>() {

                @Override
                protected void onCall(@NotNull final List<? extends IN> inputs,
                        @NotNull final ResultChannel<OUT> result) {

                    result.pass(mFunction.apply(inputs));
                }
            };
        }

        @Override
        public int hashCode() {

            return mFunction.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof FunctionInvocationFactory)) {

                return false;
            }

            final FunctionInvocationFactory<?, ?> that = (FunctionInvocationFactory<?, ?>) o;
            return mFunction.equals(that.mFunction);
        }
    }

    /**
     * Command invocation based on a supplier instance.
     *
     * @param <OUT> the output data type.
     */
    private static class SupplierCommandInvocation<OUT> extends CommandInvocation<OUT> {

        private final SupplierChain<OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier instance.
         */
        public SupplierCommandInvocation(@NotNull final SupplierChain<OUT> supplier) {

            if (!supplier.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the supplier class must have a static context: " + supplier.getClass());
            }

            mSupplier = supplier;
        }

        @Override
        public int hashCode() {

            return mSupplier.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof SupplierCommandInvocation)) {

                return false;
            }

            final SupplierCommandInvocation<?> that = (SupplierCommandInvocation<?>) o;
            return mSupplier.equals(that.mSupplier);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            result.pass(mSupplier.get());
        }
    }

    /**
     * Implementation of an invocation factory based on a supplier function.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class SupplierInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final SupplierChain<? extends Invocation<IN, OUT>> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier function.
         */
        private SupplierInvocationFactory(
                @NotNull final SupplierChain<? extends Invocation<IN, OUT>> supplier) {

            if (!supplier.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the supplier class must have a static context: " + supplier.getClass());
            }

            mSupplier = supplier;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return mSupplier.get();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof SupplierInvocationFactory)) {

                return false;
            }

            final SupplierInvocationFactory<?, ?> that = (SupplierInvocationFactory<?, ?>) o;
            return mSupplier.equals(that.mSupplier);
        }

        @Override
        public int hashCode() {

            return mSupplier.hashCode();
        }
    }
}
