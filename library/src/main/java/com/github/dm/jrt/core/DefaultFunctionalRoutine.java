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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.functional.Function;
import com.github.dm.jrt.invocation.DelegatingInvocation;
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.routine.FunctionalRoutine;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.Channels.asyncStream;
import static com.github.dm.jrt.core.Channels.parallelStream;
import static com.github.dm.jrt.core.Channels.syncStream;

/**
 * Created by davide-maestroni on 10/16/2015.
 */
public class DefaultFunctionalRoutine<IN, OUT> extends AbstractFunctionalRoutine<IN, OUT> {

    private final DelegationType mDelegationType;

    private final Routine<IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     */
    protected DefaultFunctionalRoutine(@NotNull final InvocationConfiguration configuration,
            @NotNull final Routine<IN, OUT> routine, @NotNull final DelegationType delegationType) {

        super(configuration);
        mRoutine = routine;
        mDelegationType = delegationType;
    }

    @NotNull
    @Override
    protected <AFTER> FunctionalRoutine<IN, AFTER> andThen(
            @NotNull final Routine<? super OUT, AFTER> routine,
            @NotNull final DelegationType delegationType) {

        return new AfterFunctionalRoutine<IN, OUT, AFTER>(getBuilderConfiguration(), this,
                                                          mDelegationType, routine, delegationType);
    }

