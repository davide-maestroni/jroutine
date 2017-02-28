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
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.lambda.BiConsumer;
import com.github.dm.jrt.function.lambda.BiConsumerDecorator;
import com.github.dm.jrt.function.lambda.Consumer;
import com.github.dm.jrt.function.lambda.ConsumerDecorator;
import com.github.dm.jrt.function.lambda.Function;
import com.github.dm.jrt.function.lambda.FunctionDecorator;
import com.github.dm.jrt.function.lambda.Supplier;
import com.github.dm.jrt.function.lambda.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a stateless routine builder.
 * <p>
 * Created by davide-maestroni on 02/27/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStatelessRoutineBuilder<IN, OUT> implements StatelessRoutineBuilder<IN, OUT> {

  private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

  private final Configurable<StatelessRoutineBuilder<IN, OUT>> mConfigurable =
      new Configurable<StatelessRoutineBuilder<IN, OUT>>() {

        @NotNull
        public StatelessRoutineBuilder<IN, OUT> apply(
            @NotNull final InvocationConfiguration configuration) {
          return DefaultStatelessRoutineBuilder.this.apply(configuration);
        }
      };

  private ConsumerDecorator<? super Channel<OUT, ?>> mOnComplete = ConsumerDecorator.sink();

  private ConsumerDecorator<? super RoutineException> mOnError = ConsumerDecorator.sink();

  private BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> mOnNext =
      BiConsumerDecorator.biSink();

  /**
   * Constructor.
   */
  DefaultStatelessRoutineBuilder() {
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> apply(
      @NotNull final InvocationConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Builder<? extends StatelessRoutineBuilder<IN, OUT>> invocationConfiguration() {
    return new Builder<StatelessRoutineBuilder<IN, OUT>>(mConfigurable, mConfiguration);
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onComplete(
      @NotNull final Consumer<? super Channel<OUT, ?>> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(onComplete);
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onCompleteArray(
      @NotNull final Supplier<OUT[]> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(new CompleteArraySupplier<OUT>(onComplete));
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onCompleteIterable(
      @NotNull final Supplier<? extends Iterable<? extends
          OUT>> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(new CompleteIterableSupplier<OUT>(onComplete));
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onCompleteOutput(
      @NotNull final Supplier<OUT> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(new CompleteOutputSupplier<OUT>(onComplete));
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onError(
      @NotNull final Consumer<? super RoutineException> onError) {
    mOnError = ConsumerDecorator.decorate(onError);
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onNext(@NotNull final BiConsumer<? super IN, ? super
      Channel<OUT, ?>> onNext) {
    mOnNext = BiConsumerDecorator.decorate(onNext);
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onNextArray(
      @NotNull final Function<? super IN, OUT[]> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextArrayFunction<IN, OUT>(onNext));
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onNextConsume(
      @NotNull final Consumer<? super IN> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextConsumer<IN, OUT>(onNext));
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onNextIterable(
      @NotNull final Function<? super IN, ? extends Iterable<?
          extends OUT>> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextIterableFunction<IN, OUT>(onNext));
    return this;
  }

  @NotNull
  public StatelessRoutineBuilder<IN, OUT> onNextOutput(
      @NotNull final Function<? super IN, ? extends OUT> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextOutputFunction<IN, OUT>(onNext));
    return this;
  }

  @NotNull
  public Routine<IN, OUT> buildRoutine() {
    return JRoutineCore.with(new StatelessInvocation<IN, OUT>(mOnNext, mOnError, mOnComplete))
                       .apply(mConfiguration)
                       .buildRoutine();
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
   * Consumer wrapping a complete supplier returning an array of outputs.
   *
   * @param <OUT> the output data type.
   */
  private static class CompleteArraySupplier<OUT> extends DeepEqualObject
      implements Consumer<Channel<OUT, ?>> {

    private final Supplier<OUT[]> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the supplier instance.
     */
    private CompleteArraySupplier(
        @NotNull final com.github.dm.jrt.function.lambda.Supplier<OUT[]> onComplete) {
      super(asArgs(SupplierDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public void accept(final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.get());
    }
  }

  /**
   * Consumer wrapping a complete supplier returning an iterable of outputs.
   *
   * @param <OUT> the output data type.
   */
  private static class CompleteIterableSupplier<OUT> extends DeepEqualObject
      implements Consumer<Channel<OUT, ?>> {

    private final Supplier<? extends Iterable<? extends OUT>> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the supplier instance.
     */
    private CompleteIterableSupplier(
        @NotNull final Supplier<? extends Iterable<? extends OUT>> onComplete) {
      super(asArgs(SupplierDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public void accept(final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.get());
    }
  }

  /**
   * Consumer wrapping a complete supplier returning an output.
   *
   * @param <OUT> the output data type.
   */
  private static class CompleteOutputSupplier<OUT> extends DeepEqualObject
      implements Consumer<Channel<OUT, ?>> {

    private final Supplier<OUT> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the supplier instance.
     */
    private CompleteOutputSupplier(@NotNull final Supplier<OUT> onComplete) {
      super(asArgs(SupplierDecorator.decorate(onComplete)));
      mOnComplete = onComplete;
    }

    public void accept(final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnComplete.get());
    }
  }

  /**
   * Consumer wrapping a next function returning an array of outputs.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class NextArrayFunction<IN, OUT> extends DeepEqualObject
      implements BiConsumer<IN, Channel<OUT, ?>> {

    private final Function<? super IN, OUT[]> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextArrayFunction(@NotNull final Function<? super IN, OUT[]> onNext) {
      super(asArgs(FunctionDecorator.decorate(onNext)));
      mOnNext = onNext;
    }

    public void accept(final IN input, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnNext.apply(input));
    }
  }

  /**
   * Consumer wrapping a next one taking only an input as parameter.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class NextConsumer<IN, OUT> extends DeepEqualObject
      implements BiConsumer<IN, Channel<OUT, ?>> {

    private final Consumer<? super IN> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the consumer instance.
     */
    private NextConsumer(@NotNull final Consumer<? super IN> onNext) {
      super(asArgs(ConsumerDecorator.decorate(onNext)));
      mOnNext = onNext;
    }

    public void accept(final IN input, final Channel<OUT, ?> result) throws Exception {
      mOnNext.accept(input);
    }
  }

  /**
   * Consumer wrapping a next function returning an iterable of outputs.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class NextIterableFunction<IN, OUT> extends DeepEqualObject
      implements BiConsumer<IN, Channel<OUT, ?>> {

    private final Function<? super IN, ? extends Iterable<? extends OUT>> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextIterableFunction(
        @NotNull final Function<? super IN, ? extends Iterable<? extends OUT>> onNext) {
      super(asArgs(FunctionDecorator.decorate(onNext)));
      mOnNext = onNext;
    }

    public void accept(final IN input, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnNext.apply(input));
    }
  }

  /**
   * Consumer wrapping a next function returning an output.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class NextOutputFunction<IN, OUT> extends DeepEqualObject
      implements BiConsumer<IN, Channel<OUT, ?>> {

    private final Function<? super IN, ? extends OUT> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext the function instance.
     */
    private NextOutputFunction(@NotNull final Function<? super IN, ? extends OUT> onNext) {
      super(asArgs(FunctionDecorator.decorate(onNext)));
      mOnNext = onNext;
    }

    public void accept(final IN input, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnNext.apply(input));
    }
  }

  /**
   * Stateless invocation implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StatelessInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
      implements Invocation<IN, OUT> {

    private final ConsumerDecorator<? super Channel<OUT, ?>> mOnComplete;

    private final ConsumerDecorator<? super RoutineException> mOnError;

    private final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext     the next consumer.
     * @param onError    the error consumer.
     * @param onComplete the complete consumer.
     */
    private StatelessInvocation(
        @NotNull final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> onNext,
        @NotNull final ConsumerDecorator<? super RoutineException> onError,
        @NotNull final ConsumerDecorator<? super Channel<OUT, ?>> onComplete) {
      super(asArgs(onNext, onError, onComplete));
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return this;
    }

    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mOnError.accept(reason);
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      mOnComplete.accept(result);
    }

    public void onDestroy() {
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
      mOnNext.accept(input, result);
    }

    public boolean onRecycle() {
      return true;
    }

    public void onRestart() {
    }
  }
}
