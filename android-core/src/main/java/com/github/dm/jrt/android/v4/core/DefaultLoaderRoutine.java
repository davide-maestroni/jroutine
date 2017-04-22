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

package com.github.dm.jrt.android.v4.core;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.android.core.executor.AndroidExecutors.mainExecutor;
import static com.github.dm.jrt.android.v4.core.LoaderInvocation.clearLoaders;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.zeroDelayExecutor;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Routine implementation delegating to Android Loaders the asynchronous processing.
 * <p>
 * Created by davide-maestroni on 01/10/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
    implements LoaderRoutine<IN, OUT> {

  private final LoaderConfiguration mConfiguration;

  private final LoaderContextCompat mContext;

  private final ContextInvocationFactory<IN, OUT> mFactory;

  private final int mLoaderId;

  private final OrderType mOrderType;

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param context                 the routine context.
   * @param factory                 the invocation factory.
   * @param invocationConfiguration the invocation configuration.
   * @param loaderConfiguration     the Loader configuration.
   */
  DefaultLoaderRoutine(@NotNull final LoaderContextCompat context,
      @NotNull final ContextInvocationFactory<IN, OUT> factory,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final LoaderConfiguration loaderConfiguration) {
    super(invocationConfiguration.builderFrom().withExecutor(zeroDelayExecutor(mainExecutor())).apply());
    final int invocationId = loaderConfiguration.getInvocationIdOrElse(LoaderConfiguration.AUTO);
    mContext = ConstantConditions.notNull("Loader context", context);
    ConstantConditions.notNull("Context invocation factory", factory);
    mFactory = (invocationId == LoaderConfiguration.AUTO) ? factory
        : new FactoryWrapper<IN, OUT>(factory, invocationId);
    mConfiguration = loaderConfiguration;
    mLoaderId = loaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO);
    mOrderType = invocationConfiguration.getOutputOrderTypeOrElse(null);
    mExecutor = invocationConfiguration.getExecutorOrElse(null);
    getLogger().dbg("building Context routine with configuration: %s", loaderConfiguration);
  }

  @Override
  public void clear() {
    super.clear();
    final LoaderContextCompat context = mContext;
    if (context.getComponent() != null) {
      clearLoaders(context, mLoaderId, mFactory);
    }
  }

  @NotNull
  @Override
  protected Invocation<IN, OUT> newInvocation() {
    return new LoaderInvocation<IN, OUT>(mContext, mFactory, mConfiguration, mExecutor, mOrderType,
        getLogger());
  }

  @Override
  public void clear(@Nullable final IN input) {
    final LoaderContextCompat context = mContext;
    if (context.getComponent() != null) {
      clearLoaders(context, mLoaderId, mFactory, Collections.singletonList(input));
    }
  }

  public void clear(@Nullable final IN... inputs) {
    final LoaderContextCompat context = mContext;
    if (context.getComponent() != null) {
      final List<IN> inputList;
      if (inputs == null) {
        inputList = Collections.emptyList();

      } else {
        inputList = Arrays.asList(inputs);
      }

      clearLoaders(context, mLoaderId, mFactory, inputList);
    }
  }

  @Override
  public void clear(@Nullable final Iterable<? extends IN> inputs) {
    final LoaderContextCompat context = mContext;
    if (context.getComponent() != null) {
      final List<IN> inputList;
      if (inputs == null) {
        inputList = Collections.emptyList();

      } else {
        inputList = new ArrayList<IN>();
        for (final IN input : inputs) {
          inputList.add(input);
        }
      }

      clearLoaders(context, mLoaderId, mFactory, inputList);
    }
  }

  /**
   * Wrapper of call Context invocation factories overriding {@code equals()} and
   * {@code hashCode()}.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class FactoryWrapper<IN, OUT> extends ContextInvocationFactory<IN, OUT> {

    private final ContextInvocationFactory<IN, OUT> mFactory;

    /**
     * Constructor.
     *
     * @param factory      the wrapped factory.
     * @param invocationId the invocation ID.
     */
    private FactoryWrapper(@NotNull final ContextInvocationFactory<IN, OUT> factory,
        final int invocationId) {
      super(asArgs(invocationId));
      mFactory = factory;
    }

    @NotNull
    @Override
    public ContextInvocation<IN, OUT> newInvocation() throws Exception {
      return mFactory.newInvocation();
    }
  }
}
