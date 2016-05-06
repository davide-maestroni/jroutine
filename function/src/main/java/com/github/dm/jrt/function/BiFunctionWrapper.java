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
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class wrapping a bi-function instance.
 * <p>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 * @param <OUT> the output data type.
 */
public class BiFunctionWrapper<IN1, IN2, OUT> extends DeepEqualObject
        implements BiFunction<IN1, IN2, OUT>, Wrapper {

    private static final WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>>
            mMaxFunctions = new WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>>();

    private static final WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>>
            mMinFunctions = new WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>>();

    private static final BiFunctionWrapper<Object, Object, Object> sFirst =
            new BiFunctionWrapper<Object, Object, Object>(new BiFunction<Object, Object, Object>() {

                public Object apply(final Object in1, final Object in2) {

                    return in1;
                }
            });

    private static final BiFunctionWrapper<? extends Comparable<?>, ? extends Comparable<?>, ?
            extends
            Comparable<?>> sMax =
            new BiFunctionWrapper<Comparable<Object>, Comparable<Object>, Comparable<Object>>(
                    new BiFunction<Comparable<Object>, Comparable<Object>, Comparable<Object>>() {

                        public Comparable<Object> apply(final Comparable<Object> in1,
                                final Comparable<Object> in2) {

                            return (in1.compareTo(in2) >= 0) ? in1 : in2;
                        }
                    });

    private static final BiFunctionWrapper<? extends Comparable<?>, ? extends Comparable<?>, ?
            extends
            Comparable<?>> sMin =
            new BiFunctionWrapper<Comparable<Object>, Comparable<Object>, Comparable<Object>>(
                    new BiFunction<Comparable<Object>, Comparable<Object>, Comparable<Object>>() {

                        public Comparable<Object> apply(final Comparable<Object> in1,
                                final Comparable<Object> in2) {

                            return (in1.compareTo(in2) <= 0) ? in1 : in2;
                        }
                    });

    private static final BiFunctionWrapper<Object, Object, Object> sSecond =
            new BiFunctionWrapper<Object, Object, Object>(new BiFunction<Object, Object, Object>() {

                public Object apply(final Object in1, final Object in2) {

                    return in2;
                }
            });

    private final BiFunction<IN1, IN2, ?> mBiFunction;

    private final FunctionWrapper<?, ? extends OUT> mFunction;

    /**
     * Constructor.
     *
     * @param biFunction the wrapped supplier.
     */
    private BiFunctionWrapper(@NotNull final BiFunction<IN1, IN2, ?> biFunction) {

        this(ConstantConditions.notNull("bi-function instance", biFunction),
                FunctionWrapper.<OUT>identity());
    }

    /**
     * Constructor.
     *
     * @param biFunction the initial wrapped supplier.
     * @param function   the concatenated function chain.
     */
    private BiFunctionWrapper(@NotNull final BiFunction<IN1, IN2, ?> biFunction,
            @NotNull final FunctionWrapper<?, ? extends OUT> function) {

        super(asArgs(biFunction, function));
        mBiFunction = biFunction;
        mFunction = function;
    }

    /**
     * Returns a bi-function wrapper just returning the first passed argument.
     * <br>
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
     * Returns a bi-function wrapper returning the greater of the two inputs as per natural
     * ordering.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN extends Comparable<? super IN>> BiFunctionWrapper<IN, IN, IN> max() {

        return (BiFunctionWrapper<IN, IN, IN>) sMax;
    }

    /**
     * Returns a bi-function wrapper returning the greater of the two inputs as indicated by the
     * specified comparator.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function wrapper.
     */
    @SuppressWarnings("unchecked")
    public static <IN> BiFunctionWrapper<IN, IN, IN> maxBy(
            @NotNull final Comparator<? super IN> comparator) {

        ConstantConditions.notNull("comparator", comparator);
        synchronized (mMaxFunctions) {
            final WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>> functions =
                    mMaxFunctions;
            BiFunctionWrapper<IN, IN, IN> function =
                    (BiFunctionWrapper<IN, IN, IN>) functions.get(comparator);
            if (function == null) {
                function = new BiFunctionWrapper<IN, IN, IN>(new MaxByFunction<IN>(comparator));
                functions.put(comparator, function);
            }

            return function;
        }
    }

    /**
     * Returns a bi-function wrapper returning the smaller of the two inputs as per natural
     * ordering.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the bi-function wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN extends Comparable<? super IN>> BiFunctionWrapper<IN, IN, IN> min() {

        return (BiFunctionWrapper<IN, IN, IN>) sMin;
    }

    /**
     * Returns a bi-function wrapper returning the smaller of the two inputs as indicated by the
     * specified comparator.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function wrapper.
     */
    @SuppressWarnings("unchecked")
    public static <IN> BiFunctionWrapper<IN, IN, IN> minBy(
            @NotNull final Comparator<? super IN> comparator) {

        ConstantConditions.notNull("comparator", comparator);
        synchronized (mMinFunctions) {
            final WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>> functions =
                    mMinFunctions;
            BiFunctionWrapper<IN, IN, IN> function =
                    (BiFunctionWrapper<IN, IN, IN>) functions.get(comparator);
            if (function == null) {
                function = new BiFunctionWrapper<IN, IN, IN>(new MinByFunction<IN>(comparator));
                functions.put(comparator, function);
            }

            return function;
        }
    }

    /**
     * Returns a bi-function wrapper just returning the second passed argument.
     * <br>
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
     * Wraps the specified bi-function instance so to provide additional features.
     * <br>
     * The returned object will support concatenation and comparison.
     * <p>
     * Note that the passed object is expected to behave like a function, that is, it must not
     * retain a mutable internal state.
     * <br>
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
     * Returns a composed bi-function wrapper that first applies this function to its input, and
     * then applies the after function to the result.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed bi-function.
     */
    @NotNull
    public <AFTER> BiFunctionWrapper<IN1, IN2, AFTER> andThen(
            @NotNull final Function<? super OUT, ? extends AFTER> after) {

        return new BiFunctionWrapper<IN1, IN2, AFTER>(mBiFunction, mFunction.andThen(after));
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
    public OUT apply(final IN1 in1, final IN2 in2) {

        return ((FunctionWrapper<Object, OUT>) mFunction).apply(mBiFunction.apply(in1, in2));
    }
}
