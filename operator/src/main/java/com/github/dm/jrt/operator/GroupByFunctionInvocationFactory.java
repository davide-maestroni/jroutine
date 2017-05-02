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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.function.util.Function;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.FunctionDecorator.wrapFunction;

/**
 * Factory of grouping by key invocations.
 * <p>
 * Created by davide-maestroni on 08/30/2016.
 *
 * @param <DATA> the data type.
 */
class GroupByFunctionInvocationFactory<DATA> extends InvocationFactory<DATA, List<DATA>> {

  private final Function<DATA, Object> mKeyFunction;

  /**
   * Constructor.
   *
   * @param keyFunction the function returning the group key.
   */
  GroupByFunctionInvocationFactory(@NotNull final Function<DATA, Object> keyFunction) {
    super(asArgs(wrapFunction(keyFunction)));
    mKeyFunction = keyFunction;
  }

  @NotNull
  @Override
  public Invocation<DATA, List<DATA>> newInvocation() {
    return new GroupByFunctionInvocation<DATA>(mKeyFunction);
  }

  /**
   * Routine invocation grouping data into collections based on the same key.
   *
   * @param <DATA> the data type.
   */
  private static class GroupByFunctionInvocation<DATA>
      extends TemplateInvocation<DATA, List<DATA>> {

    private final Function<DATA, Object> mKeyFunction;

    private HashMap<Object, ArrayList<DATA>> mGroups;

    /**
     * Constructor.
     *
     * @param keyFunction the function returning the group key.
     */
    private GroupByFunctionInvocation(@NotNull final Function<DATA, Object> keyFunction) {
      mKeyFunction = keyFunction;
    }

    @Override
    public void onComplete(@NotNull final Channel<List<DATA>, ?> result) {
      for (final ArrayList<DATA> inputs : mGroups.values()) {
        result.pass(inputs);
      }
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<List<DATA>, ?> result) throws
        Exception {
      final Object key = mKeyFunction.apply(input);
      final HashMap<Object, ArrayList<DATA>> groups = mGroups;
      ArrayList<DATA> inputs = groups.get(key);
      if (inputs == null) {
        inputs = new ArrayList<DATA>();
        groups.put(key, inputs);
      }

      inputs.add(input);
    }

    @Override
    public boolean onRecycle() {
      mGroups = null;
      return true;
    }

    @Override
    public void onStart() {
      mGroups = new HashMap<Object, ArrayList<DATA>>();
    }
  }
}
