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

import com.github.dm.jrt.channel.JRoutineChannels;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Retry channel consumer.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class RetryChannelConsumer<IN, OUT> implements Runnable, ChannelConsumer<OUT> {

  private final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
      mBackoffFunction;

  private final Supplier<? extends Channel<IN, OUT>> mChannelSupplier;

  private final ScheduledExecutor mExecutor;

  private final Channel<OUT, OUT> mOutputChannel;

  private final ArrayList<OUT> mOutputs = new ArrayList<OUT>();

  private final Channel<?, IN> mReplayChannel;

  private int mCount;

  /**
   * Constructor.
   *
   * @param executor        the executor instance.
   * @param configuration   the channel configuration.
   * @param channelSupplier the binding function.
   * @param backoffFunction the backoff function.
   * @param inputChannel    the input channel.
   * @param outputChannel   the output channel.
   */
  RetryChannelConsumer(@NotNull final ScheduledExecutor executor,
      @NotNull final ChannelConfiguration configuration,
      @NotNull final Supplier<? extends Channel<IN, OUT>> channelSupplier,
      @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
          backoffFunction,
      @NotNull final Channel<?, IN> inputChannel, @NotNull final Channel<OUT, ?> outputChannel) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mChannelSupplier = ConstantConditions.notNull("binding function", channelSupplier);
    mBackoffFunction = ConstantConditions.notNull("backoff function", backoffFunction);
    mReplayChannel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    outputChannel.pass(mOutputChannel =
        JRoutineCore.channelOn(executor).withConfiguration(configuration).ofType());
  }

  public void onComplete() {
    final Channel<OUT, ?> outputChannel = mOutputChannel;
    try {
      outputChannel.pass(mOutputs).close();

    } catch (final Throwable t) {
      outputChannel.abort(t);
      InterruptedInvocationException.throwIfInterrupt(t);
    }
  }

  public void run() {
    final Channel<IN, IN> channel = JRoutineCore.channel().ofType();
    mReplayChannel.consume(new SafeChannelConsumer<IN>(channel));
    try {
      mChannelSupplier.get().consume(this).pass(channel).close();

    } catch (final Throwable t) {
      abort(t);
      InterruptedInvocationException.throwIfInterrupt(t);
    }
  }

  private void abort(@NotNull final Throwable error) {
    final RoutineException ex = InvocationException.wrapIfNeeded(error);
    mOutputChannel.abort(ex);
    mReplayChannel.abort(ex);
  }

  /**
   * Channel consumer implementation avoiding the upstream propagation of errors.
   *
   * @param <IN> the input data type.
   */
  private static class SafeChannelConsumer<IN> implements ChannelConsumer<IN> {

    private final Channel<IN, ?> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel.
     */
    private SafeChannelConsumer(@NotNull final Channel<IN, ?> channel) {
      mChannel = channel;
    }

    public void onComplete() {
      mChannel.close();
    }

    public void onError(@NotNull final RoutineException error) {
      mChannel.abort(error);
    }

    public void onOutput(final IN output) {
      try {
        mChannel.pass(output);

      } catch (final InterruptedInvocationException e) {
        throw e;

      } catch (final Throwable ignored) {
      }
    }
  }

  public void onError(@NotNull final RoutineException error) {
    Long delay = null;
    if (!(error instanceof AbortException)) {
      try {
        delay = mBackoffFunction.apply(++mCount, error);

      } catch (final Throwable t) {
        abort(t);
        InterruptedInvocationException.throwIfInterrupt(t);
      }
    }

    if (delay != null) {
      mOutputs.clear();
      mExecutor.execute(this, delay, TimeUnit.MILLISECONDS);

    } else {
      mOutputChannel.abort(error);
      mReplayChannel.abort(error);
    }
  }

  public void onOutput(final OUT output) {
    mOutputs.add(output);
  }
}
