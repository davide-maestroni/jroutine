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

import com.github.dm.jrt.core.channel.ResultChannel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implementation of a function invocation simply passing on the input data.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 *
 * @param <DATA> the data type.
 */
public class PassingCallContextInvocation<DATA> extends CallContextInvocation<DATA, DATA> {

    private static final CallContextInvocationFactory<Object, Object> sFactory =
            new CallContextInvocationFactory<Object, Object>(null) {

                @NotNull
                @Override
                public CallContextInvocation<Object, Object> newInvocation() {

                    return new PassingCallContextInvocation<Object>();
                }
            };

    /**
     * Avoid instantiation.
     */
    private PassingCallContextInvocation() {

    }

    /**
     * Returns a factory of passing invocations.
     *
     * @param <DATA> the data type.
     * @return the factory.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <DATA> CallContextInvocationFactory<DATA, DATA> factoryOf() {

        return (CallContextInvocationFactory<DATA, DATA>) sFactory;
    }

    @Override
    protected void onCall(@NotNull final List<? extends DATA> inputs,
            @NotNull final ResultChannel<DATA> result) {

        result.pass(inputs);
    }
}
