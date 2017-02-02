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

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * Invocation filtering out inputs which are not unique (according to the {@code equals(Object)}
 * method).
 * <p>
 * Created by davide-maestroni on 05/01/2016.
 *
 * @param <DATA> the data type.
 */
class DistinctInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

  private static final InvocationFactory<?, ?> sFactory =
      new InvocationFactory<Object, Object>(null) {

        @NotNull
        @Override
        public Invocation<Object, Object> newInvocation() {
          return new DistinctInvocation<Object>();
        }
      };

  private HashSet<DATA> mSet;

  /**
   * Constructor.
   */
  private DistinctInvocation() {
  }

  /**
   * Returns a factory of invocations filtering out inputs which are not unique.
   *
   * @param <DATA> the data type.
   * @return the factory instance.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  static <DATA> InvocationFactory<DATA, DATA> factoryOf() {
    return (InvocationFactory<DATA, DATA>) sFactory;
  }

  @Override
  public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
    if (mSet.add(input)) {
      result.pass(input);
    }
  }

  @Override
  public boolean onRecycle(final boolean isReused) {
    mSet = null;
    return true;
  }

  @Override
  public void onRestart() {
    mSet = new HashSet<DATA>();
  }
}
