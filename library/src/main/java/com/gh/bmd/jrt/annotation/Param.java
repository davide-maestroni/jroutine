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
 * The only use case in which this annotation is useful, is when an interface is used as a mirror
 * of another class methods. The interface can take some input parameters or return an output in an
 * asynchronous way. In such cases, the value specified in the annotation will indicate the type of
 * the parameter or the return type expected by the target method.
 * <p/>
 * For example, a method taking two integers:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * can be mirrored by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(&#64;Param(int.class) OutputChannel&lt;Integer&gt; i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Additionally, the interface can return the result as an output channel whose output data will be
 * passed only when available.
 * <p/>
 * For example, a method returning an integer:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * can be mirrored by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;Param(int.class)
 *         public OutputChannel&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * The interface can also return an array or list of outputs:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;Param(int.class)
 *         public List&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Note that the type of asynchronicity is automatically inferred by the mirror and the target type,
 * unless specifically chosen through the annotation <code>mode</code> attribute.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.gh.bmd.jrt.annotation.Param *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 1/31/15.
 */
@Inherited
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {

    /**
     * The asynchronous passing mode.
     *
     * @return the mode.
     */
    PassMode mode() default PassMode.AUTO;

    /**
     * The parameter class.
     *
     * @return the class.
     */
    Class<?> value();

    /**
     * Asynchronous pass mode.<br/>
     * The type indicates in which way a parameter is passed to the wrapped method or the result
     * is passed outside.
     */
    enum PassMode {

        /**
         * Value mode.<br/>
         * The variable is just read from or passed to an output channel.
         * <p/>
         * The annotated parameter must extends an {@link com.gh.bmd.jrt.channel.OutputChannel},
         * while an annotated method must return a superclass of it.
         */
        VALUE,
        /**
         * Collection mode.<br/>
         * The inputs are collected from the channel and passed as an array or collection to the
         * wrapped method. In a dual way, the element of the result array or collection are passed
         * one by one to the output channel.
         * <p/>
         * The annotated parameter must extends an {@link com.gh.bmd.jrt.channel.OutputChannel} and
         * must be the only parameter accepted by the method, while an annotated method must return
         * a superclass of it.
         */
        COLLECTION,
        /**
         * Parallel mode.<br/>
         * Each input is passed to a different parallel invocation of the wrapped method.
         * <p/>
         * The annotated parameter must be an array or implement an {@link java.lang.Iterable} and
         * must be the only parameter accepted by the method, while an annotated method must return
         * an array or a superclass of a {@link java.util.List}.
         */
        PARALLEL,
        /**
         * Automatic mode.<br/>
         * The mode is automatically assigned based to the parameter or return type. Namely: if the
         * parameters match the COLLECTION pass mode, they are assigned it; if they match the VALUE
         * mode, they are assigned the latter; finally the PARALLEL conditions are checked.
         * <p/>
         * Dually, if the return type matches the PARALLEL pass mode, it is assigned it; if it
         * matches the COLLECTION mode, it is assigned the latter; finally the VALUE conditions are
         * checked.
         */
        AUTO
    }
}
