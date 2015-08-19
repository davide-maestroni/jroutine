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
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base invocation decorator implementation.
 * <p/>
 * Created by davide-maestroni on 19/08/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class InvocationDecorator<INPUT, OUTPUT> implements Invocation<INPUT, OUTPUT> {

    private final Invocation<INPUT, OUTPUT> mInvocation;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    @SuppressWarnings("ConstantConditions")
    public InvocationDecorator(@Nonnull final Invocation<INPUT, OUTPUT> wrapped) {

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

    public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

        mInvocation.onInput(input, result);
    }

    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

        mInvocation.onResult(result);
    }

    public void onTerminate() {

        mInvocation.onTerminate();
    }
}
