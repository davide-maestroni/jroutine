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

package com.github.dm.jrt.object.annotation;

import com.github.dm.jrt.object.annotation.AsyncOutput.OutputMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Through this annotation it is possible to indicate the original parameter types of the target
 * object method, and the wrapping routine output mode.
 * <p>
 * The only use case in which this annotation is useful, is when an interface is used as a proxy
 * of another class methods. The interface can take all its input parameters in an asynchronous way.
 * In such case, the values specified in the annotation will indicate the type of the parameters
 * expected by the target method.
 * <p>
 * For example, a method taking two integers:
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p>
 * can be proxied by a method defined as:
 * <pre>
 *     <code>
 *
 *         &#64;AsyncMethod({int.class, int.class})
 *         public Channel&lt;Integer, Integer&gt; sum();
 *     </code>
 * </pre>
 * <p>
 * The proxying method can also return the routine wrapping the target one, as:
 * <pre>
 *     <code>
 *
 *         &#64;AsyncMethod({int.class, int.class})
 *         public Routine&lt;Integer, Integer&gt; sum();
 *     </code>
 * </pre>
 * In such case, it is up to the caller to invoke it in the proper mode.
 * <p>
 * This annotation is used to decorate methods that are to be invoked in an asynchronous way.
 * <br>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.
 * <br>
 * In order to prevent unexpected behaviors, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * routines as well.
 * <p>
 * Finally, be aware that a method might need to be made accessible in order to be called. That
 * means that, in case a {@link java.lang.SecurityManager} is installed, a security exception might
 * be raised based on the specific policy implemented.
 * <p>
 * Remember also that, in order for the annotation to properly work at run time, the following rules
 * must be added to the project Proguard file (if employed for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *         -keepclassmembers class ** {
 *              &#64;com.github.dm.jrt.object.annotation.AsyncMethod *;
 *         }
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 05/22/2015.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncMethod {

  /**
   * The output transfer mode.
   *
   * @return the mode.
   */
  OutputMode mode() default OutputMode.VALUE;

  /**
   * The array of parameter types.
   *
   * @return the parameter types.
   */
  Class<?>[] value();
}
