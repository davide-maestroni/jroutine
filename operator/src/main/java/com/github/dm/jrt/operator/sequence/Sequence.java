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

package com.github.dm.jrt.operator.sequence;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.operator.math.Operation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.wrapBiFunction;
import static com.github.dm.jrt.function.util.FunctionDecorator.wrapFunction;
import static com.github.dm.jrt.operator.math.Numbers.getHigherPrecisionOperation;
import static com.github.dm.jrt.operator.math.Numbers.getOperation;

/**
 * Utility class providing producing sequences of data.
 * <p>
 * Created by davide-maestroni on 05/09/2017.
 *
 * @param <DATA> the data type.
 */
public abstract class Sequence<DATA> extends DeepEqualObject implements Consumer<Channel<DATA, ?>> {

  /**
   * Constructor.
   *
   * @param args the constructor arguments.
   */
  private Sequence(@Nullable final Object[] args) {
    super(args);
  }

  /**
   * Returns a consumer generating the specified range of data.
   * <br>
   * The generated data will start from the specified first one up to the specified last one, by
   * computing each next element through the specified function.
   * <br>
   * The endpoint values will be included or not in the range based on the specified type.
   *
   * @param endpoints         the type of endpoints inclusion.
   * @param start             the first element in the range.
   * @param end               the last element in the range.
   * @param incrementFunction the function incrementing the current element.
   * @param <DATA>            the data type.
   * @return the consumer instance.
   */
  @NotNull
  public static <DATA extends Comparable<? super DATA>> Sequence<DATA> range(
      @NotNull final EndpointsType endpoints, @NotNull final DATA start, @NotNull final DATA end,
      @NotNull final Function<DATA, DATA> incrementFunction) {
    return new RangeConsumer<DATA>(endpoints, start, end, incrementFunction);
  }

  /**
   * Returns a consumer generating the specified range of data.
   * <br>
   * The method returns the same sequence as the one returned by
   * {@link #range(EndpointsType, Comparable, Comparable, Function)} called with
   * {@link EndpointsType#INCLUSIVE} as first parameter.
   *
   * @param start             the first element in the range.
   * @param end               the last element in the range.
   * @param incrementFunction the function incrementing the current element.
   * @param <DATA>            the data type.
   * @return the consumer instance.
   */
  @NotNull
  public static <DATA extends Comparable<? super DATA>> Sequence<DATA> range(
      @NotNull final DATA start, @NotNull final DATA end,
      @NotNull final Function<DATA, DATA> incrementFunction) {
    return range(EndpointsType.INCLUSIVE, start, end, incrementFunction);
  }

  /**
   * Returns a consumer generating the specified range of numbers.
   * <br>
   * The stream will generate a range of numbers up to and including the {@code end} number, by
   * applying a default increment of {@code +1} or {@code -1} depending on the comparison between
   * the first and the last number. That is, if the first number is less than the last, the
   * increment will be {@code +1}. On the contrary, if the former is greater than the latter, the
   * increment will be {@code -1}.
   * <br>
   * The endpoint values will be included or not in the range based on the specified type.
   * <br>
   * Note that the {@code end} number will be returned only if the incremented value will exactly
   * match it.
   *
   * @param endpoints the type of endpoints inclusion.
   * @param start     the first number in the range.
   * @param end       the last number in the range.
   * @param <N>       the number type.
   * @return the consumer instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number> Sequence<N> range(@NotNull final EndpointsType endpoints,
      @NotNull final N start, @NotNull final N end) {
    final Operation<?> operation = getHigherPrecisionOperation(start.getClass(), end.getClass());
    return range(endpoints, start, end,
        (N) getOperation(start.getClass()).convert((operation.compare(start, end) <= 0) ? 1 : -1));
  }

  /**
   * Returns a consumer generating the specified range of numbers.
   * <br>
   * The stream will generate a range of numbers by applying the specified increment up to the
   * {@code end} number.
   * <br>
   * The endpoint values will be included or not in the range based on the specified type.
   * <br>
   * Note that the {@code end} number will be returned only if the incremented value will exactly
   * match it.
   *
   * @param endpoints the type of endpoints inclusion.
   * @param start     the first number in the range.
   * @param end       the last number in the range.
   * @param increment the increment to apply to the current number.
   * @param <N>       the number type.
   * @return the consumer instance.
   */
  @NotNull
  public static <N extends Number> Sequence<N> range(@NotNull final EndpointsType endpoints,
      @NotNull final N start, @NotNull final N end, @NotNull final N increment) {
    return new NumberRangeConsumer<N>(endpoints, start, end, increment);
  }

