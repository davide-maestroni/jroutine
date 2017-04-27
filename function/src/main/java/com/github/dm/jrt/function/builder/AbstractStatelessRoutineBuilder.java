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
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a stateless routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>   the input data type.
 * @param <OUT>  the output data type.
 * @param <TYPE> the type of the class extending this one.
 */
public abstract class AbstractStatelessRoutineBuilder<IN, OUT, TYPE extends
    StatelessRoutineBuilder<IN, OUT>>
    implements StatelessRoutineBuilder<IN, OUT> {

  private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

  private final Configurable<TYPE> mConfigurable = new Configurable<TYPE>() {

    @NotNull
    public TYPE withConfiguration(@NotNull final InvocationConfiguration configuration) {
      return AbstractStatelessRoutineBuilder.this.withConfiguration(configuration);
    }
  };

  private ConsumerDecorator<? super Channel<OUT, ?>> mOnComplete = ConsumerDecorator.sink();

  private ConsumerDecorator<? super RoutineException> mOnError = ConsumerDecorator.sink();

  private BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> mOnNext =
      BiConsumerDecorator.biSink();

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onComplete(@NotNull final Consumer<? super Channel<OUT, ?>> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteArray(@NotNull final Supplier<OUT[]> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(new CompleteArraySupplier<OUT>(onComplete));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteIterable(
      @NotNull final Supplier<? extends Iterable<? extends OUT>> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(new CompleteIterableSupplier<OUT>(onComplete));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteOutput(@NotNull final Supplier<? extends OUT> onComplete) {
    mOnComplete = ConsumerDecorator.decorate(new CompleteOutputSupplier<OUT>(onComplete));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onError(@NotNull final Consumer<? super RoutineException> onError) {
    mOnError = ConsumerDecorator.decorate(onError);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNext(@NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext) {
    mOnNext = BiConsumerDecorator.decorate(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextArray(@NotNull final Function<? super IN, OUT[]> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextArrayFunction<IN, OUT>(onNext));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextConsume(@NotNull final Consumer<? super IN> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextConsumer<IN, OUT>(onNext));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextIterable(
      @NotNull final Function<? super IN, ? extends Iterable<? extends OUT>> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextIterableFunction<IN, OUT>(onNext));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextOutput(@NotNull final Function<? super IN, ? extends OUT> onNext) {
    mOnNext = BiConsumerDecorator.decorate(new NextOutputFunction<IN, OUT>(onNext));
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE withConfiguration(@NotNull final InvocationConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Builder<? extends TYPE> withInvocation() {
    return new Builder<TYPE>(mConfigurable, mConfiguration);
  }

  /**
   * Returns the current invocation configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected InvocationConfiguration getConfiguration() {
    return mConfiguration;
  }

  /**
   * Returns the completion consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected ConsumerDecorator<? super Channel<OUT, ?>> getOnComplete() {
    return mOnComplete;
  }

  /**
   * Returns the error consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected ConsumerDecorator<? super RoutineException> getOnError() {
    return mOnError;
  }

  /**
   * Returns the next consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> getOnNext() {
    return mOnNext;
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
    private CompleteArraySupplier(@NotNull final Supplier<OUT[]> onComplete) {
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

    private final Supplier<? extends OUT> mOnComplete;

    /**
     * Constructor.
     *
     * @param onComplete the supplier instance.
     */
    private CompleteOutputSupplier(@NotNull final Supplier<? extends OUT> onComplete) {
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
}
