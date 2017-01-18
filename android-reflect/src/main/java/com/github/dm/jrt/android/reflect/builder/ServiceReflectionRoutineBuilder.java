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
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilder;
import com.github.dm.jrt.reflect.config.ReflectionConfiguration;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines wrapping an object methods.
 * <p>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p>
 * Created by davide-maestroni on 03/29/2015.
 */
public interface ServiceReflectionRoutineBuilder
    extends ReflectionRoutineBuilder, ServiceConfigurable<ServiceReflectionRoutineBuilder> {

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ServiceReflectionRoutineBuilder apply(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ServiceReflectionRoutineBuilder apply(@NotNull ReflectionConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationConfiguration.Builder<? extends ServiceReflectionRoutineBuilder>
  applyInvocationConfiguration();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ReflectionConfiguration.Builder<? extends ServiceReflectionRoutineBuilder>
  applyReflectionConfiguration();
}
