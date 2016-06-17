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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Utility class providing several operators implemented as invocation factories.
 * <p>
 * Created by davide-maestroni on 06/13/2016.
 */
public class Operators {

    private static final InvocationFactory<?, ?> sMax =
            BinaryOperatorInvocation.functionFactory(Functions.max());

    private static final InvocationFactory<?, ?> sMin =
            BinaryOperatorInvocation.functionFactory(Functions.min());

    private static final InvocationFactory<?, ?> sNone =
            Functions.predicateFilter(Functions.negative());

    private static final InvocationFactory<?, ?> sNotNull =
            Functions.predicateFilter(Functions.isNotNull());

    private static final InvocationFactory<?, ?> sNull =
            Functions.predicateFilter(Functions.isNull());

    private static final MappingInvocation<? extends Iterable<?>, ?> sUnfoldInvocation =
            new MappingInvocation<Iterable<?>, Object>(null) {

                @SuppressWarnings("unchecked")
                public void onInput(final Iterable<?> input,
                        @NotNull final ResultChannel<Object> result) {
                    result.pass((Iterable) input);
                }
            };

    /**
     * Avoid explicit instantiation.
     */
    protected Operators() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a factory of invocations verifying that all the inputs satisfy a specific conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN> InvocationFactory<IN, Boolean> allMatch(
            @NotNull final Predicate<? super IN> predicate) {
        return new AllMatchInvocationFactory<IN>(wrap(predicate));
    }

