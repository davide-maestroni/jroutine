/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDecorator;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Invocation decorator used to test the correctness of an invocation lifecycle.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class LifecycleInvocationDecorator<IN, OUT> extends InvocationDecorator<IN, OUT> {

    private static final List<State> TO_ABORT_STATES = Arrays.asList(State.INIT, State.RESULT);

    private static final List<State> TO_DESTROY_STATES =
            Arrays.asList(State.INIT, State.ABORT, State.TERM);

    private static final List<State> TO_INITIALIZE_STATES = Arrays.asList(State.TERM, null);

    private static final List<State> TO_INPUT_STATES = Collections.singletonList(State.INIT);

    private static final List<State> TO_RESULT_STATES = Collections.singletonList(State.INIT);

    private static final List<State> TO_TERMINATE_STATES = Arrays.asList(State.ABORT, State.RESULT);

    private State mState;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.?
     */
    public LifecycleInvocationDecorator(@NotNull final Invocation<IN, OUT> wrapped) {

        super(wrapped);
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {

        if (!TO_ABORT_STATES.contains(mState)) {
            throw new InvocationInterruptedException(null);
        }

        mState = State.ABORT;
        super.onAbort(reason);
    }

    @Override
    public void onDestroy() throws Exception {

        if (!TO_DESTROY_STATES.contains(mState)) {
            throw new InvocationInterruptedException(null);
        }

        mState = State.DESTROY;
        super.onDestroy();
    }

    @Override
    public void onInitialize() throws Exception {

        if (!TO_INITIALIZE_STATES.contains(mState)) {
            throw new InvocationInterruptedException(null);
        }

        mState = State.INIT;
        super.onInitialize();
    }

    @Override
    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) throws Exception {

        if (!TO_INPUT_STATES.contains(mState)) {
            throw new InvocationInterruptedException(null);
        }

        super.onInput(input, result);
    }

    @Override
    public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {

        if (!TO_RESULT_STATES.contains(mState)) {
            throw new InvocationInterruptedException(null);
        }

        mState = State.RESULT;
        super.onResult(result);
    }

    @Override
    public void onTerminate() throws Exception {

        if (!TO_TERMINATE_STATES.contains(mState)) {
            throw new InvocationInterruptedException(null);
        }

        mState = State.TERM;
        super.onTerminate();
    }

    private enum State {
        INIT,
        RESULT,
        ABORT,
        TERM,
        DESTROY
    }
}
