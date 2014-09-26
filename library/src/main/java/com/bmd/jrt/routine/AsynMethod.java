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
package com.bmd.jrt.routine;

import com.bmd.jrt.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate methods which can be invoked in an asynchronous way.<br/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issue. Though, other parts of the code inside the same class will be not.
 * <p/>
 * In order to avoid unexpected behavior it is advisable to avoid using the same class fields in
 * protected and non-protected code, or to use the framework to call synchronous methods too.
 * <p/>
 * This annotation allow to identify the method through a constant, thus avoiding problem when
 * running obfuscation tools.<br/>
 * For example, the following code:
 * <pre>
 *     <code>
 *         public class MyClass {
 *
 *             public static final String GET_METHOD = "get";
 *
 *             &#64;AsynMethod(name = GET_METHOD)
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
 *         JRoutine.on(new MyClass()).method(MyClass.GET_METHOD).callAsyn();
 *     </code>
 * </pre>
 * <p/>
 * If no name is specified the original one will be used instead.
 * <p/>
 * Additionally, through this annotation it is possible to indicate a specific runner
 * implementation to be used for asynchronous and synchronous invocations.<br/>
 * Note however that the specified runner classes must declare a default constructor to be
 * instantiated via reflection.
 * <p/>
 * The same considerations apply to static class methods.
 * <p/>
 * Finally, be aware that a method might need to be made accessible in order to be called via
 * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
 * security exception might be raised based on the specific policy implemented.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the proper rules to your Proguard file if employing it for shrinking or obfuscation:
 * <pre>
 *     <code>
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.routine.AsynMethod *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 9/21/14.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsynMethod {

    /**
     * The name used to identify the method independently from its original signature.
     *
     * @return the name.
     */
    String name() default "";

    /**
     * The class of the runner to be used for asynchronous invocations.
     *
     * @return the runner class.
     */
    Class<? extends Runner> runner() default DefaultRunner.class;

    /**
     * If the sequential runner should be used for synchronous invocations.
     *
     * @return whether the sequential runner will be used.
     */
    boolean sequential() default false;
}