    /**
     * Returns a factory of invocations verifying that any of the inputs satisfy a specific
     * conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN> InvocationFactory<IN, Boolean> anyMatch(
            @NotNull final Predicate<? super IN> predicate) {
        return new AnyMatchInvocationFactory<IN>(wrap(predicate));
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers.
     * <br>
     * The result will have the type matching the input with the highest precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Number> average() {
        return (InvocationFactory<N, Number>) AverageInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers by
     * employing a {@code BigDecimal}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, BigDecimal> averageBig() {
        return (InvocationFactory<N, BigDecimal>) AverageBigInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers in byte
     * precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Byte> averageByte() {
        return (InvocationFactory<N, Byte>) AverageByteInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers in double
     * precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Double> averageDouble() {
        return (InvocationFactory<N, Double>) AverageDoubleInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers in floating
     * precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Float> averageFloat() {
        return (InvocationFactory<N, Float>) AverageFloatInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers in integer
     * precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Integer> averageInteger() {
        return (InvocationFactory<N, Integer>) AverageIntegerInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers in long
     * precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Long> averageLong() {
        return (InvocationFactory<N, Long>) AverageLongInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the average value of the input numbers in short
     * precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Short> averageShort() {
        return (InvocationFactory<N, Short>) AverageShortInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations casting the passed inputs to the specified class.
     *
     * @param type  the class type.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> castTo(
            @NotNull final Class<? extends OUT> type) {
        return Functions.functionMapping(Functions.castTo(type));
    }

    /**
     * Returns a factory of invocations casting the passed inputs to the specified class token type.
     *
     * @param token the class token.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> castTo(
            @NotNull final ClassToken<? extends OUT> token) {
        return castTo(token.getRawClass());
    }

    /**
     * Returns a factory of invocations counting the number of input data.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> InvocationFactory<DATA, Long> count() {
        return (InvocationFactory<DATA, Long>) CountInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations grouping the input data in collections of the specified
     * size.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a size of 3, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [(0, 1, 2), (3, 4, 5), ..., (N, N + 1)]
     *     </code>
     * </pre>
     *
     * @param size   the group size.
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     * @throws java.lang.IllegalArgumentException if the size is not positive.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(final int size) {
        return new GroupByInvocationFactory<DATA>(size);
    }

    /**
     * Returns a factory of invocations grouping the input data in collections of the specified
     * size.
     * <br>
     * If the inputs complete and the last group length is less than the target size, the missing
     * spaces will be filled with the specified placeholder instance.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a size of 3, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [(0, 1, 2), (3, 4, 5), ..., (N, N + 1, PH)]
     *     </code>
     * </pre>
     *
     * @param size        the group size.
     * @param placeholder the placeholder object used to fill the missing data needed to reach
     *                    the group size.
     * @param <DATA>      the data type.
     * @return the invocation factory instance.
     * @throws java.lang.IllegalArgumentException if the size is not positive.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(final int size,
            @Nullable final DATA placeholder) {
        return new GroupByInvocationFactory<DATA>(size, placeholder);
    }

    /**
     * Returns a factory of invocations passing on inputs unchanged.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> identity() {
        return IdentityInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations filtering out all the inputs that are not equal to the
     * specified object.
     *
     * @param targetRef the target reference.
     * @param <DATA>    the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> isEqualTo(@Nullable final Object targetRef) {
        if (targetRef == null) {
            return isNull();
        }

        return Functions.predicateFilter(Functions.isEqualTo(targetRef));
    }

    /**
     * Returns a factory of invocations filtering out all the inputs that are not instances of the
     * specified class.
     *
     * @param type   the class type.
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> isInstanceOf(@NotNull final Class<?> type) {
        return Functions.predicateFilter(Functions.isInstanceOf(type));
    }

    /**
     * Returns a factory of invocations filtering out all the inputs that are equal to the specified
     * object.
     *
     * @param targetRef the target reference.
     * @param <DATA>    the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> isNotEqualTo(
            @Nullable final Object targetRef) {
        if (targetRef == null) {
            return isNotNull();
        }

        return Functions.predicateFilter(Functions.isEqualTo(targetRef).negate());
    }

    /**
     * Returns a factory of invocations filtering out all the inputs that are instances of the
     * specified class.
     *
     * @param type   the class type.
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> isNotInstanceOf(
            @NotNull final Class<?> type) {
        return Functions.predicateFilter(Functions.isInstanceOf(type).negate());
    }

    /**
     * Returns a factory of invocations filtering out all the inputs that are null.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> InvocationFactory<DATA, DATA> isNotNull() {
        return (InvocationFactory<DATA, DATA>) sNotNull;
    }

    /**
     * Returns factory of invocations filtering out all the inputs that are the same as the
     * specified object.
     *
     * @param targetRef the target reference.
     * @param <DATA>    the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> isNotSameAs(
            @Nullable final Object targetRef) {
        if (targetRef == null) {
            return isNotNull();
        }

        return Functions.predicateFilter(Functions.isEqualTo(targetRef).negate());
    }

    /**
     * Returns a factory of invocations filtering out all the inputs that are not null.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> InvocationFactory<DATA, DATA> isNull() {
        return (InvocationFactory<DATA, DATA>) sNull;
    }

    /**
     * Returns factory of invocations filtering out all the inputs that are not the same as the
     * specified object.
     *
     * @param targetRef the target reference.
     * @param <DATA>    the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> isSameAs(@Nullable final Object targetRef) {
        if (targetRef == null) {
            return isNull();
        }

        return Functions.predicateFilter(Functions.isEqualTo(targetRef));
    }

    /**
     * Returns a factory of invocations passing at max the specified number of input data and
     * discarding the following ones.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a limit count of 5, the final output
     * will be:
     * <pre>
     *     <code>
     *
     *         [0, 1, 2, 3, 4]
     *     </code>
     * </pre>
     *
     * @param count  the maximum number of data to pass.
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> limit(final int count) {
        return new LimitInvocationFactory<DATA>(count);
    }

    /**
     * Returns a factory of invocations returning the greater of the inputs as per natural ordering.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA extends Comparable<? super DATA>> InvocationFactory<DATA, DATA> max() {
        return (InvocationFactory<DATA, DATA>) sMax;
    }

    /**
     * Returns a factory of invocations returning the greater of the inputs as per by the specified
     * comparator.
     *
     * @param comparator the comparator instance.
     * @param <DATA>     the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> maxBy(
            @NotNull final Comparator<? super DATA> comparator) {
        return BinaryOperatorInvocation.functionFactory(Functions.maxBy(comparator));
    }

    /**
     * Returns a factory of invocations returning the smaller of the inputs as per natural ordering.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA extends Comparable<? super DATA>> InvocationFactory<DATA, DATA> min() {
        return (InvocationFactory<DATA, DATA>) sMin;
    }

    /**
     * Returns a factory of invocations returning the smaller of the inputs as per by the specified
     * comparator.
     *
     * @param comparator the comparator instance.
     * @param <DATA>     the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> minBy(
            @NotNull final Comparator<? super DATA> comparator) {
        return BinaryOperatorInvocation.functionFactory(Functions.minBy(comparator));
    }

    /**
     * Returns factory of invocations filtering out all the inputs.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> InvocationFactory<DATA, DATA> none() {
        return (InvocationFactory<DATA, DATA>) sNone;
    }

    /**
     * Returns a factory of invocations verifying that none of the inputs satisfy a specific
     * conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN> InvocationFactory<IN, Boolean> noneMatch(
            @NotNull final Predicate<? super IN> predicate) {
        return new AllMatchInvocationFactory<IN>(wrap(predicate).negate());
    }

    /**
     * Returns a factory of invocations verifying that not all the inputs satisfy a specific
     * conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN> InvocationFactory<IN, Boolean> notAllMatch(
            @NotNull final Predicate<? super IN> predicate) {
        return new AnyMatchInvocationFactory<IN>(wrap(predicate).negate());
    }

    /**
     * Returns a factory of invocations skipping the specified number of input data.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a skip count of 5, the final output
     * will be:
     * <pre>
     *     <code>
     *
     *         [5, 6, 7, ...]
     *     </code>
     * </pre>
     *
     * @param count  the number of data to skip.
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> skip(final int count) {
        return new SkipInvocationFactory<DATA>(count);
    }

    /**
     * Returns a factory of invocations sorting inputs in their natural order.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN extends Comparable<? super IN>> InvocationFactory<IN, IN> sort() {
        return SortInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations sorting input data by the specified comparator.
     *
     * @param comparator the comparator instance.
     * @param <DATA>     the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> sortBy(
            @NotNull final Comparator<? super DATA> comparator) {
        return new SortByInvocationFactory<DATA>(comparator);
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers.
     * <br>
     * The result will have the type matching the input with the highest precision.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Number> sum() {
        return (InvocationFactory<N, Number>) SumInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers by employing a
     * {@code BigDecimal}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, BigDecimal> sumBig() {
        return (InvocationFactory<N, BigDecimal>) SumBigInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers as a {@code byte}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Byte> sumByte() {
        return (InvocationFactory<N, Byte>) SumByteInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers as a {@code double}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Double> sumDouble() {
        return (InvocationFactory<N, Double>) SumDoubleInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers as a {@code float}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Float> sumFloat() {
        return (InvocationFactory<N, Float>) SumFloatInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers as an {@code int}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Integer> sumInteger() {
        return (InvocationFactory<N, Integer>) SumIntegerInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers as a {@code long}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Long> sumLong() {
        return (InvocationFactory<N, Long>) SumLongInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers as a {@code short}.
     *
     * @param <N> the number type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Short> sumShort() {
        return (InvocationFactory<N, Short>) SumShortInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations collecting inputs into a list.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN> InvocationFactory<? super IN, List<IN>> toList() {
        return ToListInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations collecting inputs into a map.
     *
     * @param keyFunction the key function.
     * @param <IN>        the input data type.
     * @param <KEY>       the map key type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN, KEY> InvocationFactory<? super IN, Map<KEY, IN>> toMap(
            @NotNull final Function<? super IN, KEY> keyFunction) {
        return new ToMapInvocationFactory<IN, KEY>(wrap(keyFunction));
    }

    /**
     * Returns a factory of invocations collecting inputs into a set.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <IN> InvocationFactory<? super IN, Set<IN>> toSet() {
        return ToSetInvocation.factoryOf();
    }

    /**
     * Returns a bi-consumer unfolding iterable inputs into the returned elements.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> InvocationFactory<Iterable<? extends IN>, IN> unfold() {
        return (InvocationFactory<Iterable<? extends IN>, IN>) sUnfoldInvocation;
    }

    /**
     * Returns a factory of invocations filtering inputs which are not unique.
     *
     * @param <DATA> the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> unique() {
        return UniqueInvocation.factoryOf();
    }
}
