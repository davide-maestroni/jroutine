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
 * TODO
 * <p/>
 * Created by davide on 1/31/15.
 */
@Inherited
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {

    /**
     * The asynchronous variable type.
     *
     * @return the type.
     */
    AsyncType type() default AsyncType.AUTO;

    /**
     * The bound class.
     *
     * @return the class.
     */
    Class<?> value();

    /**
     * Asynchronous variable type.<br/>
     * The type indicates in which way a parameter is passed to the wrapped method or the result
     * is passed outside.
     */
    enum AsyncType {

        /**
         * Pass type.<br/>
         * The variable is just read from or passed to an output channel.
         * <p/>
         * The annotated parameter must extends an {@link com.bmd.jrt.channel.OutputChannel}, while
         * an annotated method must return a super class of it.
         */
        PASS,
        /**
         * Collect type.<br/>
         * The inputs are collected from the channel and passed as an array or collection to the
         * wrapped method. In a dual way, the element of the result array or collection are passed
         * one by one to the output channel.
         * <p/>
         * The annotated parameter must extends an {@link com.bmd.jrt.channel.OutputChannel} and
         * must be the only parameter accepted by the method, while an annotated method must return
         * a super class of it.
         */
        COLLECT,
        /**
         * Parallel type.<br/>
         * Each input is passed to a different parallel invocation of the wrapped method.
         * <p/>
         * The annotated parameter must be an array or implement an {@link java.lang.Iterable} and
         * must be the only parameter accepted by the method, while an annotated method must return
         * an array or a super class of a {@link java.util.List}.
         */
        PARALLEL,
        /**
         * Automatic type.<br/>
         * The type is automatically assigned based on the parameter or return type. Namely: if the
         * parameters or return type match the PARALLEL async type, they are assigned it; if they
         * match the COLLECT type, they are assigned the latter; finally the PASS conditions are
         * checked.
         */
        AUTO
    }
}
