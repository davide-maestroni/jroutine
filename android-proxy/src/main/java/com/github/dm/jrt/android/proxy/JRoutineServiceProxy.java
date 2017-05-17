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

package com.github.dm.jrt.android.proxy;

import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods in a dedicated Service.
 * <p>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.
 * <br>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 *
 * @see com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy
 * @see com.github.dm.jrt.reflect.annotation Annotations
 */
public class JRoutineServiceProxy {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineServiceProxy() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a Context based builder of Service proxy routine builders.
   *
   * @param serviceSource the Service source.
   * @return the Context based builder.
   */
  @NotNull
  public static ServiceProxyBuilder on(@NotNull final ServiceSource serviceSource) {
    return new ServiceProxyBuilder(serviceSource);
  }

  /**
   * Context based builder of Service routine builders.
   */
  @SuppressWarnings("WeakerAccess")
  public static class ServiceProxyBuilder {

    private final ServiceSource mmServiceSource;

    /**
     * Constructor.
     *
     * @param serviceSource the Service source.
     */
    private ServiceProxyBuilder(@NotNull final ServiceSource serviceSource) {
      mmServiceSource = ConstantConditions.notNull("Service source", serviceSource);
    }

    /**
     * Returns a builder of routines, wrapping the specified object instance, running in a Service
     * based on the builder context.
     * <br>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.github.dm.jrt.android.reflect.builder.FactoryContext FactoryContext} as the
     * invocation Service.
     * <p>
     * Note that the built routine results will be dispatched into the configured Looper, thus,
     * waiting for the outputs on the very same Looper thread, immediately after its invocation,
     * will result in a deadlock. By default, output results are dispatched in the main Looper.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     */
    @NotNull
    public ServiceProxyRoutineBuilder with(@NotNull final ContextInvocationTarget<?> target) {
      return new DefaultServiceProxyRoutineBuilder(mmServiceSource, target);
    }
  }
}
