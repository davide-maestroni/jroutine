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
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDecorator;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Invocation decorator used to test the correctness of an invocation lifecycle.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
@SuppressWarnings("unused")
public class LifecycleInvocationDecorator<IN, OUT> extends InvocationDecorator<IN, OUT> {

  private static final List<State> TO_ABORT_STATES =
      Arrays.asList(State.START, State.INPUT, State.COMPLETE);

  private static final List<State> TO_INPUT_STATES = Arrays.asList(State.START, State.INPUT);

  private static final List<State> TO_RECYCLE_STATES =
      Arrays.asList(State.START, State.ABORT, State.COMPLETE, State.RECYCLE);

  private static final List<State> TO_RESULT_STATES = Arrays.asList(State.START, State.INPUT);

  private static final List<State> TO_START_STATES =
      Arrays.asList(State.ABORT, State.RECYCLE, null);

  private State mState;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped invocation instance.?
   */
  public LifecycleInvocationDecorator(@NotNull final Invocation<IN, OUT> wrapped) {
    super(wrapped);
  }

  @Override
  public void onAbort(@NotNull final RoutineException reason) throws Exception {
    if (!TO_ABORT_STATES.contains(mState)) {
      throw new InvocationInterruptedException(null);
    }

    mState = State.ABORT;
    super.onAbort(reason);
  }

  @Override
  public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
    if (!TO_RESULT_STATES.contains(mState)) {
      throw new InvocationInterruptedException(null);
    }

    mState = State.COMPLETE;
    super.onComplete(result);
  }

  @Override
  public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
    if (!TO_INPUT_STATES.contains(mState)) {
      throw new InvocationInterruptedException(null);
    }

    mState = State.INPUT;
    super.onInput(input, result);
  }

  @Override
  public void onRecycle(final boolean isReused) throws Exception {
    if (!TO_RECYCLE_STATES.contains(mState)) {
      throw new InvocationInterruptedException(null);
    }

    if (isReused && (mState == State.RECYCLE)) {
      throw new InvocationInterruptedException(null);
    }

    mState = State.RECYCLE;
    super.onRecycle(isReused);
  }

  @Override
  public void onRestart() throws Exception {
    if (!TO_START_STATES.contains(mState)) {
      throw new InvocationInterruptedException(null);
    }

    mState = State.START;
    super.onRestart();
  }

  private enum State {
    START,
    INPUT,
    COMPLETE,
    ABORT,
    RECYCLE,
  }
}
