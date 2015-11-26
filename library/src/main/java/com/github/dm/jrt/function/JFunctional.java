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
package com.github.dm.jrt.function;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.DelegatingInvocation;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.core.Channels.syncStream;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.supplierCommand;

/**
 * Utility class acting as a factory of functional routine builders.
 * <p/>
 * Created by davide-maestroni on 11/26/2015.
 */
public class JFunctional {

    /**
     * Avoid direct instantiation.
     */
    protected JFunctional() {

    }

    /**
     * Returns a functional routine builder.
     *
     * @return the routine builder instance.
     */
    @NotNull
    public static FunctionalRoutineBuilder routine() {

        return new DefaultFunctionalRoutineBuilder();
    }

    /**
     * Abstract implementation of a functional routine.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static abstract class AbstractFunctionalRoutine<IN, OUT>
            extends AbstractRoutine<IN, OUT>
            implements FunctionalRoutine<IN, OUT>, Configurable<FunctionalRoutine<IN, OUT>> {

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.DEFAULT_CONFIGURATION;

        /**
         * Constructor.
         */
        private AbstractFunctionalRoutine() {

            super(InvocationConfiguration.DEFAULT_CONFIGURATION);
        }

        @NotNull
        public Builder<? extends FunctionalRoutine<IN, OUT>> invocations() {

            return new Builder<FunctionalRoutine<IN, OUT>>(this, mConfiguration);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public FunctionalRoutine<IN, OUT> setConfiguration(
                @NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConfiguration = configuration;
            return this;
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> thenAsyncAccumulate(
                @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> thenAsyncFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return andThen(routine, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncReduce(
                @NotNull final BiConsumer<? super List<? extends OUT>, ? super
                        ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncReduce(
                @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> thenFlatLift(
                @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends
                        FunctionalRoutine<BEFORE, AFTER>> function) {

            return function.apply(this);
        }

        /**
         * Concatenates a functional routine based on the specified instance to this one.
         *
         * @param routine        the routine instance.
         * @param delegationType the delegation type.
         * @param <AFTER>        the concatenation output type.
         * @return the concatenated functional routine.
         */
        @NotNull
        protected abstract <AFTER> FunctionalRoutine<IN, AFTER> andThen(
                @NotNull Routine<? super OUT, AFTER> routine,
                @NotNull DelegationType delegationType);

        @NotNull
        private <AFTER> FunctionalRoutine<IN, AFTER> fromFactory(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory,
                @NotNull final DelegationType delegationType) {

            return andThen(
                    JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine(),
                    delegationType);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> thenParallelFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }


        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return andThen(routine, DelegationType.PARALLEL);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> thenSyncAccumulate(
                @NotNull final BiFunction<? super OUT, ? super OUT, ?
                        extends OUT> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> thenSyncFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return andThen(routine, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncReduce(
                @NotNull final BiConsumer<? super List<? extends OUT>, ? super
                        ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncReduce(
                @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }


    }

    /**
     * Default implementation of a functional routine.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultFunctionalRoutine<IN, OUT>
            extends AbstractFunctionalRoutine<IN, OUT> {

        private final DelegationType mDelegationType;

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine        the backing routine instance.
         * @param delegationType the delegation type.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultFunctionalRoutine(@NotNull final Routine<IN, OUT> routine,
                @NotNull final DelegationType delegationType) {

            if (routine == null) {

                throw new NullPointerException("the backing routine must not be null");
            }

            mRoutine = routine;
            mDelegationType = delegationType;
        }

        @NotNull
        @Override
        protected <AFTER> FunctionalRoutine<IN, AFTER> andThen(
                @NotNull final Routine<? super OUT, AFTER> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterFunctionalRoutine<IN, OUT, AFTER>(this, routine, delegationType);
        }

        @NotNull
        @Override
        protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) {

            return new DelegatingInvocation<IN, OUT>(mRoutine, mDelegationType);
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

                return new DefaultFunctionalRoutine<BEFORE, NEXT>(function.apply(this),
                                                                  DelegationType.SYNC);
            }

            @NotNull
            @Override
            protected Invocation<IN, AFTER> newInvocation(@NotNull final InvocationType type) {

                return new AfterInvocation<IN, OUT, AFTER>(mRoutine, mAfterRoutine,
                                                           mDelegationType);
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
                mInputChannel = streamingChannel;

                if (delegationType == DelegationType.ASYNC) {

                    mOutputChannel = streamingChannel.passTo(mAfterRoutine.asyncInvoke()).result();

                } else if (delegationType == DelegationType.PARALLEL) {

                    mOutputChannel =
                            streamingChannel.passTo(mAfterRoutine.parallelInvoke()).result();

                } else {

                    mOutputChannel = streamingChannel.passTo(mAfterRoutine.syncInvoke()).result();
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
        public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> thenLift(
                @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function) {

            return new DefaultFunctionalRoutine<BEFORE, AFTER>(function.apply(this),
                                                               DelegationType.SYNC);
        }
    }

    /**
     * Default implementation of a functional routine builder.
     */
    private static class DefaultFunctionalRoutineBuilder
            implements FunctionalRoutineBuilder, Configurable<FunctionalRoutineBuilder> {

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.DEFAULT_CONFIGURATION;

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> from(
                @NotNull final CommandInvocation<OUT> invocation) {

            return fromFactory(invocation, DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> from(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerCommand(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> from(@NotNull final Supplier<OUT> supplier) {

            return fromFactory(supplierCommand(supplier), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenAsyncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenAsyncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(routine, DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenParallelFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(routine, DelegationType.PARALLEL);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenSyncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> thenSyncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(routine, DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        private <IN, OUT> FunctionalRoutine<IN, OUT> fromFactory(
                @NotNull final InvocationFactory<IN, OUT> factory,
                @NotNull final DelegationType delegationType) {

            return new DefaultFunctionalRoutine<IN, OUT>(
                    JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine(),
                    delegationType);
        }

        @NotNull
        public Builder<? extends FunctionalRoutineBuilder> invocations() {

            return new Builder<FunctionalRoutineBuilder>(this, mConfiguration);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public FunctionalRoutineBuilder setConfiguration(
                @NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConfiguration = configuration;
            return this;
        }
    }
}
