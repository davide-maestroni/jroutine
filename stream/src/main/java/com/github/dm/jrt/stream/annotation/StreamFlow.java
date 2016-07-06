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

package com.github.dm.jrt.stream.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for documenting stream methods.
 * <p>
 * This annotation is meant to document the effect of a method on the underlying stream.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface StreamFlow {

    /**
     * The stream method transformation type.
     *
     * @return the transformation type.
     */
    TransformationType value();

    /**
     * Transformation type enumeration.
     */
    enum TransformationType {

        /**
         * The effect on the flow will depend on the logic implemented in the mapping routine or
         * function.
         */
        MAP,
        /**
         * The flow of data is blocked, so that all incoming inputs are processed before a new
         * output is produced.
         */
        REDUCE,
        /**
         * The flow of data is blocked and incoming inputs are possibly retained, so that outputs
         * are produced only when inputs complete.
         */
        COLLECT,
        /**
         * The current stream configuration is modified.
         */
        CONFIG
    }
}
