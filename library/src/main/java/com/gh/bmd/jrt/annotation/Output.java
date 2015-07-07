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
package com.gh.bmd.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to decorate methods that are to be invoked in an asynchronous way.<br/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to prevent unexpected behaviors, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * the framework as well.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a proxy
 * of another class methods. The interface can return the output in an asynchronous way. In such
 * case, the value specified in the annotation will indicate the mode in which the output is
 * transferred outside the routine.
 * <p/>
 * For example, a method returning an integer:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * can be proxied by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;Output
 *         public OutputChannel&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * The interface can also return an array or list of outputs:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;Output
 *         public List&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Note that the transfer mode is automatically inferred by the proxy and the target types, unless
 * specifically chosen through the annotation value.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.gh.bmd.jrt.annotation.Output *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 24/05/15.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Output {

    /**
     * The output transfer mode.
     *
     * @return the mode.
     */
    OutputMode value() default OutputMode.AUTO;

    /**
     * Output transfer mode type.<br/>
     * The mode indicates in which way the result is passed outside.
     */
    enum OutputMode {

        /**
         * Value mode.<br/>
         * The variable is just passed to an output channel.
         * <p/>
         * The annotated method must return a superclass of
         * {@link com.gh.bmd.jrt.channel.OutputChannel OutputChannel}.
         */
        VALUE,
        /**
         * Element mode.<br/>
         * The elements of the result array or iterable are passed one by one to the output channel.
         * <p/>
         * The annotated method must return a superclass of
         * {@link com.gh.bmd.jrt.channel.OutputChannel OutputChannel}.
         */
        ELEMENT,
        /**
         * Collection mode.<br/>
         * The results are collected before being returned by the annotated method.
         * <p/>
         * The annotated method must return an array or a superclass of {@link java.util.List}.
         */
        COLLECTION,
        /**
         * Automatic mode.<br/>
         * The mode is automatically assigned based on the return type. Namely: if the return type
         * matches the COLLECTION output mode, that one is chosen; if it  matches the ELEMENT output
         * mode, it is chosen the latter; finally the VALUE output mode conditions are checked.
         */
        AUTO
    }
}
