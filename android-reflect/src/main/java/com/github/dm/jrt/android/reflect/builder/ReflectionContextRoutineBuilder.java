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

package com.github.dm.jrt.android.reflect.builder;

import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.reflect.config.WrapperConfigurable;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Interface defining a builder of routines wrapping an object methods.
 * <p>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p>
 * Created by davide-maestroni on 05/19/2017.
 */
public interface ReflectionContextRoutineBuilder
    extends InvocationConfigurable<ReflectionContextRoutineBuilder>,
    WrapperConfigurable<ReflectionContextRoutineBuilder> {

  /**
   * Returns a routine used to call the method whose identifying name is specified in a
   * {@link com.github.dm.jrt.reflect.annotation.Alias Alias} annotation.
   * <br>
   * If no method with the specified alias is found, this method will behave like
   * {@link #methodOf(ContextInvocationTarget, String, Class[])} with no parameter.
   * <br>
   * Optional <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> and
   * <i>{@code com.github.dm.jrt.android.reflect.annotation.*}</i> method annotations will be
   * honored as well. Such annotations will override any configuration set through the builder.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors. It is also up to the caller to ensure that the
   * input data are passed to the routine in the correct order.
   *
   * @param target the invocation target.
   * @param name   the name specified in the annotation.
   * @param <IN>   the input data type.
   * @param <OUT>  the output data type.
   * @return the routine.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the specified method is not found or its annotations
   *                                            are invalid.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   * @see <a href='{@docRoot}/com/github/dm/jrt/android/reflect/annotation/package-summary.html'>
   * Android Annotations</a>
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> methodOf(@NotNull ContextInvocationTarget<?> target,
      @NotNull String name);

  /**
   * Returns a routine used to call the specified method.
   * <p>
   * The method is searched via reflection ignoring a name specified in a
   * {@link com.github.dm.jrt.reflect.annotation.Alias Alias} annotation. Though, optional
   * <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> and
   * <i>{@code com.github.dm.jrt.android.reflect.annotation.*}</i> method annotations will be
   * honored. Such annotations will override any configuration set through the builder.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors. It is also up to the caller to ensure that the
   * input data are passed to the routine in the correct order.
   *
   * @param target         the invocation target.
   * @param name           the method name.
   * @param parameterTypes the method parameter types.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the routine.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            no matching method is found or its annotations are
   *                                            invalid.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   * @see <a href='{@docRoot}/com/github/dm/jrt/android/reflect/annotation/package-summary.html'>
   * Android Annotations</a>
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> methodOf(@NotNull ContextInvocationTarget<?> target,
      @NotNull String name, @NotNull Class<?>... parameterTypes);

  /**
   * Returns a routine used to call the specified method.
   * <p>
   * The method is invoked ignoring a name specified in a
   * {@link com.github.dm.jrt.reflect.annotation.Alias Alias} annotation. Though, optional
   * <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> and
   * <i>{@code com.github.dm.jrt.android.reflect.annotation.*}</i> method annotations will be
   * honored. Such annotations will override any configuration set through the builder.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors. It is also up to the caller to ensure that the
   * input data are passed to the routine in the correct order.
   *
   * @param target the invocation target.
   * @param method the method instance.
   * @param <IN>   the input data type.
   * @param <OUT>  the output data type.
   * @return the routine.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the method annotations are invalid.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   * @see <a href='{@docRoot}/com/github/dm/jrt/android/reflect/annotation/package-summary.html'>
   * Android Annotations</a>
   */
  @NotNull
  <IN, OUT> Routine<IN, OUT> methodOf(@NotNull ContextInvocationTarget<?> target,
      @NotNull Method method);

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> and
   * <i>{@code com.github.dm.jrt.android.reflect.annotation.*}</i> annotations. Such annotations
   * will override any configuration set through the builder.
   * <p>
   * In case the wrapped object does not implement the specified interface, the alias annotation
   * value will be used to bind the interface method with the instance ones. If no annotation is
   * present, the method name will be used instead.
   * <br>
   * The interface will be interpreted as a proxy of the target object methods, and the optional
   * {@link com.github.dm.jrt.reflect.annotation.AsyncInput AsyncInput},
   * {@link com.github.dm.jrt.reflect.annotation.AsyncMethod AsyncMethod} and
   * {@link com.github.dm.jrt.reflect.annotation.AsyncOutput AsyncOutput} annotations will be
   * honored.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   *
   * @param target the invocation target.
   * @param itf    the token of the interface implemented by the returned object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the specified proxy is not an interface.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   * @see <a href='{@docRoot}/com/github/dm/jrt/android/reflect/annotation/package-summary.html'>
   * Android Annotations</a>
   */
  @NotNull
  <TYPE> TYPE proxyOf(@NotNull ContextInvocationTarget<?> target, @NotNull ClassToken<TYPE> itf);

  /**
   * Returns a proxy object enabling asynchronous call of the target instance methods.
   * <p>
   * The routines used for calling the methods will honor the attributes specified in any optional
   * <i>{@code com.github.dm.jrt.reflect.annotation.*}</i> and
   * <i>{@code com.github.dm.jrt.android.reflect.annotation.*}</i> annotations. Such annotations
   * will override any configuration set through the builder.
   * <p>
   * In case the wrapped object does not implement the specified interface, the alias annotation
   * value will be used to bind the interface method with the instance ones. If no annotation is
   * present, the method name will be used instead.
   * <br>
   * The interface will be interpreted as a proxy of the target object methods, and the optional
   * {@link com.github.dm.jrt.reflect.annotation.AsyncInput AsyncInput},
   * {@link com.github.dm.jrt.reflect.annotation.AsyncMethod AsyncMethod} and
   * {@link com.github.dm.jrt.reflect.annotation.AsyncOutput AsyncOutput} annotations will be
   * honored.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   *
   * @param target the invocation target.
   * @param itf    the interface implemented by the returned object.
   * @param <TYPE> the interface type.
   * @return the proxy object.
   * @throws java.lang.IllegalArgumentException if the target does not represent a concrete class or
   *                                            the specified proxy is not an interface.
   * @see com.github.dm.jrt.reflect.annotation Annotations
   * @see <a href='{@docRoot}/com/github/dm/jrt/android/reflect/annotation/package-summary.html'>
   * Android Annotations</a>
   */
  @NotNull
  <TYPE> TYPE proxyOf(@NotNull ContextInvocationTarget<?> target, @NotNull Class<TYPE> itf);
}
