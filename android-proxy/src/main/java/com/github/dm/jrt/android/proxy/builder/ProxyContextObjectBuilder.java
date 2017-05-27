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
import com.github.dm.jrt.reflect.config.WrapperConfigurable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of async proxy objects.
 * <p>
 * Created by davide-maestroni on 05/23/2017.
 *
 * @param <TYPE> the interface type.
 */
public interface ProxyContextObjectBuilder<TYPE>
    extends InvocationConfigurable<ProxyContextObjectBuilder<TYPE>>,
    WrapperConfigurable<ProxyContextObjectBuilder<TYPE>> {

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.annotation.*}</i> annotations. Such annotations will override any
   * configuration set through the builder.
   * <br>
   * Note that you'll need to enable annotation pre-processing by adding the processor artifact
   * to the specific project dependencies.
   *
   * @param target the invocation target.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   */
  @NotNull
  TYPE proxyOf(@NotNull ContextInvocationTarget<?> target);
}