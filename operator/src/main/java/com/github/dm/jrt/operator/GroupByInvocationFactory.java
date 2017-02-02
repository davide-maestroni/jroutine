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
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of grouping invocations.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the data type.
 */
class GroupByInvocationFactory<DATA> extends InvocationFactory<DATA, List<DATA>> {

  private final boolean mIsPlaceholder;

  private final DATA mPlaceholder;

  private final int mSize;

  /**
   * Constructor.
   *
   * @param size the group size.
   * @throws java.lang.IllegalArgumentException if the size is not positive.
   */
  GroupByInvocationFactory(final int size) {
    super(asArgs(ConstantConditions.positive("group size", size)));
    mSize = size;
    mPlaceholder = null;
    mIsPlaceholder = false;
  }

  /**
   * Constructor.
   *
   * @param size        the group size.
   * @param placeholder the placeholder object used to fill the missing data needed to reach the
   *                    group size.
   * @throws java.lang.IllegalArgumentException if the size is not positive.
   */
  GroupByInvocationFactory(final int size, @Nullable final DATA placeholder) {
    super(asArgs(ConstantConditions.positive("group size", size), placeholder));
    mSize = size;
    mPlaceholder = placeholder;
    mIsPlaceholder = true;
  }

  @NotNull
  @Override
  public Invocation<DATA, List<DATA>> newInvocation() {
    return (mIsPlaceholder) ? new GroupByInvocation<DATA>(mSize, mPlaceholder)
        : new GroupByInvocation<DATA>(mSize);
  }

  /**
   * Routine invocation grouping data into collections of the same size.
   *
   * @param <DATA> the data type.
   */
  private static class GroupByInvocation<DATA> extends TemplateInvocation<DATA, List<DATA>> {

    private final ArrayList<DATA> mInputs = new ArrayList<DATA>();

    private final boolean mIsPlaceholder;

    private final DATA mPlaceholder;

    private final int mSize;

    /**
     * Constructor.
     *
     * @param size the group size.
     */
    private GroupByInvocation(final int size) {
      mSize = size;
      mPlaceholder = null;
      mIsPlaceholder = false;
    }

    /**
     * Constructor.
     *
     * @param size        the group size.
     * @param placeholder the placeholder object used to fill the missing data needed to reach
     *                    the group size.
     */
    private GroupByInvocation(final int size, @Nullable final DATA placeholder) {
      mSize = size;
      mPlaceholder = placeholder;
      mIsPlaceholder = true;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mInputs.clear();
    }

    @Override
    public void onComplete(@NotNull final Channel<List<DATA>, ?> result) {
      final ArrayList<DATA> inputs = mInputs;
      final int inputSize = inputs.size();
      if (inputSize > 0) {
        final ArrayList<DATA> data = new ArrayList<DATA>(inputs);
        final int size = mSize - inputSize;
        if (mIsPlaceholder && (size > 0)) {
          data.addAll(Collections.nCopies(size, mPlaceholder));
        }

        result.pass(data);
        inputs.clear();
      }
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<List<DATA>, ?> result) {
      final ArrayList<DATA> inputs = mInputs;
      final int size = mSize;
      if (inputs.size() < size) {
        inputs.add(input);
        if (inputs.size() == size) {
          result.pass(new ArrayList<DATA>(inputs));
          inputs.clear();
        }
      }
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }
}
