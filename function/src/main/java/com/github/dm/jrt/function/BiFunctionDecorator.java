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

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class decorating a bi-function instance.
 * <p>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 * @param <OUT> the output data type.
 */
public class BiFunctionDecorator<IN1, IN2, OUT> extends DeepEqualObject
        implements BiFunction<IN1, IN2, OUT>, Decorator {

    private static final BiFunctionDecorator<Object, Object, Object> sFirst =
            new BiFunctionDecorator<Object, Object, Object>(
                    new BiFunction<Object, Object, Object>() {

                        public Object apply(final Object in1, final Object in2) {
                            return in1;
                        }
                    });

    private static final BiFunctionDecorator<? extends Comparable<?>, ? extends Comparable<?>, ?
            extends
            Comparable<?>> sMax =
            new BiFunctionDecorator<Comparable<Object>, Comparable<Object>, Comparable<Object>>(
                    new BiFunction<Comparable<Object>, Comparable<Object>, Comparable<Object>>() {

                        public Comparable<Object> apply(final Comparable<Object> in1,
                                final Comparable<Object> in2) {
                            return (in1.compareTo(in2) >= 0) ? in1 : in2;
                        }
                    });

    private static final BiFunctionDecorator<? extends Comparable<?>, ? extends Comparable<?>, ?
            extends
            Comparable<?>> sMin =
            new BiFunctionDecorator<Comparable<Object>, Comparable<Object>, Comparable<Object>>(
                    new BiFunction<Comparable<Object>, Comparable<Object>, Comparable<Object>>() {

                        public Comparable<Object> apply(final Comparable<Object> in1,
                                final Comparable<Object> in2) {
                            return (in1.compareTo(in2) <= 0) ? in1 : in2;
                        }
                    });

    private static final BiFunctionDecorator<Object, Object, Object> sSecond =
            new BiFunctionDecorator<Object, Object, Object>(
                    new BiFunction<Object, Object, Object>() {

                        public Object apply(final Object in1, final Object in2) {
                            return in2;
                        }
                    });

    private final BiFunction<IN1, IN2, ?> mBiFunction;

    private final FunctionDecorator<?, ? extends OUT> mFunction;

    /**
     * Constructor.
     *
     * @param biFunction the wrapped supplier.
     */
    private BiFunctionDecorator(@NotNull final BiFunction<IN1, IN2, ?> biFunction) {
        this(ConstantConditions.notNull("bi-function instance", biFunction),
                FunctionDecorator.<OUT>identity());
    }

    /**
     * Constructor.
     *
     * @param biFunction the initial wrapped supplier.
     * @param function   the concatenated function chain.
     */
    private BiFunctionDecorator(@NotNull final BiFunction<IN1, IN2, ?> biFunction,
            @NotNull final FunctionDecorator<?, ? extends OUT> function) {
        super(asArgs(biFunction, function));
        mBiFunction = biFunction;
        mFunction = function;
    }

    /**
     * Decorates the specified bi-function instance so to provide additional features.
     * <br>
     * The returned object will support concatenation and comparison.
     * <p>
     * Note that the passed object is expected to have a functional behavior, that is, it must not
     * retain a mutable internal state.
     * <br>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param function the bi-function instance.
     * @param <IN1>    the first input data type.
     * @param <IN2>    the second input data type.
     * @param <OUT>    the output data type.
     * @return the decorated bi-function.
     */
    @NotNull
    public static <IN1, IN2, OUT> BiFunctionDecorator<IN1, IN2, OUT> decorate(
            @NotNull final BiFunction<IN1, IN2, OUT> function) {
        if (function instanceof BiFunctionDecorator) {
            return (BiFunctionDecorator<IN1, IN2, OUT>) function;
        }

        return new BiFunctionDecorator<IN1, IN2, OUT>(function);
    }

    /**
     * Returns a bi-function decorator just returning the first passed argument.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-function decorator.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiFunctionDecorator<IN1, IN2, IN1> first() {
        return (BiFunctionDecorator<IN1, IN2, IN1>) sFirst;
    }

    /**
     * Returns a bi-function decorator returning the greater of the two inputs as per natural
     * ordering.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the bi-function decorator.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN extends Comparable<? super IN>> BiFunctionDecorator<IN, IN, IN> max() {
        return (BiFunctionDecorator<IN, IN, IN>) sMax;
    }

    /**
     * Returns a bi-function decorator returning the greater of the two inputs as indicated by the
     * specified comparator.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function decorator.
     */
    public static <IN> BiFunctionDecorator<IN, IN, IN> maxBy(
            @NotNull final Comparator<? super IN> comparator) {
        return new BiFunctionDecorator<IN, IN, IN>(
                new MaxByFunction<IN>(ConstantConditions.notNull("comparator", comparator)));
    }

    /**
     * Returns a bi-function decorator returning the smaller of the two inputs as per natural
     * ordering.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the bi-function decorator.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN extends Comparable<? super IN>> BiFunctionDecorator<IN, IN, IN> min() {
        return (BiFunctionDecorator<IN, IN, IN>) sMin;
    }

    /**
     * Returns a bi-function decorator returning the smaller of the two inputs as indicated by the
     * specified comparator.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function decorator.
     */
    public static <IN> BiFunctionDecorator<IN, IN, IN> minBy(
            @NotNull final Comparator<? super IN> comparator) {
        return new BiFunctionDecorator<IN, IN, IN>(
                new MinByFunction<IN>(ConstantConditions.notNull("comparator", comparator)));
    }

    /**
     * Returns a bi-function decorator just returning the second passed argument.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN1> the first input data type.
     * @param <IN2> the second input data type.
     * @return the bi-function decorator.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN1, IN2> BiFunctionDecorator<IN1, IN2, IN2> second() {
        return (BiFunctionDecorator<IN1, IN2, IN2>) sSecond;
    }

    /**
     * Returns a composed bi-function decorator that first applies this function to its input, and
     * then applies the after function to the result.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed bi-function.
     */
    @NotNull
    public <AFTER> BiFunctionDecorator<IN1, IN2, AFTER> andThen(
            @NotNull final Function<? super OUT, ? extends AFTER> after) {
        return new BiFunctionDecorator<IN1, IN2, AFTER>(mBiFunction, mFunction.andThen(after));
    }

    public boolean hasStaticScope() {
        return Reflection.hasStaticScope(mBiFunction) && mFunction.hasStaticScope();
    }

    /**
     * Bi-function returning the maximum between the two inputs.
     *
     * @param <IN> the input data type.
     */
    private static class MaxByFunction<IN> extends DeepEqualObject
            implements BiFunction<IN, IN, IN> {

        private final Comparator<? super IN> mComparator;

        /**
         * Constructor.
         *
         * @param comparator the input comparator.
         */
        private MaxByFunction(@NotNull final Comparator<? super IN> comparator) {
            super(asArgs(comparator));
            mComparator = comparator;
        }

        public IN apply(final IN in1, final IN in2) {
            return (mComparator.compare(in1, in2) > 0) ? in1 : in2;
        }
    }

    /**
     * Bi-function returning the minimum between the two inputs.
     *
     * @param <IN> the input data type.
     */
    private static class MinByFunction<IN> extends DeepEqualObject
            implements BiFunction<IN, IN, IN> {

        private final Comparator<? super IN> mComparator;

        /**
         * Constructor.
         *
         * @param comparator the input comparator.
         */
        private MinByFunction(@NotNull final Comparator<? super IN> comparator) {
            super(asArgs(comparator));
            mComparator = comparator;
        }

        public IN apply(final IN in1, final IN in2) {
            return (mComparator.compare(in1, in2) < 0) ? in1 : in2;
        }
    }

    @SuppressWarnings("unchecked")
    public OUT apply(final IN1 in1, final IN2 in2) throws Exception {
        return ((FunctionDecorator<Object, OUT>) mFunction).apply(mBiFunction.apply(in1, in2));
    }
}
