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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.ActionDecorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.routine.StreamRoutine;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.function.util.ActionDecorator.wrapAction;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Default implementation of a stream routine.
 * <p>
 * Created by davide-maestroni on 04/28/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStreamRoutine<IN, OUT> implements StreamRoutine<IN, OUT> {

  private final Supplier<Channel<IN, OUT>> mChannelSupplier;

  private final Action mClearAction;

  /**
   * Constructor.
   *
   * @param invocation the wrapped invocation.
   */
  DefaultStreamRoutine(@NotNull final Invocation<IN, OUT> invocation) {
    this(new InvocationSupplier<IN, OUT>(invocation));
  }

  /**
   * Constructor.
   *
   * @param factory the wrapped factory.
   */
  DefaultStreamRoutine(@NotNull final InvocationFactory<IN, OUT> factory) {
    this(new FactorySupplier<IN, OUT>(factory));
  }

  /**
   * Constructor.
   *
   * @param routine the wrapped routine.
   */
  DefaultStreamRoutine(@NotNull final Routine<? super IN, ? extends OUT> routine) {
    this(new RoutineSupplier<IN, OUT>(routine), new RoutineAction(routine));
  }

  /**
   * Constructor.
   *
   * @param channelSupplier the supplier of the invocation channel.
   */
  private DefaultStreamRoutine(@NotNull final Supplier<Channel<IN, OUT>> channelSupplier) {
    this(channelSupplier, ActionDecorator.noOp());
  }

  /**
   * Constructor.
   *
   * @param channelSupplier the supplier of the invocation channel.
   * @param clearAction     the clear action.
   */
  private DefaultStreamRoutine(@NotNull final Supplier<Channel<IN, OUT>> channelSupplier,
      @NotNull final Action clearAction) {
    mChannelSupplier = channelSupplier;
    mClearAction = clearAction;
  }

  public void clear() {
    try {
      mClearAction.perform();

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @NotNull
  public Channel<IN, OUT> invoke() {
    try {
      return mChannelSupplier.get();

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @NotNull
  public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> lift(
      @NotNull final Function<? super Supplier<? extends Channel<IN, OUT>>, ? extends Supplier<?
          extends Channel<BEFORE, AFTER>>> liftingFunction) {
    try {
      return new DefaultStreamRoutine<BEFORE, AFTER>(ConstantConditions.notNull("supplier instance",
          (Supplier<Channel<BEFORE, AFTER>>) liftingFunction.apply(mChannelSupplier)),
          mClearAction);

    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @NotNull
  public <AFTER> StreamRoutine<IN, AFTER> map(
      @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
    return new DefaultStreamRoutine<IN, AFTER>(
        wrapSupplier(mChannelSupplier).andThen(new FactoryFunction<IN, OUT, AFTER>(factory)),
        mClearAction);
  }

  @NotNull
  public <AFTER> StreamRoutine<IN, AFTER> map(
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    return new DefaultStreamRoutine<IN, AFTER>(
        wrapSupplier(mChannelSupplier).andThen(new RoutineFunction<IN, OUT, AFTER>(routine)),
        wrapAction(new RoutineAction(routine)).andThen(mClearAction));
  }

  @NotNull
  public <AFTER> StreamRoutine<IN, AFTER> mapSingleton(
      @NotNull final Invocation<? super OUT, ? extends AFTER> invocation) {
    return new DefaultStreamRoutine<IN, AFTER>(
        wrapSupplier(mChannelSupplier).andThen(new InvocationFunction<IN, OUT, AFTER>(invocation)),
        mClearAction);
  }

  @NotNull
  public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> transform(
      @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends StreamRoutine<BEFORE,
          AFTER>> transformingFunction) {
    try {
      return ConstantConditions.notNull("stream routine", transformingFunction.apply(this));

    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Implementation of a function concatenating invocations returned by a factory instance.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <AFTER> the output type of the resulting routine.
   */
  private static class FactoryFunction<IN, OUT, AFTER>
      implements Function<Channel<IN, OUT>, Channel<IN, AFTER>> {

    private final InvocationFactory<? super OUT, ? extends AFTER> mFactory;

    /**
     * Constructor.
     *
     * @param factory the factory instance.
     */
    FactoryFunction(@NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
      mFactory = ConstantConditions.notNull("factory instance", factory);
    }

    public Channel<IN, AFTER> apply(final Channel<IN, OUT> channel) throws Exception {
      final Channel<AFTER, AFTER> outputChannel = JRoutineCore.channel().ofType();
      channel.consume(new InvocationConsumer<OUT, AFTER>(mFactory.newInvocation(), outputChannel));
      return JRoutineCore.flatten(channel, JRoutineCore.readOnly(outputChannel));
    }
  }

  /**
   * Implementation of a supplier wrapping invocations returned by a factory instance.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class FactorySupplier<IN, OUT> implements Supplier<Channel<IN, OUT>> {

    private final InvocationFactory<? super IN, ? extends OUT> mFactory;

    /**
     * Constructor.
     *
     * @param factory the factory instance.
     */
    FactorySupplier(@NotNull final InvocationFactory<? super IN, ? extends OUT> factory) {
      mFactory = ConstantConditions.notNull("factory instance", factory);
    }

    public Channel<IN, OUT> get() throws Exception {
      final Channel<IN, IN> inputChannel = JRoutineCore.channelOn(syncExecutor()).ofType();
      final Channel<OUT, OUT> outputChannel = JRoutineCore.channel().ofType();
      inputChannel.consume(
          new InvocationConsumer<IN, OUT>(mFactory.newInvocation(), outputChannel));
      return JRoutineCore.flatten(inputChannel, JRoutineCore.readOnly(outputChannel));
    }
  }

  /**
   * Channel consumer implementation handling an invocation lifecycle.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class InvocationConsumer<IN, OUT> implements ChannelConsumer<IN> {

    private final Invocation<? super IN, ? extends OUT> mInvocation;

    private final Channel<OUT, ?> mOutputChannel;

    /**
     * Constructor.
     *
     * @param invocation    the invocation instance.
     * @param outputChannel the output channel.
     */
    InvocationConsumer(@NotNull final Invocation<? super IN, ? extends OUT> invocation,
        @NotNull final Channel<OUT, ?> outputChannel) {
      try {
        invocation.onStart();

      } catch (final Exception e) {
        destroy(invocation, outputChannel);
        throw new IllegalStateException(e);
      }

      mInvocation = invocation;
      mOutputChannel = outputChannel;
    }

    private static void destroy(@NotNull final Invocation<?, ?> invocation,
        @NotNull final Channel<?, ?> outputChannel) {
      try {
        invocation.onRecycle();

      } catch (final Exception ignored) {

      } finally {
        try {
          invocation.onDestroy();

        } catch (final Exception ignored) {
        }

        outputChannel.close();
      }
    }

    @SuppressWarnings("unchecked")
    public void onComplete() throws Exception {
      final Invocation<? super IN, ? extends OUT> invocation = mInvocation;
      final Channel<OUT, ?> outputChannel = mOutputChannel;
      try {
        ((Invocation<IN, OUT>) invocation).onComplete(outputChannel);

      } catch (final Exception e) {
        try {
          invocation.onAbort(InvocationException.wrapIfNeeded(e));

        } catch (final Exception ignored) {
        }

        throw e;

      } finally {
        destroy(invocation, outputChannel);
      }
    }

    public void onError(@NotNull final RoutineException error) throws Exception {
      final Invocation<? super IN, ? extends OUT> invocation = mInvocation;
      try {
        invocation.onAbort(error);

      } finally {
        destroy(invocation, mOutputChannel);
      }
    }

    @SuppressWarnings("unchecked")
    public void onOutput(final IN output) throws Exception {
      ((Invocation<IN, OUT>) mInvocation).onInput(output, mOutputChannel);
    }
  }

  /**
   * Implementation of a function concatenating an invocation instance.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <AFTER> the output type of the resulting routine.
   */
  private static class InvocationFunction<IN, OUT, AFTER>
      implements Function<Channel<IN, OUT>, Channel<IN, AFTER>> {

    private final Invocation<? super OUT, ? extends AFTER> mInvocation;

    private final AtomicBoolean mIsCalled = new AtomicBoolean(false);

    /**
     * Constructor.
     *
     * @param invocation the invocation instance.
     */
    InvocationFunction(@NotNull final Invocation<? super OUT, ? extends AFTER> invocation) {
      mInvocation = ConstantConditions.notNull("invocation instance", invocation);
    }

    public Channel<IN, AFTER> apply(final Channel<IN, OUT> channel) throws Exception {
      if (mIsCalled.getAndSet(true)) {
        throw new IllegalStateException("cannot execute the invocation more than once.");
      }

      final Channel<AFTER, AFTER> outputChannel = JRoutineCore.channel().ofType();
      channel.consume(new InvocationConsumer<OUT, AFTER>(mInvocation, outputChannel));
      return JRoutineCore.flatten(channel, JRoutineCore.readOnly(outputChannel));
    }
  }

  /**
   * Implementation of a supplier wrapping an invocation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class InvocationSupplier<IN, OUT> implements Supplier<Channel<IN, OUT>> {

    private final Invocation<? super IN, ? extends OUT> mInvocation;

    private final AtomicBoolean mIsCalled = new AtomicBoolean(false);

    /**
     * Constructor.
     *
     * @param invocation the invocation instance.
     */
    InvocationSupplier(@NotNull final Invocation<? super IN, ? extends OUT> invocation) {
      mInvocation = ConstantConditions.notNull("invocation instance", invocation);
    }

    public Channel<IN, OUT> get() throws Exception {
      if (mIsCalled.getAndSet(true)) {
        throw new IllegalStateException("cannot execute the invocation more than once.");
      }

      final Channel<IN, IN> inputChannel = JRoutineCore.channelOn(syncExecutor()).ofType();
      final Channel<OUT, OUT> outputChannel = JRoutineCore.channel().ofType();
      inputChannel.consume(new InvocationConsumer<IN, OUT>(mInvocation, outputChannel));
      return JRoutineCore.flatten(inputChannel, JRoutineCore.readOnly(outputChannel));
    }
  }

  /**
   * Implementation of an action clearing a routine.
   */
  private static class RoutineAction implements Action {

    private final Routine<?, ?> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to clear.
     */
    private RoutineAction(@NotNull final Routine<?, ?> routine) {
      mRoutine = routine;
    }

    public void perform() {
      mRoutine.clear();
    }
  }

  /**
   * Implementation of a function concatenating a routine to the passed channel.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <AFTER> the output type of the resulting routine.
   */
  private static class RoutineFunction<IN, OUT, AFTER>
      implements Function<Channel<IN, OUT>, Channel<IN, AFTER>> {

    private final Routine<? super OUT, ? extends AFTER> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to concatenate.
     */
    private RoutineFunction(@NotNull final Routine<? super OUT, ? extends AFTER> routine) {
      mRoutine = ConstantConditions.notNull("routine instance", routine);
    }

    @SuppressWarnings("unchecked")
    public Channel<IN, AFTER> apply(final Channel<IN, OUT> channel) {
      return (Channel<IN, AFTER>) channel.pipe(mRoutine.invoke());
    }
  }

  /**
   * Implementation of a supplier invoking a routine.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineSupplier<IN, OUT> implements Supplier<Channel<IN, OUT>> {

    private final Routine<? super IN, ? extends OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to invoke.
     */
    private RoutineSupplier(@NotNull final Routine<? super IN, ? extends OUT> routine) {
      mRoutine = ConstantConditions.notNull("routine instance", routine);
    }

    @SuppressWarnings("unchecked")
    public Channel<IN, OUT> get() {
      return (Channel<IN, OUT>) mRoutine.invoke();
    }
  }
}
