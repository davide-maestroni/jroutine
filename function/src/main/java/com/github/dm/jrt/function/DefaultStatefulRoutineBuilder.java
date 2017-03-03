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
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
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

  private BiFunctionDecorator<? super STATE, ? super
      Channel<OUT, ?>, ? extends STATE> mOnComplete =
      BiFunctionDecorator.<STATE, Channel<OUT, ?>>first();

  private SupplierDecorator<? extends STATE> mOnCreate = SupplierDecorator.constant(null);

  private ConsumerDecorator<? super STATE> mOnDestroy = ConsumerDecorator.sink();

  private BiFunctionDecorator<? super STATE, ? super
      RoutineException, ? extends STATE> mOnError =
      BiFunctionDecorator.<STATE, RoutineException>first();

  private FunctionDecorator<? super STATE, ? extends STATE> mOnFinalize =
      FunctionDecorator.decorate(new Function<STATE, STATE>() {

        public STATE apply(final STATE state) {
          return null;
        }
      });

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
  public StatefulRoutineBuilder<IN, OUT, STATE> onCompleteConsume(
      @NotNull final BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete) {
    mOnComplete = BiFunctionDecorator.decorate(new CompleteConsumer<OUT, STATE>(onComplete));
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
  public StatefulRoutineBuilder<IN, OUT, STATE> onFinalize(
      @NotNull final Function<? super STATE, ? extends STATE> onFinalize) {
    mOnFinalize = FunctionDecorator.decorate(onFinalize);
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onFinalizeConsume(
      @NotNull final Consumer<? super STATE> onFinalize) {
    mOnFinalize = FunctionDecorator.decorate(new FinalizeConsumer<STATE>(onFinalize));
    return this;
  }

  @NotNull
  public StatefulRoutineBuilder<IN, OUT, STATE> onFinalizeRetain() {
    mOnFinalize = FunctionDecorator.<STATE>identity();
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
            mOnFinalize, mOnDestroy)).apply(mConfiguration).buildRoutine();
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
      super(asArgs(FunctionDecorator.decorate(onComplete)));
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
      super(asArgs(BiConsumerDecorator.decorate(onComplete)));
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
      super(asArgs(FunctionDecorator.decorate(onComplete)));
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
      super(asArgs(ConsumerDecorator.decorate(onFinalize)));
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

    private final BiFunctionDecorator<? super STATE, ? super
        Channel<OUT, ?>, ? extends STATE> mOnComplete;

    private final SupplierDecorator<? extends STATE> mOnCreate;

    private final ConsumerDecorator<? super STATE> mOnDestroy;

    private final BiFunctionDecorator<? super STATE, ? super
        RoutineException, ? extends STATE> mOnError;

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnFinalize;

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
     * @param onComplete the completion function.
     * @param onFinalize the finalization function.
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
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onFinalize,
        @NotNull final ConsumerDecorator<? super STATE> onDestroy) {
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnFinalize = onFinalize;
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
      mState = mOnFinalize.apply(mState);
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

    private final BiFunctionDecorator<? super STATE, ? super
        Channel<OUT, ?>, ? extends STATE> mOnComplete;

    private final SupplierDecorator<? extends STATE> mOnCreate;

    private final ConsumerDecorator<? super STATE> mOnDestroy;

    private final BiFunctionDecorator<? super STATE, ? super
        RoutineException, ? extends STATE> mOnError;

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnFinalize;

    private final TriFunctionDecorator<? super STATE, ? super
        IN, ? super Channel<OUT, ?>, ?
        extends STATE> mOnNext;

    /**
     * Constructor.
     *
     * @param onCreate   the state supplier.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the completion function.
     * @param onFinalize the finalization function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulInvocationFactory(@NotNull final SupplierDecorator<? extends STATE> onCreate,
        @NotNull final TriFunctionDecorator<? super STATE, ? super IN, ? super Channel<OUT, ?>, ?
            extends STATE> onNext, @NotNull final BiFunctionDecorator<? super STATE, ?
        super RoutineException, ? extends
        STATE> onError,
        @NotNull final BiFunctionDecorator<? super STATE, ? super Channel<OUT, ?>, ? extends
            STATE> onComplete,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onFinalize,
        @NotNull final ConsumerDecorator<? super STATE> onDestroy) {
      super(asArgs(onCreate, onNext, onError, onComplete, onFinalize, onDestroy));
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnFinalize = onFinalize;
      mOnDestroy = onDestroy;
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return new StatefulInvocation<IN, OUT, STATE>(mOnCreate, mOnNext, mOnError, mOnComplete,
          mOnFinalize, mOnDestroy);
    }
  }
}
