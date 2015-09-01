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

import android.content.Context;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    @SuppressWarnings("ConstantConditions")
    public ContextInvocationDecorator(@Nonnull final ContextInvocation<IN, OUT> wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped invocation instance must not be null");
        }

        mInvocation = wrapped;
    }

    public void onAbort(@Nullable final RoutineException reason) {

        mInvocation.onAbort(reason);
    }

    public void onDestroy() {

        mInvocation.onDestroy();
    }

    public void onInitialize() {

        mInvocation.onInitialize();
    }

    public void onInput(final IN input, @Nonnull final ResultChannel<OUT> result) {

        mInvocation.onInput(input, result);
    }

    public void onResult(@Nonnull final ResultChannel<OUT> result) {

        mInvocation.onResult(result);
    }

    public void onTerminate() {

        mInvocation.onTerminate();
    }

    public void onContext(@Nonnull final Context context) {

        mInvocation.onContext(context);
    }
}
