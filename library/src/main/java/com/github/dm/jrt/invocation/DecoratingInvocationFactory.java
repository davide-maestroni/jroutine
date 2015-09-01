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
package com.github.dm.jrt.invocation;

import javax.annotation.Nonnull;

/**
 * Class decorating the invocations produced by an invocation factory.
 * <p/>
 * Created by davide-maestroni on 08/19/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class DecoratingInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final InvocationFactory<IN, OUT> mFactory;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped factory instance.
     */
    @SuppressWarnings("ConstantConditions")
    public DecoratingInvocationFactory(@Nonnull final InvocationFactory<IN, OUT> wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped invocation factory must not be null");
        }

        mFactory = wrapped;
    }

    @Nonnull
    @Override
    public final Invocation<IN, OUT> newInvocation() {

        return decorate(mFactory.newInvocation());
    }

    /**
     * Decorates the specified invocation.
     *
     * @param invocation the invocation instance to decorate.
     * @return the decorated invocation.
     */
    @Nonnull
    protected abstract Invocation<IN, OUT> decorate(@Nonnull final Invocation<IN, OUT> invocation);
}
