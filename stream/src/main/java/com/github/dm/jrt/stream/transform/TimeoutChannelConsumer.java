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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Timeout channel consumer.
 * <p>
 * Created by davide-maestroni on 07/29/2016.
 *
 * @param <OUT> the output data type.
 */
class TimeoutChannelConsumer<OUT> implements ChannelConsumer<OUT> {

  private final AtomicLong mCount = new AtomicLong();

  private final Channel<OUT, ?> mOutputChannel;

  private final Runner mRunner;

  private final long mTimeout;

  private final TimeUnit mTimeoutUnit;

  private AbortExecution mExecution = new AbortExecution();

  /**
   * Constructor.
   *
   * @param timeout       the timeout value.
   * @param timeUnit      the timeout unit.
   * @param runner        the runner instance.
   * @param outputChannel the output channel.
   */
  TimeoutChannelConsumer(final long timeout, @NotNull final TimeUnit timeUnit,
      @NotNull final Runner runner, @NotNull final Channel<OUT, ?> outputChannel) {
    mTimeout = ConstantConditions.notNegative("timeout value", timeout);
    mTimeoutUnit = ConstantConditions.notNull("timeout unit", timeUnit);
    mRunner = ConstantConditions.notNull("runner instance", runner);
    mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
  }

  public void onComplete() throws Exception {
    mOutputChannel.close();
  }

  public void onError(@NotNull final RoutineException error) {
    mOutputChannel.abort(error);
  }

  public void onOutput(final OUT output) {
    restartTimeout();
    mOutputChannel.pass(output);
  }

  private void restartTimeout() {
    final Runner runner = mRunner;
    runner.cancel(mExecution);
    final AbortExecution execution = (mExecution = new AbortExecution());
    runner.run(execution, mTimeout, mTimeoutUnit);
  }

  /**
   * Execution aborting the output channel.
   */
  private class AbortExecution implements Execution {

    private final long mExecutionCount;

    /**
     * Constructor.
     */
    AbortExecution() {
      mExecutionCount = mCount.incrementAndGet();
    }

    public void run() {
      if (mExecutionCount == mCount.get()) {
        mOutputChannel.abort(new ResultTimeoutException(
            "timeout while waiting for inputs: [" + mTimeout + " " + mTimeout + "]"));
      }
    }
  }
}
