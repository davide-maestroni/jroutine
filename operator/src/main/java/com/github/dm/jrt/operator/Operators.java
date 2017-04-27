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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.ActionDecorator;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.BiFunctionDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.PredicateDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility class providing several operators implemented as invocation factories.
 * <p>
 * Created by davide-maestroni on 06/13/2016.
 */
@SuppressWarnings("WeakerAccess")
public class Operators {

  private static final BiConsumer<? extends Collection<?>, ?> sCollectConsumer =
      new BiConsumer<Collection<Object>, Object>() {

        public void accept(final Collection<Object> outs, final Object out) {
          outs.add(out);
        }
      };

  @SuppressWarnings("unchecked")
  private static final InvocationFactory<?, ?> sMax = binary(BiFunctionDecorator.max());

  @SuppressWarnings("unchecked")
  private static final InvocationFactory<?, ?> sMin = binary(BiFunctionDecorator.min());

  private static final InvocationFactory<?, ?> sNone =
      new PredicateMappingInvocation<Object>(PredicateDecorator.negative());

  private static final InvocationFactory<?, ?> sNotNull =
      new PredicateMappingInvocation<Object>(PredicateDecorator.isNotNull());

  private static final InvocationFactory<?, ?> sNull =
      new PredicateMappingInvocation<Object>(PredicateDecorator.isNull());

