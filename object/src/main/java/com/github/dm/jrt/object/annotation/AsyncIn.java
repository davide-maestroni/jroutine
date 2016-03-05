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

package com.github.dm.jrt.object.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Through this annotation it is possible to indicate the original parameter type of the target
 * object method.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a proxy
 * of another class methods. The interface can take some input parameters in an asynchronous way. In
 * such case, the value specified in the annotation will indicate the type of the parameter expected
 * by the target method.
 * <p/>
 * For example, a method taking two integers:
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
 *         public int sum(&#64;AsyncIn(int.class) OutputChannel&lt;Integer&gt; i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Note that the transfer mode is specifically chosen through the annotation {@code mode} attribute
 * (it's {@link AsyncIn.InputMode#VALUE VALUE} by default).
 * <p/>
 * This annotation is used to decorate methods that are to be invoked in an asynchronous way.<br/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to prevent unexpected behaviors, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * routines as well.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *         -keepclassmembers class ** {
 *              &#64;com.github.dm.jrt.object.annotation.AsyncIn *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 05/23/2015.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncIn {

    /**
     * The input transfer mode.
     *
     * @return the mode.
     */
    InputMode mode() default InputMode.VALUE;

    /**
     * The parameter class.
     *
     * @return the class.
     */
    Class<?> value();

    /**
     * Input transfer mode type.<br/>
     * The mode indicates in which way a parameter is passed to the wrapped method.
     */
    enum InputMode {

        /**
         * Value mode.<br/>
         * The variable is just read from an output channel.
         * <p/>
         * The annotated parameters must extend an
         * {@link com.github.dm.jrt.core.channel.Channel.OutputChannel OutputChannel}.
         */
        VALUE,
        /**
         * Collection mode.<br/>
         * The inputs are collected from the channel and passed as an array or collection to the
         * wrapped method.
         * <p/>
         * The annotated parameter must extend an
         * {@link com.github.dm.jrt.core.channel.Channel.OutputChannel OutputChannel} and must be
         * the only parameter accepted by the method.
         */
        COLLECTION
    }
}
