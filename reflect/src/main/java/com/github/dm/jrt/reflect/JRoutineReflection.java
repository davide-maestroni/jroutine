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

package com.github.dm.jrt.reflect;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilder;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * This utility class provides an additional way to build a routine, based on the asynchronous
 * invocation of a method of an existing class or object via reflection.
 * <p>
 * It is possible to annotate selected methods to be asynchronously invoked, or to simply select
 * a method through its signature. It is also possible to build a proxy object whose methods will
 * in turn asynchronously invoke the target object ones.
 * <br>
 * Note that a proxy object can be simply defined as an interface implemented by the target, but
 * also as a completely unrelated one mirroring the target methods. In this way it is possible to
 * apply the library functionality to objects defined by third party libraries which are not under
 * direct control.
 * <br>
 * A mirror interface adds the possibility to override input and output parameters with output
 * channels, so that data are transferred asynchronously, avoiding the need to block execution while
 * waiting for them to be available.
 * <p>
 * <b>Some usage examples</b>
 * <p>
 * <b>Example 1:</b> Asynchronously get the output of two routines.
 * <pre><code>
 * public interface AsyncCallback {
 *
 *   public void onResults(&#64;AsyncInput(Result.class) Channel&lt;?, Result&gt; result1,
 *       &#64;AsyncInput(Result.class) Channel&lt;?, Result&gt; result2);
 * }
 *
 * final AsyncCallback callback =
 *         JRoutineReflection.wrapper()
 *                           .proxyOf(instance(myCallback), AsyncCallback.class);
 * callback.onResults(routine1.invoke(), routine2.invoke());
 * </code></pre>
 * Where the object {@code myCallback} implements a method
 * {@code public void onResults(Result result1, Result result2)}.
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @see <a href='{@docRoot}/com/github/dm/jrt/reflect/annotation/package-summary.html'>
 * Annotations</a>
 */
public class JRoutineReflection {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineReflection() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of routines running on the specified executor, wrapping a target object.
   *
   * @return the routine builder instance.
   */
  @NotNull
  public static ReflectionRoutineBuilder wrapper() {
    return wrapperOn(defaultExecutor());
  }

  /**
   * Returns a builder of routines wrapping a target object.
   *
   * @param executor the executor instance.
   * @return the routine builder instance.
   */
  @NotNull
  public static ReflectionRoutineBuilder wrapperOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultReflectionRoutineBuilder(executor);
  }
}
