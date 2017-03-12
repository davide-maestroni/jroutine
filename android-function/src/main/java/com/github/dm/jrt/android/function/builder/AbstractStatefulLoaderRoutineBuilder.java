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

package com.github.dm.jrt.android.function.builder;

import android.content.Context;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.builder.AbstractStatefulRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.BiFunctionDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.Decorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;
import com.github.dm.jrt.function.util.TriFunction;
import com.github.dm.jrt.function.util.TriFunctionDecorator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a stateful Loader routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 * @param <TYPE>  the type of the class extending this one.
 */
public abstract class AbstractStatefulLoaderRoutineBuilder<IN, OUT, STATE, TYPE extends
    StatefulLoaderRoutineBuilder<IN, OUT, STATE>>
    extends AbstractStatefulRoutineBuilder<IN, OUT, STATE, TYPE>
    implements StatefulLoaderRoutineBuilder<IN, OUT, STATE> {

  private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

  private final Configurable<TYPE> mConfigurable = new Configurable<TYPE>() {

    @NotNull
    public TYPE apply(@NotNull final LoaderConfiguration configuration) {
      return AbstractStatefulLoaderRoutineBuilder.this.apply(configuration);
    }
  };

  private FunctionDecorator<? super Context, ? extends STATE> mOnContext =
      FunctionDecorator.decorate(new Function<Context, STATE>() {

        @Override
        public STATE apply(final Context context) {
          return null;
        }
      });

  private FunctionDecorator<? super STATE, ? extends STATE> mOnCreate =
      FunctionDecorator.<STATE>identity();

  @NotNull
  private static <TYPE extends Decorator> TYPE checkStaticScope(@NotNull final TYPE decorator) {
    if (!decorator.hasStaticScope()) {
      throw new IllegalArgumentException("the specified function must have a static scope");
    }

    return decorator;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE apply(@NotNull final LoaderConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("loader configuration", configuration);
    return (TYPE) this;
  }

  @NotNull
  @Override
  public Builder<? extends TYPE> loaderConfiguration() {
    return new Builder<TYPE>(mConfigurable, mConfiguration);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE onContext(@NotNull final Function<? super Context, ? extends STATE> onContext) {
    mOnContext = checkStaticScope(FunctionDecorator.decorate(onContext));
    return (TYPE) this;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE onContextConsume(@NotNull final Consumer<? super Context> onContext) {
    mOnContext =
        checkStaticScope(FunctionDecorator.decorate(new ContextConsumer<STATE>(onContext)));
    return (TYPE) this;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE onCreateState(@NotNull final Function<? super STATE, ? extends STATE> onCreate) {
    mOnCreate = checkStaticScope(FunctionDecorator.decorate(onCreate));
    return (TYPE) this;
  }

  @Override
  public void clear(@Nullable final IN input) {
    buildRoutine().clear(input);
  }

  @Override
  public void clear(@Nullable final IN... inputs) {
    buildRoutine().clear(inputs);
  }

  @Override
  public void clear(@Nullable final Iterable<? extends IN> inputs) {
    buildRoutine().clear(inputs);
  }

  @NotNull
  @Override
  public TYPE onComplete(
      @NotNull final BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE>
          onComplete) {
    return super.onComplete(checkStaticScope(BiFunctionDecorator.decorate(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteArray(@NotNull final Function<? super STATE, OUT[]> onComplete) {
    return super.onCompleteArray(checkStaticScope(FunctionDecorator.decorate(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteConsume(
      @NotNull final BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete) {
    return super.onCompleteConsume(checkStaticScope(BiConsumerDecorator.decorate(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteIterable(
      @NotNull final Function<? super STATE, ? extends Iterable<? extends OUT>> onComplete) {
    return super.onCompleteIterable(checkStaticScope(FunctionDecorator.decorate(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteOutput(@NotNull final Function<? super STATE, ? extends OUT> onComplete) {
    return super.onCompleteOutput(checkStaticScope(FunctionDecorator.decorate(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteState(@NotNull final Function<? super STATE, ? extends STATE> onComplete) {
    return super.onCompleteState(checkStaticScope(FunctionDecorator.decorate(onComplete)));
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE onCreate(@NotNull final Supplier<? extends STATE> onCreate) {
    mOnCreate = FunctionDecorator.decorate(new CreateSupplier<STATE>(onCreate));
    return (TYPE) this;
  }

  @NotNull
  @Override
  public TYPE onDestroy(@NotNull final Consumer<? super STATE> onDestroy) {
    return super.onDestroy(checkStaticScope(ConsumerDecorator.decorate(onDestroy)));
  }

  @NotNull
  @Override
  public TYPE onError(
      @NotNull final BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError) {
    return super.onError(checkStaticScope(BiFunctionDecorator.decorate(onError)));
  }

  @NotNull
  @Override
  public TYPE onErrorException(
      @NotNull final Function<? super RoutineException, ? extends STATE> onError) {
    return super.onErrorException(checkStaticScope(FunctionDecorator.decorate(onError)));
  }

  @NotNull
  @Override
  public TYPE onErrorState(@NotNull final Function<? super STATE, ? extends STATE> onError) {
    return super.onErrorState(checkStaticScope(FunctionDecorator.decorate(onError)));
  }

  @NotNull
  @Override
  public TYPE onFinalize(@NotNull final Function<? super STATE, ? extends STATE> onFinalize) {
    return super.onFinalize(checkStaticScope(FunctionDecorator.decorate(onFinalize)));
  }

  @NotNull
  @Override
  public TYPE onFinalizeConsume(@NotNull final Consumer<? super STATE> onFinalize) {
    return super.onFinalizeConsume(checkStaticScope(ConsumerDecorator.decorate(onFinalize)));
  }

  @NotNull
  @Override
  public TYPE onNext(
      @NotNull final TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends
          STATE> onNext) {
    return super.onNext(checkStaticScope(TriFunctionDecorator.decorate(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextArray(@NotNull final BiFunction<? super STATE, ? super IN, OUT[]> onNext) {
    return super.onNextArray(checkStaticScope(BiFunctionDecorator.decorate(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextConsume(@NotNull final BiConsumer<? super STATE, ? super IN> onNext) {
    return super.onNextConsume(checkStaticScope(BiConsumerDecorator.decorate(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextIterable(
      @NotNull final BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>>
          onNext) {
    return super.onNextIterable(checkStaticScope(BiFunctionDecorator.decorate(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextOutput(
      @NotNull final BiFunction<? super STATE, ? super IN, ? extends OUT> onNext) {
    return super.onNextOutput(checkStaticScope(BiFunctionDecorator.decorate(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextState(
      @NotNull final BiFunction<? super STATE, ? super IN, ? extends STATE> onNext) {
    return super.onNextState(checkStaticScope(BiFunctionDecorator.decorate(onNext)));
  }

  @Override
  public void clear() {
    buildRoutine().clear();
  }

  /**
   * Returns the current Loader configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected LoaderConfiguration getLoaderConfiguration() {
    return mConfiguration;
  }

  /**
   * Returns the Context function decorator.
   *
   * @return the function decorator.
   */
  @NotNull
  protected FunctionDecorator<? super Context, ? extends STATE> getOnContext() {
    return mOnContext;
  }

  /**
   * Returns the state creation function decorator.
   *
   * @return the function decorator.
   */
  @NotNull
  protected FunctionDecorator<? super STATE, ? extends STATE> getOnCreateState() {
    return mOnCreate;
  }

  /**
   * Function wrapping a Context consumer.
   *
   * @param <STATE> the state data type.
   */
  private static class ContextConsumer<STATE> extends DeepEqualObject
      implements Function<Context, STATE> {

    private final Consumer<? super Context> mOnContext;

    /**
     * Constructor.
     *
     * @param onContext the consumer instance.
     */
    private ContextConsumer(@NotNull final Consumer<? super Context> onContext) {
      super(asArgs(ConsumerDecorator.decorate(onContext)));
      mOnContext = onContext;
    }

    @Override
    public STATE apply(final Context context) throws Exception {
      mOnContext.accept(context);
      return null;
    }
  }

  /**
   * Function wrapping a state supplier.
   *
   * @param <STATE> the state data type.
   */
  private static class CreateSupplier<STATE> extends DeepEqualObject
      implements Function<STATE, STATE> {

    private final Supplier<? extends STATE> mOnCreate;

    /**
     * Constructor.
     *
     * @param onCreate the supplier instance.
     */
    private CreateSupplier(@NotNull final Supplier<? extends STATE> onCreate) {
      super(asArgs(SupplierDecorator.decorate(onCreate)));
      mOnCreate = onCreate;
    }

    @Override
    public STATE apply(final STATE state) throws Exception {
      return (state != null) ? state : mOnCreate.get();
    }
  }
}
