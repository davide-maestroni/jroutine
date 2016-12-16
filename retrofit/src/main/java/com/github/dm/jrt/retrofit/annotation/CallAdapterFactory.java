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

package com.github.dm.jrt.retrofit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate the adapter factory to be used for the specific method.
 * <p>
 * Remember that, in order for the annotation to properly work at run time, the following rules must
 * be added to the project Proguard file (if employed for shrinking or obfuscation):
 * <pre><code>
 * -keepattributes RuntimeVisibleAnnotations
 * -keepclassmembers class ** {
 *   &#64;com.github.dm.jrt.retrofit.annotation.CallAdapterFactory *;
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 05/20/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CallAdapterFactory {

  /**
   * The name of the registered adapter factory to employ for the method.
   *
   * @return the factory name.
   */
  String value();
}
