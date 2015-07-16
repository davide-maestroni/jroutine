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
package com.gh.bmd.jrt.android.invocation;

import android.content.Context;

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.invocation.Invocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of an invocation decorator implementing the platform specific Android invocation.
 * <p/>
 * Created by davide-maestroni on 3/21/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class ContextInvocationDecorator<INPUT, OUTPUT> implements ContextInvocation<INPUT, OUTPUT> {

    private final Invocation<INPUT, OUTPUT> mInvocation;

    /**
     * Constructor.
     *
     * @param invocation the wrapped invocation.
     */
    @SuppressWarnings("ConstantConditions")
    public ContextInvocationDecorator(@Nonnull final Invocation<INPUT, OUTPUT> invocation) {

        if (invocation == null) {

            throw new NullPointerException("the invocation must not be null");
        }

        mInvocation = invocation;
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

    public void onContext(@Nonnull final Context context) {

    }
}
