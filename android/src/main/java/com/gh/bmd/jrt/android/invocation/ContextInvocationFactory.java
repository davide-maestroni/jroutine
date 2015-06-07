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

import com.gh.bmd.jrt.invocation.InvocationFactory;

import javax.annotation.Nonnull;

/**
 * Context invocation factory interface.
 * <p/>
 * Created by davide-maestroni on 5/1/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface ContextInvocationFactory<INPUT, OUTPUT> extends InvocationFactory<INPUT, OUTPUT> {

    /**
     * Creates and return a new context invocation instance.
     *
     * @return the context invocation instance.
     */
    @Nonnull
    ContextInvocation<INPUT, OUTPUT> newInvocation();
}
