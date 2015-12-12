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
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.core.Channels.syncStream;
import static com.github.dm.jrt.core.DelegatingInvocation.factoryFrom;
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

        return routine(InvocationConfiguration.DEFAULT_CONFIGURATION);
    }

    /**
     * Returns a functional routine builder.
     *
     * @param configuration the initial configuration.
     * @return the routine builder instance.
     */
    @NotNull
    public static FunctionalRoutineBuilder routine(
            @NotNull final InvocationConfiguration configuration) {

        return new DefaultFunctionalRoutineBuilder(configuration);
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

        private InvocationConfiguration mConcatConfiguration;

        /**
         * Constructor.
         *
         * @param configuration       the initial configuration.
         * @param concatConfiguration the concatenated routine configuration.
         */
        private AbstractFunctionalRoutine(@NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationConfiguration concatConfiguration) {

            super(configuration);
            mConcatConfiguration = concatConfiguration;
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> asyncAccumulate(
                @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, Void> asyncConsume(
                @NotNull final Consumer<? super OUT> consumer) {

            return fromFactory(new ConsumerInvocation<OUT>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> asyncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<OUT>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> asyncFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return concat(routine, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
                @NotNull final BiConsumer<? super List<? extends OUT>, ? super
                        ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
                @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncThen(
                @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

            return fromFactory(new ConsumerThenInvocation<OUT, AFTER>(consumer),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> asyncThen(
                @NotNull final Supplier<AFTER> supplier) {

            return fromFactory(new SupplierThenInvocation<OUT, AFTER>(supplier),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> flatLift(
                @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends
                        FunctionalRoutine<BEFORE, AFTER>> function) {

            return function.apply(this);
        }

        @NotNull
        public Builder<? extends FunctionalRoutine<IN, OUT>> invocations() {

            return new Builder<FunctionalRoutine<IN, OUT>>(this, mConcatConfiguration);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public FunctionalRoutine<IN, OUT> setConfiguration(
                @NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConcatConfiguration = configuration;
            return this;
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
        protected abstract <AFTER> FunctionalRoutine<IN, AFTER> concat(
                @NotNull Routine<? super OUT, AFTER> routine,
                @NotNull DelegationType delegationType);

        @NotNull
        protected InvocationConfiguration getConcatConfiguration() {

            return mConcatConfiguration;
        }

        @NotNull
        private <AFTER> FunctionalRoutine<IN, AFTER> fromFactory(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory,
                @NotNull final DelegationType delegationType) {

            return concat(JRoutine.on(factory)
                                  .invocations()
                                  .with(mConcatConfiguration)
                                  .set()
                                  .buildRoutine(), delegationType);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> parallelFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return concat(routine, DelegationType.PARALLEL);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> syncAccumulate(
                @NotNull final BiFunction<? super OUT, ? super OUT, ?
                        extends OUT> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, Void> syncConsume(
                @NotNull final Consumer<? super OUT> consumer) {

            return fromFactory(new ConsumerInvocation<OUT>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> syncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<OUT>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public FunctionalRoutine<IN, OUT> syncFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return concat(routine, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
                @NotNull final BiConsumer<? super List<? extends OUT>, ? super
                        ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
                @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncThen(
                @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

            return fromFactory(new ConsumerThenInvocation<OUT, AFTER>(consumer),
                               DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> FunctionalRoutine<IN, AFTER> syncThen(
                @NotNull final Supplier<AFTER> supplier) {

            return fromFactory(new SupplierThenInvocation<OUT, AFTER>(supplier),
                               DelegationType.SYNC);
        }
    }

    /**
     * Invocation implementation wrapping a consumer accepting output data.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerInvocation<OUT> extends FilterInvocation<OUT, Void> {

        private final Consumer<? super OUT> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        @SuppressWarnings("ConstantConditions")
        private ConsumerInvocation(@NotNull final Consumer<? super OUT> consumer) {

            if (consumer == null) {

                throw new NullPointerException("the consumer instance must not be null");
            }

            mConsumer = consumer;
        }

        public void onInput(final OUT input, @NotNull final ResultChannel<Void> result) {

            mConsumer.accept(input);
        }
    }

    /**
     * Invocation implementation wrapping a consumer instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ConsumerThenInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
            implements Invocation<IN, OUT> {

        private final Consumer<? super ResultChannel<OUT>> mConsumer;

        @SuppressWarnings("ConstantConditions")
        private ConsumerThenInvocation(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            if (consumer == null) {

                throw new NullPointerException("the consumer instance must not be null");
            }

            mConsumer = consumer;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return this;
        }

        public void onAbort(@Nullable final RoutineException reason) {

        }

        public void onDestroy() {

        }

        public void onInitialize() {

        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(result);
        }

        public void onTerminate() {

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

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial configuration.
         * @param configuration        the routine configuration.
         * @param routine              the backing routine instance.
         * @param delegationType       the delegation type.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultFunctionalRoutine(
                @NotNull final InvocationConfiguration initialConfiguration,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final Routine<IN, OUT> routine,
                @NotNull final DelegationType delegationType) {

            super(initialConfiguration, configuration);
            mFactory = factoryFrom(routine, delegationType);
        }

        @NotNull
        @Override
        protected <AFTER> FunctionalRoutine<IN, AFTER> concat(
                @NotNull final Routine<? super OUT, AFTER> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterFunctionalRoutine<IN, OUT, AFTER>(getConfiguration(),
                                                              getConcatConfiguration(), this,
                                                              routine, delegationType);
        }

        @NotNull
        @Override
        protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) {

            return mFactory.newInvocation();
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
             * @param initialConfiguration the initial configuration.
             * @param configuration        the routine configuration.
             * @param routine              the backing routine instance.
             * @param afterRoutine         the concatenated routine instance.
             * @param delegationType       the concatenated delegation type.
             */
            @SuppressWarnings("ConstantConditions")
            private AfterFunctionalRoutine(
                    @NotNull final InvocationConfiguration initialConfiguration,
                    @NotNull final InvocationConfiguration configuration,
                    @NotNull final FunctionalRoutine<IN, OUT> routine,
                    @NotNull final Routine<? super OUT, AFTER> afterRoutine,
                    @NotNull final DelegationType delegationType) {

                super(initialConfiguration, configuration);

                if (afterRoutine == null) {

                    throw new NullPointerException("the concatenated routine must not be null");
                }

                mRoutine = routine;
                mAfterRoutine = afterRoutine;
                mDelegationType = delegationType;
            }

            @NotNull
            @Override
            protected <NEXT> FunctionalRoutine<IN, NEXT> concat(
                    @NotNull final Routine<? super AFTER, NEXT> routine,
                    @NotNull final DelegationType delegationType) {

                return new AfterFunctionalRoutine<IN, AFTER, NEXT>(getConfiguration(),
                                                                   getConcatConfiguration(), this,
                                                                   routine, delegationType);
            }

            @NotNull
            public <BEFORE, NEXT> FunctionalRoutine<BEFORE, NEXT> lift(
                    @NotNull final Function<? super FunctionalRoutine<IN, AFTER>, ? extends
                            Routine<BEFORE, NEXT>> function) {

                return new DefaultFunctionalRoutine<BEFORE, NEXT>(getConfiguration(),
                                                                  getConcatConfiguration(),
                                                                  function.apply(this),
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
        public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> lift(
                @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function) {

            return new DefaultFunctionalRoutine<BEFORE, AFTER>(getConfiguration(),
                                                               getConcatConfiguration(),
                                                               function.apply(this),
                                                               DelegationType.SYNC);
        }
    }

    /**
     * Default implementation of a functional routine builder.
     */
    private static class DefaultFunctionalRoutineBuilder
            implements FunctionalRoutineBuilder, Configurable<FunctionalRoutineBuilder> {

        private final InvocationConfiguration mInitialConfiguration;

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.DEFAULT_CONFIGURATION;

        @SuppressWarnings("ConstantConditions")
        private DefaultFunctionalRoutineBuilder(
                @NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mInitialConfiguration = configuration;
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> asyncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, Void> asyncConsume(
                @NotNull final Consumer<? super DATA> consumer) {

            return fromFactory(new ConsumerInvocation<DATA>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> asyncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<DATA>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> asyncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> asyncFrom(
                @NotNull final CommandInvocation<OUT> invocation) {

            return fromFactory(invocation, DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> asyncFrom(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerCommand(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> asyncFrom(@NotNull final Supplier<OUT> supplier) {

            return fromFactory(supplierCommand(supplier), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(mInitialConfiguration, mConfiguration,
                                                         routine, DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> asyncOf(@Nullable final Iterable<OUT> outputs) {

            return asyncFrom(new Consumer<ResultChannel<OUT>>() {

                public void accept(final ResultChannel<OUT> result) {

                    result.pass(outputs);
                }
            });
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> asyncOf(@Nullable final OUT output) {

            return asyncFrom(new Consumer<ResultChannel<OUT>>() {

                public void accept(final ResultChannel<OUT> result) {

                    result.pass(output);
                }
            });
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> asyncOf(@Nullable final OUT... outputs) {

            return asyncFrom(new Consumer<ResultChannel<OUT>>() {

                public void accept(final ResultChannel<OUT> result) {

                    result.pass(outputs);
                }
            });
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> parallelFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(mInitialConfiguration, mConfiguration,
                                                         routine, DelegationType.PARALLEL);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> syncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, Void> syncConsume(
                @NotNull final Consumer<? super DATA> consumer) {

            return fromFactory(new ConsumerInvocation<DATA>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> syncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<DATA>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> FunctionalRoutine<DATA, DATA> syncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> syncFrom(
                @NotNull final CommandInvocation<OUT> invocation) {

            return fromFactory(invocation, DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> syncFrom(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerCommand(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> syncFrom(@NotNull final Supplier<OUT> supplier) {

            return fromFactory(supplierCommand(supplier), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultFunctionalRoutine<IN, OUT>(mInitialConfiguration, mConfiguration,
                                                         routine, DelegationType.SYNC);
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> syncOf(@Nullable final Iterable<OUT> outputs) {

            return syncFrom(new Consumer<ResultChannel<OUT>>() {

                public void accept(final ResultChannel<OUT> result) {

                    result.pass(outputs);
                }
            });
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> syncOf(@Nullable final OUT output) {

            return syncFrom(new Consumer<ResultChannel<OUT>>() {

                public void accept(final ResultChannel<OUT> result) {

                    result.pass(output);
                }
            });
        }

        @NotNull
        public <OUT> FunctionalRoutine<Void, OUT> syncOf(@Nullable final OUT... outputs) {

            return syncFrom(new Consumer<ResultChannel<OUT>>() {

                public void accept(final ResultChannel<OUT> result) {

                    result.pass(outputs);
                }
            });
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        private <IN, OUT> FunctionalRoutine<IN, OUT> fromFactory(
                @NotNull final InvocationFactory<IN, OUT> factory,
                @NotNull final DelegationType delegationType) {

            final Routine<IN, OUT> routine =
                    JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine();
            return new DefaultFunctionalRoutine<IN, OUT>(mInitialConfiguration, mConfiguration,
                                                         routine, delegationType);
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

    /**
     * Invocation implementation wrapping a consumer accepting output data.
     *
     * @param <DATA> the data type.
     */
    private static class ErrorInvocation<DATA> extends InvocationFactory<DATA, DATA>
            implements Invocation<DATA, DATA> {

        private final Consumer<? super RoutineException> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        @SuppressWarnings("ConstantConditions")
        private ErrorInvocation(@NotNull final Consumer<? super RoutineException> consumer) {

            if (consumer == null) {

                throw new NullPointerException("the consumer instance must not be null");
            }

            mConsumer = consumer;
        }

        @NotNull
        @Override
        public Invocation<DATA, DATA> newInvocation() {

            return this;
        }

        public void onAbort(@Nullable final RoutineException reason) {

            mConsumer.accept(reason);
        }

        public void onDestroy() {

        }

        public void onInitialize() {

        }

        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

            result.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<DATA> result) {

        }

        public void onTerminate() {

        }
    }

    /**
     * Invocation implementation wrapping a supplier instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class SupplierThenInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
            implements Invocation<IN, OUT> {

        private final Supplier<OUT> mSupplier;

        @SuppressWarnings("ConstantConditions")
        private SupplierThenInvocation(@NotNull final Supplier<OUT> supplier) {

            if (supplier == null) {

                throw new NullPointerException("the supplier instance must not be null");
            }

            mSupplier = supplier;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return this;
        }

        public void onAbort(@Nullable final RoutineException reason) {

        }

        public void onDestroy() {

        }

        public void onInitialize() {

        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            result.pass(mSupplier.get());
        }

        public void onTerminate() {

        }
    }
}
