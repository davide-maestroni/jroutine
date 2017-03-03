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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
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

import java.util.Comparator;
import java.util.List;

/**
 * Utility class back-porting functional programming.
 * <p>
 * Created by davide-maestroni on 09/21/2015.
 */
@SuppressWarnings("WeakerAccess")
public class Functions {

  /**
   * Avoid explicit instantiation.
   */
  protected Functions() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a bi-consumer decorator just discarding the passed inputs.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN1> the first input data type.
   * @param <IN2> the second input data type.
   * @return the bi-consumer decorator.
   */
  @NotNull
  public static <IN1, IN2> BiConsumerDecorator<IN1, IN2> biSink() {
    return BiConsumerDecorator.biSink();
  }

  /**
   * Returns a function decorator casting the passed inputs to the specified class.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param type  the class type.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the function decorator.
   */
  @NotNull
  public static <IN, OUT> FunctionDecorator<IN, OUT> castTo(
      @NotNull final Class<? extends OUT> type) {
    return FunctionDecorator.castTo(type);
  }

  /**
   * Returns a function decorator casting the passed inputs to the specified class token type.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param token the class token.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the function decorator.
   */
  @NotNull
  public static <IN, OUT> FunctionDecorator<IN, OUT> castTo(
      @NotNull final ClassToken<? extends OUT> token) {
    return FunctionDecorator.castTo(token);
  }

  /**
   * Returns a supplier decorator always returning the same result.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param result the result.
   * @param <OUT>  the output data type.
   * @return the supplier decorator.
   */
  @NotNull
  public static <OUT> SupplierDecorator<OUT> constant(final OUT result) {
    return SupplierDecorator.constant(result);
  }

  /**
   * Builds and returns a new call invocation factory based on the specified bi-consumer instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
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
      @NotNull final BiConsumer<? super List<IN>, ? super Channel<OUT, ?>> consumer) {
    // TODO: 01/03/2017 remove
    return new ConsumerInvocationFactory<IN, OUT>(decorate(consumer));
  }

  /**
   * Builds and returns a new command invocation based on the specified consumer instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param consumer the consumer instance.
   * @param <OUT>    the output data type.
   * @return the command invocation.
   */
  @NotNull
  public static <OUT> CommandInvocation<OUT> consumerCommand(
      @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
    // TODO: 01/03/2017 remove
    return new ConsumerCommandInvocation<OUT>(decorate(consumer));
  }

  /**
   * Builds and returns a new mapping invocation based on the specified bi-consumer instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param consumer the bi-consumer instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the mapping invocation.
   */
  @NotNull
  public static <IN, OUT> MappingInvocation<IN, OUT> consumerMapping(
      @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> consumer) {
    // TODO: 01/03/2017 remove
    return new ConsumerMappingInvocation<IN, OUT>(decorate(consumer));
  }

  /**
   * Decorates the specified action instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param action the action instance.
   * @return the decorated action.
   */
  @NotNull
  public static ActionDecorator decorate(@NotNull final Action action) {
    return ActionDecorator.decorate(action);
  }

  /**
   * Decorates the specified bi-consumer instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param consumer the bi-consumer instance.
   * @param <IN1>    the first input data type.
   * @param <IN2>    the second input data type.
   * @return the decorated bi-consumer.
   */
  @NotNull
  public static <IN1, IN2> BiConsumerDecorator<IN1, IN2> decorate(
      @NotNull final BiConsumer<IN1, IN2> consumer) {
    return BiConsumerDecorator.decorate(consumer);
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
    return BiFunctionDecorator.decorate(function);
  }

  /**
   * Decorates the specified consumer instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @return the decorated consumer.
   */
  @NotNull
  public static <IN> ConsumerDecorator<IN> decorate(@NotNull final Consumer<IN> consumer) {
    return ConsumerDecorator.decorate(consumer);
  }

  /**
   * Decorates the specified function instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the decorated function.
   */
  @NotNull
  public static <IN, OUT> FunctionDecorator<IN, OUT> decorate(
      @NotNull final Function<IN, OUT> function) {
    return FunctionDecorator.decorate(function);
  }

  /**
   * Decorates the specified predicate instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param predicate the predicate instance.
   * @param <IN>      the input data type.
   * @return the decorated predicate.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> decorate(@NotNull final Predicate<IN> predicate) {
    return PredicateDecorator.decorate(predicate);
  }

  /**
   * Decorates the specified runnable instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param action the runnable instance.
   * @return the decorated action.
   */
  @NotNull
  public static ActionDecorator decorate(@NotNull final Runnable action) {
    return ActionDecorator.decorate(action);
  }

