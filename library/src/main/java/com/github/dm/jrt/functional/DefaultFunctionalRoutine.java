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
package com.github.dm.jrt.functional;

import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.invocation.DelegatingInvocation;
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.Channels.syncStream;

/**
 * Default implementation of a functional routine.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultFunctionalRoutine<IN, OUT> extends AbstractFunctionalRoutine<IN, OUT> {

    private final Routine<IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the backing routine instance.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultFunctionalRoutine(@NotNull final Routine<IN, OUT> routine) {

        if (routine == null) {

            throw new NullPointerException("the backing routine must not be null");
        }

        mRoutine = routine;
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> thenLift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function) {

        return new DefaultFunctionalRoutine<BEFORE, AFTER>(function.apply(this));
    }

    @NotNull
    @Override
    protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) {

        return new DelegatingInvocation<IN, OUT>(mRoutine, DelegationType.SYNC);
    }

    /**
     * Functional routine implementation concatenating two different routines.
     *
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @param <AFTER> the concatenation output type.
     */
    private static class AfterFunctionalRoutine<IN, OUT, AFTER>
            extends AbstractFunctionalRoutine<IN, AFTER> {

        private final Routine<? super OUT, AFTER> mAfterRoutine;

        private final DelegationType mDelegationType;

        private final FunctionalRoutine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine        the backing routine instance.
         * @param afterRoutine   the concatenated routine instance.
         * @param delegationType the concatenated delegation type.
         */
        @SuppressWarnings("ConstantConditions")
        private AfterFunctionalRoutine(@NotNull final FunctionalRoutine<IN, OUT> routine,
                @NotNull final Routine<? super OUT, AFTER> afterRoutine,
                @NotNull final DelegationType delegationType) {

            if (afterRoutine == null) {

                throw new NullPointerException("the concatenated routine must not be null");
            }

            if (delegationType == null) {

                throw new NullPointerException("the concatenated delegation type must not be null");
            }

            mRoutine = routine;
            mAfterRoutine = afterRoutine;
            mDelegationType = delegationType;
        }

        @NotNull
        @Override
        protected <NEXT> FunctionalRoutine<IN, NEXT> andThen(
                @NotNull final Routine<? super AFTER, NEXT> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterFunctionalRoutine<IN, AFTER, NEXT>(this, routine, delegationType);
        }

        @NotNull
        public <BEFORE, NEXT> FunctionalRoutine<BEFORE, NEXT> thenLift(
                @NotNull final Function<? super FunctionalRoutine<IN, AFTER>, ? extends
                        Routine<BEFORE, NEXT>> function) {

            return new DefaultFunctionalRoutine<BEFORE, NEXT>(function.apply(this));
        }

        @NotNull
        @Override
        protected Invocation<IN, AFTER> newInvocation(@NotNull final InvocationType type) {

            return new AfterInvocation<IN, OUT, AFTER>(mRoutine, mAfterRoutine, mDelegationType);
        }
    }

    /**
     * Invocation implementation concatenating two different routines.
     *
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @param <AFTER> the concatenation output type.
     */
    private static class AfterInvocation<IN, OUT, AFTER> implements Invocation<IN, AFTER> {

        private final Routine<? super OUT, AFTER> mAfterRoutine;

        private final DelegationType mDelegationType;

        private final FunctionalRoutine<IN, OUT> mRoutine;

        private StreamingChannel<IN, OUT> mInputChannel;

        private OutputChannel<AFTER> mOutputChannel;

        /**
         * Constructor.
         *
         * @param routine        the backing routine instance.
         * @param afterRoutine   the concatenated routine instance.
         * @param delegationType the concatenated delegation type.
         */
        private AfterInvocation(@NotNull final FunctionalRoutine<IN, OUT> routine,
                @NotNull final Routine<? super OUT, AFTER> afterRoutine,
                @NotNull final DelegationType delegationType) {

            mRoutine = routine;
            mAfterRoutine = afterRoutine;
            mDelegationType = delegationType;
        }

        public void onAbort(@Nullable final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

            mInputChannel = null;
            mOutputChannel = null;
        }

        public void onInitialize() {

            final StreamingChannel<IN, OUT> streamingChannel = syncStream(mRoutine);
            final DelegationType delegationType = mDelegationType;

            if (delegationType == DelegationType.ASYNC) {

                mOutputChannel = streamingChannel.passTo(mAfterRoutine.asyncInvoke()).result();
                mInputChannel = streamingChannel;

            } else if (delegationType == DelegationType.PARALLEL) {

                mOutputChannel = streamingChannel.passTo(mAfterRoutine.parallelInvoke()).result();
                mInputChannel = streamingChannel;

            } else {

                mOutputChannel = streamingChannel.passTo(mAfterRoutine.syncInvoke()).result();
                mInputChannel = streamingChannel;
            }
        }

        public void onInput(final IN input, @NotNull final ResultChannel<AFTER> result) {

            final OutputChannel<AFTER> channel = mOutputChannel;

            if (!channel.isBound()) {

                channel.passTo(result);
            }

            mInputChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<AFTER> result) {

            final OutputChannel<AFTER> channel = mOutputChannel;

            if (!channel.isBound()) {

                channel.passTo(result);
            }

            mInputChannel.close();
        }

        public void onTerminate() {

            mInputChannel = null;
            mOutputChannel = null;
        }
    }

    @NotNull
    @Override
    protected <AFTER> FunctionalRoutine<IN, AFTER> andThen(
            @NotNull final Routine<? super OUT, AFTER> routine,
            @NotNull final DelegationType delegationType) {

        return new AfterFunctionalRoutine<IN, OUT, AFTER>(this, routine, delegationType);
    }
}
