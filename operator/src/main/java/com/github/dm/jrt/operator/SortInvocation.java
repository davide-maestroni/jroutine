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

import java.util.ArrayList;
import java.util.Collections;

/**
 * Invocation sorting the input in natural ordering.
 * <p>
 * Created by davide-maestroni on 05/06/2016.
 *
 * @param <DATA> the data type.
 */
class SortInvocation<DATA extends Comparable<? super DATA>> extends TemplateInvocation<DATA, DATA> {

    private static final InvocationFactory<? extends Comparable<?>, ? extends Comparable<?>>
            sFactory = new InvocationFactory<Comparable<Object>, Comparable<Object>>(null) {

        @NotNull
        @Override
        public Invocation<Comparable<Object>, Comparable<Object>> newInvocation() {
            return new SortInvocation<Comparable<Object>>();
        }
    };

    private ArrayList<DATA> mList = new ArrayList<DATA>();

    /**
     * Constructor.
     */
    private SortInvocation() {
    }

    /**
     * Returns the factory of sorting invocations.
     *
     * @param <DATA> the data type.
     * @return the factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    static <DATA extends Comparable<? super DATA>> InvocationFactory<DATA, DATA> factoryOf() {
        return (InvocationFactory<DATA, DATA>) sFactory;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
        mList.clear();
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) {
        final ArrayList<DATA> list = mList;
        Collections.sort(list);
        result.pass(list);
        list.clear();
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
        mList.add(input);
    }
}
