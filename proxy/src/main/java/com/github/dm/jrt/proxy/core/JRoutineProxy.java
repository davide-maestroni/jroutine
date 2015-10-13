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
package com.github.dm.jrt.proxy.core;

import com.github.dm.jrt.core.InvocationTarget;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods.
 * <p/>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.<br/>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p/>
 * Created by davide-maestroni on 03/23/2015.
 *
 * @see com.github.dm.jrt.annotation.Alias Alias
 * @see com.github.dm.jrt.annotation.CoreInstances CoreInstances
 * @see com.github.dm.jrt.annotation.Input Input
 * @see com.github.dm.jrt.annotation.InputMaxSize InputMaxSize
 * @see com.github.dm.jrt.annotation.InputOrder InputOrder
 * @see com.github.dm.jrt.annotation.Inputs Inputs
 * @see com.github.dm.jrt.annotation.InputTimeout InputTimeout
 * @see com.github.dm.jrt.annotation.MaxInstances MaxInstances
 * @see com.github.dm.jrt.annotation.Invoke Invoke
 * @see com.github.dm.jrt.annotation.Output Output
 * @see com.github.dm.jrt.annotation.OutputMaxSize OutputMaxSize
 * @see com.github.dm.jrt.annotation.OutputOrder OutputOrder
 * @see com.github.dm.jrt.annotation.OutputTimeout OutputTimeout
 * @see com.github.dm.jrt.annotation.Priority Priority
 * @see com.github.dm.jrt.annotation.ReadTimeout ReadTimeout
 * @see com.github.dm.jrt.annotation.ReadTimeoutAction ReadTimeoutAction
 * @see com.github.dm.jrt.annotation.SharedFields SharedFields
 * @see com.github.dm.jrt.proxy.annotation.Proxy Proxy
 */
public class JRoutineProxy {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineProxy() {

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
    public static ProxyRoutineBuilder on(@NotNull final InvocationTarget<?> target) {

        return new DefaultProxyRoutineBuilder(target);
    }
}
