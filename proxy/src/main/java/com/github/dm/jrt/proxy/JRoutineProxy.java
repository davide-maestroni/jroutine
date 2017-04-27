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

package com.github.dm.jrt.proxy;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods.
 * <p>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.
 * <br>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p>
 * Created by davide-maestroni on 03/23/2015.
 *
 * @see com.github.dm.jrt.proxy.annotation.Proxy Proxy
 * @see com.github.dm.jrt.reflect.annotation Annotations
 */
public class JRoutineProxy {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineProxy() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of routines running on the specified executor, wrapping a target object.
   *
   * @return the routine builder instance.
   */
  @NotNull
  public static ProxyRoutineBuilder wrapper() {
    return wrapperOn(defaultExecutor());
  }

  /**
   * Returns a builder of routines wrapping a target object.
   *
   * @param executor the executor instance.
   * @return the routine builder instance.
   */
  @NotNull
  public static ProxyRoutineBuilder wrapperOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultProxyRoutineBuilder(executor);
  }
}
