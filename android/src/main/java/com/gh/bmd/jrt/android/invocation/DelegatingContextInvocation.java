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

import com.gh.bmd.jrt.invocation.DelegatingInvocation;
import com.gh.bmd.jrt.routine.Routine;

import javax.annotation.Nonnull;

/**
 * Implementation of a delegating invocation implementing the platform specific Android invocation.
 * <p/>
 * Created by davide on 19/04/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class DelegatingContextInvocation<INPUT, OUTPUT> extends DelegatingInvocation<INPUT, OUTPUT>
        implements ContextInvocation<INPUT, OUTPUT> {

    /**
     * Constructor.
     *
     * @param routine the routine used to execute this invocation.
     */
    public DelegatingContextInvocation(@Nonnull final Routine<INPUT, OUTPUT> routine) {

        super(routine);
    }

    /**
     * Returns a factory of delegating invocations.
     *
     * @param routine        the routine used to execute this invocation.
     * @param invocationType the invocation type.
     * @param <INPUT>        the input data type.
     * @param <OUTPUT>       the output data type.
     * @return the factory.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryWith(
            @Nonnull final Routine<INPUT, OUTPUT> routine, @Nonnull final String invocationType) {

        if (routine == null) {

            throw new NullPointerException("the routine must not be null");
        }

        if (invocationType == null) {

            throw new NullPointerException("the invocation type must not be null");
        }

        return new ContextInvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public String getInvocationType() {

                return invocationType;
            }

            @Nonnull
            public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new DelegatingContextInvocation<INPUT, OUTPUT>(routine);
            }
        };
    }

    public void onContext(@Nonnull final Context context) {

    }
}
