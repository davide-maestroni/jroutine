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

package com.github.dm.jrt;

import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.RoutineBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.ext.channel.Channels;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.object.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.object.core.InvocationTarget;
import com.github.dm.jrt.object.core.JRoutineObject;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;
import com.github.dm.jrt.proxy.core.JRoutineProxy;

import org.jetbrains.annotations.NotNull;

/**
 * Class aggregating the library features.
 * <p/>
 * Created by davide-maestroni on 02/29/2016.
 */
public class JRoutine extends Channels {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutine() {

    }

    /**
     * Returns an I/O channel builder.
     *
     * @return the channel builder instance.
     */
    @NotNull
    public static IOChannelBuilder io() {

        return JRoutineCore.io();
    }

    /**
     * Returns a routine builder based on the specified invocation factory.<br/>
     * In order to prevent undesired leaks, the class of the specified factory should have a static
     * scope.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final InvocationFactory<IN, OUT> factory) {

        return JRoutineCore.on(factory);
    }

    /**
     * Returns a routine builder wrapping the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.<br/>
     * Note also that the invocation input data will be cached, and the results will be produced
     * only after the invocation channel is closed, so be sure to avoid streaming inputs in
     * order to prevent starvation or out of memory errors.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the specified object class represents an
     *                                            interface.
     */
    @NotNull
    public static ObjectRoutineBuilder on(@NotNull final InvocationTarget<?> target) {

        return JRoutineObject.on(target);
    }

    /**
     * Returns a routine builder wrapping the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.<br/>
     * Note also that the invocation input data will be cached, and the results will be produced
     * only after the invocation channel is closed, so be sure to avoid streaming inputs in
     * order to prevent starvation or out of memory errors.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     */
    @NotNull
    public static ProxyRoutineBuilder on(@NotNull final ProxyTarget<?> target) {

        return JRoutineProxy.on(target.mTarget);
    }

    // TODO: 02/03/16 functions 

    /**
     * Class representing a proxy invocation target.
     *
     * @param <TYPE> the target object type.
     */
    public static class ProxyTarget<TYPE> {

        private final InvocationTarget<TYPE> mTarget;

        /**
         * Constructor.
         *
         * @param target the wrapped invocation target.
         */
        private ProxyTarget(@NotNull final InvocationTarget<TYPE> target) {

            mTarget = target;
        }

        /**
         * Returns a target based on the specified class.
         *
         * @param targetClass the target class.
         * @param <TYPE>      the target object type.
         * @return the invocation target.
         */
        @NotNull
        public static <TYPE> ProxyTarget<TYPE> classOfType(@NotNull final Class<TYPE> targetClass) {

            return new ProxyTarget<TYPE>(InvocationTarget.classOfType(targetClass));
        }

        /**
         * Returns a target based on the specified instance.
         *
         * @param target the target instance.
         * @param <TYPE> the target object type.
         * @return the invocation target.
         */
        @NotNull
        public static <TYPE> ProxyTarget<TYPE> instance(@NotNull final TYPE target) {

            return new ProxyTarget<TYPE>(InvocationTarget.instance(target));
        }
    }
}
