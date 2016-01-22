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

package com.github.dm.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Through this annotation it is possible to indicate the invocation mode of the routine wrapping
 * the target object method.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a proxy
 * of another class methods. The annotation will indicate the type of invocation to be applied to
 * the wrapping routine.
 * <p/>
 * Note that, unless the {@link Invoke.InvocationMode#PARALLEL} is specified, to each call to a
 * method of the proxy interface will correspond a single invocation of the wrapping routine.<br/>
 * In case this annotation is absent, the asynchronous invocation mode will be employed.
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
 *              &#64;com.github.dm.jrt.annotation.Invoke *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 09/27/2015.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invoke {

    /**
     * The routine invocation mode.
     *
     * @return the invocation mode.
     */
    InvocationMode value() default InvocationMode.ASYNC;

    /**
     * Routine invocation mode type.<br/>
     * The mode indicates in which way the wrapping routine should be invoked.
     */
    enum InvocationMode {

        /**
         * Synchronous mode.
         */
        SYNC,
        /**
         * Asynchronous mode.
         */
        ASYNC,
        /**
         * Parallel mode.
         */
        PARALLEL
    }
}
