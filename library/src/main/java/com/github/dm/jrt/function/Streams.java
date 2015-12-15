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
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.StreamingIOChannel;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.core.Channels.syncIo;
import static com.github.dm.jrt.core.DelegatingInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.supplierCommand;

/**
 * Utility class acting as a factory of stream routine builders.
 * <p/>
 * Created by davide-maestroni on 11/26/2015.
 */
public class Streams {

    /**
     * Avoid direct instantiation.
     */
    protected Streams() {

    }

    /**
     * Returns a stream routine builder.
     *
     * @return the routine builder instance.
     */
    @NotNull
    public static StreamRoutineBuilder streamRoutine() {

        return streamRoutine(InvocationConfiguration.DEFAULT_CONFIGURATION);
    }

    /**
     * Returns a stream routine builder.
     *
     * @param configuration the initial configuration.
     * @return the routine builder instance.
     */
    @NotNull
    public static StreamRoutineBuilder streamRoutine(
            @NotNull final InvocationConfiguration configuration) {

        return new DefaultStreamRoutineBuilder(configuration);
    }

    /**
     * Abstract implementation of a stream routine.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static abstract class AbstractStreamRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
            implements StreamRoutine<IN, OUT>, Configurable<StreamRoutine<IN, OUT>> {

        private InvocationConfiguration mConcatConfiguration;

        /**
         * Constructor.
         *
         * @param configuration       the initial configuration.
         * @param concatConfiguration the concatenated routine configuration.
         */
        private AbstractStreamRoutine(@NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationConfiguration concatConfiguration) {

            super(configuration);
            mConcatConfiguration = concatConfiguration;
        }

