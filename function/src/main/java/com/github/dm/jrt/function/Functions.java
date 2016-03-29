/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Utility class back-porting functional programming.
 * <p/>
 * Created by davide-maestroni on 09/21/2015.
 */
public class Functions {

    /**
     * Avoid explicit instantiation.
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
     * Builds and returns a new call invocation factory based on the specified bi-consumer instance.
     * <br/>
     * It's up to the caller to prevent undesired leaks.
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
    public static <IN, OUT> InvocationFactory<IN, OUT> consumerCall(
            @NotNull final BiConsumer<? super List<IN>, ? super ResultChannel<OUT>> consumer) {

        return new ConsumerInvocationFactory<IN, OUT>(wrap(consumer));
    }

    /**
     * Builds and returns a new command invocation based on the specified consumer instance.<br/>
     * It's up to the caller to prevent undesired leaks.
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

        return new ConsumerCommandInvocation<OUT>(wrap(consumer));
    }

    /**
     * Builds and returns a new filter invocation based on the specified bi-consumer instance.<br/>
     * It's up to the caller to prevent undesired leaks.
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

        return new ConsumerFilterInvocation<IN, OUT>(wrap(consumer));
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
     * Builds and returns a new call invocation factory based on the specified function instance.
     * <br/>
     * It's up to the caller to prevent undesired leaks.
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
    public static <IN, OUT> InvocationFactory<IN, OUT> functionCall(
            @NotNull final Function<? super List<IN>, ? extends OUT> function) {

        return new FunctionInvocationFactory<IN, OUT>(wrap(function));
    }

    /**
     * Builds and returns a new filter invocation based on the specified function instance.<br/>
     * It's up to the caller to prevent undesired leaks.
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
            @NotNull final Function<? super IN, ? extends OUT> function) {

        return new FunctionFilterInvocation<IN, OUT>(wrap(function));
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
     * Returns a bi-function wrapper returning the greater of the two inputs as per natural
     * ordering.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    public static <IN extends Comparable<IN>> BiFunctionWrapper<IN, IN, IN> max() {

        return BiFunctionWrapper.max();
    }

    /**
     * Returns a bi-function wrapper returning the greater of the two inputs as indicated by the
     * specified comparator.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function wrapper.
     */
    public static <IN> BiFunctionWrapper<IN, IN, IN> maxBy(
            @NotNull final Comparator<? super IN> comparator) {

        return BiFunctionWrapper.maxBy(comparator);
    }