  /**
   * Returns a consumer generating the specified range of numbers.
   * <br>
   * The method returns the same sequence as the one returned by
   * {@link #range(EndpointsType, Number, Number)} called with {@link EndpointsType#INCLUSIVE} as
   * first parameter.
   *
   * @param start the first number in the range.
   * @param end   the last number in the range.
   * @param <N>   the number type.
   * @return the consumer instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number> Sequence<N> range(@NotNull final N start, @NotNull final N end) {
    return range(EndpointsType.INCLUSIVE, start, end);
  }

  /**
   * Returns a consumer generating the specified range of numbers.
   * <br>
   * The method returns the same sequence as the one returned by
   * {@link #range(EndpointsType, Number, Number, Number)} called with
   * {@link EndpointsType#INCLUSIVE} as first parameter.
   *
   * @param start     the first number in the range.
   * @param end       the last number in the range.
   * @param increment the increment to apply to the current number.
   * @param <N>       the number type.
   * @return the consumer instance.
   */
  @NotNull
  public static <N extends Number> Sequence<N> range(@NotNull final N start, @NotNull final N end,
      @NotNull final N increment) {
    return range(EndpointsType.INCLUSIVE, start, end, increment);
  }

  /**
   * Returns a consumer generating the specified sequence of data.
   * <br>
   * The generated data will start from the specified first and will produce the specified number
   * of elements, by computing each next one through the specified function.
   *
   * @param start        the first element of the sequence.
   * @param count        the size of the sequence.
   * @param nextFunction the function computing the next element.
   * @param <DATA>       the data type.
   * @return the consumer instance.
   * @throws java.lang.IllegalArgumentException if the count is not positive.
   */
  @NotNull
  public static <DATA> Sequence<DATA> sequence(@NotNull final DATA start, final long count,
      @NotNull final BiFunction<DATA, Long, DATA> nextFunction) {
    return new SequenceConsumer<DATA>(start, count, nextFunction);
  }

  /**
   * Enumeration defining whether the endpoints are included or not in the range.
   */
  public enum EndpointsType {

    /**
     * Inclusive endpoints.
     * <br>
     * Both starting and ending endpoints are included in the range.
     */
    INCLUSIVE(true, true),

    /**
     * Exclusive starting endpoint.
     * <br>
     * Only the ending endpoint is included in the range.
     */
    START_EXCLUSIVE(false, true),

    /**
     * Exclusive ending endpoint.
     * <br>
     * Only the starting endpoint is included in the range.
     */
    END_EXCLUSIVE(true, false),

    /**
     * Exclusive endpoints.
     * <br>
     * Both starting and ending endpoints are excluded from the range.
     */
    EXCLUSIVE(false, false);

    private final boolean mIsEndInclusive;

    private final boolean mIsStartInclusive;

    /**
     * Constructor.
     *
     * @param isStartInclusive if the starting endpoint is included in the range.
     * @param isEndInclusive   if the ending endpoint is included in the range.
     */
    EndpointsType(final boolean isStartInclusive, final boolean isEndInclusive) {
      mIsStartInclusive = isStartInclusive;
      mIsEndInclusive = isEndInclusive;
    }

    /**
     * Returns whether the ending endpoint is included in the range.
     *
     * @return if the ending endpoint is included in the range.
     */
    public boolean isEndInclusive() {
      return mIsEndInclusive;
    }

    /**
     * Returns whether the starting endpoint is included in the range.
     *
     * @return if the starting endpoint is included in the range.
     */
    public boolean isStartInclusive() {
      return mIsStartInclusive;
    }
  }

  /**
   * Consumer implementation generating a range of numbers.
   *
   * @param <N> the number type.
   */
  private static class NumberRangeConsumer<N extends Number> extends Sequence<N> {

    private final N mEnd;

    private final EndpointsType mEndpoints;

    private final N mIncrement;

    private final Operation<? extends Number> mOperation;

    private final N mStart;

