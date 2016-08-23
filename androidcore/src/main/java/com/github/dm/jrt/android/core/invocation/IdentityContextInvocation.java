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

package com.github.dm.jrt.android.core.invocation;

import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of a Context invocation simply passing on the input data.
 * <p>
 * Created by davide-maestroni on 10/06/2015.
 *
 * @param <DATA> the data type.
 */
public class IdentityContextInvocation<DATA> extends TemplateContextInvocation<DATA, DATA> {

    private static final ContextInvocationFactory<Object, Object> sFactory =
            new ContextInvocationFactory<Object, Object>(null) {

                @NotNull
                @Override
                public ContextInvocation<Object, Object> newInvocation() {
                    return new IdentityContextInvocation<Object>();
                }
            };

    /**
     * Avoid instantiation.
     */
    private IdentityContextInvocation() {
    }

    /**
     * Returns a factory of identity invocations.
     *
     * @param <DATA> the data type.
     * @return the factory.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> ContextInvocationFactory<DATA, DATA> factoryOf() {
        return (ContextInvocationFactory<DATA, DATA>) sFactory;
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
        result.pass(input);
    }
}
