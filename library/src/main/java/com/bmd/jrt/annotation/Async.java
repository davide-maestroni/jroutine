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

import com.bmd.jrt.builder.RoutineConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * This annotation is used to customize methods that are to be invoked in an asynchronous way.
 * <p/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to avoid unexpected behavior, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to use the framework to call
 * synchronous methods as well.<br/>
 * In a dual way, it is possible to exclude single methods from this kind of protection by
 * indicating them as having a different lock. Each lock has a name associated, and every method
 * with a specific lock is protected only from the other methods with the same lock name.<br/>
 * Additionally, through this annotation it is possible to indicate the timeout for a result
 * to become available.
 * <p/>
 * Finally, be aware that a method might need to be made accessible in order to be called. That
 * means that, in case a {@link java.lang.SecurityManager} is installed, a security exception might
 * be raised based on the specific policy implemented.
 * <p/>
 * When used to annotate a class instead of a method, its attributes are applied to all the class
 * methods unless specific values are set in the method annotation.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.Async *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 9/21/14.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {

    /**
     * Constant indicating a default name value.
     */
    static final String DEFAULT_LOCK = "com.bmd.jrt.annotation.Async.DEFAULT_LOCK";

    /**
     * Constant indicating a null lock name.
     */
    static final String NULL_LOCK = "";

    /**
     * The type of action to take on output channel timeout.
     *
     * @return the action type.
     */
    TimeoutAction eventually() default TimeoutAction.DEFAULT;

    /**
     * The name of the lock associated with the annotated method.
     *
     * @return the lock name.
     */
    String lockName() default DEFAULT_LOCK;

    /**
     * The time unit of the timeout for an invocation instance to produce a result.
     *
     * @return the time unit.
     */
    TimeUnit resultTimeUnit() default TimeUnit.MILLISECONDS;

    /**
     * The timeout for an invocation instance to produce a result.
     *
     * @return the timeout.
     */
    long resultTimeout() default RoutineConfiguration.DEFAULT;

    /**
     * Enumeration indicating the action to take on output channel timeout.
     */
    public enum TimeoutAction {

        /**
         * Deadlock.<br/>
         * If no result is available after the specified timeout, the called method will throw a
         * {@link com.bmd.jrt.channel.ReadDeadlockException}.
         */
        DEADLOCK,
        /**
         * Break execution.<br/>
         * If no result is available after the specified timeout, the called method will its
         * execution and exit immediately.
         */
        EXIT,
        /**
         * Default action.<br/>
         * This value is used to indicated that the choice of the action is left to the framework.
         */
        DEFAULT
    }
}