    /**
     * Constructor.
     *
     * @param endpoints the endpoints type.
     * @param start     the first number in the range.
     * @param end       the last number in the range.
     * @param increment the increment.
     */
    private NumberRangeConsumer(@NotNull final EndpointsType endpoints, @NotNull final N start,
        @NotNull final N end, @NotNull final N increment) {
      super(asArgs(ConstantConditions.notNull("endpoints type", endpoints),
          ConstantConditions.notNull("start element", start),
          ConstantConditions.notNull("end element", end),
          ConstantConditions.notNull("increment", increment)));
      mEndpoints = endpoints;
      mStart = start;
      mEnd = end;
      mIncrement = increment;
      mOperation = getHigherPrecisionOperation(
          getHigherPrecisionOperation(start.getClass(), increment.getClass()).convert(0).getClass(),
          end.getClass());
    }

    @SuppressWarnings("unchecked")
    public void accept(final Channel<N, ?> result) {
      final N start = mStart;
      final N end = mEnd;
      final N increment = mIncrement;
      final EndpointsType endpoints = mEndpoints;
      final Operation<? extends Number> operation = mOperation;
      N current = (N) (endpoints.isStartInclusive() ? operation.convert(start)
          : operation.add(start, increment));
      if (operation.compare(start, end) <= 0) {
        while (operation.compare(current, end) < 0) {
          result.pass(current);
          current = (N) operation.add(current, increment);
        }

      } else {
        while (operation.compare(current, end) > 0) {
          result.pass(current);
          current = (N) operation.add(current, increment);
        }
      }

      if (endpoints.isEndInclusive() && (operation.compare(current, end) == 0)) {
        result.pass((N) operation.convert(end));
      }
    }
  }

  /**
   * Consumer implementation generating a range of data.
   *
   * @param <OUT> the output data type.
   */
  private static class RangeConsumer<OUT extends Comparable<? super OUT>> extends Sequence<OUT> {

    private final OUT mEnd;

    private final EndpointsType mEndpoints;

    private final Function<OUT, OUT> mIncrementFunction;

    private final OUT mStart;

    /**
     * Constructor.
     *
     * @param endpoints         the endpoints type.
     * @param start             the first element in the range.
     * @param end               the last element in the range.
     * @param incrementFunction the function incrementing the current element.
     */
    private RangeConsumer(@NotNull final EndpointsType endpoints, @NotNull final OUT start,
        @NotNull final OUT end, @NotNull final Function<OUT, OUT> incrementFunction) {
      super(asArgs(ConstantConditions.notNull("endpoints type", endpoints),
          ConstantConditions.notNull("start element", start),
          ConstantConditions.notNull("end element", end), wrapFunction(incrementFunction)));
      mEndpoints = endpoints;
      mStart = start;
      mEnd = end;
      mIncrementFunction = incrementFunction;
    }

    public void accept(final Channel<OUT, ?> result) throws Exception {
      final OUT start = mStart;
      final OUT end = mEnd;
      final Function<OUT, OUT> increment = mIncrementFunction;
      final EndpointsType endpoints = mEndpoints;
      OUT current = endpoints.isStartInclusive() ? start : increment.apply(start);
      if (start.compareTo(end) <= 0) {
        while (current.compareTo(end) < 0) {
          result.pass(current);
          current = increment.apply(current);
        }

      } else {
        while (current.compareTo(end) > 0) {
          result.pass(current);
          current = increment.apply(current);
        }
      }

      if (endpoints.isEndInclusive() && (current.compareTo(end) == 0)) {
        result.pass(current);
      }
    }
  }

  /**
   * Consumer implementation generating a sequence of data.
   *
   * @param <OUT> the output data type.
   */
  private static class SequenceConsumer<OUT> extends Sequence<OUT> {

    private final long mCount;

    private final BiFunction<OUT, Long, OUT> mNextFunction;

    private final OUT mStart;

    /**
     * Constructor.
     *
     * @param start        the first element of the sequence.
     * @param count        the size of the sequence.
     * @param nextFunction the function computing the next element.
     */
    private SequenceConsumer(@NotNull final OUT start, final long count,
        @NotNull final BiFunction<OUT, Long, OUT> nextFunction) {
      super(asArgs(ConstantConditions.notNull("start element", start),
          ConstantConditions.positive("sequence size", count), wrapBiFunction(nextFunction)));
      mStart = start;
      mCount = count;
      mNextFunction = nextFunction;
    }

    public void accept(final Channel<OUT, ?> result) throws Exception {
      final BiFunction<OUT, Long, OUT> next = mNextFunction;
      OUT current = mStart;
      final long last = mCount - 1;
      if (last >= 0) {
        result.pass(current);
      }

      for (long i = 0; i < last; ++i) {
        current = next.apply(current, i);
        result.pass(current);
      }
    }
  }
}
