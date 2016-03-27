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

import android.content.Context;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Base context invocation decorator implementation.
 * <p/>
 * Created by davide-maestroni on 08/19/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class ContextInvocationDecorator<IN, OUT> implements ContextInvocation<IN, OUT> {

    private final ContextInvocation<IN, OUT> mInvocation;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    public ContextInvocationDecorator(@NotNull final ContextInvocation<IN, OUT> wrapped) {

        mInvocation = ConstantConditions.notNull("wrapped invocation instance", wrapped);
    }

    public void onAbort(@NotNull final RoutineException reason) throws Exception {

        mInvocation.onAbort(reason);
    }

    public void onDestroy() throws Exception {

        mInvocation.onDestroy();
    }

    public void onInitialize() throws Exception {

        mInvocation.onInitialize();
    }

    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) throws Exception {

        mInvocation.onInput(input, result);
    }

    public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {

        mInvocation.onResult(result);
    }

    public void onTerminate() throws Exception {

        mInvocation.onTerminate();
    }

    public void onContext(@NotNull final Context context) throws Exception {

        mInvocation.onContext(context);
    }
}
