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
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;
import static com.github.dm.jrt.function.util.FunctionDecorator.wrapFunction;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Abstract implementation of a stateless builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>   the input data type.
 * @param <OUT>  the output data type.
 * @param <TYPE> the type of the class extending this one.
 */
public abstract class AbstractStatelessBuilder<IN, OUT, TYPE extends StatelessBuilder<IN, OUT,
    TYPE>>
    implements StatelessBuilder<IN, OUT, TYPE> {

  private Consumer<? super Channel<OUT, ?>> mOnComplete = ConsumerDecorator.sink();

  private Consumer<? super RoutineException> mOnError = ConsumerDecorator.sink();

  private BiConsumer<? super IN, ? super Channel<OUT, ?>> mOnNext = BiConsumerDecorator.biSink();

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onComplete(@NotNull final Consumer<? super Channel<OUT, ?>> onComplete) {
    mOnComplete = ConstantConditions.notNull("consumer instance", onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteArray(@NotNull final Supplier<OUT[]> onComplete) {
    mOnComplete = new CompleteArraySupplier<OUT>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteIterable(
      @NotNull final Supplier<? extends Iterable<? extends OUT>> onComplete) {
    mOnComplete = new CompleteIterableSupplier<OUT>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onCompleteOutput(@NotNull final Supplier<? extends OUT> onComplete) {
    mOnComplete = new CompleteOutputSupplier<OUT>(onComplete);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onError(@NotNull final Consumer<? super RoutineException> onError) {
    mOnError = ConstantConditions.notNull("consumer instance", onError);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNext(@NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext) {
    mOnNext = ConstantConditions.notNull("consumer instance", onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextArray(@NotNull final Function<? super IN, OUT[]> onNext) {
    mOnNext = new NextArrayFunction<IN, OUT>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextConsume(@NotNull final Consumer<? super IN> onNext) {
    mOnNext = new NextConsumer<IN, OUT>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextIterable(
      @NotNull final Function<? super IN, ? extends Iterable<? extends OUT>> onNext) {
    mOnNext = new NextIterableFunction<IN, OUT>(onNext);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE onNextOutput(@NotNull final Function<? super IN, ? extends OUT> onNext) {
    mOnNext = new NextOutputFunction<IN, OUT>(onNext);
    return (TYPE) this;
  }

  /**
   * Returns the completion consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected Consumer<? super Channel<OUT, ?>> getOnComplete() {
    return mOnComplete;
  }

  /**
   * Returns the error consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected Consumer<? super RoutineException> getOnError() {
    return mOnError;
  }

  /**
   * Returns the next consumer decorator.
   *
   * @return the consumer decorator.
   */
  @NotNull
  protected BiConsumer<? super IN, ? super Channel<OUT, ?>> getOnNext() {
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
      super(asArgs(wrapSupplier(onComplete)));
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
      super(asArgs(wrapSupplier(onComplete)));
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
      super(asArgs(wrapSupplier(onComplete)));
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
      super(asArgs(wrapFunction(onNext)));
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
      super(asArgs(wrapConsumer(onNext)));
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
      super(asArgs(wrapFunction(onNext)));
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
      super(asArgs(wrapFunction(onNext)));
      mOnNext = onNext;
    }

    public void accept(final IN input, final Channel<OUT, ?> result) throws Exception {
      result.pass(mOnNext.apply(input));
    }
  }
}
