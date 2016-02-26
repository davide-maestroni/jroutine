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

package com.github.dm.jrt.android.invocation;

import com.github.dm.jrt.channel.ResultChannel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implementation of a function invocation simply passing on the input data.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 *
 * @param <DATA> the data type.
 */
public class PassingFunctionContextInvocation<DATA> extends FunctionContextInvocation<DATA, DATA> {

    private static final FunctionContextInvocationFactory<Object, Object> sFactory =
            new FunctionContextInvocationFactory<Object, Object>(null) {

                @NotNull
                @Override
                public FunctionContextInvocation<Object, Object> newInvocation() {

                    return new PassingFunctionContextInvocation<Object>();
                }
            };

    /**
     * Avoid instantiation.
     */
    private PassingFunctionContextInvocation() {

    }

    /**
     * Returns a factory of passing invocations.
     *
     * @param <DATA> the data type.
     * @return the factory.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> FunctionContextInvocationFactory<DATA, DATA> factoryOf() {

        return (FunctionContextInvocationFactory<DATA, DATA>) sFactory;
    }

    @Override
    protected void onCall(@NotNull final List<? extends DATA> inputs,
            @NotNull final ResultChannel<DATA> result) {

        result.pass(inputs);
    }
}
