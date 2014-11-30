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

import com.bmd.jrt.builder.RoutineBuilder;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * This annotation is used to indicate methods that are to be invoked in an asynchronous way.
 * <p/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to avoid unexpected behavior, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to use the framework to call
 * synchronous methods as well.<br/>
 * In a dual way, it is possible to exclude single methods from this kind of protection by
 * indicating them as having a different lock. Each lock has an ID associated, and every method
 * with a specific lock is protected only from the other methods with the same lock ID.
 * <p/>
 * This annotation allows to identify the method through a constant, thus avoiding issues when
 * running obfuscation tools.<br/>
 * For example, the following code:
 * <pre>
 *     <code>
 *
 *         public class MyClass {
 *
 *             public static final String METHOD_TAG = "get";
 *
 *             &#64;Async(METHOD_TAG)
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
 *         JavaRoutine.on(new MyClass()).method(MyClass.METHOD_TAG).callAsync();
 *     </code>
 * </pre>
 * <p/>
 * Additionally, through this annotation it is possible to indicate: a specific runner
 * implementation to be used for asynchronous and synchronous invocations; the maximum invocation
 * instances running at the same time; the maximum ones retained; the timeout for an invocation
 * instance to become available; a specific log and log level.
 * <br/>
 * Note however that the runner and log classes must declare a default constructor to be
 * instantiated via reflection.
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
     * Constant indicating a default ID value.
     */
    static final String DEFAULT_ID = "";

    /**
     * Constant indicating a null lock ID.
     */
    static final String UNLOCKED = "com.bmd.jrt.annotation.Async.UNLOCKED";

    /**
     * The time unit of the timeout for an invocation instance to become available.
     *
     * @return the time unit.
     */
    TimeUnit availTimeUnit() default TimeUnit.MILLISECONDS;

    /**
     * The timeout for an invocation instance to become available.
     *
     * @return the timeout.
     */
    long availTimeout() default RoutineBuilder.DEFAULT;

    /**
     * The ID of the lock associated with the annotated method.
     *
     * @return the lock ID.
     */
    String lockId() default DEFAULT_ID;

    /**
     * The class of the log to be used.
     *
     * @return the log class.
     */
    Class<? extends Log> log() default DefaultLog.class;

    /**
     * The log level.
     *
     * @return the log level.
     */
    LogLevel logLevel() default LogLevel.DEFAULT;

    /**
     * The max number of retained routine instances.
     *
     * @return the max retained instances.
     */
    int maxRetained() default RoutineBuilder.DEFAULT;

    /**
     * The max number of concurrently running routine instances.
     *
     * @return the max concurrently running instances.
     */
    int maxRunning() default RoutineBuilder.DEFAULT;

    /**
     * The class of the runner to be used for asynchronous invocations.
     *
     * @return the runner class.
     */
    Class<? extends Runner> runnerClass() default DefaultRunner.class;

    /**
     * The type of the runner to be used for synchronous invocations.
     *
     * @return the runner type.
     */
    RunnerType runnerType() default RunnerType.DEFAULT;

    /**
     * The tag used to identify the method independently from its original signature.
     *
     * @return the tag.
     */
    String value() default DEFAULT_ID;
}
