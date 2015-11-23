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

import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.FunctionInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utility class supporting functional programming.
 * <p/>
 * Created by davide-maestroni on 09/21/2015.
 */
public class Functions {

    private static final BiConsumerWrapper<?, ?> sBiSink =
            wrapBiConsumer(new BiConsumer<Object, Object>() {

                public void accept(final Object in1, final Object in2) {}
            });

    private static final FunctionWrapper<?, ?> sIdentity =
            wrapFunction(new Function<Object, Object>() {

                public Object apply(final Object in) {

                    return in;
                }
            });

    private static final BiFunctionWrapper<?, ?, ?> sFirst =
            wrapBiFunction(new BiFunction<Object, Object, Object>() {

                public Object apply(final Object in1, final Object in2) {

                    return in1;
                }
            });

    private static final PredicateWrapper<?> sNegative = wrapPredicate(new Predicate<Object>() {

        public boolean test(final Object o) {

            return false;
        }
    });

    private static final PredicateWrapper<?> sNotNull = wrapPredicate(new Predicate<Object>() {

        public boolean test(final Object o) {

            return (o != null);
        }
    });

    private static final PredicateWrapper<?> sIsNull = sNotNull.negate();

    private static final PredicateWrapper<?> sPositive = sNegative.negate();

    private static final BiFunctionWrapper<?, ?, ?> sSecond =
            wrapBiFunction(new BiFunction<Object, Object, Object>() {

                public Object apply(final Object in1, final Object in2) {

                    return in2;
                }
            });

    private static final ConsumerWrapper<?> sSink = wrapConsumer(new Consumer<Object>() {

        public void accept(final Object in) {}
    });

