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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory of invocations collecting inputs into lists.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 *
 * @param <DATA> the data type.
 */
class ToListInvocation<DATA> extends TemplateInvocation<DATA, List<DATA>> {

    private static final InvocationFactory<?, ? extends List<?>> sFactory =
            new InvocationFactory<Object, List<Object>>(null) {

                @NotNull
                @Override
                public Invocation<Object, List<Object>> newInvocation() throws Exception {

                    return new ToListInvocation<Object>();
                }
            };

    private ArrayList<DATA> mList;

    /**
     * Constructor.
     */
    private ToListInvocation() {

    }

    /**
     * Returns the factory of collecting invocations.
     *
     * @param <DATA> the data type.
     * @return the factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> InvocationFactory<DATA, List<DATA>> factoryOf() {

        return (InvocationFactory<DATA, List<DATA>>) sFactory;
    }

    @Override
    public void onInitialize() {

        mList = new ArrayList<DATA>();
    }

    @Override
    public void onInput(final DATA input, @NotNull final ResultChannel<List<DATA>> result) {

        mList.add(input);
    }

    @Override
    public void onResult(@NotNull final ResultChannel<List<DATA>> result) {

        result.pass(mList);
        mList = null;
    }

    @Override
    public void onTerminate() {

        mList = null;
    }
}
