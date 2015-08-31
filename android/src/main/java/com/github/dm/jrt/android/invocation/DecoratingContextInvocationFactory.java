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
package com.github.dm.jrt.android.invocation;

import javax.annotation.Nonnull;

/**
 * Class decorating the invocations produced by a context invocation factory.
 * <p/>
 * Created by davide-maestroni on 19/08/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class DecoratingContextInvocationFactory<INPUT, OUTPUT>
        extends ContextInvocationFactory<INPUT, OUTPUT> {

    private final ContextInvocationFactory<INPUT, OUTPUT> mFactory;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped factory instance.
     */
    @SuppressWarnings("ConstantConditions")
    public DecoratingContextInvocationFactory(
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped invocation factory must not be null");
        }

        mFactory = wrapped;
    }

    @Nonnull
    @Override
    public final ContextInvocation<INPUT, OUTPUT> newInvocation() {

        return decorate(mFactory.newInvocation());
    }

    /**
     * Decorates the specified context invocation.
     *
     * @param invocation the context invocation instance to decorate.
     * @return the decorated context invocation.
     */
    @Nonnull
    protected abstract ContextInvocation<INPUT, OUTPUT> decorate(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> invocation);
}
