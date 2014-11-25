/**
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
package com.bmd.jrt.routine;

import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.invocation.BasicInvocation;

import javax.annotation.Nonnull;

/**
 * Implementation of an invocation handling parallel mode.
 * <p/>
 * Created by davide on 9/17/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
class ParallelInvocation<INPUT, OUTPUT> extends BasicInvocation<INPUT, OUTPUT> {

    private final Routine<INPUT, OUTPUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to invoke in parallel mode.
     * @throws NullPointerException if the routine instance is null;
     */
    @SuppressWarnings("ConstantConditions")
    ParallelInvocation(@Nonnull final Routine<INPUT, OUTPUT> routine) {

        if (routine == null) {

            throw new NullPointerException("the routine instance must not be null");
        }

        mRoutine = routine;
    }

    @Override
    public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

        result.pass(mRoutine.callAsync(input));
    }
}
