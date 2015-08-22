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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract implementation of an invocation performing a procedure  (that is, no input is required)
 * eventually returning output data.
 * <p/>
 * Note that the implementing class must not retain an internal variable state.
 *
 * @param <OUTPUT> the output data type.
 */
public abstract class ProcedureContextInvocation<OUTPUT>
        extends ContextInvocationFactory<Void, OUTPUT> implements ContextInvocation<Void, OUTPUT> {

    @Nonnull
    @Override
    public final ContextInvocation<Void, OUTPUT> newInvocation() {

        return this;
    }

    public final void onAbort(@Nullable final RoutineException reason) {

    }

    public final void onDestroy() {

    }

    public final void onInitialize() {

    }

    public final void onInput(final Void input, @Nonnull final ResultChannel<OUTPUT> result) {

    }

    public final void onTerminate() {

    }

    public final void onContext(@Nonnull final Context context) {

    }
}
