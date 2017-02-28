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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base invocation transforming data coming from an input channel into an output one.
 * <p>
 * Created by davide-maestroni on 06/11/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ChannelInvocation<IN, OUT> implements Invocation<IN, OUT> {

  private Channel<IN, IN> mInputChannel;

  private Channel<?, OUT> mOutputChannel;

  /**
   * Constructor.
   */
  public ChannelInvocation() {
    mInputChannel = null;
    mOutputChannel = null;
  }

  public final void onAbort(@NotNull final RoutineException reason) {
    mInputChannel.abort(reason);
  }

  public final void onComplete(@NotNull final Channel<OUT, ?> result) {
    bind(result);
    mInputChannel.close();
  }

  public void onDestroy() {
  }

  public final void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
    bind(result);
    mInputChannel.pass(input);
  }

  public boolean onRecycle() throws Exception {
    mInputChannel = null;
    mOutputChannel = null;
    return true;
  }

  public final void onRestart() throws Exception {
    final Channel<IN, IN> inputChannel = (mInputChannel = JRoutineCore.<IN>ofData().buildChannel());
    mOutputChannel = ConstantConditions.notNull("stream channel", onChannel(inputChannel));
  }

  /**
   * Transforms the channel providing the input data into the output one.
   *
   * @param channel the input channel.
   * @return the output channel.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  protected abstract Channel<?, OUT> onChannel(@NotNull Channel<?, IN> channel) throws Exception;

  private void bind(@NotNull final Channel<OUT, ?> result) {
    final Channel<?, OUT> outputChannel = mOutputChannel;
    if (!outputChannel.isBound()) {
      result.pass(outputChannel);
    }
  }
}
