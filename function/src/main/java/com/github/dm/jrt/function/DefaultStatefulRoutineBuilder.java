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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.lambda.BiConsumer;
import com.github.dm.jrt.function.lambda.BiConsumerDecorator;
import com.github.dm.jrt.function.lambda.BiFunction;
import com.github.dm.jrt.function.lambda.BiFunctionDecorator;
import com.github.dm.jrt.function.lambda.Consumer;
import com.github.dm.jrt.function.lambda.ConsumerDecorator;
import com.github.dm.jrt.function.lambda.Function;
import com.github.dm.jrt.function.lambda.FunctionDecorator;
import com.github.dm.jrt.function.lambda.Supplier;
import com.github.dm.jrt.function.lambda.SupplierDecorator;
import com.github.dm.jrt.function.lambda.TriFunction;
import com.github.dm.jrt.function.lambda.TriFunctionDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a stateful routine builder.
 * <p>
 * Created by davide-maestroni on 02/27/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
class DefaultStatefulRoutineBuilder<IN, OUT, STATE>
    implements StatefulRoutineBuilder<IN, OUT, STATE> {

  private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

  private final Configurable<StatefulRoutineBuilder<IN, OUT, STATE>> mConfigurable =
      new Configurable<StatefulRoutineBuilder<IN, OUT, STATE>>() {

        @NotNull
        public StatefulRoutineBuilder<IN, OUT, STATE> apply(
            @NotNull final InvocationConfiguration configuration) {
          return DefaultStatefulRoutineBuilder.this.apply(configuration);
        }
      };

  private FunctionDecorator<? super STATE, ? extends STATE> mOnCleanup =
      FunctionDecorator.decorate(new com.github.dm.jrt.function.lambda.Function<STATE, STATE>() {

        public STATE apply(final STATE state) {
          return null;
        }
      });

  private BiFunctionDecorator<? super STATE, ? super
      Channel<OUT, ?>, ? extends STATE> mOnComplete =
      BiFunctionDecorator.<STATE, Channel<OUT, ?>>first();

  private SupplierDecorator<? extends STATE> mOnCreate = SupplierDecorator.constant(null);

  private ConsumerDecorator<? super STATE> mOnDestroy = ConsumerDecorator.sink();

  private BiFunctionDecorator<? super STATE, ? super
      RoutineException, ? extends STATE> mOnError =
      BiFunctionDecorator.<STATE, RoutineException>first();

  private TriFunctionDecorator<? super STATE, ? super IN, ?
      super Channel<OUT, ?>, ? extends STATE> mOnNext =
      TriFunctionDecorator.decorate(new TriFunction<STATE, IN, Channel<OUT, ?>, STATE>() {

        public STATE apply(final STATE state, final IN in, final Channel<OUT, ?> objects) {
          return state;
        }
      });

  /**
   * Constructor.
   */
  DefaultStatefulRoutineBuilder() {
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> apply(
      @NotNull final InvocationConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Builder<? extends StatefulRoutineBuilder<IN, OUT, STATE>> invocationConfiguration() {
    return new Builder<StatefulRoutineBuilder<IN, OUT, STATE>>(mConfigurable, mConfiguration);
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCleanup(
      @NotNull final Function<? super STATE, ? extends STATE> onCleanup) {
    mOnCleanup = FunctionDecorator.decorate(onCleanup);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCleanupConsume(
      @NotNull final Consumer<? super STATE> onCleanup) {
    mOnCleanup = FunctionDecorator.decorate(new CleanupConsumer<STATE>(onCleanup));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onComplete(
      @NotNull final BiFunction<? super STATE, ? super
          Channel<OUT, ?>, ? extends STATE> onComplete) {
    mOnComplete = BiFunctionDecorator.decorate(onComplete);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCompleteArray(
      @NotNull final Function<? super STATE, OUT[]> onComplete) {
    mOnComplete = BiFunctionDecorator.decorate(new CompleteArrayFunction<OUT, STATE>(onComplete));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCompleteIterable(
      @NotNull final Function<? super STATE, ? extends
          Iterable<? extends OUT>> onComplete) {
    mOnComplete =
        BiFunctionDecorator.decorate(new CompleteIterableFunction<OUT, STATE>(onComplete));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCompleteOutput(
      @NotNull final Function<? super STATE, ? extends OUT> onComplete) {
    mOnComplete = BiFunctionDecorator.decorate(new CompleteOutputFunction<OUT, STATE>(onComplete));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCompleteState(
      @NotNull final Function<? super STATE, ? extends STATE> onComplete) {
    mOnComplete = BiFunctionDecorator.decorate(new CompleteStateFunction<OUT, STATE>(onComplete));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onCreate(
      @NotNull final Supplier<? extends STATE> onCreate) {
    mOnCreate = SupplierDecorator.decorate(onCreate);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onDestroy(
      @NotNull final Consumer<? super STATE> onDestroy) {
    mOnDestroy = ConsumerDecorator.decorate(onDestroy);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onError(
      @NotNull final BiFunction<? super STATE, ? super
          RoutineException, ? extends STATE> onError) {
    mOnError = BiFunctionDecorator.decorate(onError);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onErrorException(
      @NotNull final Function<? super RoutineException, ?
          extends STATE> onError) {
    mOnError = BiFunctionDecorator.decorate(new ErrorExceptionFunction<STATE>(onError));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onErrorState(
      @NotNull final Function<? super STATE, ? extends STATE> onError) {
    mOnError = BiFunctionDecorator.decorate(new ErrorStateFunction<STATE>(onError));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onNext(
      @NotNull final TriFunction<? super STATE, ? super IN, ?
          super Channel<OUT, ?>, ? extends
          STATE> onNext) {
    mOnNext = TriFunctionDecorator.decorate(onNext);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onNextArray(
      @NotNull final BiFunction<? super STATE, ? super IN, OUT[]> onNext) {
    mOnNext = TriFunctionDecorator.decorate(new NextArrayFunction<IN, OUT, STATE>(onNext));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onNextConsume(
      @NotNull final BiConsumer<? super STATE, ? super IN> onNext) {
    mOnNext = TriFunctionDecorator.decorate(new NextConsumer<IN, OUT, STATE>(onNext));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onNextIterable(
      @NotNull final BiFunction<? super STATE, ? super IN, ?
          extends Iterable<? extends OUT>> onNext) {
    mOnNext = TriFunctionDecorator.decorate(new NextIterableFunction<IN, OUT, STATE>(onNext));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onNextOutput(
      @NotNull final BiFunction<? super STATE, ? super IN, ?
          extends OUT> onNext) {
    mOnNext = TriFunctionDecorator.decorate(new NextOutputFunction<IN, OUT, STATE>(onNext));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onNextState(
      @NotNull final BiFunction<? super STATE, ? super IN, ?
          extends
          STATE> onNext) {
    mOnNext = TriFunctionDecorator.decorate(new NextStateFunction<IN, OUT, STATE>(onNext));
    return this;
  }

  @NotNull
  public Routine<IN, OUT> buildRoutine() {
    return JRoutineCore.with(
        new StatefulInvocationFactory<IN, OUT, STATE>(mOnCreate, mOnNext, mOnError, mOnComplete,
            mOnCleanup, mOnDestroy)).apply(mConfiguration).buildRoutine();
  }

  public void clear() {
  }

  @NotNull
  public Channel<IN, OUT> invoke() {
    return buildRoutine().invoke();
  }

  @NotNull
  public Channel<IN, OUT> invokeParallel() {
    return buildRoutine().invokeParallel();
  }

  /**
   * Function wrapping a cleanup consumer taking a state object as parameter.
   *
   * @param <STATE> the state data type.
   */
  private static class CleanupConsumer<STATE> extends DeepEqualObject
      implements Function<STATE, STATE> {

    private final Consumer<? super STATE> mOnCleanup;

    /**
     * Constructor.
     *
     * @param onCleanup the consumer instance.
     */
    private CleanupConsumer(@NotNull final Consumer<? super STATE> onCleanup) {
      super(asArgs(ConsumerDecorator.decorate(onCleanup)));
      mOnCleanup = onCleanup;
    }

    public STATE apply(final STATE state) throws Exception {
      mOnCleanup.accept(state);
      return null;
    }
  }

  /**
   * Function wrapping a complete one returning an array of outputs.
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
      super(asArgs(FunctionDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.apply(state));
      return state;
    }
  }

  /**
   * Function wrapping a complete one returning an iterable of outputs.
   *
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class CompleteIterableFunction<OUT, STATE> extends DeepEqualObject
      implements BiFunction<STATE, Channel<OUT, ?>, STATE> {

    private final Function<? super STATE, ? extends Iterable<?
        extends OUT>> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the function instance.
     */
    private CompleteIterableFunction(@NotNull final Function<? super STATE, ? extends
        Iterable<? extends OUT>> onComplete) {
      super(asArgs(FunctionDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.apply(state));
      return state;
    }
  }

  /**
   * Function wrapping a complete one returning an output.
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
      super(asArgs(FunctionDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.apply(state));
      return state;
    }
  }

  /**
   * Function wrapping a complete one taking only a state object as parameter.
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
      super(asArgs(FunctionDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public STATE apply(final STATE state, final Channel<OUT, ?> result) throws Exception {
      return mOnComplete.apply(state);
    }
  }

  /**
   * Function wrapping an error one taking only an exception as parameter.
   *
   * @param <STATE> the state data type.
   */
  private static class ErrorExceptionFunction<STATE> extends DeepEqualObject
      implements BiFunction<STATE, RoutineException, STATE> {

    private final Function<? super RoutineException, ? extends
        STATE> mOnError;

    /**
     * Constructor.
     *
     * @param onError the function instance.
     */
    private ErrorExceptionFunction(@NotNull final Function<? super RoutineException, ?
        extends STATE> onError) {
      super(asArgs(FunctionDecorator.decorate(onError)));
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
      super(asArgs(FunctionDecorator.decorate(onError)));
      mOnError = onError;
    }

    public STATE apply(final STATE state, final RoutineException e) throws Exception {
      return mOnError.apply(state);
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
      super(asArgs(BiFunctionDecorator.decorate(onNext)));
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
      super(asArgs(BiConsumerDecorator.decorate(onNext)));
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

    private final BiFunction<? super STATE, ? super IN, ?
        extends Iterable<? extends OUT>> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextIterableFunction(@NotNull final BiFunction<? super STATE, ? super IN, ?
        extends Iterable<? extends OUT>> onNext) {
      super(asArgs(BiFunctionDecorator.decorate(onNext)));
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

    private final BiFunction<? super STATE, ? super IN, ?
        extends OUT> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextOutputFunction(@NotNull final BiFunction<? super STATE, ? super IN, ?
        extends OUT> onNext) {
      super(asArgs(BiFunctionDecorator.decorate(onNext)));
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

    private final BiFunction<? super STATE, ? super IN, ?
        extends STATE> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextStateFunction(@NotNull final BiFunction<? super STATE, ? super IN, ?
        extends STATE> onNext) {
      super(asArgs(BiFunctionDecorator.decorate(onNext)));
      mOnNext = onNext;
    }

    public STATE apply(final STATE state, final IN input, final Channel<OUT, ?> result) throws
        Exception {
      return mOnNext.apply(state, input);
    }
  }

  /**
   * Stateful invocation implementation.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class StatefulInvocation<IN, OUT, STATE> implements Invocation<IN, OUT> {

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnCleanup;

    private final BiFunctionDecorator<? super STATE, ? super
        Channel<OUT, ?>, ? extends STATE> mOnComplete;

    private final SupplierDecorator<? extends STATE> mOnCreate;

    private final ConsumerDecorator<? super STATE> mOnDestroy;

    private final BiFunctionDecorator<? super STATE, ? super
        RoutineException, ? extends STATE> mOnError;

    private final TriFunctionDecorator<? super STATE, ? super
        IN, ? super Channel<OUT, ?>, ?
        extends STATE> mOnNext;

    private STATE mState;

    /**
     * Constructor.
     *
     * @param onCreate   the state supplier.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the complete function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulInvocation(@NotNull final SupplierDecorator<? extends STATE> onCreate,
        @NotNull final TriFunctionDecorator<? super STATE, ?
            super IN, ? super Channel<OUT, ?>, ?
            extends STATE> onNext, @NotNull final BiFunctionDecorator<? super STATE, ?
        super RoutineException, ? extends
        STATE> onError, @NotNull final BiFunctionDecorator<? super STATE, ?
        super Channel<OUT, ?>, ? extends
        STATE> onComplete,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onCleanup,
        @NotNull final ConsumerDecorator<? super STATE> onDestroy) {
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnCleanup = onCleanup;
      mOnDestroy = onDestroy;
    }

    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mState = mOnError.apply(mState, reason);
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      mState = mOnComplete.apply(mState, result);
    }

    public void onDestroy() throws Exception {
      mOnDestroy.accept(mState);
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
      mState = mOnNext.apply(mState, input, result);
    }

    public boolean onRecycle() throws Exception {
      mState = mOnCleanup.apply(mState);
      return true;
    }

    public void onRestart() throws Exception {
      if (mState == null) {
        mState = mOnCreate.get();
      }
    }
  }

  /**
   * Factory of stateful invocations implementation.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class StatefulInvocationFactory<IN, OUT, STATE>
      extends InvocationFactory<IN, OUT> {

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnCleanup;

    private final BiFunctionDecorator<? super STATE, ? super
        Channel<OUT, ?>, ? extends STATE> mOnComplete;

    private final SupplierDecorator<? extends STATE> mOnCreate;

    private final ConsumerDecorator<? super STATE> mOnDestroy;

    private final BiFunctionDecorator<? super STATE, ? super
        RoutineException, ? extends STATE> mOnError;

    private final TriFunctionDecorator<? super STATE, ? super
        IN, ? super Channel<OUT, ?>, ?
        extends STATE> mOnNext;

    /**
     * Constructor.
     *
     * @param onCreate   the state supplier.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the complete function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulInvocationFactory(@NotNull final SupplierDecorator<? extends STATE> onCreate,
        @NotNull final TriFunctionDecorator<? super STATE, ? super IN, ? super Channel<OUT, ?>, ?
            extends STATE> onNext, @NotNull final BiFunctionDecorator<? super STATE, ?
        super RoutineException, ? extends
        STATE> onError,
        @NotNull final BiFunctionDecorator<? super STATE, ? super Channel<OUT, ?>, ? extends
            STATE> onComplete,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onCleanup,
        @NotNull final ConsumerDecorator<? super STATE> onDestroy) {
      super(asArgs(onCreate, onNext, onError, onComplete, onCleanup, onDestroy));
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnCleanup = onCleanup;
      mOnDestroy = onDestroy;
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return new StatefulInvocation<IN, OUT, STATE>(mOnCreate, mOnNext, mOnError, mOnComplete,
          mOnCleanup, mOnDestroy);
    }
  }
}