        @NotNull
        public StreamRoutine<IN, OUT> asyncAccumulate(
                @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public StreamRoutine<IN, OUT> asyncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<OUT>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public StreamRoutine<IN, OUT> asyncFilter(@NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public StreamRoutine<IN, Void> asyncForEach(@NotNull final Consumer<? super OUT> consumer) {

            return fromFactory(new ConsumerInvocation<OUT>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> asyncLift(
                @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function) {

            return lift(function, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return concat(routine, DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncReduce(
                @NotNull final BiConsumer<? super List<? extends OUT>, ? super
                        ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncReduce(
                @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncThen(
                @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

            return fromFactory(new ConsumerThenInvocation<OUT, AFTER>(consumer),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> asyncThen(@NotNull final Supplier<AFTER> supplier) {

            return fromFactory(new SupplierThenInvocation<OUT, AFTER>(supplier),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> flatLift(
                @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends
                        StreamRoutine<BEFORE, AFTER>> function) {

            return function.apply(this);
        }

        @NotNull
        public StreamRoutine<IN, OUT> parallelFilter(
                @NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }

        @NotNull
        public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> parallelLift(
                @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function) {

            return lift(function, DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> parallelMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> parallelMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> parallelMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> parallelMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return concat(routine, DelegationType.PARALLEL);
        }

        @NotNull
        public StreamRoutine<IN, OUT> syncAccumulate(
                @NotNull final BiFunction<? super OUT, ? super OUT, ?
                        extends OUT> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public StreamRoutine<IN, OUT> syncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<OUT>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public StreamRoutine<IN, OUT> syncFilter(@NotNull final Predicate<? super OUT> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public StreamRoutine<IN, Void> syncForEach(@NotNull final Consumer<? super OUT> consumer) {

            return fromFactory(new ConsumerInvocation<OUT>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> syncLift(
                @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function) {

            return lift(function, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncMap(
                @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncMap(
                @NotNull final Function<? super OUT, AFTER> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncMap(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncMap(
                @NotNull final Routine<? super OUT, AFTER> routine) {

            return concat(routine, DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncReduce(
                @NotNull final BiConsumer<? super List<? extends OUT>, ? super
                        ResultChannel<AFTER>> consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncReduce(
                @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncThen(
                @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

            return fromFactory(new ConsumerThenInvocation<OUT, AFTER>(consumer),
                               DelegationType.SYNC);
        }

        @NotNull
        public <AFTER> StreamRoutine<IN, AFTER> syncThen(@NotNull final Supplier<AFTER> supplier) {

            return fromFactory(new SupplierThenInvocation<OUT, AFTER>(supplier),
                               DelegationType.SYNC);
        }

        @NotNull
        public Builder<? extends StreamRoutine<IN, OUT>> invocations() {

            return new Builder<StreamRoutine<IN, OUT>>(this, mConcatConfiguration);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public StreamRoutine<IN, OUT> setConfiguration(
                @NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConcatConfiguration = configuration;
            return this;
        }

        /**
         * Concatenates a stream routine based on the specified instance to this one.
         *
         * @param routine        the routine instance.
         * @param delegationType the delegation type.
         * @param <AFTER>        the concatenation output type.
         * @return the concatenated stream routine.
         */
        @NotNull
        protected abstract <AFTER> StreamRoutine<IN, AFTER> concat(
                @NotNull Routine<? super OUT, AFTER> routine,
                @NotNull DelegationType delegationType);

        @NotNull
        protected InvocationConfiguration getConcatConfiguration() {

            return mConcatConfiguration;
        }

        @NotNull
        protected abstract <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> lift(
                @NotNull Function<? super StreamRoutine<IN, OUT>, ? extends Routine<BEFORE,
                        AFTER>> function,
                @NotNull DelegationType delegationType);

        @NotNull
        private <AFTER> StreamRoutine<IN, AFTER> fromFactory(
                @NotNull final InvocationFactory<? super OUT, AFTER> factory,
                @NotNull final DelegationType delegationType) {

            return concat(JRoutine.on(factory)
                                  .invocations()
                                  .with(mConcatConfiguration)
                                  .set()
                                  .buildRoutine(), delegationType);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public StreamRoutine<IN, OUT> tryCatch(
                @NotNull final Consumer<? super RoutineException> consumer) {

            if (consumer == null) {

                throw new NullPointerException("the consumer instance must not be null");
            }

            return tryCatch(new TryCatchBiConsumerConsumer<OUT>(consumer));
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public StreamRoutine<IN, OUT> tryCatch(
                @NotNull final Function<? super RoutineException, ? extends OUT> function) {

            if (function == null) {

                throw new NullPointerException("the function instance must not be null");
            }

            return tryCatch(new TryCatchBiConsumerFunction<OUT>(function));
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

        private final StreamRoutine<IN, OUT> mRoutine;

        private StreamingIOChannel<IN, OUT> mInputChannel;

        private OutputChannel<AFTER> mOutputChannel;

        /**
         * Constructor.
         *
         * @param routine        the backing routine instance.
         * @param afterRoutine   the concatenated routine instance.
         * @param delegationType the concatenated delegation type.
         */
        private AfterInvocation(@NotNull final StreamRoutine<IN, OUT> routine,
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

            final StreamingIOChannel<IN, OUT> streamingChannel = syncIo(mRoutine);
            final DelegationType delegationType = mDelegationType;
            mInputChannel = streamingChannel;

            if (delegationType == DelegationType.ASYNC) {

                mOutputChannel = streamingChannel.passTo(mAfterRoutine.asyncInvoke()).result();

            } else if (delegationType == DelegationType.PARALLEL) {

                mOutputChannel = streamingChannel.passTo(mAfterRoutine.parallelInvoke()).result();

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

    /**
     * Stream routine implementation concatenating two different routines.
     *
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @param <AFTER> the concatenation output type.
     */
    private static class AfterStreamRoutine<IN, OUT, AFTER>
            extends AbstractStreamRoutine<IN, AFTER> {

        private final Routine<? super OUT, AFTER> mAfterRoutine;

        private final DelegationType mDelegationType;

        private final StreamRoutine<IN, OUT> mRoutine;

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
        private AfterStreamRoutine(@NotNull final InvocationConfiguration initialConfiguration,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final StreamRoutine<IN, OUT> routine,
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
        protected <NEXT> StreamRoutine<IN, NEXT> concat(
                @NotNull final Routine<? super AFTER, NEXT> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterStreamRoutine<IN, AFTER, NEXT>(getConfiguration(),
                                                           getConcatConfiguration(), this, routine,
                                                           delegationType);
        }

        @NotNull
        @Override
        protected Invocation<IN, AFTER> newInvocation(@NotNull final InvocationType type) {

            return new AfterInvocation<IN, OUT, AFTER>(mRoutine, mAfterRoutine, mDelegationType);
        }

        @NotNull
        public StreamRoutine<IN, AFTER> tryCatch(
                @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<AFTER>>
                        consumer) {

            return new TryCatchStreamRoutine<IN, AFTER>(getConfiguration(),
                                                        getConcatConfiguration(), this, consumer);
        }

        @NotNull
        protected <BEFORE, NEXT> StreamRoutine<BEFORE, NEXT> lift(
                @NotNull final Function<? super StreamRoutine<IN, AFTER>, ? extends
                        Routine<BEFORE, NEXT>> function,
                @NotNull final DelegationType delegationType) {

            return new DefaultStreamRoutine<BEFORE, NEXT>(getConfiguration(),
                                                          getConcatConfiguration(),
                                                          function.apply(this), delegationType);
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
     * Consumer implementation passing an output to the result channel.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerOutput<OUT> implements Consumer<ResultChannel<OUT>> {

        private final OUT mOutput;

        /**
         * Constructor.
         *
         * @param output the output.
         */
        private ConsumerOutput(@Nullable final OUT output) {

            mOutput = output;
        }

        public void accept(final ResultChannel<OUT> result) {

            result.pass(mOutput);
        }
    }

    /**
     * Consumer implementation passing a list of outputs to the result channel.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerOutputs<OUT> implements Consumer<ResultChannel<OUT>> {

        private final ArrayList<OUT> mOutputs;

        /**
         * Constructor.
         *
         * @param outputs the list of outputs.
         */
        private ConsumerOutputs(@Nullable final ArrayList<OUT> outputs) {

            mOutputs = outputs;
        }

        public void accept(final ResultChannel<OUT> result) {

            result.pass(mOutputs);
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

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
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
     * Default implementation of a stream routine.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultStreamRoutine<IN, OUT> extends AbstractStreamRoutine<IN, OUT> {

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
        private DefaultStreamRoutine(@NotNull final InvocationConfiguration initialConfiguration,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final Routine<IN, OUT> routine,
                @NotNull final DelegationType delegationType) {

            super(initialConfiguration, configuration);
            mFactory = factoryFrom(routine, delegationType);
        }

        @NotNull
        public StreamRoutine<IN, OUT> tryCatch(
                @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                        consumer) {

            return new TryCatchStreamRoutine<IN, OUT>(getConfiguration(), getConcatConfiguration(),
                                                      this, consumer);
        }

        @NotNull
        @Override
        protected <AFTER> StreamRoutine<IN, AFTER> concat(
                @NotNull final Routine<? super OUT, AFTER> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterStreamRoutine<IN, OUT, AFTER>(getConfiguration(),
                                                          getConcatConfiguration(), this, routine,
                                                          delegationType);
        }

        @NotNull
        @Override
        protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) {

            return mFactory.newInvocation();
        }

        @NotNull
        protected <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> lift(
                @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function,
                @NotNull final DelegationType delegationType) {

            return new DefaultStreamRoutine<BEFORE, AFTER>(getConfiguration(),
                                                           getConcatConfiguration(),
                                                           function.apply(this), delegationType);
        }
    }

    /**
     * Default implementation of a stream routine builder.
     */
    private static class DefaultStreamRoutineBuilder
            implements StreamRoutineBuilder, Configurable<StreamRoutineBuilder> {

        private final InvocationConfiguration mInitialConfiguration;

        private InvocationConfiguration mConfiguration =
                InvocationConfiguration.DEFAULT_CONFIGURATION;

        /**
         * Constructor.
         *
         * @param configuration the initial configuration.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultStreamRoutineBuilder(@NotNull final InvocationConfiguration configuration) {

            if (configuration == null) {

                throw new NullPointerException("the invocation configuration must not be null");
            }

            mInitialConfiguration = configuration;
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> asyncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function),
                               DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> asyncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<DATA>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> asyncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, Void> asyncForEach(
                @NotNull final Consumer<? super DATA> consumer) {

            return fromFactory(new ConsumerInvocation<DATA>(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> asyncFrom(
                @NotNull final CommandInvocation<OUT> invocation) {

            return fromFactory(invocation, DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> asyncFrom(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerCommand(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> asyncFrom(@NotNull final Supplier<OUT> supplier) {

            return fromFactory(supplierCommand(supplier), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> asyncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> asyncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> asyncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> asyncMap(@NotNull final Routine<IN, OUT> routine) {

            return new DefaultStreamRoutine<IN, OUT>(mInitialConfiguration, mConfiguration, routine,
                                                     DelegationType.ASYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> asyncOf(@Nullable final Iterable<OUT> outputs) {

            if (outputs != null) {

                final ArrayList<OUT> outputList = new ArrayList<OUT>();

                for (final OUT output : outputs) {

                    outputList.add(output);
                }

                return asyncFrom(new ConsumerOutputs<OUT>(outputList));
            }

            return asyncFrom(ConsumerWrapper.sink());
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> asyncOf(@Nullable final OUT output) {

            return asyncFrom(new ConsumerOutput<OUT>(output));
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> asyncOf(@Nullable final OUT... outputs) {

            if (outputs != null) {

                final ArrayList<OUT> outputList = new ArrayList<OUT>();
                Collections.addAll(outputList, outputs);
                return asyncFrom(new ConsumerOutputs<OUT>(outputList));
            }

            return asyncFrom(ConsumerWrapper.sink());
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> asyncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> asyncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.ASYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> parallelFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> parallelMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> parallelMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> parallelMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.PARALLEL);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> parallelMap(
                @NotNull final Routine<IN, OUT> routine) {

            return new DefaultStreamRoutine<IN, OUT>(mInitialConfiguration, mConfiguration, routine,
                                                     DelegationType.PARALLEL);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> syncAccumulate(
                @NotNull final BiFunction<? super DATA, ? super DATA, DATA> function) {

            return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> syncError(
                @NotNull final Consumer<? super RoutineException> consumer) {

            return fromFactory(new ErrorInvocation<DATA>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, DATA> syncFilter(
                @NotNull final Predicate<? super DATA> predicate) {

            return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
        }

        @NotNull
        public <DATA> StreamRoutine<DATA, Void> syncForEach(
                @NotNull final Consumer<? super DATA> consumer) {

            return fromFactory(new ConsumerInvocation<DATA>(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> syncFrom(
                @NotNull final CommandInvocation<OUT> invocation) {

            return fromFactory(invocation, DelegationType.SYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> syncFrom(
                @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerCommand(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> syncFrom(@NotNull final Supplier<OUT> supplier) {

            return fromFactory(supplierCommand(supplier), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> syncMap(
                @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

            return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> syncMap(
                @NotNull final Function<? super IN, OUT> function) {

            return fromFactory(functionFilter(function), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> syncMap(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            return fromFactory(factory, DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> syncMap(@NotNull final Routine<IN, OUT> routine) {

            return new DefaultStreamRoutine<IN, OUT>(mInitialConfiguration, mConfiguration, routine,
                                                     DelegationType.SYNC);
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> syncOf(@Nullable final Iterable<OUT> outputs) {

            if (outputs != null) {

                final ArrayList<OUT> outputList = new ArrayList<OUT>();

                for (final OUT output : outputs) {

                    outputList.add(output);
                }

                return syncFrom(new ConsumerOutputs<OUT>(outputList));
            }

            return syncFrom(ConsumerWrapper.sink());
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> syncOf(@Nullable final OUT output) {

            return syncFrom(new ConsumerOutput<OUT>(output));
        }

        @NotNull
        public <OUT> StreamRoutine<Void, OUT> syncOf(@Nullable final OUT... outputs) {

            if (outputs != null) {

                final ArrayList<OUT> outputList = new ArrayList<OUT>();
                Collections.addAll(outputList, outputs);
                return syncFrom(new ConsumerOutputs<OUT>(outputList));
            }

            return syncFrom(ConsumerWrapper.sink());
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> syncReduce(
                @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                        consumer) {

            return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
        }

        @NotNull
        public <IN, OUT> StreamRoutine<IN, OUT> syncReduce(
                @NotNull final Function<? super List<? extends IN>, OUT> function) {

            return fromFactory(functionFactory(function), DelegationType.SYNC);
        }

        @NotNull
        private <IN, OUT> StreamRoutine<IN, OUT> fromFactory(
                @NotNull final InvocationFactory<IN, OUT> factory,
                @NotNull final DelegationType delegationType) {

            final Routine<IN, OUT> routine =
                    JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine();
            return new DefaultStreamRoutine<IN, OUT>(mInitialConfiguration, mConfiguration, routine,
                                                     delegationType);
        }

        @NotNull
        public Builder<? extends StreamRoutineBuilder> invocations() {

            return new Builder<StreamRoutineBuilder>(this, mConfiguration);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public StreamRoutineBuilder setConfiguration(
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

        /**
         * Constructor.
         *
         * @param supplier the supplier instance.
         */
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

    /**
     * Bi-consumer implementation wrapping a try/catch consumer.
     *
     * @param <OUT> the output data type.
     */
    private static class TryCatchBiConsumerConsumer<OUT>
            implements BiConsumer<RoutineException, InputChannel<OUT>> {

        private final Consumer<? super RoutineException> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private TryCatchBiConsumerConsumer(
                @NotNull final Consumer<? super RoutineException> consumer) {

            mConsumer = consumer;
        }

        public void accept(final RoutineException error, final InputChannel<OUT> channel) {

            mConsumer.accept(error);
        }
    }

    /**
     * Bi-consumer implementation wrapping a try/catch function.
     *
     * @param <OUT> the output data type.
     */
    private static class TryCatchBiConsumerFunction<OUT>
            implements BiConsumer<RoutineException, InputChannel<OUT>> {

        private final Function<? super RoutineException, ? extends OUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private TryCatchBiConsumerFunction(
                @NotNull final Function<? super RoutineException, ? extends OUT> function) {

            mFunction = function;
        }

        public void accept(final RoutineException error, final InputChannel<OUT> channel) {

            channel.pass(mFunction.apply(error));
        }
    }

    /**
     * Invocation implementation binding a try/catch output consumer.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class TryCatchInvocation<IN, OUT>
            implements Invocation<IN, OUT>, OutputConsumer<OUT> {

        private final BiConsumer<? super RoutineException, ? super InputChannel<OUT>> mConsumer;

        private final Routine<IN, OUT> mRoutine;

        private StreamingIOChannel<IN, OUT> mInputChannel;

        private IOChannel<OUT, OUT> mOutputChannel;

        /**
         * Constructor.
         *
         * @param routine  the routine instance.
         * @param consumer the consumer instance.
         */
        private TryCatchInvocation(@NotNull final Routine<IN, OUT> routine,
                @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                        consumer) {

            mRoutine = routine;
            mConsumer = consumer;
        }

        public void onComplete() {

            mOutputChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            final IOChannel<OUT, OUT> channel = mOutputChannel;

            try {

                mConsumer.accept(error, channel);
                channel.close();

            } catch (final Throwable t) {

                channel.abort(t);
            }
        }

        public void onOutput(final OUT output) {

            mOutputChannel.pass(output);
        }

        public void onAbort(@Nullable final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

            mInputChannel = null;
            mOutputChannel = null;
        }

        public void onInitialize() {

            final StreamingIOChannel<IN, OUT> streamingChannel = syncIo(mRoutine);
            streamingChannel.passTo(this);
            mInputChannel = streamingChannel;
            mOutputChannel = JRoutine.io().buildChannel();
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            final IOChannel<OUT, OUT> channel = mOutputChannel;

            if (!channel.isBound()) {

                channel.passTo(result);
            }

            mInputChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            final IOChannel<OUT, OUT> channel = mOutputChannel;

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

    /**
     * Stream routine implementation binding a try/catch output consumer.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class TryCatchStreamRoutine<IN, OUT> extends AbstractStreamRoutine<IN, OUT> {

        private final BiConsumer<? super RoutineException, ? super InputChannel<OUT>> mConsumer;

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param initialConfiguration the initial configuration.
         * @param configuration        the routine configuration.
         * @param routine              the backing routine instance.
         * @param consumer             the consumer instance.
         */
        @SuppressWarnings("ConstantConditions")
        private TryCatchStreamRoutine(@NotNull final InvocationConfiguration initialConfiguration,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final Routine<IN, OUT> routine,
                @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                        consumer) {

            super(initialConfiguration, configuration);

            if (consumer == null) {

                throw new NullPointerException("the consumer instance must not be null");
            }

            mRoutine = routine;
            mConsumer = consumer;
        }

        @NotNull
        public StreamRoutine<IN, OUT> tryCatch(
                @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                        consumer) {

            return new TryCatchStreamRoutine<IN, OUT>(getConfiguration(), getConcatConfiguration(),
                                                      this, consumer);
        }

        @NotNull
        @Override
        protected <AFTER> StreamRoutine<IN, AFTER> concat(
                @NotNull final Routine<? super OUT, AFTER> routine,
                @NotNull final DelegationType delegationType) {

            return new AfterStreamRoutine<IN, OUT, AFTER>(getConfiguration(),
                                                          getConcatConfiguration(), this, routine,
                                                          delegationType);
        }

        @NotNull
        @Override
        protected Invocation<IN, OUT> newInvocation(@NotNull final InvocationType type) {

            return new TryCatchInvocation<IN, OUT>(mRoutine, mConsumer);
        }

        @NotNull
        protected <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> lift(
                @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends
                        Routine<BEFORE, AFTER>> function,
                @NotNull final DelegationType delegationType) {

            return new DefaultStreamRoutine<BEFORE, AFTER>(getConfiguration(),
                                                           getConcatConfiguration(),
                                                           function.apply(this), delegationType);
        }
    }
}
