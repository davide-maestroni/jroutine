/*
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
package com.github.dm.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to decorate methods that are to be invoked in an asynchronous way.<br/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to prevent unexpected behaviors, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * routines as well.
 * <p/>
 * This annotation allows to identify the method through a constant, thus avoiding issues when
 * running obfuscation tools.<br/>
 * For example, the following code:
 * <pre>
 *     <code>
 *
 *         public class MyClass {
 *
 *             public static final String METHOD_NAME = "get";
 *
 *             &#64;Alias(METHOD_NAME)
 *             public int getOne() {
 *
 *                 return 1;
 *             }
 *         }
 *     </code>
 * </pre>
 * allows to asynchronously call the method independently from its original name like:
 * <pre>
 *     <code>
 *
 *         JRoutine.on(instance(new MyClass())).aliasMethod(MyClass.METHOD_NAME).asyncCall();
 *     </code>
 * </pre>
 * <p/>
 * The same considerations apply to static class methods.
 * <p/>
 * Finally, be aware that a method might need to be made accessible in order to be called. That
 * means that, in case a {@link java.lang.SecurityManager} is installed, a security exception might
 * be raised based on the specific policy implemented.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.github.dm.jrt.annotation.Alias *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 01/22/2015.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Alias {

    /**
     * The name used to identify the method independently from its original signature.
     *
     * @return the name.
     */
    String value();
}
