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
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDecorator;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
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

  private static final List<State> TO_DESTROY_STATES = Collections.singletonList(State.RECYCLE);

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

  private static void throwInvalidState(final State state) {
    throw new LifecycleInterruptedException(
        "Cannot call " + Thread.currentThread().getStackTrace()[2].getMethodName() + "() in state: "
            + state);
  }

  @Override
  public void onAbort(@NotNull final RoutineException reason) throws Exception {
    if (!TO_ABORT_STATES.contains(mState)) {
      throwInvalidState(mState);
    }

    mState = State.ABORT;
    super.onAbort(reason);
  }

  @Override
  public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
    if (!TO_RESULT_STATES.contains(mState)) {
      throwInvalidState(mState);
    }

    mState = State.COMPLETE;
    super.onComplete(result);
  }

  @Override
  public void onDestroy() throws Exception {
    if (!TO_DESTROY_STATES.contains(mState)) {
      throwInvalidState(mState);
    }

    mState = State.DESTROY;
    super.onDestroy();
  }

  @Override
  public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
    if (!TO_INPUT_STATES.contains(mState)) {
      throwInvalidState(mState);
    }

    mState = State.INPUT;
    super.onInput(input, result);
  }

  @Override
  public boolean onRecycle() throws Exception {
    if (!TO_RECYCLE_STATES.contains(mState)) {
      throwInvalidState(mState);
    }

    mState = State.RECYCLE;
    return super.onRecycle();
  }

  @Override
  public void onRestart() throws Exception {
    if (!TO_START_STATES.contains(mState)) {
      throwInvalidState(mState);
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
    DESTROY
  }

  /**
   * Interrupted exception.
   */
  private static class LifecycleInterruptedException extends InterruptedInvocationException {

    private final String mMessage;

    /**
     * Constructor.
     *
     * @param message the error message.
     */
    private LifecycleInterruptedException(final String message) {
      super(null);
      mMessage = message;
    }

    @Override
    public String getMessage() {
      return mMessage;
    }
  }
}
