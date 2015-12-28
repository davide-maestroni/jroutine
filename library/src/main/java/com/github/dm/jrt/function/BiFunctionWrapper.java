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

import java.util.Comparator;

/**
 * Class wrapping a bi-function instance.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 * @param <OUT> the output data type.
 */
public class BiFunctionWrapper<IN1, IN2, OUT> implements BiFunction<IN1, IN2, OUT> {

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
    @SuppressWarnings("ConstantConditions")
    BiFunctionWrapper(@NotNull final BiFunction<IN1, IN2, ?> biFunction) {

        this(biFunction, FunctionWrapper.<OUT>identity());

        if (biFunction == null) {

            throw new NullPointerException("the bi-function instance must not be null");
        }
    }

    /**
     * Constructor.
     *
     * @param biFunction the initial wrapped supplier.
     * @param function   the concatenated function chain.
     */
    private BiFunctionWrapper(@NotNull final BiFunction<IN1, IN2, ?> biFunction,
            @NotNull final FunctionWrapper<?, ? extends OUT> function) {

        mBiFunction = biFunction;
        mFunction = function;
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
     * Returns a bi-function wrapper returning the greater of the two inputs as indicated by the
     * specified comparator.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param comparator the comparator instance.
     * @param <IN>       the input data type.
     * @return the bi-function wrapper.
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static <IN> BiFunctionWrapper<IN, IN, IN> maxBy(
            @NotNull final Comparator<? super IN> comparator) {

        if (comparator == null) {

            throw new NullPointerException("the comparator must not be null");
        }

        synchronized (mMaxFunctions) {

            final WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>> functions =
                    mMaxFunctions;
            BiFunctionWrapper<IN, IN, IN> function =
                    (BiFunctionWrapper<IN, IN, IN>) functions.get(comparator);

            if (function == null) {

                function = new BiFunctionWrapper<IN, IN, IN>(new BiFunction<IN, IN, IN>() {

                    public IN apply(final IN in1, final IN in2) {

                        return (comparator.compare(in1, in2) > 0) ? in1 : in2;
                    }
                });

                functions.put(comparator, function);
            }

            return function;
        }
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
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static <IN> BiFunctionWrapper<IN, IN, IN> minBy(
            @NotNull final Comparator<? super IN> comparator) {

        if (comparator == null) {

            throw new NullPointerException("the comparator must not be null");
        }

        synchronized (mMinFunctions) {

            final WeakIdentityHashMap<Comparator<?>, BiFunctionWrapper<?, ?, ?>> functions =
                    mMinFunctions;
            BiFunctionWrapper<IN, IN, IN> function =
                    (BiFunctionWrapper<IN, IN, IN>) functions.get(comparator);

            if (function == null) {

                function = new BiFunctionWrapper<IN, IN, IN>(new BiFunction<IN, IN, IN>() {

                    public IN apply(final IN in1, final IN in2) {

                        return (comparator.compare(in1, in2) < 0) ? in1 : in2;
                    }
                });

                functions.put(comparator, function);
            }

            return function;
        }
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

    @Override
    public int hashCode() {

        int result = mBiFunction.hashCode();
        result = 31 * result + mFunction.hashCode();
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

        final BiFunctionWrapper<?, ?, ?> that = (BiFunctionWrapper<?, ?, ?>) o;
        return mBiFunction.equals(that.mBiFunction) && mFunction.equals(that.mFunction);
    }

    @SuppressWarnings("unchecked")
    public OUT apply(final IN1 in1, final IN2 in2) {

        return ((FunctionWrapper<Object, OUT>) mFunction).apply(mBiFunction.apply(in1, in2));
    }
}
