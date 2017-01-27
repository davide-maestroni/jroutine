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
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionDecorator;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionDecorator;
import com.github.dm.jrt.operator.math.Operation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.operator.math.Numbers.getHigherPrecisionOperationSafe;
import static com.github.dm.jrt.operator.math.Numbers.getOperationSafe;

/**
 * Utility class providing functions that produce sequences of data.
 * <p>
 * Created by davide-maestroni on 07/02/2016.
 */
public class Sequences {

  /**
   * Avoid explicit instantiation.
   */
  protected Sequences() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a consumer generating the specified range of data.
   * <br>
   * The generated data will start from the specified first one up to and including the specified
   * last one, by computing each next element through the specified function.
   *
   * @param start             the first element in the range.
   * @param end               the last element in the range.
   * @param incrementFunction the function incrementing the current element.
   * @param <DATA>            the data type.
   * @return the consumer instance.
   */
  @NotNull
  public static <DATA extends Comparable<? super DATA>> Consumer<Channel<DATA, ?>> range(
      @NotNull final DATA start, @NotNull final DATA end,
      @NotNull final Function<DATA, DATA> incrementFunction) {
    return new RangeConsumer<DATA>(ConstantConditions.notNull("start element", start),
        ConstantConditions.notNull("end element", end),
        FunctionDecorator.decorate(incrementFunction));
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
   * Note that the {@code end} number will be returned only if the incremented value will exactly
   * match it.
   *
   * @param start the first number in the range.
   * @param end   the last number in the range.
   * @param <N>   the number type.
   * @return the consumer instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number> Consumer<Channel<N, ?>> range(@NotNull final N start,
      @NotNull final N end) {
    final Operation<?> operation =
        getHigherPrecisionOperationSafe(start.getClass(), end.getClass());
    return range(start, end, (N) getOperationSafe(start.getClass()).convert(
        (operation.compare(start, end) <= 0) ? 1 : -1));
  }

  /**
   * Returns a consumer generating the specified range of numbers.
   * <br>
   * The stream will generate a range of numbers by applying the specified increment up to and
   * including the {@code end} number.
   * <br>
   * Note that the {@code end} number will be returned only if the incremented value will exactly
   * match it.
   *
   * @param start     the first number in the range.
   * @param end       the last number in the range.
   * @param increment the increment to apply to the current number.
   * @param <N>       the number type.
   * @return the consumer instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number> Consumer<Channel<N, ?>> range(@NotNull final N start,
      @NotNull final N end, @NotNull final N increment) {
    return new NumberRangeConsumer<N>(start, end, increment);
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
   * @param <DATA>       the data type.
   * @return the consumer instance.
   * @throws java.lang.IllegalArgumentException if the count is not positive.
   */
  @NotNull
  public static <DATA> Consumer<Channel<DATA, ?>> sequence(@NotNull final DATA start,
      final long count, @NotNull final BiFunction<DATA, Long, DATA> nextFunction) {
    return new SequenceConsumer<DATA>(ConstantConditions.notNull("start element", start),
        ConstantConditions.positive("sequence size", count),
        BiFunctionDecorator.decorate(nextFunction));
  }

  /**
   * Consumer implementation generating a range of numbers.
   *
   * @param <N> the number type.
   */
  private static class NumberRangeConsumer<N extends Number> extends DeepEqualObject
      implements Consumer<Channel<N, ?>> {

    private final Operation<? extends Number> mAddOperation;

    private final Operation<? extends Number> mCompareOperation;

    private final N mEnd;

    private final N mIncrement;

    private final N mStart;

    /**
     * Constructor.
     *
     * @param start     the first number in the range.
     * @param end       the last number in the range.
     * @param increment the increment.
     */
    private NumberRangeConsumer(@NotNull final N start, @NotNull final N end,
        @NotNull final N increment) {
      super(asArgs(start, end, increment));
      mStart = start;
      mEnd = end;
      mIncrement = increment;
      final Operation<?> addOperation =
          (mAddOperation = getHigherPrecisionOperationSafe(start.getClass(), increment.getClass()));
      mCompareOperation =
          getHigherPrecisionOperationSafe(addOperation.convert(0).getClass(), end.getClass());
    }

    public void accept(final Channel<N, ?> result) throws Exception {
      final N start = mStart;
      final N end = mEnd;
      final N increment = mIncrement;
      final Operation<? extends Number> addOperation = mAddOperation;
      final Operation<? extends Number> compareOperation = mCompareOperation;
      N current = start;
      if (compareOperation.compare(start, end) <= 0) {
        while (compareOperation.compare(current, end) < 0) {
          result.pass(current);
          current = (N) addOperation.add(current, increment);
        }

        if (compareOperation.compare(current, end) == 0) {
          result.pass(end);
        }

      } else {
        while (compareOperation.compare(current, end) > 0) {
          result.pass(current);
          current = (N) addOperation.add(current, increment);
        }

        if (compareOperation.compare(current, end) == 0) {
          result.pass(end);
        }
      }
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
     * @param start             the first element in the range.
     * @param end               the last element in the range.
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
