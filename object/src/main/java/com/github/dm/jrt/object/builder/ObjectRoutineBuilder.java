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

package com.github.dm.jrt.object.builder;

import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.object.config.ObjectConfigurable;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Interface defining a builder of routines wrapping object methods.
 * <p>
 * Created by davide-maestroni on 03/07/2015.
 */
public interface ObjectRoutineBuilder
    extends InvocationConfigurable<ObjectRoutineBuilder>, ObjectConfigurable<ObjectRoutineBuilder> {

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.object.annotation.*}</i> annotations.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * In case the wrapped object does not implement the specified interface, the alias annotation
   * value will be used to bind the interface method with the instance ones. If no annotation is
   * present, the method name will be used instead.
   * <br>
   * The interface will be interpreted as a proxy of the target object methods, and the optional
   * {@link com.github.dm.jrt.object.annotation.AsyncInput AsyncInput},
   * {@link com.github.dm.jrt.object.annotation.AsyncMethod AsyncMethod},
   * {@link com.github.dm.jrt.object.annotation.AsyncOutput AsyncOutput} and
   * {@link com.github.dm.jrt.object.annotation.Invoke Invoke} annotations will be honored.
   *
   * @param itf    the interface implemented by the returned object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the specified class does not represent an
   *                                            interface.
   * @see <a href='{@docRoot}/com/github/dm/jrt/object/annotation/package-summary.html'>
   * Annotations</a>
   */
  @NotNull
  <TYPE> TYPE buildProxy(@NotNull Class<TYPE> itf);

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.object.annotation.*}</i> annotations.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * In case the wrapped object does not implement the specified interface, the alias annotation
   * value will be used to bind the interface method with the instance ones. If no annotation is
   * present, the method name will be used instead.
   * <br>
   * The interface will be interpreted as a proxy of the target object methods, and the optional
   * {@link com.github.dm.jrt.object.annotation.AsyncInput AsyncInput},
   * {@link com.github.dm.jrt.object.annotation.AsyncMethod AsyncMethod},
   * {@link com.github.dm.jrt.object.annotation.AsyncOutput AsyncOutput} and
   * {@link com.github.dm.jrt.object.annotation.Invoke Invoke} annotations will be honored.
   *
   * @param itf    the token of the interface implemented by the returned object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
   *                                            interface.
   * @see <a href='{@docRoot}/com/github/dm/jrt/object/annotation/package-summary.html'>
   * Annotations</a>
   */
  @NotNull
  <TYPE> TYPE buildProxy(@NotNull ClassToken<TYPE> itf);

  /**
   * Returns a routine used to call the method whose identifying name is specified in a
   * {@link com.github.dm.jrt.object.annotation.Alias Alias} annotation.
   * <br>
   * If no method with the specified alias is found, this method will behave like
   * {@link #method(String, Class[])} with no parameter.
   * <br>
   * Optional <i>{@code com.github.dm.jrt.object.annotation.*}</i> method annotations will be
   * honored as well.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * Note that it is up to the caller to ensure that the input data are passed to the routine in
   * the correct order.
   *
   * @param name  the name specified in the annotation.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine.
   * @throws java.lang.IllegalArgumentException if the specified method is not found or its
   *                                            annotations are invalid.
   * @see <a href='{@docRoot}/com/github/dm/jrt/object/annotation/package-summary.html'>
   * Annotations</a>
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> method(@NotNull String name);

  /**
   * Returns a routine used to call the specified method.
   * <p>
   * The method is searched via reflection ignoring a name specified in a
   * {@link com.github.dm.jrt.object.annotation.Alias Alias} annotation. Though, optional
   * <i>{@code com.github.dm.jrt.object.annotation.*}</i> method annotations will be honored.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * Note that it is up to the caller to ensure that the input data are passed to the routine in
   * the correct order.
   *
   * @param name           the method name.
   * @param parameterTypes the method parameter types.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the routine.
   * @throws java.lang.IllegalArgumentException if no matching method is found or its annotations
   *                                            are invalid.
   * @see <a href='{@docRoot}/com/github/dm/jrt/object/annotation/package-summary.html'>
   * Annotations</a>
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> method(@NotNull String name, @NotNull Class<?>... parameterTypes);

  /**
   * Returns a routine used to call the specified method.
   * <p>
   * The method is invoked ignoring a name specified in a
   * {@link com.github.dm.jrt.object.annotation.Alias Alias} annotation. Though, optional
   * <i>{@code com.github.dm.jrt.object.annotation.*}</i> method annotations will be honored.
   * <br>
   * Note that such annotations will override any configuration set through the builder.
   * <p>
   * Note that it is up to the caller to ensure that the input data are passed to the routine in
   * the correct order.
   *
   * @param method the method instance.
   * @param <IN>   the input data type.
   * @param <OUT>  the output data type.
   * @return the routine.
   * @throws java.lang.IllegalArgumentException the method annotations are invalid.
   * @see <a href='{@docRoot}/com/github/dm/jrt/object/annotation/package-summary.html'>
   * Annotations</a>
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> method(@NotNull Method method);
}
