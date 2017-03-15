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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.ChannelInvocation;
import com.github.dm.jrt.core.builder.AbstractRoutineBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.config.InvocationConfiguration.InvocationModeType;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuildingException;
import com.github.dm.jrt.stream.config.StreamConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.decorate;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;

/**
 * Abstract implementation of a stream routine builder.
 * <p>
 * This class provides a default implementation of all the stream builder features. The inheriting
 * class just needs to create routine and configuration instances when required.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractStreamBuilder<IN, OUT> extends AbstractRoutineBuilder<IN, OUT>
    implements StreamBuilder<IN, OUT> {

  private final FunctionDecorator<? super Channel<?, IN>, ? extends Channel<?, OUT>>
      mBindingFunction;

  private final StreamConfiguration mStreamConfiguration;

  private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration()
                                                                          .builderFrom()
                                                                          .withRunner(
                                                                              Runners
                                                                                  .immediateRunner())
                                                                          .apply();

  private final Configurable<StreamBuilder<IN, OUT>> mNextConfigurable =
      new Configurable<StreamBuilder<IN, OUT>>() {

        @NotNull
        public StreamBuilder<IN, OUT> apply(@NotNull final InvocationConfiguration configuration) {
          return AbstractStreamBuilder.this.nextApply(configuration);
        }
      };

  private final Configurable<StreamBuilder<IN, OUT>> mStreamConfigurable =
      new Configurable<StreamBuilder<IN, OUT>>() {

        @NotNull
        public StreamBuilder<IN, OUT> apply(@NotNull final InvocationConfiguration configuration) {
          return AbstractStreamBuilder.this.streamApply(configuration);
        }
      };

  /**
   * Constructor.
   *
   * @param streamConfiguration the stream configuration.
   */
  protected AbstractStreamBuilder(@NotNull final StreamConfiguration streamConfiguration) {
    mStreamConfiguration = ConstantConditions.notNull("stream configuration", streamConfiguration);
    mBindingFunction = decorate(new Function<Channel<?, IN>, Channel<?, OUT>>() {

      @SuppressWarnings("unchecked")
      public Channel<?, OUT> apply(final Channel<?, IN> channel) {
        return (Channel<?, OUT>) channel;
      }
    });
  }

  /**
   * Constructor.
   *
   * @param invocationConfiguration the invocation configuration.
   * @param streamConfiguration     the stream configuration.
   * @param bindingFunction         the binding function.
   */
  protected AbstractStreamBuilder(@NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final StreamConfiguration streamConfiguration,
      @NotNull final FunctionDecorator<? super Channel<?, IN>, ? extends Channel<?, OUT>>
          bindingFunction) {
    mConfiguration =
        ConstantConditions.notNull("invocation configuration", invocationConfiguration);
    mStreamConfiguration = ConstantConditions.notNull("stream configuration", streamConfiguration);
    mBindingFunction = ConstantConditions.notNull("binding function", bindingFunction);
  }

  @NotNull
  @Override
  public StreamBuilder<IN, OUT> apply(@NotNull final InvocationConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public Builder<? extends StreamBuilder<IN, OUT>> invocationConfiguration() {
    return (Builder<? extends StreamBuilder<IN, OUT>>) super.invocationConfiguration();
  }

  @NotNull
  public StreamBuilder<IN, OUT> async() {
    return async(Runners.sharedRunner());
  }

  @NotNull
  public StreamBuilder<IN, OUT> async(@Nullable final Runner runner) {
    return applyRunner(runner, InvocationModeType.SIMPLE);
  }

  @NotNull
  public StreamBuilder<IN, OUT> asyncParallel() {
    return asyncParallel(Runners.sharedRunner());
  }

  @NotNull
  public StreamBuilder<IN, OUT> asyncParallel(final int maxInvocations) {
    return asyncParallel(Runners.sharedRunner(), maxInvocations);
  }

  @NotNull
  public StreamBuilder<IN, OUT> asyncParallel(@Nullable final Runner runner) {
    return applyRunner(runner, InvocationModeType.PARALLEL);
  }

  @NotNull
  public StreamBuilder<IN, OUT> asyncParallel(@Nullable final Runner runner,
      final int maxInvocations) {
    return applyParallelRunner(runner, maxInvocations);
  }

  @NotNull
  public InvocationFactory<IN, OUT> buildFactory() {
    return new StreamInvocationFactory<IN, OUT>(getBindingFunction());
  }

  @NotNull
  public StreamBuilder<IN, OUT> consumeOn(@Nullable final Runner runner) {
    return map(newRoutine(
        mStreamConfiguration.toInvocationConfiguration().builderFrom().withRunner(runner).apply(),
        IdentityInvocation.<OUT>factoryOf()));
  }

  @NotNull
  public <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> convert(
      @NotNull final BiFunction<? super StreamConfiguration, ? super StreamBuilder<IN, OUT>, ?
          extends StreamBuilder<BEFORE, AFTER>> transformingFunction) {
    try {
      return ConstantConditions.notNull("transformed stream builder",
          transformingFunction.apply(mStreamConfiguration, this));

    } catch (final Exception e) {
      throw StreamBuildingException.wrapIfNeeded(e);
    }
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> flatMap(
      @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction) {
    return map(new MapInvocation<OUT, AFTER>(decorate(mappingFunction)));
  }

  @NotNull
  public StreamBuilder<IN, OUT> immediate() {
    return applyRunner(Runners.immediateRunner(), InvocationModeType.SIMPLE);
  }

  @NotNull
  public StreamBuilder<IN, OUT> immediateParallel() {
    return applyRunner(Runners.immediateRunner(), InvocationModeType.PARALLEL);
  }

  @NotNull
  public StreamBuilder<IN, OUT> immediateParallel(final int maxInvocations) {
    return applyParallelRunner(Runners.immediateRunner(), maxInvocations);
  }

  @NotNull
  public <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> lift(
      @NotNull final BiFunction<? super StreamConfiguration, ? super Function<Channel<?, IN>,
          Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends Channel<?,
          AFTER>>> liftingFunction) {
    try {
      return newBuilder(
          decorate(liftingFunction.apply(mStreamConfiguration, getBindingFunction())));

    } catch (final Exception e) {
      throw StreamBuildingException.wrapIfNeeded(e);
    }
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
    if (canOptimizeBinding()) {
      return newBuilder(decorate(getBindingFunction().andThen(
          new BindMappingFunction<OUT, AFTER>(mStreamConfiguration.toChannelConfiguration(),
              mappingFunction))));
    }

    return map(functionMapping(mappingFunction));
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
    return map(newRoutine(mStreamConfiguration.toInvocationConfiguration(), factory));
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    return newBuilder(getBindingFunction().andThen(new BindMap<OUT, AFTER>(routine)));
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
    return map(builder.apply(mStreamConfiguration.toInvocationConfiguration()).buildRoutine());
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> mapAccept(
      @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
    if (canOptimizeBinding()) {
      return newBuilder(decorate(getBindingFunction().andThen(
          new BindMappingConsumer<OUT, AFTER>(mStreamConfiguration.toChannelConfiguration(),
              mappingConsumer))));
    }

    return map(consumerMapping(mappingConsumer));
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> mapAll(
      @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
    if (canOptimizeBinding()) {
      final StreamConfiguration streamConfiguration = mStreamConfiguration;
      return newBuilder(decorate(getBindingFunction().andThen(
          new BindMappingAllFunction<OUT, AFTER>(streamConfiguration.toChannelConfiguration(),
              streamConfiguration.toInvocationConfiguration()
                                 .getInvocationModeOrElse(InvocationModeType.SIMPLE),
              mappingFunction))));
    }

    return map(functionCall(mappingFunction));
  }

  @NotNull
  public <AFTER> StreamBuilder<IN, AFTER> mapAllAccept(
      @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer) {
    if (canOptimizeBinding()) {
      final StreamConfiguration streamConfiguration = mStreamConfiguration;
      return newBuilder(decorate(getBindingFunction().andThen(
          new BindMappingAllConsumer<OUT, AFTER>(streamConfiguration.toChannelConfiguration(),
              streamConfiguration.toInvocationConfiguration()
                                 .getInvocationModeOrElse(InvocationModeType.SIMPLE),
              mappingConsumer))));
    }

    return map(consumerCall(mappingConsumer));
  }

  @NotNull
  public StreamBuilder<IN, OUT> nextApply(@NotNull final InvocationConfiguration configuration) {
    return newBuilder(
        new StreamConfiguration(mStreamConfiguration.getStreamInvocationConfiguration(),
            configuration));
  }

  @NotNull
  public Builder<? extends StreamBuilder<IN, OUT>> nextInvocationConfiguration() {
    return new Builder<StreamBuilder<IN, OUT>>(mNextConfigurable,
        mStreamConfiguration.getStreamInvocationConfiguration());
  }

  @NotNull
  public StreamBuilder<IN, OUT> sorted() {
    return applyOrder(OrderType.SORTED);
  }

  @NotNull
  public StreamBuilder<IN, OUT> streamApply(@NotNull final InvocationConfiguration configuration) {
    return newBuilder(new StreamConfiguration(configuration,
        mStreamConfiguration.getNextInvocationConfiguration()));
  }

  @NotNull
  public Builder<? extends StreamBuilder<IN, OUT>> streamInvocationConfiguration() {
    return new Builder<StreamBuilder<IN, OUT>>(mStreamConfigurable,
        mStreamConfiguration.getStreamInvocationConfiguration());
  }

  @NotNull
  public StreamBuilder<IN, OUT> sync() {
    return applyRunner(Runners.syncRunner(), InvocationModeType.SIMPLE);
  }

  @NotNull
  public StreamBuilder<IN, OUT> syncParallel() {
    return applyRunner(Runners.syncRunner(), InvocationModeType.PARALLEL);
  }

  @NotNull
  public StreamBuilder<IN, OUT> syncParallel(final int maxInvocations) {
    return applyParallelRunner(Runners.syncRunner(), maxInvocations);
  }

  @NotNull
  public StreamBuilder<IN, OUT> unsorted() {
    return applyOrder(OrderType.UNSORTED);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Routine<IN, OUT> buildRoutine() {
    final Routine<? super IN, ? extends OUT> routine =
        ConstantConditions.notNull("routine instance", newRoutine(mConfiguration, buildFactory()));
    return (Routine<IN, OUT>) routine;
  }

  /**
   * Checks if the current configuration allows to optimize the binding of the next mapping
   * function or consumer.
   * <br>
   * The optimization will consist in avoiding the creation of a routine, by employing a simple
   * channel consumer instead.
   *
   * @return whether the next binding can be optimized.
   */
  protected boolean canOptimizeBinding() {
    final InvocationConfiguration configuration = mStreamConfiguration.toInvocationConfiguration();
    return (configuration.getRunnerOrElse(null) == Runners.immediateRunner()) && (
        configuration.getPriorityOrElse(InvocationConfiguration.DEFAULT)
            == InvocationConfiguration.DEFAULT) && (
        configuration.getMaxInvocationsOrElse(InvocationConfiguration.DEFAULT)
            == InvocationConfiguration.DEFAULT) && (configuration.getInputBackoffOrElse(null)
        == null) && (configuration.getInputMaxSizeOrElse(InvocationConfiguration.DEFAULT)
        == InvocationConfiguration.DEFAULT) && (configuration.getInputOrderTypeOrElse(null)
        == null);
  }

  /**
   * Creates a new builder instance.
   *
   * @param invocationConfiguration the invocation configuration.
   * @param streamConfiguration     the stream configuration.
   * @param bindingFunction         the binding function.
   * @param <BEFORE>                the new stream input type.
   * @param <AFTER>                 the new stream output type.
   * @return the new stream.
   */
  @NotNull
  protected abstract <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> newBuilder(
      @NotNull InvocationConfiguration invocationConfiguration,
      @NotNull StreamConfiguration streamConfiguration,
      @NotNull FunctionDecorator<? super Channel<?, BEFORE>, ? extends Channel<?, AFTER>>
          bindingFunction);

  /**
   * Creates a new routine instance based on the specified factory.
   *
   * @param invocationConfiguration the invocation configuration.
   * @param factory                 the invocation factory.
   * @param <AFTER>                 the concatenation output type.
   * @return the newly created routine instance.
   */
  @NotNull
  protected abstract <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
      @NotNull InvocationConfiguration invocationConfiguration,
      @NotNull InvocationFactory<? super BEFORE, ? extends AFTER> factory);

  @NotNull
  private StreamBuilder<IN, OUT> applyOrder(@Nullable final OrderType orderType) {
    final StreamConfiguration streamConfiguration = mStreamConfiguration;
    return newBuilder(new StreamConfiguration(streamConfiguration.getStreamInvocationConfiguration()
                                                                 .builderFrom()
                                                                 .withOutputOrder(orderType)
                                                                 .apply(),
        streamConfiguration.getNextInvocationConfiguration()));
  }

  @NotNull
  private StreamBuilder<IN, OUT> applyParallelRunner(@Nullable final Runner runner,
      final int maxInvocations) {
    final StreamConfiguration streamConfiguration = mStreamConfiguration;
    return newBuilder(new StreamConfiguration(streamConfiguration.getStreamInvocationConfiguration()
                                                                 .builderFrom()
                                                                 .withRunner(runner)
                                                                 .withInvocationMode(
                                                                     InvocationModeType.PARALLEL)
                                                                 .withMaxInvocations(maxInvocations)
                                                                 .apply(),
        streamConfiguration.getNextInvocationConfiguration()));
  }

  @NotNull
  private StreamBuilder<IN, OUT> applyRunner(@Nullable final Runner runner,
      @NotNull final InvocationModeType invocationMode) {
    final StreamConfiguration streamConfiguration = mStreamConfiguration;
    return newBuilder(new StreamConfiguration(streamConfiguration.getStreamInvocationConfiguration()
                                                                 .builderFrom()
                                                                 .withRunner(runner)
                                                                 .withInvocationMode(invocationMode)
                                                                 .apply(),
        streamConfiguration.getNextInvocationConfiguration()));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> getBindingFunction() {
    return (FunctionDecorator<Channel<?, IN>, Channel<?, OUT>>) mBindingFunction;
  }

  @NotNull
  private <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> newBuilder(
      @NotNull final FunctionDecorator<? super Channel<?, BEFORE>, ? extends Channel<?, AFTER>>
          bindingFunction) {
    return ConstantConditions.notNull("stream builder",
        newBuilder(mConfiguration, resetConfiguration(), bindingFunction));
  }

  @NotNull
  private StreamBuilder<IN, OUT> newBuilder(
      @NotNull final StreamConfiguration streamConfiguration) {
    return ConstantConditions.notNull("stream builder",
        newBuilder(mConfiguration, streamConfiguration, mBindingFunction));
  }

  @NotNull
  private StreamConfiguration resetConfiguration() {
    return new StreamConfiguration(mStreamConfiguration.getStreamInvocationConfiguration(),
        InvocationConfiguration.defaultConfiguration());
  }

  /**
   * Invocations building a stream of routines by applying a binding function.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StreamInvocation<IN, OUT> extends ChannelInvocation<IN, OUT> {

    private final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

    /**
     * Constructor.
     *
     * @param bindingFunction the binding function.
     */
    private StreamInvocation(
        @NotNull final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
      mBindingFunction = bindingFunction;
    }

    @NotNull
    @Override
    protected Channel<?, OUT> onChannel(@NotNull final Channel<?, IN> channel) throws Exception {
      return mBindingFunction.apply(channel);
    }
  }

  /**
   * Factory of stream invocations.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StreamInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

    /**
     * Constructor.
     *
     * @param bindingFunction the binding function.
     */
    private StreamInvocationFactory(
        @NotNull final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
      super(asArgs(bindingFunction));
      mBindingFunction = bindingFunction;
    }

    @NotNull
    @Override
    public Invocation<IN, OUT> newInvocation() {
      return new StreamInvocation<IN, OUT>(mBindingFunction);
    }
  }
}