  /**
   * Decorates the specified supplier instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param supplier the supplier instance.
   * @param <OUT>    the output data type.
   * @return the decorated supplier.
   */
  @NotNull
  public static <OUT> SupplierDecorator<OUT> decorate(@NotNull final Supplier<OUT> supplier) {
    return SupplierDecorator.decorate(supplier);
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
  public static <IN1, IN2> BiFunctionDecorator<IN1, IN2, IN1> first() {
    return BiFunctionDecorator.first();
  }

  /**
   * Builds and returns a new call invocation factory based on the specified function instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
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
    // TODO: 01/03/2017 remove
    return new FunctionInvocationFactory<IN, OUT>(decorate(function));
  }

  /**
   * Builds and returns a new mapping invocation based on the specified function instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the mapping invocation.
   */
  @NotNull
  public static <IN, OUT> MappingInvocation<IN, OUT> functionMapping(
      @NotNull final Function<? super IN, ? extends OUT> function) {
    // TODO: 01/03/2017 remove
    return new FunctionMappingInvocation<IN, OUT>(decorate(function));
  }

  /**
   * Returns the identity function decorator.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the function decorator.
   */
  @NotNull
  public static <IN> FunctionDecorator<IN, IN> identity() {
    return FunctionDecorator.identity();
  }

  /**
   * Returns a predicate decorator testing for equality to the specified object.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param targetRef the target reference.
   * @param <IN>      the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> isEqualTo(@Nullable final Object targetRef) {
    return PredicateDecorator.isEqualTo(targetRef);
  }

  /**
   * Returns a predicate decorator testing whether the passed inputs are instances of the specified
   * class.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param type the class type.
   * @param <IN> the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> isInstanceOf(@NotNull final Class<?> type) {
    return PredicateDecorator.isInstanceOf(type);
  }

  /**
   * Returns a predicate decorator returning true when the passed argument is not null.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> isNotNull() {
    return PredicateDecorator.isNotNull();
  }

  /**
   * Returns a predicate decorator returning true when the passed argument is null.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> isNull() {
    return PredicateDecorator.isNull();
  }

  /**
   * Returns a predicate decorator testing for identity to the specified object.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param targetRef the target reference.
   * @param <IN>      the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> isSameAs(@Nullable final Object targetRef) {
    return PredicateDecorator.isSameAs(targetRef);
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
  public static <IN extends Comparable<? super IN>> BiFunctionDecorator<IN, IN, IN> max() {
    return BiFunctionDecorator.max();
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
  @NotNull
  public static <IN> BiFunctionDecorator<IN, IN, IN> maxBy(
      @NotNull final Comparator<? super IN> comparator) {
    return BiFunctionDecorator.maxBy(comparator);
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
  public static <IN extends Comparable<? super IN>> BiFunctionDecorator<IN, IN, IN> min() {
    return BiFunctionDecorator.min();
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
  @NotNull
  public static <IN> BiFunctionDecorator<IN, IN, IN> minBy(
      @NotNull final Comparator<? super IN> comparator) {
    return BiFunctionDecorator.minBy(comparator);
  }

  /**
   * Returns a predicate decorator always returning false.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> negative() {
    return PredicateDecorator.negative();
  }

  /**
   * Returns an action decorator doing nothing.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @return the action decorator.
   */
  @NotNull
  public static ActionDecorator noOp() {
    return ActionDecorator.noOp();
  }

  /**
   * Returns a predicate decorator always returning true.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the predicate decorator.
   */
  @NotNull
  public static <IN> PredicateDecorator<IN> positive() {
    return PredicateDecorator.positive();
  }

  /**
   * Builds and returns a new mapping invocation based on the specified predicate instance.
   * <br>
   * Only the inputs which satisfies the predicate will be passed on, while the others will be
   * filtered out.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param predicate the predicate instance.
   * @param <IN>      the input data type.
   * @return the mapping invocation.
   */
  @NotNull
  public static <IN> MappingInvocation<IN, IN> predicateFilter(
      @NotNull final Predicate<? super IN> predicate) {
    // TODO: 03/03/2017 move to operator?
    return new PredicateMappingInvocation<IN>(decorate(predicate));
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
  public static <IN1, IN2> BiFunctionDecorator<IN1, IN2, IN2> second() {
    return BiFunctionDecorator.second();
  }

  /**
   * Returns a consumer decorator just discarding the passed inputs.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the consumer decorator.
   */
  @NotNull
  public static <IN> ConsumerDecorator<IN> sink() {
    return ConsumerDecorator.sink();
  }

  /**
   * Builds and returns a new command invocation based on the specified supplier instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
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
    // TODO: 01/03/2017 remove
    return new SupplierCommandInvocation<OUT>(decorate(supplier));
  }

  /**
   * Builds and returns a new invocation factory based on the specified supplier instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
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
    return new SupplierInvocationFactory<IN, OUT>(decorate(supplier));
  }
}
