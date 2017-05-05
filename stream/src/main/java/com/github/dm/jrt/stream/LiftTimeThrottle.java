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
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Lifting function making the stream throttle the invocation instances.
 * <p>
 * Created by davide-maestroni on 07/30/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class LiftTimeThrottle<IN, OUT> implements LiftingFunction<IN, OUT, IN, OUT> {

  private final ChannelConfiguration mConfiguration;

  private final ScheduledExecutor mExecutor;

  private final int mMaxCount;

  private final Object mMutex = new Object();

  private final SimpleQueue<Runnable> mQueue = new SimpleQueue<Runnable>();

  private final long mRangeMillis;

  private int mCount;

  private long mNextTimeSlot = Long.MIN_VALUE;

  /**
   * Constructor.
   *
   * @param executor      the executor instance.
   * @param configuration the channel configuration.
   * @param count         the maximum invocation count.
   * @param range         the time range value.
   * @param timeUnit      the time range unit.
   */
  LiftTimeThrottle(@NotNull final ScheduledExecutor executor,
      @NotNull final ChannelConfiguration configuration, final int count, final long range,
      @NotNull final TimeUnit timeUnit) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    mMaxCount = ConstantConditions.positive("max count", count);
    mRangeMillis = timeUnit.toMillis(range);
  }

  public Supplier<? extends Channel<IN, OUT>> apply(
      final Supplier<? extends Channel<IN, OUT>> supplier) {
    return new ThrottleSupplier(supplier);
  }

  /**
   * Binding function implementation.
   */
  private class ThrottleSupplier implements Supplier<Channel<IN, OUT>>, Runnable {

    private final Supplier<? extends Channel<IN, OUT>> mChannelSupplier;

    /**
     * Constructor.
     *
     * @param channelSupplier the channel supplier.
     */
    private ThrottleSupplier(@NotNull final Supplier<? extends Channel<IN, OUT>> channelSupplier) {
      mChannelSupplier = channelSupplier;
    }

    public Channel<IN, OUT> get() throws Exception {
      final ScheduledExecutor executor = mExecutor;
      final Channel<IN, IN> inputChannel = JRoutineCore.channelOn(executor).ofType();
      final Channel<OUT, OUT> outputChannel =
          JRoutineCore.channelOn(executor).withConfiguration(mConfiguration).ofType();
      final long delay;
      final boolean isBind;
      synchronized (mMutex) {
        final long rangeMillis = mRangeMillis;
        final long nextTimeSlot = mNextTimeSlot;
        final long now = System.currentTimeMillis();
        if ((nextTimeSlot == Long.MIN_VALUE) || (now >= (nextTimeSlot + rangeMillis))) {
          mNextTimeSlot = now + rangeMillis;
          mCount = 0;
        }

        final int maxCount = mMaxCount;
        isBind = (++mCount <= maxCount);
        if (!isBind) {
          final SimpleQueue<Runnable> queue = mQueue;
          queue.add(new Runnable() {

            public void run() {
              try {
                final Channel<IN, OUT> invocationChannel = mChannelSupplier.get();
                outputChannel.pass(invocationChannel).close();
                invocationChannel.pass(inputChannel).close();

              } catch (final Throwable t) {
                outputChannel.abort(t);
                InterruptedInvocationException.throwIfInterrupt(t);
              }
            }
          });

          delay = (((queue.size() - 1) / maxCount) + 1) * rangeMillis;

        } else {
          delay = 0;
        }
      }

      if (isBind) {
        final Channel<IN, OUT> invocationChannel = mChannelSupplier.get();
        outputChannel.pass(invocationChannel).close();
        invocationChannel.pass(inputChannel).close();

      } else {
        executor.execute(this, delay, TimeUnit.MILLISECONDS);
      }

      return JRoutineCore.flatten(inputChannel, JRoutineCore.readOnly(outputChannel));
    }

    public void run() {
      final Runnable runnable;
      synchronized (mMutex) {
        final SimpleQueue<Runnable> queue = mQueue;
        if (queue.isEmpty()) {
          return;
        }

        runnable = queue.removeFirst();
      }

      runnable.run();
    }
  }
}
