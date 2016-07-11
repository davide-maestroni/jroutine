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

package com.github.dm.jrt.core.routine;

import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

/**
 * Routine invocation mode type.
 * <br>
 * The mode indicates in which way the routine should be invoked.
 * <p>
 * Created by davide-maestroni on 04/12/2016.
 */
public enum InvocationMode {

    /**
     * Synchronous mode.
     *
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    SYNC(new Invoker() {

        @NotNull
        public <IN, OUT> Channel<IN, OUT> invoke(@NotNull final Routine<IN, OUT> routine) {
            return routine.syncCall();
        }
    }),

    /**
     * Asynchronous mode.
     *
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    ASYNC(new Invoker() {

        @NotNull
        public <IN, OUT> Channel<IN, OUT> invoke(@NotNull final Routine<IN, OUT> routine) {
            return routine.asyncCall();
        }
    }),

    /**
     * Parallel mode.
     *
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    PARALLEL(new Invoker() {

        @NotNull
        public <IN, OUT> Channel<IN, OUT> invoke(@NotNull final Routine<IN, OUT> routine) {
            return routine.parallelCall();
        }
    }),

    /**
     * Sequential mode.
     *
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    SEQUENTIAL(new Invoker() {

        @NotNull
        public <IN, OUT> Channel<IN, OUT> invoke(@NotNull final Routine<IN, OUT> routine) {
            return routine.sequentialCall();
        }
    });

    private final Invoker mInvoker;

    /**
     * Constructor.
     *
     * @param invoker the invoker.
     */
    InvocationMode(@NotNull final Invoker invoker) {
        mInvoker = invoker;
    }

    /**
     * Invokes the specified routine with this mode.
     *
     * @param routine the routine to invoke.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the invocation channel.
     */
    @NotNull
    public <IN, OUT> Channel<IN, OUT> invoke(@NotNull final Routine<IN, OUT> routine) {
        return mInvoker.invoke(routine);
    }

    /**
     * Invoker interface.
     */
    private interface Invoker {

        /**
         * Invokes the specified routine.
         *
         * @param routine the routine to invoke.
         * @param <IN>    the input data type.
         * @param <OUT>   the output data type.
         * @return the invocation channel.
         */
        @NotNull
        <IN, OUT> Channel<IN, OUT> invoke(@NotNull Routine<IN, OUT> routine);
    }
}
