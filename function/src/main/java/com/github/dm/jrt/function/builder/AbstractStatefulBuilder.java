/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.function.builder;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.BiFunctionDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;
import com.github.dm.jrt.function.util.TriFunction;
import com.github.dm.jrt.function.util.TriFunctionDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.BiConsumerDecorator.wrapBiConsumer;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.wrapBiFunction;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;
import static com.github.dm.jrt.function.util.FunctionDecorator.wrapFunction;

/**
 * Abstract implementation of a stateful builder.
 * <p>
 * Created by davide-maestroni on 03/05/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 * @param <TYPE>  the builder type.
 */
public abstract class AbstractStatefulBuilder<IN, OUT, STATE, TYPE extends StatefulBuilder<IN,
    OUT, STATE, TYPE>>
    implements StatefulBuilder<IN, OUT, STATE, TYPE> {

  private BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> mOnComplete =
      BiFunctionDecorator.<STATE, Channel<OUT, ?>>first();

  private Supplier<? extends STATE> mOnCreate = SupplierDecorator.constant(null);

  private Consumer<? super STATE> mOnDestroy = ConsumerDecorator.sink();

  private BiFunction<? super STATE, ? super RoutineException, ? extends STATE> mOnError =
      BiFunctionDecorator.<STATE, RoutineException>first();

  private Function<? super STATE, ? extends STATE> mOnFinalize = FunctionDecorator.constant(null);

  private TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE> mOnNext =
      TriFunctionDecorator.<STATE, IN, Channel<OUT, ?>>first();

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onComplete(
      @NotNull final BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE>
          onComplete) {
    mOnComplete = ConstantConditions.notNull("function instance", onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteArray(@NotNull final Function<? super STATE, OUT[]> onComplete) {
    mOnComplete = new CompleteArrayFunction<OUT, STATE>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteConsume(
      @NotNull final BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete) {
    mOnComplete = new CompleteConsumer<OUT, STATE>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteIterable(
      @NotNull final Function<? super STATE, ? extends Iterable<? extends OUT>> onComplete) {
    mOnComplete = new CompleteIterableFunction<OUT, STATE>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteOutput(@NotNull final Function<? super STATE, ? extends OUT> onComplete) {
    mOnComplete = new CompleteOutputFunction<OUT, STATE>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteState(@NotNull final Function<? super STATE, ? extends STATE> onComplete) {
    mOnComplete = new CompleteStateFunction<OUT, STATE>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCreate(@NotNull final Supplier<? extends STATE> onCreate) {
    mOnCreate = ConstantConditions.notNull("supplier instance", onCreate);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onDestroy(@NotNull final Consumer<? super STATE> onDestroy) {
    mOnDestroy = ConstantConditions.notNull("consumer instance", onDestroy);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onError(
      @NotNull final BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError) {
    mOnError = ConstantConditions.notNull("function instance", onError);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onErrorConsume(
      @NotNull final BiConsumer<? super STATE, ? super RoutineException> onError) {
    mOnError = new ErrorConsumer<STATE>(onError);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onErrorException(
      @NotNull final Function<? super RoutineException, ? extends STATE> onError) {
    mOnError = new ErrorExceptionFunction<STATE>(onError);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onErrorState(@NotNull final Function<? super STATE, ? extends STATE> onError) {
    mOnError = new ErrorStateFunction<STATE>(onError);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onFinalize(@NotNull final Function<? super STATE, ? extends STATE> onFinalize) {
    mOnFinalize = ConstantConditions.notNull("function instance", onFinalize);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onFinalizeConsume(@NotNull final Consumer<? super STATE> onFinalize) {
    mOnFinalize = new FinalizeConsumer<STATE>(onFinalize);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNext(
      @NotNull final TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends
          STATE> onNext) {
    mOnNext = ConstantConditions.notNull("function instance", onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextArray(@NotNull final BiFunction<? super STATE, ? super IN, OUT[]> onNext) {
    mOnNext = new NextArrayFunction<IN, OUT, STATE>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextConsume(@NotNull final BiConsumer<? super STATE, ? super IN> onNext) {
    mOnNext = new NextConsumer<IN, OUT, STATE>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextIterable(
      @NotNull final BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>>
          onNext) {
    mOnNext = new NextIterableFunction<IN, OUT, STATE>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextOutput(
      @NotNull final BiFunction<? super STATE, ? super IN, ? extends OUT> onNext) {
    mOnNext = new NextOutputFunction<IN, OUT, STATE>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextState(
      @NotNull final BiFunction<? super STATE, ? super IN, ? extends STATE> onNext) {
    mOnNext = new NextStateFunction<IN, OUT, STATE>(onNext);
    return (TYPE) this;
  }

  /**
   * Returns the complete function decorator.
   *
   * @return the function decorator.
   */
  @NotNull
  protected BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> getOnComplete() {
    return mOnComplete;
  }

  /**
   * Returns the creation supplier decorator.
   *
   * @return the supplier decorator.
   */
  @NotNull
  protected Supplier<? extends STATE> getOnCreate() {
    return mOnCreate;
  }

  /**
   * Returns the destroy consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected Consumer<? super STATE> getOnDestroy() {
    return mOnDestroy;
  }

  /**
   * Returns the error function decorator.
   *
   * @return the function decorator.
   */
  @NotNull
  protected BiFunction<? super STATE, ? super RoutineException, ? extends STATE> getOnError() {
    return mOnError;
  }

  /**
   * Returns the finalization function decorator.
   *
   * @return the function decorator.
   */
  @NotNull
  protected Function<? super STATE, ? extends STATE> getOnFinalize() {
    return mOnFinalize;
  }

  /**
   * Returns the next function decorator.
   *
   * @return the function decorator.
   */
  @NotNull
  protected TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE>
  getOnNext() {
    return mOnNext;
  }

  /**
   * Function wrapping a completion one returning an array of outputs.
   *
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class CompleteArrayFunction<OUT, STATE> extends DeepEqualObject
      implements BiFunction<STATE, Channel<OUT, ?>, STATE> {

    private final Function<? super STATE, OUT[]> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the function instance.
     */
    private CompleteArrayFunction(@NotNull final Function<? super STATE, OUT[]> onComplete) {
      super(asArgs(wrapFunction(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.apply(state));
      return state;
    }
  }

  /**
   * Function wrapping a completion consumer taking only a state object and the result channel as
   * parameters.
   *
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class CompleteConsumer<OUT, STATE> extends DeepEqualObject
      implements BiFunction<STATE, Channel<OUT, ?>, STATE> {

    private final BiConsumer<? super STATE, ? super Channel<OUT, ?>> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the consumer instance.
     */
    private CompleteConsumer(
        @NotNull final BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete) {
      super(asArgs(wrapBiConsumer(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      mOnComplete.accept(state, result);
      return state;
    }
  }

  /**
   * Function wrapping a completion one returning an iterable of outputs.
   *
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class CompleteIterableFunction<OUT, STATE> extends DeepEqualObject
      implements BiFunction<STATE, Channel<OUT, ?>, STATE> {

    private final Function<? super STATE, ? extends Iterable<? extends OUT>> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the function instance.
     */
    private CompleteIterableFunction(
        @NotNull final Function<? super STATE, ? extends Iterable<? extends OUT>> onComplete) {
      super(asArgs(wrapFunction(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.apply(state));
      return state;
    }
  }

  /**
   * Function wrapping a completion one returning an output.
   *
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class CompleteOutputFunction<OUT, STATE> extends DeepEqualObject
      implements BiFunction<STATE, Channel<OUT, ?>, STATE> {

    private final Function<? super STATE, ? extends OUT> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the function instance.
     */
    private CompleteOutputFunction(
        @NotNull final Function<? super STATE, ? extends OUT> onComplete) {
      super(asArgs(wrapFunction(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.apply(state));
      return state;
    }
  }

  /**
   * Function wrapping a completion one taking only a state object as parameter.
   *
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class CompleteStateFunction<OUT, STATE> extends DeepEqualObject
      implements BiFunction<STATE, Channel<OUT, ?>, STATE> {

    private final Function<? super STATE, ? extends STATE> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the function instance.
     */
    private CompleteStateFunction(
        @NotNull final Function<? super STATE, ? extends STATE> onComplete) {
      super(asArgs(wrapFunction(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      return mOnComplete.apply(state);
    }
  }

  /**
   * Function wrapping an error consumer taking a state object and an exception as parameters.
   *
   * @param <STATE> the state data type.
   */
  private static class ErrorConsumer<STATE> extends DeepEqualObject
      implements BiFunction<STATE, RoutineException, STATE> {

    private final BiConsumer<? super STATE, ? super RoutineException> mOnError;

    /**
     * Constructor.
     *
     * @param onError the consumer instance.
     */
    private ErrorConsumer(
        @NotNull final BiConsumer<? super STATE, ? super RoutineException> onError) {
      super(asArgs(wrapBiConsumer(onError)));
      mOnError = onError;
    }

    public STATE apply(final STATE state, final RoutineException e) throws Exception {
      mOnError.accept(state, e);
      return state;
    }
  }

  /**
   * Function wrapping an error one taking only an exception as parameter.
   *
   * @param <STATE> the state data type.
   */
  private static class ErrorExceptionFunction<STATE> extends DeepEqualObject
      implements BiFunction<STATE, RoutineException, STATE> {

    private final Function<? super RoutineException, ? extends STATE> mOnError;

    /**
     * Constructor.
     *
     * @param onError the function instance.
     */
    private ErrorExceptionFunction(
        @NotNull final Function<? super RoutineException, ? extends STATE> onError) {
      super(asArgs(wrapFunction(onError)));
      mOnError = onError;
    }

    public STATE apply(final STATE state, final RoutineException e) throws Exception {
      return mOnError.apply(e);
    }
  }

  /**
   * Function wrapping an error one taking only a state object as parameter.
   *
   * @param <STATE> the state data type.
   */
  private static class ErrorStateFunction<STATE> extends DeepEqualObject
      implements BiFunction<STATE, RoutineException, STATE> {

    private final Function<? super STATE, ? extends STATE> mOnError;

    /**
     * Constructor.
     *
     * @param onError the function instance.
     */
    private ErrorStateFunction(@NotNull final Function<? super STATE, ? extends STATE> onError) {
      super(asArgs(wrapFunction(onError)));
      mOnError = onError;
    }

    public STATE apply(final STATE state, final RoutineException e) throws Exception {
      return mOnError.apply(state);
    }
  }

  /**
   * Function wrapping a finalization consumer taking a state object as parameter.
   *
   * @param <STATE> the state data type.
   */
  private static class FinalizeConsumer<STATE> extends DeepEqualObject
      implements Function<STATE, STATE> {

    private final Consumer<? super STATE> mOnFinalize;

    /**
     * Constructor.
     *
     * @param onFinalize the consumer instance.
     */
    private FinalizeConsumer(@NotNull final Consumer<? super STATE> onFinalize) {
      super(asArgs(wrapConsumer(onFinalize)));
      mOnFinalize = onFinalize;
    }

    public STATE apply(final STATE state) throws Exception {
      mOnFinalize.accept(state);
      return null;
    }
  }

  /**
   * Function wrapping a next one returning an array of outputs.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class NextArrayFunction<IN, OUT, STATE> extends DeepEqualObject
      implements TriFunction<STATE, IN, Channel<OUT, ?>, STATE> {

    private final BiFunction<? super STATE, ? super IN, OUT[]> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextArrayFunction(@NotNull final BiFunction<? super STATE, ? super IN, OUT[]> onNext) {
      super(asArgs(wrapBiFunction(onNext)));
      mOnNext = onNext;
    }

    public STATE apply(final STATE state, final IN input, final Channel<OUT, ?> result) throws
        Exception {
      result.pass(mOnNext.apply(state, input));
      return state;
    }
  }

  /**
   * Function wrapping a next consumer taking only a state object and an input as parameters.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class NextConsumer<IN, OUT, STATE> extends DeepEqualObject
      implements TriFunction<STATE, IN, Channel<OUT, ?>, STATE> {

    private final BiConsumer<? super STATE, ? super IN> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the consumer instance.
     */
    private NextConsumer(@NotNull final BiConsumer<? super STATE, ? super IN> onNext) {
      super(asArgs(wrapBiConsumer(onNext)));
      mOnNext = onNext;
    }

    public STATE apply(final STATE state, final IN input, final Channel<OUT, ?> result) throws
        Exception {
      mOnNext.accept(state, input);
      return state;
    }
  }

  /**
   * Function wrapping a next one returning an iterable of outputs.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class NextIterableFunction<IN, OUT, STATE> extends DeepEqualObject
      implements TriFunction<STATE, IN, Channel<OUT, ?>, STATE> {

    private final BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextIterableFunction(
        @NotNull final BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>>
            onNext) {
      super(asArgs(wrapBiFunction(onNext)));
      mOnNext = onNext;
    }

    public STATE apply(final STATE state, final IN input, final Channel<OUT, ?> result) throws
        Exception {
      result.pass(mOnNext.apply(state, input));
      return state;
    }
  }

  /**
   * Function wrapping a next one returning an output.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class NextOutputFunction<IN, OUT, STATE> extends DeepEqualObject
      implements TriFunction<STATE, IN, Channel<OUT, ?>, STATE> {

    private final BiFunction<? super STATE, ? super IN, ? extends OUT> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextOutputFunction(
        @NotNull final BiFunction<? super STATE, ? super IN, ? extends OUT> onNext) {
      super(asArgs(wrapBiFunction(onNext)));
      mOnNext = onNext;
    }

    public STATE apply(final STATE state, final IN input, final Channel<OUT, ?> result) throws
        Exception {
      result.pass(mOnNext.apply(state, input));
      return state;
    }
  }

  /**
   * Function wrapping a next one taking only a state object and an input as parameters.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class NextStateFunction<IN, OUT, STATE> extends DeepEqualObject
      implements TriFunction<STATE, IN, Channel<OUT, ?>, STATE> {

    private final BiFunction<? super STATE, ? super IN, ? extends STATE> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextStateFunction(
        @NotNull final BiFunction<? super STATE, ? super IN, ? extends STATE> onNext) {
      super(asArgs(wrapBiFunction(onNext)));
      mOnNext = onNext;
    }

    public STATE apply(final STATE state, final IN input, final Channel<OUT, ?> result) throws
        Exception {
      return mOnNext.apply(state, input);
    }
  }
}
