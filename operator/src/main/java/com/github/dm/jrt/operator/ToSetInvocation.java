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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Factory of invocations collecting inputs into sets.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 *
 * @param <DATA> the data type.
 */
class ToSetInvocation<DATA> extends TemplateInvocation<DATA, Set<DATA>> {

    private static final InvocationFactory<?, ? extends Set<?>> sFactory =
            new InvocationFactory<Object, Set<Object>>(null) {

                @NotNull
                @Override
                public Invocation<Object, Set<Object>> newInvocation() {
                    return new ToSetInvocation<Object>();
                }
            };

    private HashSet<DATA> mSet;

    /**
     * Constructor.
     */
    private ToSetInvocation() {
    }

    /**
     * Returns the factory of collecting invocations.
     *
     * @param <DATA> the data type.
     * @return the factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    static <DATA> InvocationFactory<DATA, Set<DATA>> factoryOf() {
        return (InvocationFactory<DATA, Set<DATA>>) sFactory;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
        mSet = null;
    }

    @Override
    public void onComplete(@NotNull final Channel<Set<DATA>, ?> result) {
        result.pass(mSet);
        mSet = null;
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<Set<DATA>, ?> result) {
        mSet.add(input);
    }

    @Override
    public void onRecycle() {
        mSet = new HashSet<DATA>();
    }
}
