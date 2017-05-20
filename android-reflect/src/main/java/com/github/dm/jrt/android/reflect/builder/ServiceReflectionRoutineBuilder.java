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

package com.github.dm.jrt.android.reflect.builder;

import com.github.dm.jrt.android.core.config.ServiceConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines wrapping an object methods, running in a dedicated
 * Service instance.
 * <p>
 * Created by davide-maestroni on 03/29/2015.
 */
public interface ServiceReflectionRoutineBuilder
    extends ReflectionContextRoutineBuilder, ServiceConfigurable<ServiceReflectionRoutineBuilder> {

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ServiceReflectionRoutineBuilder withConfiguration(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ServiceReflectionRoutineBuilder withConfiguration(@NotNull WrapperConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationConfiguration.Builder<? extends ServiceReflectionRoutineBuilder> withInvocation();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  WrapperConfiguration.Builder<? extends ServiceReflectionRoutineBuilder> withWrapper();
}
