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

package com.github.dm.jrt.android.core;

import com.github.dm.jrt.android.core.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.core.builder.AbstractRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Class implementing a builder of routine objects executed in a dedicated Service.
 * <p>
 * Created by davide-maestroni on 01/08/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultServiceRoutineBuilder<IN, OUT> extends AbstractRoutineBuilder<IN, OUT>
    implements ServiceRoutineBuilder<IN, OUT> {

  private final ServiceContext mContext;

  private final TargetInvocationFactory<IN, OUT> mTargetFactory;

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param context the routine context.
   * @param target  the invocation factory target.
   */
  DefaultServiceRoutineBuilder(@NotNull final ServiceContext context,
      @NotNull final TargetInvocationFactory<IN, OUT> target) {
    mContext = ConstantConditions.notNull("Service context", context);
    mTargetFactory = ConstantConditions.notNull("target invocation factory", target);
  }

  @NotNull
  @Override
  public ServiceRoutineBuilder<IN, OUT> apply(
      @NotNull final InvocationConfiguration configuration) {
    super.apply(configuration);
    return this;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends ServiceRoutineBuilder<IN, OUT>>
  applyInvocationConfiguration() {
    return (Builder<? extends ServiceRoutineBuilder<IN, OUT>>) super.applyInvocationConfiguration();
  }

  @NotNull
  @Override
  public ServiceRoutineBuilder<IN, OUT> apply(@NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceRoutineBuilder<IN, OUT>>
  applyServiceConfiguration() {
    final ServiceConfiguration config = mServiceConfiguration;
    return new ServiceConfiguration.Builder<ServiceRoutineBuilder<IN, OUT>>(this, config);
  }

  @NotNull
  @Override
  public Routine<IN, OUT> buildRoutine() {
    return new ServiceRoutine<IN, OUT>(mContext, mTargetFactory, getConfiguration(),
        mServiceConfiguration);
  }
}
