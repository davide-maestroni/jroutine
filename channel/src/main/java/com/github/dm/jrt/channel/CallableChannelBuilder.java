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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Builder implementation returning a channel wrapping a Callable instance.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 *
 * @param <OUT> the output data type.
 */
class CallableChannelBuilder<OUT> extends AbstractChannelBuilder<Void, OUT> {

  private final Callable<OUT> mCallable;

  /**
   * Constructor.
   *
   * @param callable the Callable instance.
   */
  CallableChannelBuilder(@NotNull final Callable<OUT> callable) {
    mCallable = ConstantConditions.notNull("Callable instance", callable);
  }

  @NotNull
  public Channel<Void, OUT> buildChannel() {
    final InvocationConfiguration configuration =
        InvocationConfiguration.builderFromOutput(getConfiguration()).apply();
    return JRoutineCore.with(new CallableInvocation<OUT>(mCallable))
                       .invocationConfiguration()
                       .withRunner(Runners.immediateRunner())
                       .with(configuration)
                       .apply()
                       .close();
  }

  /**
   * Invocation backed by a Callable instance.
   *
   * @param <OUT> the output data type.
   */
  private static class CallableInvocation<OUT> extends CommandInvocation<OUT> {

    private final Callable<OUT> mCallable;

    /**
     * Constructor.
     *
     * @param callable the Callable instance.
     */
    private CallableInvocation(@NotNull final Callable<OUT> callable) {
      super(asArgs(callable));
      mCallable = callable;
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      result.pass(mCallable.call());
    }
  }
}
