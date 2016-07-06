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

package com.github.dm.jrt.stream.input;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionDecorator;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.operator.math.Numbers.toBigDecimalSafe;

/**
 * Utility class providing functions that produce streams of inputs.
 * <p>
 * Created by davide-maestroni on 07/02/2016.
 */
public class Streams {

    /**
     * Avoid explicit instantiation.
     */
    protected Streams() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The generated data will start from the specified first one up to and including the specified
     * last one, by computing each next element through the specified function.
     *
     * @param start             the first element of the range.
     * @param end               the last element of the range.
     * @param incrementFunction the function incrementing the current element.
     * @param <AFTER>           the concatenation output type.
     * @return the consumer instance.
     */
    @NotNull
    public static <AFTER extends Comparable<? super AFTER>> Consumer<Channel<AFTER, ?>> range(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> incrementFunction) {
        return new RangeConsumer<AFTER>(ConstantConditions.notNull("start element", start),
                ConstantConditions.notNull("end element", end),
                Functions.decorate(incrementFunction));
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The stream will generate a range of numbers up to and including the {@code end} element, by
     * applying a default increment of {@code +1} or {@code -1} depending on the comparison between
     * the first and the last element. That is, if the first element is less than the last, the
     * increment will be {@code +1}. On the contrary, if the former is greater than the latter, the
     * increment will be {@code -1}.
     *
     * @param start the first element of the range.
     * @param end   the last element of the range.
     * @param <N>   the number type.
     * @return the consumer instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> Consumer<Channel<N, ?>> range(@NotNull final N start,
            @NotNull final N end) {
        return (Consumer<Channel<N, ?>>) numberRange(start, end);
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The stream will generate a range of numbers by applying the specified increment up to and
     * including the {@code end} element.
     *
     * @param start     the first element of the range.
     * @param end       the last element of the range.
     * @param increment the increment to apply to the current element.
     * @param <N>       the number type.
     * @return the consumer instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> Consumer<Channel<N, ?>> range(@NotNull final N start,
            @NotNull final N end, @NotNull final N increment) {
        return (Consumer<Channel<N, ?>>) numberRange(start, end, increment);
    }

    /**
     * Returns a consumer generating the specified sequence of data.
     * <br>
     * The generated data will start from the specified first and will produce the specified number
     * of elements, by computing each next one through the specified function.
     *
     * @param start        the first element of the sequence.
     * @param count        the number of generated elements.
     * @param nextFunction the function computing the next element.
     * @param <AFTER>      the concatenation output type.
     * @return the consumer instance.
     * @throws java.lang.IllegalArgumentException if the count is not positive.
     */
    @NotNull
    public static <AFTER> Consumer<Channel<AFTER, ?>> sequence(@NotNull final AFTER start,
            final long count, @NotNull final BiFunction<AFTER, Long, AFTER> nextFunction) {
        return new SequenceConsumer<AFTER>(ConstantConditions.notNull("start element", start),
                ConstantConditions.positive("sequence size", count),
                Functions.decorate(nextFunction));
    }

    @NotNull
    private static <N extends Number> Consumer<? extends Channel<? extends Number, ?>> numberRange(
            @NotNull final N start, @NotNull final N end) {
        if ((start instanceof BigDecimal) || (end instanceof BigDecimal)) {
            final BigDecimal startValue = toBigDecimalSafe(start);
            final BigDecimal endValue = toBigDecimalSafe(end);
            return numberRange(startValue, endValue,
                    (startValue.compareTo(endValue) <= 0) ? 1 : -1);

        } else if ((start instanceof BigInteger) || (end instanceof BigInteger)) {
            final BigDecimal startDecimal = toBigDecimalSafe(start);
            final BigDecimal endDecimal = toBigDecimalSafe(end);
            if ((startDecimal.scale() > 0) || (endDecimal.scale() > 0)) {
                return numberRange(startDecimal, endDecimal,
                        (startDecimal.compareTo(endDecimal) <= 0) ? 1 : -1);
            }

            final BigInteger startValue = startDecimal.toBigInteger();
            final BigInteger endValue = endDecimal.toBigInteger();
            return numberRange(startValue, endValue,
                    (startValue.compareTo(endValue) <= 0) ? 1 : -1);

        } else if ((start instanceof Double) || (end instanceof Double)) {
            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Float) || (end instanceof Float)) {
            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Long) || (end instanceof Long)) {
            final long startValue = start.longValue();
            final long endValue = end.longValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Integer) || (end instanceof Integer)) {
            final int startValue = start.intValue();
            final int endValue = end.intValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Short) || (end instanceof Short)) {
            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            return numberRange(start, end, (short) ((startValue <= endValue) ? 1 : -1));

        } else if ((start instanceof Byte) || (end instanceof Byte)) {
            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            return numberRange(start, end, (byte) ((startValue <= endValue) ? 1 : -1));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + "]");
    }

    @NotNull
    private static <N extends Number> Consumer<? extends Channel<? extends Number, ?>> numberRange(
            @NotNull final N start, @NotNull final N end, @NotNull final N increment) {
        if ((start instanceof BigDecimal) || (end instanceof BigDecimal)
                || (increment instanceof BigDecimal)) {
            final BigDecimal startValue = toBigDecimalSafe(start);
            final BigDecimal endValue = toBigDecimalSafe(end);
            final BigDecimal incValue = toBigDecimalSafe(increment);
            return new RangeConsumer<BigDecimal>(startValue, endValue, new BigDecimalInc(incValue));

        } else if ((start instanceof BigInteger) || (end instanceof BigInteger)
                || (increment instanceof BigInteger)) {
            final BigDecimal startDecimal = toBigDecimalSafe(start);
            final BigDecimal endDecimal = toBigDecimalSafe(end);
            final BigDecimal incDecimal = toBigDecimalSafe(increment);
            if ((startDecimal.scale() > 0) || (endDecimal.scale() > 0) || (incDecimal.scale()
                    > 0)) {
                return new RangeConsumer<BigDecimal>(startDecimal, endDecimal,
                        new BigDecimalInc(incDecimal));
            }

            final BigInteger startValue = startDecimal.toBigInteger();
            final BigInteger endValue = endDecimal.toBigInteger();
            final BigInteger incValue = incDecimal.toBigInteger();
            return new RangeConsumer<BigInteger>(startValue, endValue, new BigIntegerInc(incValue));

        } else if ((start instanceof Double) || (end instanceof Double)
                || (increment instanceof Double)) {
            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            final double incValue = increment.doubleValue();
            return new RangeConsumer<Double>(startValue, endValue, new DoubleInc(incValue));

        } else if ((start instanceof Float) || (end instanceof Float)
                || (increment instanceof Float)) {
            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            final float incValue = increment.floatValue();
            return new RangeConsumer<Float>(startValue, endValue, new FloatInc(incValue));

        } else if ((start instanceof Long) || (end instanceof Long)
                || (increment instanceof Long)) {
            final long startValue = start.longValue();
            final long endValue = end.longValue();
            final long incValue = increment.longValue();
            return new RangeConsumer<Long>(startValue, endValue, new LongInc(incValue));

        } else if ((start instanceof Integer) || (end instanceof Integer)
                || (increment instanceof Integer)) {
            final int startValue = start.intValue();
            final int endValue = end.intValue();
            final int incValue = increment.intValue();
            return new RangeConsumer<Integer>(startValue, endValue, new IntegerInc(incValue));

        } else if ((start instanceof Short) || (end instanceof Short)
                || (increment instanceof Short)) {
            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            final short incValue = increment.shortValue();
            return new RangeConsumer<Short>(startValue, endValue, new ShortInc(incValue));

        } else if ((start instanceof Byte) || (end instanceof Byte)
                || (increment instanceof Byte)) {
            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            final byte incValue = increment.byteValue();
            return new RangeConsumer<Byte>(startValue, endValue, new ByteInc(incValue));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + ", " + increment.getClass()
                                                                              .getCanonicalName()
                        + "]");
    }

    /**
     * Function incrementing a big decimal of a specific value.
     */
    private static class BigDecimalInc extends NumberInc<BigDecimal> {

        private final BigDecimal mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private BigDecimalInc(final BigDecimal incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public BigDecimal apply(final BigDecimal bigDecimal) {
            return bigDecimal.add(mIncValue);
        }
    }

    /**
     * Function incrementing a big integer of a specific value.
     */
    private static class BigIntegerInc extends NumberInc<BigInteger> {

        private final BigInteger mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private BigIntegerInc(final BigInteger incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public BigInteger apply(final BigInteger bigInteger) {
            return bigInteger.add(mIncValue);
        }
    }

    /**
     * Function incrementing a short of a specific value.
     */
    private static class ByteInc extends NumberInc<Byte> {

        private final byte mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private ByteInc(final byte incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Byte apply(final Byte aByte) {
            return (byte) (aByte + mIncValue);
        }
    }

    /**
     * Function incrementing a double of a specific value.
     */
    private static class DoubleInc extends NumberInc<Double> {

        private final double mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private DoubleInc(final double incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Double apply(final Double aDouble) {
            return aDouble + mIncValue;
        }
    }

    /**
     * Function incrementing a float of a specific value.
     */
    private static class FloatInc extends NumberInc<Float> {

        private final float mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private FloatInc(final float incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Float apply(final Float aFloat) {
            return aFloat + mIncValue;
        }
    }

    /**
     * Function incrementing an integer of a specific value.
     */
    private static class IntegerInc extends NumberInc<Integer> {

        private final int mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private IntegerInc(final int incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Integer apply(final Integer integer) {
            return integer + mIncValue;
        }
    }

    /**
     * Function incrementing a long of a specific value.
     */
    private static class LongInc extends NumberInc<Long> {

        private final long mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private LongInc(final long incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Long apply(final Long aLong) {
            return aLong + mIncValue;
        }
    }

    /**
     * Base abstract function incrementing a number of a specific value.
     * <br>
     * It provides an implementation for {@code equals()} and {@code hashCode()} methods.
     */
    private static abstract class NumberInc<N extends Number> extends DeepEqualObject
            implements Function<N, N> {

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private NumberInc(@NotNull final N incValue) {
            super(asArgs(incValue));
        }
    }

    /**
     * Consumer implementation generating a range of data.
     *
     * @param <OUT> the output data type.
     */
    private static class RangeConsumer<OUT extends Comparable<? super OUT>> extends DeepEqualObject
            implements Consumer<Channel<OUT, ?>> {

        private final OUT mEnd;

        private final Function<OUT, OUT> mIncrementFunction;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start             the first element of the range.
         * @param end               the last element of the range.
         * @param incrementFunction the function incrementing the current element.
         */
        private RangeConsumer(@NotNull final OUT start, @NotNull final OUT end,
                @NotNull final Function<OUT, OUT> incrementFunction) {
            super(asArgs(start, end, incrementFunction));
            mStart = start;
            mEnd = end;
            mIncrementFunction = incrementFunction;
        }

        public void accept(final Channel<OUT, ?> result) throws Exception {
            final OUT start = mStart;
            final OUT end = mEnd;
            final Function<OUT, OUT> increment = mIncrementFunction;
            OUT current = start;
            if (start.compareTo(end) <= 0) {
                while (current.compareTo(end) <= 0) {
                    result.pass(current);
                    current = increment.apply(current);
                }

            } else {
                while (current.compareTo(end) >= 0) {
                    result.pass(current);
                    current = increment.apply(current);
                }
            }
        }
    }

    /**
     * Consumer implementation generating a sequence of data.
     *
     * @param <OUT> the output data type.
     */
    private static class SequenceConsumer<OUT> extends DeepEqualObject
            implements Consumer<Channel<OUT, ?>> {

        private final long mCount;

        private final BiFunctionDecorator<OUT, Long, OUT> mNextFunction;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start        the first element of the sequence.
         * @param count        the size of the sequence.
         * @param nextFunction the function computing the next element.
         */
        private SequenceConsumer(@NotNull final OUT start, final long count,
                @NotNull final BiFunctionDecorator<OUT, Long, OUT> nextFunction) {
            super(asArgs(start, count, nextFunction));
            mStart = start;
            mCount = count;
            mNextFunction = nextFunction;
        }

        public void accept(final Channel<OUT, ?> result) throws Exception {
            final BiFunctionDecorator<OUT, Long, OUT> next = mNextFunction;
            OUT current = mStart;
            final long count = mCount;
            final long last = count - 1;
            for (long i = 0; i < count; ++i) {
                result.pass(current);
                if (i < last) {
                    current = next.apply(current, i);
                }
            }
        }
    }

    /**
     * Function incrementing a short of a specific value.
     */
    private static class ShortInc extends NumberInc<Short> {

        private final short mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private ShortInc(final short incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Short apply(final Short aShort) {
            return (short) (aShort + mIncValue);
        }
    }
}
