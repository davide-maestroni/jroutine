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
package com.github.dm.jrt.invocation;

import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.routine.Routine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Invocation implementation delegating the execution to another routine.
 * <p/>
 * Created by davide-maestroni on 18/04/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class DelegatingInvocation<INPUT, OUTPUT> implements Invocation<INPUT, OUTPUT> {

    private final DelegationType mDelegationType;

    private final Routine<INPUT, OUTPUT> mRoutine;

    private InvocationChannel<INPUT, OUTPUT> mChannel = null;

    /**
     * Constructor.
     *
     * @param routine        the routine used to execute this invocation.
     * @param delegationType the type of routine invocation.
     */
    @SuppressWarnings("ConstantConditions")
    public DelegatingInvocation(@Nonnull final Routine<INPUT, OUTPUT> routine,
            @Nonnull final DelegationType delegationType) {

        if (routine == null) {

            throw new NullPointerException("the routine must not be null");
        }

        if (delegationType == null) {

            throw new NullPointerException("the invocation type must not be null");
        }

        mRoutine = routine;
        mDelegationType = delegationType;
    }

    /**
     * Returns a factory of delegating invocations.
     *
     * @param routine        the routine used to execute this invocation.
     * @param delegationType the type of routine invocation.
     * @param <INPUT>        the input data type.
     * @param <OUTPUT>       the output data type.
     * @return the factory.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryFrom(
            @Nonnull final Routine<INPUT, OUTPUT> routine,
            @Nonnull final DelegationType delegationType) {

        return new DelegatingInvocationFactory<INPUT, OUTPUT>(routine, delegationType);
    }

    public void onAbort(@Nullable final RoutineException reason) {

        mChannel.abort(reason);
    }

    public void onDestroy() {

        mRoutine.purge();
    }

    public void onInitialize() {

        final DelegationType delegationType = mDelegationType;
        mChannel = (delegationType == DelegationType.SYNCHRONOUS) ? mRoutine.syncInvoke()
                : (delegationType == DelegationType.ASYNCHRONOUS) ? mRoutine.asyncInvoke()
                        : mRoutine.parallelInvoke();
    }

    public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

        mChannel.pass(input);
    }

    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

        result.pass(mChannel.result());
    }

    public void onTerminate() {

        mChannel = null;
    }

    /**
     * Delegation type enumeration.
     */
    public enum DelegationType {

        /**
         * The delegated routine is invoked in synchronous mode.
         */
        SYNCHRONOUS,
        /**
         * The delegated routine is invoked in asynchronous mode.
         */
        ASYNCHRONOUS,
        /**
         * The delegated routine is invoked in parallel mode.
         */
        PARALLEL
    }

    /**
     * Factory creating delegating invocation instances.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DelegatingInvocationFactory<INPUT, OUTPUT>
            extends InvocationFactory<INPUT, OUTPUT> {

        private final DelegationType mDelegationType;

        private final Routine<INPUT, OUTPUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine        the delegated routine.
         * @param delegationType the type of routine invocation.
         */
        @SuppressWarnings("ConstantConditions")
        private DelegatingInvocationFactory(@Nonnull final Routine<INPUT, OUTPUT> routine,
                @Nonnull final DelegationType delegationType) {

            if (routine == null) {

                throw new NullPointerException("the routine must not be null");
            }

            if (delegationType == null) {

                throw new NullPointerException("the invocation type must not be null");
            }

            mRoutine = routine;
            mDelegationType = delegationType;
        }

        @Nonnull
        @Override
        public Invocation<INPUT, OUTPUT> newInvocation() {

            return new DelegatingInvocation<INPUT, OUTPUT>(mRoutine, mDelegationType);
        }
    }
}
