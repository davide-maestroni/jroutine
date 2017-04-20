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

package com.github.dm.jrt.reflect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Through this annotation it is possible to indicate that the result returned by the target object
 * method must be dispatched in an asynchronous way.
 * <br>
 * If, on the contrary, this annotation is missing, be aware that the proxy method will block until
 * the target one completes, even if no result is expected (that is, the method returns a void
 * result).
 * <p>
 * The only use case in which this annotation is useful, is when an interface is used as a proxy
 * of another class methods. The interface can return the output in an asynchronous way. In such
 * case, the value specified in the annotation will indicate the mode in which the output is
 * transferred outside the routine.
 * <p>
 * For example, a method returning an integer:
 * <pre><code>
 * public int sum(int i1, int i2);
 * </code></pre>
 * <p>
 * can be proxied by a method defined as:
 * <pre><code>
 * &#64;AsyncOutput
 * public Channel&lt;?, Integer&gt; sum(int i1, int i2);
 * </code></pre>
 * Note that the transfer mode is specifically chosen through the annotation {@code mode} attribute
 * (it's {@link AsyncOutput.OutputMode#DEFAULT DEFAULT} by default).
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
 * <pre><code>
 * -keepattributes RuntimeVisibleAnnotations
 * -keepclassmembers class ** {
 *   &#64;com.github.dm.jrt.reflect.annotation.AsyncOutput *;
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 05/24/2015.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncOutput {

  /**
   * The output transfer mode.
   *
   * @return the mode.
   */
  OutputMode value() default OutputMode.DEFAULT;

  /**
   * Output transfer mode type.
   * <br>
   * The mode indicates in which way the result is passed outside.
   */
  enum OutputMode {

    /**
     * Default mode.
     * <br>
     * The variable is just passed to the channel.
     * <p>
     * The annotated method must return a superclass of
     * {@link com.github.dm.jrt.core.channel.Channel Channel}.
     */
    DEFAULT,
    /**
     * Element mode.
     * <br>
     * The elements of the result array or iterable are passed one by one to the channel.
     * <p>
     * The annotated method must return a superclass of
     * {@link com.github.dm.jrt.core.channel.Channel Channel}.
     */
    ELEMENT
  }
}
