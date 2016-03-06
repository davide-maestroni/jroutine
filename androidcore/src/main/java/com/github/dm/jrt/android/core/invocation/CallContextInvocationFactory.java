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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract class defining a factory of function context invocations.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class CallContextInvocationFactory<IN, OUT>
        extends ContextInvocationFactory<IN, OUT> {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected CallContextInvocationFactory(@Nullable final Object[] args) {

        super(args);
    }

    @NotNull
    @Override
    public abstract CallContextInvocation<IN, OUT> newInvocation() throws Exception;
}