    @NotNull
    @Override
    protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) {

        return new DelegatingInvocation<IN, OUT>(mRoutine, mDelegationType);
    }

    private static class AfterFunctionalRoutine<IN, OUT, AFTER>
            extends AbstractFunctionalRoutine<IN, AFTER> {

        private final DelegationType mAfterDelegationType;

        private final Routine<? super OUT, AFTER> mAfterRoutine;

        private final DelegationType mDelegationType;

        private final FunctionalRoutine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param configuration the invocation configuration.
         */
        @SuppressWarnings("ConstantConditions")
        protected AfterFunctionalRoutine(@NotNull final InvocationConfiguration configuration,
                @NotNull final FunctionalRoutine<IN, OUT> routine,
                @NotNull final DelegationType delegationType,
                @NotNull final Routine<? super OUT, AFTER> afterRoutine,
                @NotNull final DelegationType afterDelegationType) {

            super(configuration);

            if (afterRoutine == null) {

                throw new NullPointerException("the after routine must not be null");
            }

            mRoutine = routine;
            mDelegationType = delegationType;
            mAfterRoutine = afterRoutine;
            mAfterDelegationType = afterDelegationType;
        }

        @NotNull
        @Override
        protected <BEFORE, NEXT> FunctionalRoutine<BEFORE, NEXT> lift(
                @NotNull final Function<? super FunctionalRoutine<IN, AFTER>, ? extends
                        Routine<BEFORE, NEXT>> function,
                @NotNull final DelegationType delegationType) {

            return new DefaultFunctionalRoutine<BEFORE, NEXT>(getBuilderConfiguration(),
                                                              function.apply(this), delegationType);
        }

        @NotNull
        @Override
        protected <NEXT> FunctionalRoutine<IN, NEXT> andThen(
                @NotNull final Routine<? super AFTER, NEXT> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterFunctionalRoutine<IN, AFTER, NEXT>(getBuilderConfiguration(), this,
                                                               DelegationType.SYNC, routine,
                                                               delegationType);
        }

        @NotNull
        @Override
        protected <BEFORE> FunctionalRoutine<BEFORE, AFTER> compose(
                @NotNull final Routine<BEFORE, ? extends IN> routine,
                @NotNull final DelegationType delegationType) {

            return new BeforeFunctionalRoutine<BEFORE, IN, AFTER>(getBuilderConfiguration(), this,
                                                                  DelegationType.SYNC, routine,
                                                                  delegationType);
        }


        @NotNull
        @Override
        protected Invocation<IN, AFTER> newInvocation(@NotNull final InvocationType type) {

            return new AfterInvocation<IN, OUT, AFTER>(mRoutine, mDelegationType, mAfterRoutine,
                                                       mAfterDelegationType);
        }
    }

    private static class AfterInvocation<IN, OUT, AFTER> implements Invocation<IN, AFTER> {

        private final DelegationType mAfterDelegationType;

        private final Routine<? super OUT, AFTER> mAfterRoutine;

        private final DelegationType mDelegationType;

        private final FunctionalRoutine<IN, OUT> mRoutine;

        private StreamingChannel<IN, OUT> mInputChannel;

        private OutputChannel<AFTER> mOutputChannel;

        private AfterInvocation(@NotNull final FunctionalRoutine<IN, OUT> routine,
                @NotNull final DelegationType delegationType,
                @NotNull final Routine<? super OUT, AFTER> afterRoutine,
                @NotNull final DelegationType afterDelegationType) {

            mRoutine = routine;
            mDelegationType = delegationType;
            mAfterRoutine = afterRoutine;
            mAfterDelegationType = afterDelegationType;
        }

        public void onAbort(@Nullable final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

            mInputChannel = null;
            mOutputChannel = null;
        }

        public void onInitialize() {

            final DelegationType delegationType = mDelegationType;
            final StreamingChannel<IN, OUT> streamingChannel =
                    (delegationType == DelegationType.ASYNC) ? asyncStream(mRoutine)
                            : (delegationType == DelegationType.PARALLEL) ? parallelStream(mRoutine)
                                    : syncStream(mRoutine);
            final DelegationType afterDelegationType = mAfterDelegationType;

            if (afterDelegationType == DelegationType.ASYNC) {

                mOutputChannel = streamingChannel.passTo(mAfterRoutine.asyncInvoke()).result();
                mInputChannel = streamingChannel;

            } else if (afterDelegationType == DelegationType.PARALLEL) {

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

    private static class BeforeFunctionalRoutine<BEFORE, IN, OUT>
            extends AbstractFunctionalRoutine<BEFORE, OUT> {

        private final DelegationType mBeforeDelegationType;

        private final Routine<BEFORE, ? extends IN> mBeforeRoutine;

        private final DelegationType mDelegationType;

        private final FunctionalRoutine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param configuration the invocation configuration.
         */
        @SuppressWarnings("ConstantConditions")
        protected BeforeFunctionalRoutine(@NotNull final InvocationConfiguration configuration,
                @NotNull final FunctionalRoutine<IN, OUT> routine,
                @NotNull final DelegationType delegationType,
                @NotNull final Routine<BEFORE, ? extends IN> beforeRoutine,
                @NotNull final DelegationType beforeDelegationType) {

            super(configuration);

            if (beforeRoutine == null) {

                throw new NullPointerException("the after routine must not be null");
            }

            mRoutine = routine;
            mDelegationType = delegationType;
            mBeforeRoutine = beforeRoutine;
            mBeforeDelegationType = beforeDelegationType;
        }

        @NotNull
        @Override
        protected <PREV, AFTER> FunctionalRoutine<PREV, AFTER> lift(
                @NotNull final Function<? super FunctionalRoutine<BEFORE, OUT>, ? extends
                        Routine<PREV, AFTER>> function,
                @NotNull final DelegationType delegationType) {

            return new DefaultFunctionalRoutine<PREV, AFTER>(getBuilderConfiguration(),
                                                             function.apply(this), delegationType);
        }

        @NotNull
        @Override
        protected <AFTER> FunctionalRoutine<BEFORE, AFTER> andThen(
                @NotNull final Routine<? super OUT, AFTER> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterFunctionalRoutine<BEFORE, OUT, AFTER>(getBuilderConfiguration(), this,
                                                                  DelegationType.SYNC, routine,
                                                                  delegationType);
        }

        @NotNull
        @Override
        protected <PREV> FunctionalRoutine<PREV, OUT> compose(
                @NotNull final Routine<PREV, ? extends BEFORE> routine,
                @NotNull final DelegationType delegationType) {

            return new BeforeFunctionalRoutine<PREV, BEFORE, OUT>(getBuilderConfiguration(), this,
                                                                  DelegationType.SYNC, routine,
                                                                  delegationType);
        }


        @NotNull
        @Override
        protected Invocation<BEFORE, OUT> newInvocation(@NotNull final InvocationType type) {

            return new BeforeInvocation<BEFORE, IN, OUT>(mRoutine, mDelegationType, mBeforeRoutine,
                                                         mBeforeDelegationType);
        }
    }

    private static class BeforeInvocation<BEFORE, IN, OUT> implements Invocation<BEFORE, OUT> {

        private final DelegationType mBeforeDelegationType;

        private final Routine<BEFORE, ? extends IN> mBeforeRoutine;

        private final DelegationType mDelegationType;

        private final FunctionalRoutine<IN, OUT> mRoutine;

        private StreamingChannel<BEFORE, ? extends IN> mInputChannel;

        private OutputChannel<OUT> mOutputChannel;

        private BeforeInvocation(@NotNull final FunctionalRoutine<IN, OUT> routine,
                @NotNull final DelegationType delegationType,
                @NotNull final Routine<BEFORE, ? extends IN> beforeRoutine,
                @NotNull final DelegationType beforeDelegationType) {

            mRoutine = routine;
            mDelegationType = delegationType;
            mBeforeRoutine = beforeRoutine;
            mBeforeDelegationType = beforeDelegationType;
        }

        public void onAbort(@Nullable final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

            mInputChannel = null;
            mOutputChannel = null;
        }

        public void onInitialize() {

            final DelegationType beforeDelegationType = mBeforeDelegationType;
            final StreamingChannel<BEFORE, ? extends IN> streamingChannel =
                    (beforeDelegationType == DelegationType.ASYNC) ? asyncStream(mBeforeRoutine)
                            : (beforeDelegationType == DelegationType.PARALLEL) ? parallelStream(
                                    mBeforeRoutine) : syncStream(mBeforeRoutine);
            final DelegationType delegationType = mDelegationType;

            if (delegationType == DelegationType.ASYNC) {

                mOutputChannel = streamingChannel.passTo(mRoutine.asyncInvoke()).result();
                mInputChannel = streamingChannel;

            } else if (delegationType == DelegationType.PARALLEL) {

                mOutputChannel = streamingChannel.passTo(mRoutine.parallelInvoke()).result();
                mInputChannel = streamingChannel;

            } else {

                mOutputChannel = streamingChannel.passTo(mRoutine.syncInvoke()).result();
                mInputChannel = streamingChannel;
            }
        }

        public void onInput(final BEFORE input, @NotNull final ResultChannel<OUT> result) {

            final OutputChannel<OUT> channel = mOutputChannel;

            if (!channel.isBound()) {

                channel.passTo(result);
            }

            mInputChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            final OutputChannel<OUT> channel = mOutputChannel;

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
    protected <BEFORE> FunctionalRoutine<BEFORE, OUT> compose(
            @NotNull final Routine<BEFORE, ? extends IN> routine,
            @NotNull final DelegationType delegationType) {

        return new BeforeFunctionalRoutine<BEFORE, IN, OUT>(getBuilderConfiguration(), this,
                                                            DelegationType.SYNC, routine,
                                                            delegationType);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> lift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function,
            @NotNull final DelegationType delegationType) {

        return new DefaultFunctionalRoutine<BEFORE, AFTER>(getBuilderConfiguration(),
                                                           function.apply(this), delegationType);
    }
}
