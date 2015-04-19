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

import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.routine.Routine;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.invocation.Invocations.withArgs;

/**
 * Invocation implementation delegating the execution to another routine.
 * <p/>
 * Created by davide on 18/04/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class DelegatingInvocation<INPUT, OUTPUT> implements Invocation<INPUT, OUTPUT> {

    private final Routine<INPUT, OUTPUT> mRoutine;

    private ParameterChannel<INPUT, OUTPUT> mChannel = null;

    /**
     * Constructor.
     *
     * @param routine the routine used to execute this invocation.
     * @throws java.lang.NullPointerException if the specified routine is null.
     */
    @SuppressWarnings("ConstantConditions")
    public DelegatingInvocation(@Nonnull final Routine<INPUT, OUTPUT> routine) {

        if (routine == null) {

            throw new NullPointerException("the routine must not be null");
        }

        mRoutine = routine;
    }

    /**
     * Returns a factory of delegating invocations.
     *
     * @param routine  the routine used to execute this invocation.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryWith(
            @Nonnull final Routine<INPUT, OUTPUT> routine) {

        return withArgs(routine).factoryOf(
                new ClassToken<DelegatingInvocation<INPUT, OUTPUT>>() {});
    }

    public void onAbort(final Throwable reason) {

        mChannel.abort(reason);
    }

    public void onDestroy() {

        mRoutine.purge();
    }

    public void onInit() {

        mChannel = mRoutine.invokeAsync();
    }

    public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

        mChannel.pass(input);
    }

    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

        result.pass(mChannel.result());
    }

    public void onReturn() {

        mChannel = null;
    }
}
