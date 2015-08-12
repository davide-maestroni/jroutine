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
package com.gh.bmd.jrt.invocation;

import javax.annotation.Nonnull;

/**
 * Interface defining a decorator of method invocations.
 * <p/>
 * Created by davide-maestroni on 11/08/15.
 */
public interface MethodInvocationDecorator {

    /**
     * Decorator just returning the same invocation unchanged.
     */
    MethodInvocationDecorator NO_DECORATION = new MethodInvocationDecorator() {

        @Nonnull
        public <INPUT, OUTPUT> MethodInvocation<INPUT, OUTPUT> decorate(
                @Nonnull final MethodInvocation<INPUT, OUTPUT> invocation,
                @Nonnull final String methodName, @Nonnull final Class<?>... methodParameterTypes) {

            return invocation;
        }
    };

    /**
     * Decorates the invocation used to wrap the specified target method.
     *
     * @param invocation           the invocation instance.
     * @param methodName           the target method name.
     * @param methodParameterTypes the target method parameter types.
     * @param <INPUT>              the input data type.
     * @param <OUTPUT>             the output data type.
     * @return the decorated invocation.
     */
    @Nonnull
    <INPUT, OUTPUT> MethodInvocation<INPUT, OUTPUT> decorate(
            @Nonnull MethodInvocation<INPUT, OUTPUT> invocation, @Nonnull String methodName,
            @Nonnull Class<?>... methodParameterTypes);
}
