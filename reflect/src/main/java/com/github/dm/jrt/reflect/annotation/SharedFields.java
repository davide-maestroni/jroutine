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
 * Through this annotation it is possible to indicate the fields accessed by the target object
 * method.
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
 * This annotation is meant to have a finer control on this kind of protection. Each method can be
 * associated to specific fields accessed by the implementation, so that, shared ones are
 * guaranteed to be handled in a thread safe way. By default, that is when this annotation is
 * missing, all fields are protected.
 * <br>
 * Note that methods sharing the same fields cannot be executed in parallel.
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
 *   &#64;com.github.dm.jrt.reflect.annotation.SharedFields *;
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 01/26/2015.
 *
 * @see com.github.dm.jrt.reflect.config.WrapperConfiguration WrapperConfiguration
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SharedFields {

  /**
   * The shared field names associated with the annotated method.
   *
   * @return the field names.
   */
  String[] value();
}
