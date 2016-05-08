/*
 * Copyright (c) 2016. Davide Maestroni
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
 * This annotation is meant to document the effect of a method on the underlying stream. In fact,
 * a method may return a new object or just modify the instance internal configuration, or cause
 * different side effects.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface StreamTransform {

    /**
     * The stream method transform type.
     *
     * @return the transform type.
     */
    TransformType value();

    /**
     * Transform type enumeration.
     */
    enum TransformType {

        /**
         * A new instance is created and chained to the current one.
         * <br>
         * Any further attempt to bind or read from the current stream will raise an exception.
         */
        MAP,
        /**
         * The flow of data is blocked, so that all incoming inputs are collected before a new
         * output is produced.
         * <br>
         * A new stream instance is returned by the method.
         */
        REDUCE,
        /**
         * The flow of data is cached, so that all incoming inputs are possibly retained, but
         * outputs are produced anyway without interruption.
         * <br>
         * A new stream instance is returned by the method.
         */
        CACHE,
        /**
         * The flow of data is blocked and incoming inputs are possibly retained, so that outputs
         * are produced only when inputs complete.
         * <br>
         * A new stream instance is returned by the method.
         */
        COLLECT,
        /**
         * The current stream configuration is modified and the same instance is returned.
         * <br>
         * Note that the stream internal configuration is not synchronized, so that modifications
         * applied on different threads may lead to unpredictable results.
         */
        CONFIG,
        /**
         * The flow of input data is initiated.
         * <br>
         * Note that all the stream instances are lazy, that is, the flow of data will not begin
         * until a method of this type gets called.
         */
        START
    }
}
