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

package com.github.dm.jrt.android.proxy.builder;

import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.reflect.config.WrapperConfigurable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 05/23/2017.
 */
public interface ProxyContextRoutineBuilder
    extends InvocationConfigurable<ProxyContextRoutineBuilder>,
    WrapperConfigurable<ProxyContextRoutineBuilder> {

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <br>
   * Note that you'll need to enable annotation pre-processing by adding the processor artifact
   * to the specific project dependencies.
   *
   * @param target the invocation target.
   * @param itf    the interface implemented by the return object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the specified proxy is not an interface.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   */
  @NotNull
  <TYPE> TYPE proxyOf(@NotNull ContextInvocationTarget<?> target, @NotNull Class<TYPE> itf);

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
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
  <TYPE> TYPE proxyOf(@NotNull ContextInvocationTarget<?> target, @NotNull ClassToken<TYPE> itf);
}