    /**
     * Returns a bi-function wrapper returning the smaller of the two inputs as per natural
     * ordering.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    public static <IN extends Comparable<IN>> BiFunctionWrapper<IN, IN, IN> min() {

        return BiFunctionWrapper.min();
    }

    /**
     * Returns a bi-function wrapper returning the smaller of the two inputs as indicated by the
     * specified comparator.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function wrapper.
     */
    public static <IN> BiFunctionWrapper<IN, IN, IN> minBy(
            @NotNull final Comparator<? super IN> comparator) {

        return BiFunctionWrapper.minBy(comparator);
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

        return new OutputConsumerBuilder<Object>(wrap(consumer), Functions.<RoutineException>sink(),
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

        return new OutputConsumerBuilder<Object>(Functions.<Void>sink(), wrap(consumer),
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
                Functions.<RoutineException>sink(), wrap(consumer));
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
     * It's up to the caller to prevent undesired leaks.
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

        return new PredicateFilterInvocation<IN>(wrap(predicate));
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
     * It's up to the caller to prevent undesired leaks.
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
            @NotNull final Supplier<? extends OUT> supplier) {

        return new SupplierCommandInvocation<OUT>(wrap(supplier));
    }

    /**
     * Builds and returns a new invocation factory based on the specified supplier instance.<br/>
     * It's up to the caller to prevent undesired leaks.
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
            @NotNull final Supplier<? extends Invocation<? super IN, ? extends OUT>> supplier) {

        return new SupplierInvocationFactory<IN, OUT>(wrap(supplier));
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
    public static <IN1, IN2> BiConsumerWrapper<IN1, IN2> wrap(
            @NotNull final BiConsumer<IN1, IN2> consumer) {

        if (consumer instanceof BiConsumerWrapper) {
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
    public static <IN1, IN2, OUT> BiFunctionWrapper<IN1, IN2, OUT> wrap(
            @NotNull final BiFunction<IN1, IN2, OUT> function) {

        if (function instanceof BiFunctionWrapper) {
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
    public static <IN> ConsumerWrapper<IN> wrap(@NotNull final Consumer<IN> consumer) {

        if (consumer instanceof ConsumerWrapper) {
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
    public static <IN, OUT> FunctionWrapper<IN, OUT> wrap(
            @NotNull final Function<IN, OUT> function) {

        if (function instanceof FunctionWrapper) {
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
    public static <IN> PredicateWrapper<IN> wrap(@NotNull final Predicate<IN> predicate) {

        if (predicate instanceof PredicateWrapper) {
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
    public static <OUT> SupplierWrapper<OUT> wrap(@NotNull final Supplier<OUT> supplier) {

        if (supplier instanceof SupplierWrapper) {
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
        private ConsumerCommandInvocation(
                @NotNull final ConsumerWrapper<? super ResultChannel<OUT>> consumer) {

            super(asArgs(consumer));
            mConsumer = consumer;
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

            super(asArgs(consumer));
            mConsumer = consumer;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(input, result);
        }
    }

    /**
     * Factory of call invocations based on a bi-consumer instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ConsumerInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final CallInvocation<IN, OUT> mInvocation;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerInvocationFactory(@NotNull final BiConsumerWrapper<? super List<IN>, ? super
                ResultChannel<OUT>> consumer) {

            super(asArgs(consumer));
            mInvocation = new CallInvocation<IN, OUT>() {

                @Override
                protected void onCall(@NotNull final List<? extends IN> inputs,
                        @NotNull final ResultChannel<OUT> result) {

                    consumer.accept(new ArrayList<IN>(inputs), result);
                }
            };
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return mInvocation;
        }
    }

    /**
     * Filter invocation based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionFilterInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final FunctionWrapper<? super IN, ? extends OUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionFilterInvocation(
                @NotNull final FunctionWrapper<? super IN, ? extends OUT> function) {

            super(asArgs(function));
            mFunction = function;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            result.pass(mFunction.apply(input));
        }
    }

    /**
     * Factory of call invocations based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final CallInvocation<IN, OUT> mInvocation;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionInvocationFactory(
                @NotNull final FunctionWrapper<? super List<IN>, ? extends OUT> function) {

            super(asArgs(function));
            mInvocation = new CallInvocation<IN, OUT>() {

                @Override
                protected void onCall(@NotNull final List<? extends IN> inputs,
                        @NotNull final ResultChannel<OUT> result) {

                    result.pass(function.apply(new ArrayList<IN>(inputs)));
                }
            };
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return mInvocation;
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

            super(asArgs(predicate));
            mPredicate = predicate;
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

        private final SupplierWrapper<? extends OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier instance.
         */
        public SupplierCommandInvocation(@NotNull final SupplierWrapper<? extends OUT> supplier) {

            super(asArgs(supplier));
            mSupplier = supplier;
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

        private final SupplierWrapper<? extends Invocation<? super IN, ? extends OUT>> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier function.
         */
        private SupplierInvocationFactory(
                @NotNull final SupplierWrapper<? extends Invocation<? super IN, ? extends OUT>>
                        supplier) {

            super(asArgs(supplier));
            mSupplier = supplier;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public Invocation<IN, OUT> newInvocation() {

            return (Invocation<IN, OUT>) mSupplier.get();
        }
    }
}