    /**
     * Avoid direct instantiation.
     */
    protected Functions() {

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
     * Builds and returns a new functional routine generating outputs from the specified command
     * invocation.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    public static <OUT> FunctionalRoutine<Void, OUT> buildFrom(
            @NotNull final CommandInvocation<OUT> invocation) {

        return builder().buildFrom(invocation);
    }

    /**
     * Builds and returns a new functional routine generating outputs from the specified consumer.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    public static <OUT> FunctionalRoutine<Void, OUT> buildFrom(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return builder().buildFrom(consumer);
    }

    /**
     * Builds and returns a new functional routine generating outputs from the specified supplier.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    public static <OUT> FunctionalRoutine<Void, OUT> buildFrom(
            @NotNull final Supplier<OUT> supplier) {

        return builder().buildFrom(supplier);
    }

    /**
     * Builds and returns a functional routine.
     *
     * @param <DATA> the data type.
     * @return the newly created routine instance.
     */
    @NotNull
    public static <DATA> FunctionalRoutine<DATA, DATA> buildRoutine() {

        return builder().buildRoutine();
    }

    /**
     * Returns a function wrapper casting the passed inputs to the specified class.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the function wrapper.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN, OUT> FunctionWrapper<IN, OUT> castTo(
            @NotNull final Class<? extends OUT> type) {

        if (type == null) {

            throw new NullPointerException("the type must not be null");
        }

        return wrapFunction(new ClassCastFunction<IN, OUT>(type));
    }

    /**
     * Returns a function wrapper casting the passed inputs to the specified class token type.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the function wrapper.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN, OUT> FunctionWrapper<IN, OUT> castTo(
            @NotNull final ClassToken<? extends OUT> token) {

        return wrapFunction(new ClassCastFunction<IN, OUT>(token.getRawClass()));
    }

    /**
     * Returns a supplier wrapper always returning the same result.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param result the result.
     * @param <OUT>  the output data type.
     * @return the supplier wrapper.
     */
    @NotNull
    public static <OUT> SupplierWrapper<OUT> constant(final OUT result) {

        return wrapSupplier(new ConstantSupplier<OUT>(result));
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
     * @return the command invocation.
     */
    @NotNull
    public static <OUT> CommandInvocation<OUT> consumerCommand(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return new ConsumerCommandInvocation<OUT>(wrapConsumer(consumer));
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

        return new ConsumerInvocationFactory<IN, OUT>(wrapBiConsumer(consumer));
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
     * @return the filter invocation.
     */
    @NotNull
    public static <IN, OUT> FilterInvocation<IN, OUT> consumerFilter(
            @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

        return new ConsumerFilterInvocation<IN, OUT>(wrapBiConsumer(consumer));
    }

    /**
     * Returns a predicate wrapper testing for equality to the specified object.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN1, IN2 extends IN1> PredicateWrapper<IN1> equalTo(@Nullable final IN2 other) {

        if (other == null) {

            return isNull();
        }

        return wrapPredicate(new EqualToPredicate<IN1>(other));
    }

    /**
     * Returns a bi-function wrapper just returning the first passed argument.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiFunctionWrapper<IN1, IN2, IN1> first() {

        return (BiFunctionWrapper<IN1, IN2, IN1>) sFirst;
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

        return new FunctionInvocationFactory<IN, OUT>(wrapFunction(function));
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
     * @return the filter invocation.
     */
    @NotNull
    public static <IN, OUT> FilterInvocation<IN, OUT> functionFilter(
            @NotNull final Function<? super IN, OUT> function) {

        return new FunctionFilterInvocation<IN, OUT>(wrapFunction(function));
    }

    /**
     * Returns the identity function wrapper.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the function wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> FunctionWrapper<IN, IN> identity() {

        return (FunctionWrapper<IN, IN>) sIdentity;
    }

    /**
     * Returns a predicate wrapper testing whether the passed inputs are instances of the specified
     * class.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN1, IN2 extends IN1> PredicateWrapper<IN1> instanceOf(
            @NotNull final Class<? extends IN2> type) {

        if (type == null) {

            throw new NullPointerException("the type must not be null");
        }

        return wrapPredicate(new InstanceOfPredicate<IN1>(type));
    }

    /**
     * Gets the invocation configuration builder related to a functional routine builder instance.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    public static Builder<? extends FunctionalRoutineBuilder> invocations() {

        return builder().invocations();
    }

    /**
     * Returns a predicate wrapper returning true when the passed argument is null.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> isNull() {

        return (PredicateWrapper<IN>) sIsNull;
    }

    /**
     * Returns a predicate wrapper always returning the false.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> negative() {

        return (PredicateWrapper<IN>) sNegative;
    }

    /**
     * Returns a predicate wrapper returning true when the passed argument is not null.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> notNull() {

        return (PredicateWrapper<IN>) sNotNull;
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation completion.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public static OutputConsumerBuilder<Object> onComplete(@NotNull final Consumer<Void> consumer) {

        return new OutputConsumerBuilder<Object>(wrapConsumer(consumer),
                                                 Functions.<RoutineException>sink(),
                                                 Functions.sink());
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation errors.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public static OutputConsumerBuilder<Object> onError(
            @NotNull final Consumer<RoutineException> consumer) {

        return new OutputConsumerBuilder<Object>(Functions.<Void>sink(), wrapConsumer(consumer),
                                                 Functions.sink());
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation outputs.
     *
     * @param consumer the consumer function.
     * @param <OUT>    the output data type.
     * @return the builder instance.
     */
    @NotNull
    public static <OUT> OutputConsumerBuilder<OUT> onOutput(@NotNull final Consumer<OUT> consumer) {

        return new OutputConsumerBuilder<OUT>(Functions.<Void>sink(),
                                              Functions.<RoutineException>sink(),
                                              wrapConsumer(consumer));
    }

    /**
     * Returns a predicate wrapper always returning the true.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> positive() {

        return (PredicateWrapper<IN>) sPositive;
    }

    /**
     * Builds and returns a new filter invocation based on the specified predicate instance.<br/>
     * Only the inputs which satisfies the predicate will be passed on, while the others will be
     * filtered out.<br/>
     * In order to prevent undesired leaks, the class of the specified predicate must have a static
     * context.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param predicate the predicate instance.
     * @param <IN>      the input data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN> FilterInvocation<IN, IN> predicateFilter(
            @NotNull final Predicate<? super IN> predicate) {

        return new PredicateFilterInvocation<IN>(wrapPredicate(predicate));
    }

    /**
     * Returns a predicate wrapper testing for identity to the specified object.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN1, IN2 extends IN1> PredicateWrapper<IN1> sameAs(@Nullable final IN2 other) {

        if (other == null) {

            return isNull();
        }

        return wrapPredicate(new SameAsPredicate<IN1>(other));
    }

    /**
     * Returns a bi-function wrapper just returning the second passed argument.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiFunctionWrapper<IN1, IN2, IN2> second() {

        return (BiFunctionWrapper<IN1, IN2, IN2>) sSecond;
    }

    /**
     * Returns a consumer wrapper just discarding the passed inputs.<br/>
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
     * @return the command invocation.
     */
    @NotNull
    public static <OUT> CommandInvocation<OUT> supplierCommand(
            @NotNull final Supplier<OUT> supplier) {

        return new SupplierCommandInvocation<OUT>(wrapSupplier(supplier));
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

        return new SupplierInvocationFactory<IN, OUT>(wrapSupplier(supplier));
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
    public static <IN1, IN2> BiConsumerWrapper<IN1, IN2> wrapBiConsumer(
            @NotNull final BiConsumer<IN1, IN2> consumer) {

        if (consumer.getClass() == BiConsumerWrapper.class) {

            return (BiConsumerWrapper<IN1, IN2>) consumer;
        }

        return new BiConsumerWrapper<IN1, IN2>(consumer);
    }

    /**
     * Wraps the specified bi-function instance so to provide additional features.<br/>
     * The returned object will support concatenation and comparison.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param function the bi-function instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @param <OUT>    the output data type.
     * @return the wrapped bi-function.
     */
    @NotNull
    public static <IN1, IN2, OUT> BiFunctionWrapper<IN1, IN2, OUT> wrapBiFunction(
            @NotNull final BiFunction<IN1, IN2, OUT> function) {

        if (function.getClass() == BiFunctionWrapper.class) {

            return (BiFunctionWrapper<IN1, IN2, OUT>) function;
        }

        return new BiFunctionWrapper<IN1, IN2, OUT>(function, Functions.<OUT>identity());
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
    public static <IN> ConsumerWrapper<IN> wrapConsumer(@NotNull final Consumer<IN> consumer) {

        if (consumer.getClass() == ConsumerWrapper.class) {

            return (ConsumerWrapper<IN>) consumer;
        }

        return new ConsumerWrapper<IN>(consumer);
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
    public static <IN, OUT> FunctionWrapper<IN, OUT> wrapFunction(
            @NotNull final Function<IN, OUT> function) {

        if (function.getClass() == FunctionWrapper.class) {

            return (FunctionWrapper<IN, OUT>) function;
        }

        return new FunctionWrapper<IN, OUT>(function);
    }

    /**
     * Wraps the specified predicate instance so to provide additional features.<br/>
     * The returned object will support concatenation and comparison.
     * <p/>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.<br/>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param predicate the predicate instance.
     * @param <IN>      the input data type.
     * @return the wrapped predicate.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> wrapPredicate(@NotNull final Predicate<IN> predicate) {

        if (predicate.getClass() == PredicateWrapper.class) {

            return (PredicateWrapper<IN>) predicate;
        }

        return new PredicateWrapper<IN>(predicate);
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
    public static <OUT> SupplierWrapper<OUT> wrapSupplier(@NotNull final Supplier<OUT> supplier) {

        if (supplier.getClass() == SupplierWrapper.class) {

            return (SupplierWrapper<OUT>) supplier;
        }

        return new SupplierWrapper<OUT>(supplier, Functions.<OUT>identity());
    }

    @NotNull
    private static DefaultFunctionalRoutineBuilder builder() {

        return new DefaultFunctionalRoutineBuilder();
    }

    /**
     * Function implementation casting inputs to the specified class.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ClassCastFunction<IN, OUT> implements Function<IN, OUT> {

        private final Class<? extends OUT> mType;

        /**
         * Constructor.
         *
         * @param type the output class type.
         */
        @SuppressWarnings("ConstantConditions")
        private ClassCastFunction(@NotNull final Class<? extends OUT> type) {

            mType = type;
        }

        @Override
        public int hashCode() {

            return mType.hashCode();
        }

        public OUT apply(final IN in) {

            return mType.cast(in);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final ClassCastFunction<?, ?> that = (ClassCastFunction<?, ?>) o;
            return mType.equals(that.mType);
        }
    }

    /**
     * Supplier implementation returning always the same object.
     *
     * @param <OUT> the output data type.
     */
    private static class ConstantSupplier<OUT> implements Supplier<OUT> {

        private final OUT mResult;

        /**
         * Constructor.
         *
         * @param result the object to return.
         */
        private ConstantSupplier(final OUT result) {

            mResult = result;
        }

        public OUT get() {

            return mResult;
        }

        @Override
        public int hashCode() {

            return mResult != null ? mResult.hashCode() : 0;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final ConstantSupplier<?> that = (ConstantSupplier<?>) o;
            return (mResult == that.mResult);
        }
    }

    /**
     * Command invocation based on a consumer instance.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerCommandInvocation<OUT> extends CommandInvocation<OUT> {

        private final ConsumerWrapper<? super ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        public ConsumerCommandInvocation(
                @NotNull final ConsumerWrapper<? super ResultChannel<OUT>> consumer) {

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

        private final BiConsumerWrapper<? super IN, ? super ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerFilterInvocation(
                @NotNull final BiConsumerWrapper<? super IN, ? super ResultChannel<OUT>> consumer) {

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

        private final BiConsumerWrapper<? super List<? extends IN>, ? super ResultChannel<OUT>>
                mConsumer;

        private final FunctionInvocation<IN, OUT> mInvocation;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerInvocationFactory(
                @NotNull final BiConsumerWrapper<? super List<? extends IN>, ? super
                        ResultChannel<OUT>> consumer) {

            if (!consumer.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the bi-consumer class must have a static context: " + consumer.getClass());
            }

            mConsumer = consumer;
            mInvocation = new FunctionInvocation<IN, OUT>() {

                @Override
                protected void onCall(@NotNull final List<? extends IN> inputs,
                        @NotNull final ResultChannel<OUT> result) {

                    consumer.accept(inputs, result);
                }
            };
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return mInvocation;
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
     * Predicate implementation testing for equality.
     *
     * @param <IN> the input data type.
     */
    private static class EqualToPredicate<IN> implements Predicate<IN> {

        private final IN mOther;

        /**
         * Constructor.
         *
         * @param other the other object to test against.
         */
        private EqualToPredicate(@NotNull final IN other) {

            mOther = other;
        }

        @Override
        public int hashCode() {

            return mOther.hashCode();
        }

        public boolean test(final IN in) {

            return mOther.equals(in);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final EqualToPredicate<?> that = (EqualToPredicate<?>) o;
            return mOther.equals(that.mOther);
        }
    }

    /**
     * Filter invocation based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionFilterInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final FunctionWrapper<? super IN, OUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionFilterInvocation(@NotNull final FunctionWrapper<? super IN, OUT> function) {

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

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            result.pass(mFunction.apply(input));
        }
    }

    /**
     * Factory of function invocations based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final FunctionWrapper<? super List<? extends IN>, OUT> mFunction;

        private final FunctionInvocation<IN, OUT> mInvocation;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionInvocationFactory(
                @NotNull final FunctionWrapper<? super List<? extends IN>, OUT> function) {

            if (!function.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the function class must have a static context: " + function.getClass());
            }

            mFunction = function;
            mInvocation = new FunctionInvocation<IN, OUT>() {

                @Override
                protected void onCall(@NotNull final List<? extends IN> inputs,
                        @NotNull final ResultChannel<OUT> result) {

                    result.pass(function.apply(inputs));
                }
            };
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return mInvocation;
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
     * Predicate testing whether an object is an instance of a specific class.
     *
     * @param <IN> the input data type.
     */
    private static class InstanceOfPredicate<IN> implements Predicate<IN> {

        private final Class<? extends IN> mType;

        /**
         * Constructor.
         *
         * @param type the class type.
         */
        private InstanceOfPredicate(@NotNull final Class<? extends IN> type) {

            mType = type;
        }

        @Override
        public int hashCode() {

            return mType.hashCode();
        }

        public boolean test(final IN in) {

            return mType.isInstance(in);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final InstanceOfPredicate<?> that = (InstanceOfPredicate<?>) o;
            return mType.equals(that.mType);
        }
    }

    /**
     * Filter invocation based on a predicate instance.
     *
     * @param <IN> the input data type.
     */
    private static class PredicateFilterInvocation<IN> extends FilterInvocation<IN, IN> {

        private final PredicateWrapper<? super IN> mPredicate;

        /**
         * Constructor.
         *
         * @param predicate the predicate instance.
         */
        private PredicateFilterInvocation(@NotNull final PredicateWrapper<? super IN> predicate) {

            if (!predicate.hasStaticContext()) {

                throw new IllegalArgumentException(
                        "the predicate class must have a static context: " + predicate.getClass());
            }

            mPredicate = predicate;
        }

        @Override
        public int hashCode() {

            return mPredicate.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof PredicateFilterInvocation)) {

                return false;
            }

            final PredicateFilterInvocation<?> that = (PredicateFilterInvocation<?>) o;
            return mPredicate.equals(that.mPredicate);
        }

        public void onInput(final IN input, @NotNull final ResultChannel<IN> result) {

            if (mPredicate.test(input)) {

                result.pass(input);
            }
        }
    }

    /**
     * Predicate implementation testing for identity.
     *
     * @param <IN> the input data type.
     */
    private static class SameAsPredicate<IN> implements Predicate<IN> {

        private final IN mOther;

        /**
         * Constructor.
         *
         * @param other the other object to test against.
         */
        private SameAsPredicate(@NotNull final IN other) {

            mOther = other;
        }

        @Override
        public int hashCode() {

            return mOther.hashCode();
        }

        public boolean test(final IN in) {

            return (mOther == in);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {

                return false;
            }

            final SameAsPredicate<?> that = (SameAsPredicate<?>) o;
            return (mOther == that.mOther);
        }
    }

    /**
     * Command invocation based on a supplier instance.
     *
     * @param <OUT> the output data type.
     */
    private static class SupplierCommandInvocation<OUT> extends CommandInvocation<OUT> {

        private final SupplierWrapper<OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier instance.
         */
        public SupplierCommandInvocation(@NotNull final SupplierWrapper<OUT> supplier) {

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

        private final SupplierWrapper<? extends Invocation<IN, OUT>> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier function.
         */
        private SupplierInvocationFactory(
                @NotNull final SupplierWrapper<? extends Invocation<IN, OUT>> supplier) {

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
