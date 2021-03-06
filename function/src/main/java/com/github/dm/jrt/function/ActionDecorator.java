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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class decorating an action instance.
 * <p>
 * Created by davide-maestroni on 06/29/2016.
 */
@SuppressWarnings("WeakerAccess")
public class ActionDecorator extends DeepEqualObject implements Action, Decorator {

  private static final ActionDecorator sNoOp = new ActionDecorator(new Action() {

    public void perform() {
    }
  });

  private final List<Action> mActions;

  /**
   * Constructor.
   *
   * @param action the wrapped action.
   */
  private ActionDecorator(@NotNull final Action action) {
    this(Collections.singletonList(ConstantConditions.notNull("action instance", action)));
  }

  /**
   * Constructor.
   *
   * @param actions the list of wrapped actions.
   */
  private ActionDecorator(@NotNull final List<Action> actions) {
    super(asArgs(actions));
    mActions = actions;
  }

  /**
   * Decorates the specified action instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param action the action instance.
   * @return the decorated action.
   */
  @NotNull
  public static ActionDecorator decorate(@NotNull final Action action) {
    if (action instanceof ActionDecorator) {
      return (ActionDecorator) action;
    }

    return new ActionDecorator(action);
  }

  /**
   * Decorates the specified runnable instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param action the runnable instance.
   * @return the decorated action.
   */
  @NotNull
  public static ActionDecorator decorate(@NotNull final Runnable action) {
    return new ActionDecorator(new ActionRunnable(action));
  }

  /**
   * Returns an action decorator doing nothing.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @return the action decorator.
   */
  @NotNull
  public static ActionDecorator noOp() {
    return sNoOp;
  }

  /**
   * Returns a composed action decorator that performs, in sequence, this operation followed by
   * the after operation.
   *
   * @param after the operation to perform after this operation.
   * @return the composed action.
   */
  @NotNull
  public ActionDecorator andThen(@NotNull final Action after) {
    ConstantConditions.notNull("action instance", after);
    final List<Action> actions = mActions;
    final ArrayList<Action> newActions = new ArrayList<Action>(actions.size() + 1);
    newActions.addAll(actions);
    if (after instanceof ActionDecorator) {
      newActions.addAll(((ActionDecorator) after).mActions);

    } else {
      newActions.add(after);
    }

    return new ActionDecorator(newActions);
  }

  /**
   * Returns a composed action decorator that performs, in sequence, this operation followed by
   * the after operation.
   *
   * @param after the operation to perform after this operation.
   * @return the composed action.
   */
  @NotNull
  public ActionDecorator andThen(@NotNull final Runnable after) {
    ConstantConditions.notNull("runnable instance", after);
    final List<Action> actions = mActions;
    final ArrayList<Action> newActions = new ArrayList<Action>(actions.size() + 1);
    newActions.addAll(actions);
    newActions.add(new ActionRunnable(after));
    return new ActionDecorator(newActions);
  }

  public boolean hasStaticScope() {
    for (final Action action : mActions) {
      if (!Reflection.hasStaticScope(action) || ((action instanceof ActionRunnable)
          && !Reflection.hasStaticScope(((ActionRunnable) action).mRunnable))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Implementation of an action executing a runnable.
   */
  private static class ActionRunnable extends DeepEqualObject implements Action {

    private final Runnable mRunnable;

    /**
     * Constructor.
     *
     * @param runnable the runnable instance.
     */
    private ActionRunnable(@NotNull final Runnable runnable) {
      super(asArgs(ConstantConditions.notNull("runnable instance", runnable)));
      mRunnable = runnable;
    }

    public void perform() {
      mRunnable.run();
    }
  }

  public void perform() throws Exception {
    for (final Action action : mActions) {
      action.perform();
    }
  }
}
