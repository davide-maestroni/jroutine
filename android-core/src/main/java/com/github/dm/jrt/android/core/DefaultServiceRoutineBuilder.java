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
import com.github.dm.jrt.android.core.invocation.InvocationFactoryReference;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Class implementing a builder of routine objects executed in a dedicated Service.
 * <p>
 * Created by davide-maestroni on 01/08/2015.
 */
class DefaultServiceRoutineBuilder implements ServiceRoutineBuilder {

  private final ServiceSource mServiceSource;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param serviceSource the Service source.
   */
  DefaultServiceRoutineBuilder(@NotNull final ServiceSource serviceSource) {
    mServiceSource = ConstantConditions.notNull("Service source", serviceSource);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> of(@NotNull final InvocationFactoryReference<IN, OUT> target) {
    return new ServiceRoutine<IN, OUT>(mServiceSource, target, mInvocationConfiguration,
        mServiceConfiguration);
  }

  @NotNull
  @Override
  public ServiceRoutineBuilder withConfiguration(
      @NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceRoutineBuilder> withInvocation() {
    return new InvocationConfiguration.Builder<ServiceRoutineBuilder>(this,
        mInvocationConfiguration);
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceRoutineBuilder> withService() {
    return new ServiceConfiguration.Builder<ServiceRoutineBuilder>(this, mServiceConfiguration);
  }
}
