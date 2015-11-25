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

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.FunctionInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
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
    public static <IN1, IN2> BiConsumerWrapper<IN1, IN2> biSink() {

        return BiConsumerWrapper.biSink();
    }

    /**
     * Returns a function wrapper casting the passed inputs to the specified class.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param type  the class type.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the function wrapper.
     */
    @NotNull
    public static <IN, OUT> FunctionWrapper<IN, OUT> castTo(
            @NotNull final Class<? extends OUT> type) {

        return FunctionWrapper.castTo(type);
    }

    /**
     * Returns a function wrapper casting the passed inputs to the specified class token type.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param token the class token.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the function wrapper.
     */
    @NotNull
    public static <IN, OUT> FunctionWrapper<IN, OUT> castTo(
            @NotNull final ClassToken<? extends OUT> token) {

        return FunctionWrapper.castTo(token);
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

        return SupplierWrapper.constant(result);
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
     * Returns a bi-function wrapper just returning the first passed argument.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    public static <IN1, IN2> BiFunctionWrapper<IN1, IN2, IN1> first() {

        return BiFunctionWrapper.first();
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
     * Returns a functional routine builder.
     *
     * @return the routine builder instance.
     */
    @NotNull
    public static FunctionalRoutineBuilder functional() {

        return new DefaultFunctionalRoutineBuilder();
    }

    /**
     * Returns the identity function wrapper.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the function wrapper.
     */
    @NotNull
    public static <IN> FunctionWrapper<IN, IN> identity() {

        return FunctionWrapper.identity();
    }

    /**
     * Returns a predicate wrapper testing for equality to the specified object.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param targetRef the target reference.
     * @param <IN>      the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> isEqual(@Nullable final Object targetRef) {

        return PredicateWrapper.isEqual(targetRef);
    }

    /**
     * Returns a predicate wrapper testing whether the passed inputs are instances of the specified
     * class.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param type the class type.
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> isInstanceOf(@NotNull final Class<?> type) {

        return PredicateWrapper.isInstanceOf(type);
    }

    /**
     * Returns a predicate wrapper returning true when the passed argument is null.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> isNull() {

        return PredicateWrapper.isNull();
    }

    /**
     * Returns a predicate wrapper testing for identity to the specified object.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param targetRef the target reference.
     * @param <IN>      the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> isSame(@Nullable final Object targetRef) {

        return PredicateWrapper.isSame(targetRef);
    }

    /**
     * Returns a predicate wrapper always returning the false.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> negative() {

        return PredicateWrapper.negative();
    }

    /**
     * Returns a predicate wrapper returning true when the passed argument is not null.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> notNull() {

        return PredicateWrapper.notNull();
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
    public static <IN> PredicateWrapper<IN> positive() {

        return PredicateWrapper.positive();
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
     * Returns a bi-function wrapper just returning the second passed argument.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    public static <IN1, IN2> BiFunctionWrapper<IN1, IN2, IN2> second() {

        return BiFunctionWrapper.second();
    }

    /**
     * Returns a consumer wrapper just discarding the passed inputs.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the consumer wrapper.
     */
    @NotNull
    public static <IN> ConsumerWrapper<IN> sink() {

        return ConsumerWrapper.sink();
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

        return new BiFunctionWrapper<IN1, IN2, OUT>(function);
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

        return new SupplierWrapper<OUT>(supplier);
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
     * Default implementation of a functional routine builder.
     */
    private static class DefaultFunctionalRoutineBuilder
            implements FunctionalRoutineBuilder, Configurable<FunctionalRoutineBuilder> {

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.DEFAULT_CONFIGURATION;

        @NotNull
        public Builder<? extends FunctionalRoutineBuilder> invocations() {

            return new Builder<FunctionalRoutineBuilder>(this, mConfiguration);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> routineFrom(
                @NotNull final CommandInvocation<OUT> invocation) {

            return fromFactory(invocation, DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> routineFrom(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerCommand(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> routineFrom(
                @NotNull final Supplier<OUT> supplier) {

            return fromFactory(supplierCommand(supplier), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenAsyncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenAsyncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(routine, DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenParallelFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(routine, DelegationType.PARALLEL);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenSyncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenSyncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(routine, DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public FunctionalRoutineBuilder setConfiguration(
                @NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConfiguration = configuration;
            return this;
        }

        @NotNull
        private <IN, OUT> FunctionalRoutine<IN, OUT> fromFactory(
                @NotNull final InvocationFactory<IN, OUT> factory,
                @NotNull final DelegationType delegationType) {

            return new DefaultFunctionalRoutine<IN, OUT>(
                    JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine(),
                    delegationType);
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
