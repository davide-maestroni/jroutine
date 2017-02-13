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

package com.github.dm.jrt.android.v11.rx;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.JRoutineFlowable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of context invocations passing Flowable data to the result channel.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <DATA> the data type.
 */
class FlowableInvocationFactory<DATA> extends ContextInvocationFactory<Void, DATA> {

  private final Flowable<DATA> mFlowable;

  /**
   * Constructor.
   *
   * @param flowable the Flowable instance.
   */
  FlowableInvocationFactory(@NotNull final Flowable<DATA> flowable) {
    super(asArgs(ConstantConditions.notNull("flowable instance", flowable)));
    mFlowable = flowable;
  }

  @NotNull
  @Override
  public ContextInvocation<Void, DATA> newInvocation() {
    return new FlowableInvocation<DATA>(mFlowable);
  }

  /**
   * Context invocation passing Flowable data to the result channel.
   *
   * @param <DATA> the data type.
   */
  private static class FlowableInvocation<DATA> extends TemplateContextInvocation<Void, DATA> {

    private final Flowable<DATA> mFlowable;

    /**
     * Constructor.
     *
     * @param flowable the Flowable instance.
     */
    private FlowableInvocation(final Flowable<DATA> flowable) {
      mFlowable = flowable;
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) {
      JRoutineFlowable.with(mFlowable).buildChannel().bind(result);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }
}
