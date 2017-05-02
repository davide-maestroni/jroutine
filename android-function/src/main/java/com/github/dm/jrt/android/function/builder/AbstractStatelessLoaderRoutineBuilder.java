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

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.builder.AbstractStatelessRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Decorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.function.util.BiConsumerDecorator.wrapBiConsumer;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;
import static com.github.dm.jrt.function.util.FunctionDecorator.wrapFunction;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Abstract implementation of a stateless Loader routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractStatelessLoaderRoutineBuilder<IN, OUT, TYPE extends
    StatelessLoaderRoutineBuilder<IN, OUT>>
    extends AbstractStatelessRoutineBuilder<IN, OUT, TYPE>
    implements StatelessLoaderRoutineBuilder<IN, OUT> {

  private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

  private final Configurable<TYPE> mConfigurable = new Configurable<TYPE>() {

    @NotNull
    public TYPE apply(@NotNull final LoaderConfiguration configuration) {
      return AbstractStatelessLoaderRoutineBuilder.this.apply(configuration);
    }
  };

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

  @Override
  public void clear(@Nullable final IN input) {
  }

  @Override
  public void clear(@Nullable final IN... inputs) {
  }

  @Override
  public void clear(@Nullable final Iterable<? extends IN> inputs) {
  }

  @NotNull
  @Override
  public TYPE onComplete(@NotNull final Consumer<? super Channel<OUT, ?>> onComplete) {
    return super.onComplete(checkStaticScope(wrapConsumer(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteArray(@NotNull final Supplier<OUT[]> onComplete) {
    return super.onCompleteArray(checkStaticScope(wrapSupplier(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteIterable(
      @NotNull final Supplier<? extends Iterable<? extends OUT>> onComplete) {
    return super.onCompleteIterable(checkStaticScope(wrapSupplier(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onCompleteOutput(@NotNull final Supplier<? extends OUT> onComplete) {
    return super.onCompleteOutput(checkStaticScope(wrapSupplier(onComplete)));
  }

  @NotNull
  @Override
  public TYPE onError(@NotNull final Consumer<? super RoutineException> onError) {
    return super.onError(checkStaticScope(wrapConsumer(onError)));
  }

  @NotNull
  @Override
  public TYPE onNext(@NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext) {
    return super.onNext(checkStaticScope(wrapBiConsumer(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextArray(@NotNull final Function<? super IN, OUT[]> onNext) {
    return super.onNextArray(checkStaticScope(wrapFunction(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextConsume(@NotNull final Consumer<? super IN> onNext) {
    return super.onNextConsume(checkStaticScope(wrapConsumer(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextIterable(
      @NotNull final Function<? super IN, ? extends Iterable<? extends OUT>> onNext) {
    return super.onNextIterable(checkStaticScope(wrapFunction(onNext)));
  }

  @NotNull
  @Override
  public TYPE onNextOutput(@NotNull final Function<? super IN, ? extends OUT> onNext) {
    return super.onNextOutput(checkStaticScope(wrapFunction(onNext)));
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
}