  private static final MappingInvocation<? extends Iterable<?>, ?> sUnfoldInvocation =
      new MappingInvocation<Iterable<?>, Object>(null) {

        @SuppressWarnings("unchecked")
        public void onInput(final Iterable<?> input, @NotNull final Channel<Object, ?> result) {
          result.pass((Iterable<Object>) input);
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
    return new AllMatchInvocationFactory<IN>(PredicateDecorator.decorate(predicate));
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
    return new AnyMatchInvocationFactory<IN>(PredicateDecorator.decorate(predicate));
  }

  /**
   * Returns a factory of invocations appending the specified output to the invocation ones.
   *
   * @param output the output.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> append(@Nullable final DATA output) {
    return new AppendOutputInvocation<DATA>(Collections.singletonList(output));
  }

  /**
   * Returns a factory of invocations appending the specified outputs to the invocation ones.
   *
   * @param outputs the outputs.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> append(@Nullable final DATA... outputs) {
    return new AppendOutputInvocation<DATA>(toList(outputs));
  }

  /**
   * Returns a factory of invocations appending the outputs returned by the specified iterable to
   * the invocation ones.
   *
   * @param outputs the iterable returning the output data.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> append(
      @Nullable final Iterable<? extends DATA> outputs) {
    return new AppendOutputInvocation<DATA>(toList(outputs));
  }

  /**
   * Returns a factory of invocations appending the outputs returned by the specified consumer to
   * the invocation ones.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The consumer will be called {@code count} number of times only when the routine invocation
   * completes. The count number must be positive.
   *
   * @param count           the number of generated outputs.
   * @param outputsConsumer the consumer instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   * @see com.github.dm.jrt.operator.sequence.Sequences Sequences
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> appendAccept(final long count,
      @NotNull final Consumer<? super Channel<DATA, ?>> outputsConsumer) {
    return new AppendConsumerInvocation<DATA>(count, ConsumerDecorator.decorate(outputsConsumer));
  }

  /**
   * Returns a factory of invocations appending the outputs returned by the specified consumer to
   * the invocation ones.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The consumer will be called only when the routine invocation completes.
   *
   * @param outputsConsumer the consumer instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   * @see com.github.dm.jrt.operator.sequence.Sequences Sequences
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> appendAccept(
      @NotNull final Consumer<? super Channel<DATA, ?>> outputsConsumer) {
    return appendAccept(1, outputsConsumer);
  }

  /**
   * Returns a factory of invocations appending the outputs returned by the specified supplier to
   * the invocation ones.
   * <br>
   * The supplier will be called {@code count} number of times only when the routine invocation
   * completes. The count number must be positive.
   *
   * @param count          the number of generated outputs.
   * @param outputSupplier the supplier instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> appendGet(final long count,
      @NotNull final Supplier<? extends DATA> outputSupplier) {
    return new AppendSupplierInvocation<DATA>(count, SupplierDecorator.decorate(outputSupplier));
  }

  /**
   * Returns a factory of invocations appending the outputs returned by the specified supplier to
   * the invocation ones.
   * <br>
   * The supplier will be called only when the routine invocation completes.
   *
   * @param outputSupplier the supplier instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> appendGet(
      @NotNull final Supplier<? extends DATA> outputSupplier) {
    return appendGet(1, outputSupplier);
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
   * Returns a factory of invocations computing the average value of the input numbers, producing
   * an output of the specified type.
   *
   * @param type  the output type.
   * @param <N>   the number type.
   * @param <OUT> the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number, OUT extends Number> InvocationFactory<N, OUT> average(
      @NotNull final Class<OUT> type) {
    return (InvocationFactory<N, OUT>) new AverageOutputPrecisionInvocationFactory<OUT>(type);
  }

  /**
   * Returns a factory of invocations computing the average value of the input numbers, employing
   * a sum value and producing an output of the specified types.
   *
   * @param sumType the sum type.
   * @param outType the output type.
   * @param <N>     the number type.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number, IN extends Number, OUT extends Number> InvocationFactory<N,
      OUT> average(
      @NotNull final Class<IN> sumType, @NotNull final Class<OUT> outType) {
    return (InvocationFactory<N, OUT>) new AverageInputPrecisionInvocationFactory<IN, OUT>(sumType,
        outType);
  }

  /**
   * Returns a factory of invocations accumulating a value by applying the specified function
   * instance.
   *
   * @param binaryFunction the binary function.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> binary(
      @NotNull final BiFunction<DATA, DATA, DATA> binaryFunction) {
    return BinaryOperatorInvocation.functionFactory(binaryFunction);
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
    return new FunctionMappingInvocation<IN, OUT>(FunctionDecorator.castTo(type));
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
   * Returns a factory of invocations accumulating data through the specified consumer.
   * <p>
   * The output will be computed as follows:
   * <pre><code>
   * consumer.accept(acc, input);
   * </code></pre>
   * where the initial accumulated value will be the the first input.
   * <br>
   * The accumulated value will be passed as result only when the invocation completes.
   *
   * @param accumulateConsumer the bi-consumer instance.
   * @param <DATA>             the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> collect(
      @NotNull final BiConsumer<? super DATA, ? super DATA> accumulateConsumer) {
    return AccumulateConsumerInvocation.consumerFactory(accumulateConsumer);
  }

  /**
   * Returns a factory of invocations accumulating data through the specified consumer.
   * <p>
   * The output will be computed as follows:
   * <pre><code>
   * consumer.accept(acc, input);
   * </code></pre>
   * where the initial accumulated value will be the one returned by the specified supplier.
   * <br>
   * The accumulated value will be passed as result only when the invocation completes.
   *
   * @param seedSupplier       the supplier of initial accumulation values.
   * @param accumulateConsumer the bi-consumer instance.
   * @param <IN>               the input data type.
   * @param <OUT>              the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> collect(
      @NotNull final Supplier<? extends OUT> seedSupplier,
      @NotNull final BiConsumer<? super OUT, ? super IN> accumulateConsumer) {
    return AccumulateConsumerInvocation.consumerFactory(seedSupplier, accumulateConsumer);
  }

  /**
   * Returns a factory of invocations accumulating the outputs by adding them to the collections
   * returned by the specified supplier.
   * <br>
   * The accumulated value will be passed as result only when the invocation completes.
   *
   * @param collectionSupplier the supplier of collections.
   * @param <IN>               the input data type.
   * @param <OUT>              the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN, OUT extends Collection<IN>> InvocationFactory<IN, OUT> collectInto(
      @NotNull final Supplier<? extends OUT> collectionSupplier) {
    return collect(collectionSupplier, (BiConsumer<? super OUT, ? super IN>) sCollectConsumer);
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
   * Returns a factory of invocations filtering out inputs which are not unique (according to the
   * {@code equals(Object)} method).
   *
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> distinct() {
    return DistinctInvocation.factoryOf();
  }

  /**
   * Returns a factory of invocations filtering out inputs which are not unique (according to
   * identity comparison).
   *
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> distinctIdentity() {
    return DistinctIdentityInvocation.factoryOf();
  }

  /**
   * Returns a factory of invocations filtering data based on the values returned by the specified
   * predicate.
   *
   * @param filterPredicate the predicate instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> filter(
      @NotNull final Predicate<? super DATA> filterPredicate) {
    return new PredicateMappingInvocation<DATA>(PredicateDecorator.decorate(filterPredicate));
  }

  /**
   * Returns a factory of invocations grouping the input data in collections where all the data
   * correspond to the same key.
   * <br>
   * The returned keys will be compared for equalities by employing the {@code equals()} method.
   * <p>
   * Given a numeric sequence of inputs starting from 0, and a key function returning the modulo
   * 2 of such numbers, the final output will be:
   * <pre><code>
   * =&gt; [(0, 2, 4, 6, 8, ..., N), (1, 3, 5, 7, 9, ..., N + 1)]
   * </code></pre>
   * <p>
   * Note that the groups will be produced only after the invocation completes.
   * <br>
   * Note also that the group order is not guaranteed.
   *
   * @param keyFunction the function returning a key object for each input.
   * @param <DATA>      the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(
      @NotNull final Function<DATA, Object> keyFunction) {
    return new GroupByFunctionInvocationFactory<DATA>(FunctionDecorator.decorate(keyFunction));
  }

  /**
   * Returns a factory of invocations grouping the input data in collections of the specified size.
   * <p>
   * Given a numeric sequence of inputs starting from 0, and a size of 3, the final output will be:
   * <pre><code>
   * =&gt; [(0, 1, 2), (3, 4, 5), ..., (N, N + 1)]
   * </code></pre>
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
   * Returns a factory of invocations grouping the input data in collections of the specified size.
   * <br>
   * If the inputs complete and the last group length is less than the target size, the missing
   * spaces will be filled with the specified placeholder instance.
   * <p>
   * Given a numeric sequence of inputs starting from 0, and a size of 3, the final output will be:
   * <pre><code>
   * =? [(0, 1, 2), (3, 4, 5), ..., (N, N + 1, placeholder)]
   * </code></pre>
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
    return IdentityInvocation.factory();
  }

  /**
   * Returns a factory of invocations passing on data after an interval specified by a backoff
   * policy.
   *
   * @param backoff the backoff policy instance.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> interval(@NotNull final Backoff backoff) {
    return new IntervalInvocationFactory<DATA>(backoff);
  }

  /**
   * Returns a factory of invocations passing on data after the specified time interval.
   * <p>
   * Note that this is the same as calling
   * {@code interval(BackoffBuilder.afterCount(1).linearDelay(delay, timeUnit))}.
   *
   * @param delay    the delay value.
   * @param timeUnit the delay time unit.
   * @param <DATA>   the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> interval(final long delay,
      @NotNull final TimeUnit timeUnit) {
    return interval(BackoffBuilder.afterCount(1).linearDelay(delay, timeUnit));
  }

  /**
   * Returns a factory of invocations passing on data after the specified time interval.
   * <p>
   * Note that this is the same as calling
   * {@code interval(BackoffBuilder.afterCount(1).linearDelay(delay))}.
   *
   * @param delay  the delay.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> interval(
      @NotNull final DurationMeasure delay) {
    return interval(delay.value, delay.unit);
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

    return filter(PredicateDecorator.isEqualTo(targetRef));
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
    return filter(PredicateDecorator.isInstanceOf(type));
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

    return filter(PredicateDecorator.isEqualTo(targetRef).negate());
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
  public static <DATA> InvocationFactory<DATA, DATA> isNotInstanceOf(@NotNull final Class<?> type) {
    return filter(PredicateDecorator.isInstanceOf(type).negate());
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
   * Returns factory of invocations filtering out all the inputs that are the same as the specified
   * object.
   *
   * @param targetRef the target reference.
   * @param <DATA>    the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> isNotSameAs(@Nullable final Object targetRef) {
    if (targetRef == null) {
      return isNotNull();
    }

    return filter(PredicateDecorator.isSameAs(targetRef).negate());
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

    return filter(PredicateDecorator.isSameAs(targetRef));
  }

  /**
   * Returns a factory of invocations passing at max the specified number of input data and
   * discarding the following ones.
   * <p>
   * Given a numeric sequence of inputs starting from 0, and a limit count of 5, the final output
   * will be:
   * <pre><code>
   * =&gt; [0, 1, 2, 3, 4]
   * </code></pre>
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
   * Returns a factory of invocations passing at max the specified number of input data and
   * discarding the previous ones.
   * <p>
   * Given a numeric sequence of inputs from 0 to 9, and a limit count of 5, the final output
   * will be:
   * <pre><code>
   * =&gt; [5, 6, 7, 8, 9]
   * </code></pre>
   *
   * @param count  the maximum number of data to pass.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the count is negative.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> limitLast(final int count) {
    return new LimitLastInvocationFactory<DATA>(count);
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
   * Returns a factory of invocations returning the greater of the inputs as per the specified
   * comparator.
   *
   * @param comparator the comparator instance.
   * @param <DATA>     the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> maxBy(
      @NotNull final Comparator<? super DATA> comparator) {
    return binary(BiFunctionDecorator.maxBy(comparator));
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
   * Returns a factory of invocations returning the smaller of the inputs as per the specified
   * comparator.
   *
   * @param comparator the comparator instance.
   * @param <DATA>     the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> minBy(
      @NotNull final Comparator<? super DATA> comparator) {
    return binary(BiFunctionDecorator.minBy(comparator));
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
    return new AllMatchInvocationFactory<IN>(PredicateDecorator.decorate(predicate).negate());
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
    return new AnyMatchInvocationFactory<IN>(PredicateDecorator.decorate(predicate).negate());
  }

  /**
   * Returns a factory of invocations producing the specified output in case the invocation
   * produced none.
   *
   * @param output the output.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElse(@Nullable final DATA output) {
    return new OrElseInvocationFactory<DATA>(Collections.singletonList(output));
  }

  /**
   * Returns a factory of invocations producing the specified outputs in case the invocation
   * produced none.
   *
   * @param outputs the outputs.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElse(@Nullable final DATA... outputs) {
    return new OrElseInvocationFactory<DATA>(toList(outputs));
  }

  /**
   * Returns a factory of invocations producing the outputs returned by the specified iterable in
   * case the invocation produced none.
   *
   * @param outputs the iterable returning the output data.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElse(
      @Nullable final Iterable<? extends DATA> outputs) {
    return new OrElseInvocationFactory<DATA>(toList(outputs));
  }

  /**
   * Returns a factory of invocations producing the outputs returned by the specified consumer in
   * case the invocation produced none.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The consumer will be called {@code count} number of times only when the routine invocation
   * completes. The count number must be positive.
   *
   * @param count           the number of generated outputs.
   * @param outputsConsumer the consumer instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElseAccept(final long count,
      @NotNull final Consumer<? super Channel<DATA, ?>> outputsConsumer) {
    return new OrElseConsumerInvocationFactory<DATA>(count,
        ConsumerDecorator.decorate(outputsConsumer));
  }

  /**
   * Returns a factory of invocations producing the outputs returned by the specified consumer in
   * case the invocation produced none.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The consumer will be called only when the routine invocation completes.
   *
   * @param outputsConsumer the consumer instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElseAccept(
      @NotNull final Consumer<? super Channel<DATA, ?>> outputsConsumer) {
    return orElseAccept(1, outputsConsumer);
  }

  /**
   * Returns a factory of invocations producing the outputs returned by the specified supplier in
   * case the invocation produced none.
   * <br>
   * The supplier will be called {@code count} number of times only when the routine invocation
   * completes. The count number must be positive.
   *
   * @param count          the number of generated outputs.
   * @param outputSupplier the supplier instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElseGet(final long count,
      @NotNull final Supplier<? extends DATA> outputSupplier) {
    return new OrElseSupplierInvocationFactory<DATA>(count,
        SupplierDecorator.decorate(outputSupplier));
  }

  /**
   * Returns a factory of invocations producing the outputs returned by the specified supplier in
   * case the invocation produced none.
   * <br>
   * The supplier will be called only when the routine invocation completes.
   *
   * @param outputSupplier the supplier instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElseGet(
      @NotNull final Supplier<? extends DATA> outputSupplier) {
    return orElseGet(1, outputSupplier);
  }

  /**
   * Returns a factory of invocations aborting the execution with the specified error in case the
   * invocation produced no result.
   *
   * @param error  the error.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> orElseThrow(@NotNull final Throwable error) {
    return new OrElseThrowInvocationFactory<DATA>(error);
  }

  /**
   * Returns a factory of invocations performing the specified action when the routine invocation
   * completes.
   * <br>
   * Outputs will be automatically passed on, while the invocation will be aborted if an exception
   * escapes the consumer.
   *
   * @param completeAction the action instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> peekComplete(
      @NotNull final Action completeAction) {
    return new PeekCompleteInvocation<DATA>(ActionDecorator.decorate(completeAction));
  }

  /**
   * Returns a factory of invocations peeking the stream errors as they are passed on.
   * <br>
   * Outputs will be automatically passed on, while the invocation will be aborted if an exception
   * escapes the consumer.
   *
   * @param errorConsumer the consumer instance.
   * @param <DATA>        the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> peekError(
      @NotNull final Consumer<? super RoutineException> errorConsumer) {
    return new PeekErrorInvocationFactory<DATA>(ConsumerDecorator.decorate(errorConsumer));
  }

  /**
   * Returns a factory of invocations peeking the stream data as they are passed on.
   * <br>
   * Outputs will be automatically passed on, while the invocation will be aborted if an exception
   * escapes the consumer.
   *
   * @param outputConsumer the consumer instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> peekOutput(
      @NotNull final Consumer<? super DATA> outputConsumer) {
    return new PeekOutputInvocation<DATA>(ConsumerDecorator.decorate(outputConsumer));
  }

  /**
   * Returns a factory of invocations prepending the specified output to the invocation ones.
   * <br>
   * If no input is passed to the invocation, the output will be produced only when the invocation
   * completes.
   *
   * @param output the output.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prepend(@Nullable final DATA output) {
    return new PrependOutputInvocationFactory<DATA>(Collections.singletonList(output));
  }

  /**
   * Returns a factory of invocations prepending the specified outputs to the invocation ones.
   * <br>
   * If no input is passed to the invocation, the outputs will be produced only when the invocation
   * completes.
   *
   * @param outputs the outputs.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prepend(@Nullable final DATA... outputs) {
    return new PrependOutputInvocationFactory<DATA>(toList(outputs));
  }

  /**
   * Returns a factory of invocations prepending the outputs returned by the specified iterable to
   * the invocation ones.
   * <br>
   * If no input is passed to the invocation, the outputs will be produced only when the invocation
   * completes.
   *
   * @param outputs the iterable returning the output data.
   * @param <DATA>  the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prepend(
      @Nullable final Iterable<? extends DATA> outputs) {
    return new PrependOutputInvocationFactory<DATA>(toList(outputs));
  }

  /**
   * Returns a factory of invocations prepending the outputs returned by the specified consumer to
   * the invocation ones.
   * <br>
   * If no input is passed to the invocation, the outputs will be produced only when the invocation
   * completes.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The consumer will be called {@code count} number of times. The count number must be positive.
   *
   * @param count           the number of generated outputs.
   * @param outputsConsumer the consumer instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   * @see com.github.dm.jrt.operator.sequence.Sequences Sequences
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prependAccept(final long count,
      @NotNull final Consumer<? super Channel<DATA, ?>> outputsConsumer) {
    return new PrependConsumerInvocationFactory<DATA>(count,
        ConsumerDecorator.decorate(outputsConsumer));
  }

  /**
   * Returns a factory of invocations prepending the outputs returned by the specified consumer to
   * the invocation ones.
   * <br>
   * If no input is passed to the invocation, the outputs will be produced only when the invocation
   * completes.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   *
   * @param outputsConsumer the consumer instance.
   * @param <DATA>          the data type.
   * @return the invocation factory instance.
   * @see com.github.dm.jrt.operator.sequence.Sequences Sequences
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prependAccept(
      @NotNull final Consumer<? super Channel<DATA, ?>> outputsConsumer) {
    return prependAccept(1, outputsConsumer);
  }

  /**
   * Returns a factory of invocations prepending the outputs returned by the specified supplier to
   * the invocation ones.
   * <br>
   * If no input is passed to the invocation, the outputs will be produced only when the invocation
   * completes.
   * <br>
   * The supplier will be called {@code count} number of times. The count number must be positive.
   *
   * @param count          the number of generated outputs.
   * @param outputSupplier the supplier instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prependGet(final long count,
      @NotNull final Supplier<? extends DATA> outputSupplier) {
    return new PrependSupplierInvocationFactory<DATA>(count,
        SupplierDecorator.decorate(outputSupplier));
  }

  /**
   * Returns a factory of invocations prepending the outputs returned by the specified supplier to
   * the invocation ones.
   * <br>
   * If no input is passed to the invocation, the outputs will be produced only when the invocation
   * completes.
   *
   * @param outputSupplier the supplier instance.
   * @param <DATA>         the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> prependGet(
      @NotNull final Supplier<? extends DATA> outputSupplier) {
    return prependGet(1, outputSupplier);
  }

  /**
   * Returns a factory of invocations accumulating data through the specified function.
   * <p>
   * The output will be computed as follows:
   * <pre><code>
   * acc = function.apply(acc, input);
   * </code></pre>
   * where the initial accumulated value will be the the first input.
   * <br>
   * The accumulated value will be passed as result only when the invocation completes.
   *
   * @param accumulateFunction the bi-function instance.
   * @param <DATA>             the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> reduce(
      @NotNull final BiFunction<? super DATA, ? super DATA, ? extends DATA> accumulateFunction) {
    return AccumulateFunctionInvocation.functionFactory(accumulateFunction);
  }

  /**
   * Returns a factory of invocations accumulating data through the specified function.
   * <p>
   * The output will be computed as follows:
   * <pre><code>
   * acc = function.apply(acc, input);
   * </code></pre>
   * where the initial accumulated value will be the one returned by the specified supplier.
   * <br>
   * The accumulated value will be passed as result only when the invocation completes.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param seedSupplier       the supplier of initial accumulation values.
   * @param accumulateFunction the bi-function instance.
   * @param <IN>               the input data type.
   * @param <OUT>              the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> reduce(
      @NotNull final Supplier<? extends OUT> seedSupplier,
      @NotNull final BiFunction<? super OUT, ? super IN, ? extends OUT> accumulateFunction) {
    return AccumulateFunctionInvocation.functionFactory(seedSupplier, accumulateFunction);
  }

  /**
   * Returns a factory of invocations replacing all the data equal to the specified target with
   * the passed replacement.
   *
   * @param target      the target instance.
   * @param replacement the replacement instance.
   * @param <DATA>      the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replace(@Nullable final DATA target,
      @Nullable final DATA replacement) {
    return replaceIf(
        (target != null) ? PredicateDecorator.isEqualTo(target) : PredicateDecorator.isNull(),
        replacement);
  }

  /**
   * Returns a factory of invocations replacing all the data equal to the specified target with
   * the outputs published by the passed consumer.
   *
   * @param target              the target instance.
   * @param replacementConsumer the replacement consumer instance.
   * @param <DATA>              the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replaceAccept(@Nullable final DATA target,
      @NotNull final BiConsumer<DATA, ? super Channel<DATA, ?>> replacementConsumer) {
    return replaceIfAccept(
        (target != null) ? PredicateDecorator.isEqualTo(target) : PredicateDecorator.isNull(),
        BiConsumerDecorator.decorate(replacementConsumer));
  }

  /**
   * Returns a factory of invocations replacing all the data equal to the specified target with
   * the outputs returned by the passed function.
   *
   * @param target              the target instance.
   * @param replacementFunction the replacement function instance.
   * @param <DATA>              the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replaceApply(@Nullable final DATA target,
      @NotNull final Function<DATA, ? extends DATA> replacementFunction) {
    return replaceIfApply(
        (target != null) ? PredicateDecorator.isEqualTo(target) : PredicateDecorator.isNull(),
        FunctionDecorator.decorate(replacementFunction));
  }

  /**
   * Returns a factory of invocations replacing all the data satisfying the specified predicate
   * with the passed replacement.
   *
   * @param predicate   the predicate instance.
   * @param replacement the replacement instance.
   * @param <DATA>      the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <DATA> InvocationFactory<DATA, DATA> replaceIf(
      @NotNull final Predicate<? super DATA> predicate, @Nullable final DATA replacement) {
    return new ReplaceInvocation<DATA>(PredicateDecorator.decorate(predicate), replacement);
  }

  /**
   * Returns a factory of invocations replacing all the data satisfying the specified predicate
   * with the outputs published by the passed consumer.
   *
   * @param predicate           the predicate instance.
   * @param replacementConsumer the replacement consumer instance.
   * @param <DATA>              the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <DATA> InvocationFactory<DATA, DATA> replaceIfAccept(
      @NotNull final Predicate<? super DATA> predicate,
      @NotNull final BiConsumer<DATA, ? super Channel<DATA, ?>> replacementConsumer) {
    return new ReplaceConsumerInvocation<DATA>(
        PredicateDecorator.decorate((Predicate<Object>) predicate),
        BiConsumerDecorator.decorate(replacementConsumer));
  }

  /**
   * Returns a factory of invocations replacing all the data satisfying the specified predicate
   * with the outputs returned by the passed function.
   *
   * @param predicate           the predicate instance.
   * @param replacementFunction the replacement function instance.
   * @param <DATA>              the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replaceIfApply(
      @NotNull final Predicate<? super DATA> predicate,
      @NotNull final Function<DATA, ? extends DATA> replacementFunction) {
    return new ReplaceFunctionInvocation<DATA>(PredicateDecorator.decorate(predicate),
        FunctionDecorator.decorate(replacementFunction));
  }

  /**
   * Returns a factory of invocations replacing all the data that are the same instance as the
   * specified target with the passed replacement.
   *
   * @param target      the target instance.
   * @param replacement the replacement instance.
   * @param <DATA>      the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replaceSame(@Nullable final DATA target,
      @Nullable final DATA replacement) {
    return replaceIf(
        (target != null) ? PredicateDecorator.isSameAs(target) : PredicateDecorator.isNull(),
        replacement);
  }

  /**
   * Returns a factory of invocations replacing all the data that are the same instance as the
   * specified target with the outputs published by the passed consumer.
   *
   * @param target              the target instance.
   * @param replacementConsumer the replacement consumer instance.
   * @param <DATA>              the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replaceSameAccept(@Nullable final DATA target,
      @NotNull final BiConsumer<DATA, ? super Channel<DATA, ?>> replacementConsumer) {
    return replaceIfAccept(
        (target != null) ? PredicateDecorator.isSameAs(target) : PredicateDecorator.isNull(),
        BiConsumerDecorator.decorate(replacementConsumer));
  }

  /**
   * Returns a factory of invocations replacing all the data that are the same instance as the
   * specified target with the outputs returned by the passed function.
   *
   * @param target              the target instance.
   * @param replacementFunction the replacement function instance.
   * @param <DATA>              the data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> replaceSameApply(@Nullable final DATA target,
      @NotNull final Function<DATA, ? extends DATA> replacementFunction) {
    return replaceIfApply(
        (target != null) ? PredicateDecorator.isSameAs(target) : PredicateDecorator.isNull(),
        FunctionDecorator.decorate(replacementFunction));
  }

  /**
   * Returns a factory of invocations skipping the specified number of input data.
   * <p>
   * Given a numeric sequence of inputs starting from 0, and a skip count of 5, the final output
   * will be:
   * <pre><code>
   * =&gt; [5, 6, 7, ...]
   * </code></pre>
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
   * Returns a factory of invocations skipping the specified number of last input data.
   * <p>
   * Given a numeric sequence of inputs from 0 to 9, and a skip count of 5, the final output
   * will be:
   * <pre><code>
   * =&gt; [0, 1, 2, 3, 4]
   * </code></pre>
   *
   * @param count  the number of data to skip.
   * @param <DATA> the data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the count is negative.
   */
  @NotNull
  public static <DATA> InvocationFactory<DATA, DATA> skipLast(final int count) {
    return new SkipLastInvocationFactory<DATA>(count);
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
   * Returns a factory of invocations computing the sum of the input numbers as an instance of the
   * specified output type.
   *
   * @param type  the sum type.
   * @param <N>   the number type.
   * @param <OUT> the output data type.
   * @return the invocation factory instance.
   */
  @SuppressWarnings("unchecked")
  public static <N extends Number, OUT extends Number> InvocationFactory<N, OUT> sum(
      @NotNull final Class<OUT> type) {
    return (InvocationFactory<N, OUT>) new SumPrecisionInvocationFactory<OUT>(type);
  }

  /**
   * Returns a factory of invocations generating the specified output after the invocation
   * completes.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param output the output.
   * @param <IN>   the input data type.
   * @param <OUT>  the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> then(@Nullable final OUT output) {
    return new ThenOutputInvocation<IN, OUT>(Collections.singletonList(output));
  }

  /**
   * Returns a factory of invocations generating the specified outputs after the invocation
   * completes.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param outputs the outputs.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> then(@Nullable final OUT... outputs) {
    return new ThenOutputInvocation<IN, OUT>(toList(outputs));
  }

  /**
   * Returns a factory of invocations generating the outputs returned by the specified iterable
   * after the invocation completes.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param outputs the iterable returning the output data.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> then(
      @Nullable final Iterable<? extends OUT> outputs) {
    return new ThenOutputInvocation<IN, OUT>(toList(outputs));
  }

  /**
   * Returns a factory of invocations generating the outputs returned by the specified consumer
   * after the invocation completes.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param outputsConsumer the consumer instance.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> thenAccept(
      @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
    return thenAccept(1, outputsConsumer);
  }

  /**
   * Returns a factory of invocations generating the outputs returned by the specified consumer
   * after the invocation completes.
   * <br>
   * The result channel will be passed to the consumer, so that multiple or no results may be
   * generated.
   * <br>
   * The consumer will be called {@code count} number of times. The count number must be positive.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param count           the number of generated outputs.
   * @param outputsConsumer the consumer instance.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> thenAccept(final long count,
      @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
    return new ThenConsumerInvocation<IN, OUT>(count, ConsumerDecorator.decorate(outputsConsumer));
  }

  /**
   * Returns a factory of invocations generating the outputs returned by the specified supplier
   * after the invocation completes.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param outputSupplier the supplier instance.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> thenGet(
      @NotNull final Supplier<? extends OUT> outputSupplier) {
    return thenGet(1, outputSupplier);
  }

  /**
   * Returns a factory of invocations generating the outputs returned by the specified supplier
   * after the invocation completes.
   * <br>
   * The supplier will be called {@code count} number of times. The count number must be positive.
   * <br>
   * The invocation inputs will be ignored.
   *
   * @param count          the number of generated outputs.
   * @param outputSupplier the supplier instance.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the invocation factory instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> thenGet(final long count,
      @NotNull final Supplier<? extends OUT> outputSupplier) {
    return new ThenSupplierInvocation<IN, OUT>(count, SupplierDecorator.decorate(outputSupplier));
  }

  /**
   * Returns a factory of invocations collecting inputs into an array of objects.
   *
   * @param <IN> the input data type.
   * @return the invocation factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> InvocationFactory<IN, Object[]> toArray() {
    return (InvocationFactory<IN, Object[]>) toArray(Object.class);
  }

  /**
   * Returns a factory of invocations collecting inputs into an array.
   *
   * @param componentType the array component type.
   * @param <IN>          the input data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN> InvocationFactory<IN, IN[]> toArray(
      @NotNull final Class<? extends IN> componentType) {
    return new ToArrayInvocationFactory<IN>(componentType);
  }

  /**
   * Returns a factory of invocations collecting inputs into an array.
   *
   * @param componentType the array component type.
   * @param <IN>          the input data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN> InvocationFactory<IN, IN[]> toArray(
      @NotNull final ClassToken<? extends IN> componentType) {
    return toArray(componentType.getRawClass());
  }

  /**
   * Returns a factory of invocations collecting inputs into a list.
   *
   * @param <IN> the input data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN> InvocationFactory<IN, List<IN>> toList() {
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
  public static <IN, KEY> InvocationFactory<IN, Map<KEY, IN>> toMap(
      @NotNull final Function<? super IN, KEY> keyFunction) {
    return toMap(keyFunction, FunctionDecorator.<IN>identity());
  }

  /**
   * Returns a factory of invocations collecting inputs into a map.
   *
   * @param keyFunction   the key function.
   * @param valueFunction the value function.
   * @param <IN>          the input data type.
   * @param <KEY>         the map key type.
   * @param <VALUE>       the map value type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN, KEY, VALUE> InvocationFactory<IN, Map<KEY, VALUE>> toMap(
      @NotNull final Function<? super IN, KEY> keyFunction,
      @NotNull final Function<? super IN, VALUE> valueFunction) {
    return new ToMapInvocationFactory<IN, KEY, VALUE>(FunctionDecorator.decorate(keyFunction),
        FunctionDecorator.decorate(valueFunction));
  }

  /**
   * Returns a factory of invocations collecting inputs into a set.
   *
   * @param <IN> the input data type.
   * @return the invocation factory instance.
   */
  @NotNull
  public static <IN> InvocationFactory<IN, Set<IN>> toSet() {
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

  @Nullable
  private static <DATA> List<DATA> toList(@Nullable final DATA... data) {
    return (data != null) ? Arrays.asList(data) : null;
  }

  @Nullable
  private static <DATA> List<DATA> toList(@Nullable final Iterable<? extends DATA> data) {
    if (data == null) {
      return null;
    }

    final ArrayList<DATA> dataList = new ArrayList<DATA>();
    for (final DATA datum : data) {
      dataList.add(datum);
    }

    return dataList;
  }
}
