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
package com.gh.bmd.jrt.android.invocation;

import javax.annotation.Nonnull;

/**
 * Interface defining a service which acts like a context invocation factory.
 * <p/>
 * Created by davide on 04/05/15.
 */
public interface InvocationFactoryService {

    /**
     * Creates and return a new context invocation instance.
     *
     * @param invocationClass the class of the invocation.
     * @param args            the arguments.
     * @return the context invocation instance.
     */
    @Nonnull
    ContextInvocation<Object, Object> newInvocation(
            @Nonnull Class<? extends ContextInvocation<Object, Object>> invocationClass,
            @Nonnull final Object... args);
}
