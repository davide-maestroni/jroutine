/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate a class or method whose execution might take more than a
 * few dozens of seconds to complete.
 * <p/>
 * The annotation will cause the routine execution to be run into a dedicated pool,
 * unless a different runner is programmatically set through the proper methods.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the proper rules to your Proguard file if employing it for shrinking or obfuscation:
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.VeryLongExecution *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 10/19/14.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VeryLongExecution {

}