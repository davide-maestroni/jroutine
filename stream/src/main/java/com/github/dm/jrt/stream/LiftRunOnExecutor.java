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
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Lifting function publishing the output of the stream on the specified executor.
 * <p>
 * Created by davide-maestroni on 05/26/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class LiftRunOnExecutor<IN, OUT> implements LiftingFunction<IN, OUT, IN, OUT> {

  private final InvocationConfiguration mConfiguration;

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor      the executor instance.
   * @param configuration the invocation configuration.
   */
  LiftRunOnExecutor(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
  }

  public Supplier<? extends Channel<IN, OUT>> apply(
      final Supplier<? extends Channel<IN, OUT>> supplier) {
    return new Supplier<Channel<IN, OUT>>() {

      public Channel<IN, OUT> get() {
        return JRoutineCore.routineOn(mExecutor)
                           .withConfiguration(mConfiguration)
                           .of(new RunOnInvocationFactory<IN, OUT>(supplier))
                           .invoke();
      }
    };
  }

  /**
   * Invocation backing a channel supplier.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RunOnInvocation<IN, OUT> implements Invocation<IN, OUT> {

    private final Supplier<? extends Channel<IN, OUT>> mChannelSupplier;

    private Channel<IN, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channelSupplier the channel supplier.
     */
    private RunOnInvocation(@NotNull final Supplier<? extends Channel<IN, OUT>> channelSupplier) {
      mChannelSupplier = channelSupplier;
    }

    public void onAbort(@NotNull final RoutineException reason) {
      mChannel.abort(reason);
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      bind(result);
      mChannel.close();
    }

    public void onDestroy() {
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
      bind(result);
      mChannel.pass(input);
    }

    public boolean onRecycle() {
      mChannel = null;
      return true;
    }

    public void onStart() throws Exception {
      mChannel = mChannelSupplier.get();
    }

    private void bind(@NotNull final Channel<OUT, ?> result) {
      final Channel<IN, OUT> channel = mChannel;
      if (!channel.isBound()) {
        result.pass(channel);
      }
    }
  }

  /**
   * Factory of invocations backing a channel supplier.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RunOnInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final Supplier<? extends Channel<IN, OUT>> mChannelSupplier;

    /**
     * Constructor.
     *
     * @param channelSupplier the channel supplier.
     */
    private RunOnInvocationFactory(
        @NotNull final Supplier<? extends Channel<IN, OUT>> channelSupplier) {
      super(asArgs(wrapSupplier(channelSupplier)));
      mChannelSupplier = channelSupplier;
    }

    @NotNull
    @Override
    public Invocation<IN, OUT> newInvocation() {
      return new RunOnInvocation<IN, OUT>(mChannelSupplier);
    }
  }
}
