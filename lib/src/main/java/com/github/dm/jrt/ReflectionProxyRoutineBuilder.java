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

package com.github.dm.jrt;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilder;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Routine builder acting both as proxy and reflection builder.
 * <p>
 * The builder will automatically choose whether to employ reflection or code generation to build
 * the proxy instance, based on the presence of the proper annotation and target value. So, if the
 * pre-processor annotation is present in the proxy interface and the target object is assignable to
 * the annotation target class, then code generation will be employed, reflection otherwise.
 * <br>
 * Note that the use of one or the other can be forced by calling the proper method.
 * <p>
 * Created by davide-maestroni on 03/03/2016.
 */
public interface ReflectionProxyRoutineBuilder extends ReflectionRoutineBuilder {

  /**
   * {@inheritDoc}
   */
  @NotNull
  ReflectionProxyRoutineBuilder apply(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  ReflectionProxyRoutineBuilder apply(@NotNull WrapperConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  InvocationConfiguration.Builder<? extends ReflectionProxyRoutineBuilder>
  invocationConfiguration();

  /**
   * Force the type of strategy to be employed to create the proxy instance.
   * <br>
   * A null value means default algorithm will be applied, that is, the type will be automatically
   * chosen based on the presence of the specific annotation.
   *
   * @param strategyType the strategy type.
   * @return this builder.
   * @see com.github.dm.jrt.proxy.JRoutineProxy JRoutineProxy
   */
  @NotNull
  ReflectionProxyRoutineBuilder withStrategy(@Nullable ProxyStrategyType strategyType);

  /**
   * {@inheritDoc}
   */
  @NotNull
  WrapperConfiguration.Builder<? extends ReflectionProxyRoutineBuilder> wrapperConfiguration();

  /**
   * Proxy instantiation strategy type enumeration.
   */
  enum ProxyStrategyType {

    /**
     * Reflection strategy.
     * <br>
     * The proxy instance will be created through reflection.
     */
    REFLECTION,
    /**
     * Code generation strategy.
     * <br>
     * The proxy instance will be created through code generation.
     */
    CODE_GENERATION
  }
}
