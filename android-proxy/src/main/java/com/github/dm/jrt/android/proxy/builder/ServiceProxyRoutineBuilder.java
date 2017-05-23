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

package com.github.dm.jrt.android.proxy.builder;

import com.github.dm.jrt.android.core.config.ServiceConfigurable;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines wrapping an object, whose methods are executed in a
 * dedicated Service.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 */
public interface ServiceProxyRoutineBuilder
    extends ProxyContextRoutineBuilder, ServiceConfigurable<ServiceProxyRoutineBuilder> {

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> annotations.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * The proxy object is created through code generation based on the interfaces annotated with
   * {@link com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy}. The generated
   * class name and package will be chosen according to the specific annotation attributes.
   * <br>
   * It is actually possible to avoid the use of reflection for the proxy object instantiation by
   * explicitly calling the {@code &lt;generated_class_name&gt;.wrapperOn()} method.
   * <br>
   * Note that you'll need to enable annotation pre-processing by adding the processor artifact
   * to the specific project dependencies.
   *
   * @param target the invocation target.
   * @param itf    the token of the interface implemented by the return object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the specified proxy is not an interface.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   */
  @NotNull
  @Override
  <TYPE> TYPE proxyOf(@NotNull ContextInvocationTarget<?> target, @NotNull Class<TYPE> itf);

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> annotations.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * The proxy object is created through code generation based on the interfaces annotated with
   * {@link com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy}. The generated
   * class name and package will be chosen according to the specific annotation attributes.
   * <br>
   * It is actually possible to avoid the use of reflection for the proxy object instantiation by
   * explicitly calling the {@code &lt;generated_class_name&gt;.wrapperOn()} method.
   * <br>
   * Note that you'll need to enable annotation pre-processing by adding the processor artifact
   * to the specific project dependencies.
   *
   * @param target the invocation target.
   * @param itf    the token of the interface implemented by the return object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the specified proxy is not an interface.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   */
  @NotNull
  @Override
  <TYPE> TYPE proxyOf(@NotNull ContextInvocationTarget<?> target, @NotNull ClassToken<TYPE> itf);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ServiceProxyRoutineBuilder withConfiguration(@NotNull WrapperConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  ServiceProxyRoutineBuilder withConfiguration(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withInvocation();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  WrapperConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withWrapper();
}
