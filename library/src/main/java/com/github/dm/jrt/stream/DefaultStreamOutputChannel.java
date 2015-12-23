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
package com.github.dm.jrt.stream;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.predicateFilter;

/**
 * Created by davide-maestroni on 12/23/2015.
 */
class DefaultStreamOutputChannel<OUT>
        implements StreamOutputChannel<OUT>, Configurable<StreamOutputChannel<OUT>> {

    private final OutputChannel<OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    DefaultStreamOutputChannel(@NotNull final OutputChannel<OUT> channel) {

        mChannel = channel;
    }

    private DefaultStreamOutputChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final OutputChannel<OUT> channel) {

        this(channel);
        mConfiguration = configuration;
    }

    public boolean abort() {

        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return mChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mChannel.isEmpty();
    }

    public boolean isOpen() {

        return mChannel.isOpen();
    }

    @NotNull
    public OutputChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        return mChannel.afterMax(timeout);
    }

    @NotNull
    public OutputChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        return mChannel.afterMax(timeout, timeUnit);
    }

    @NotNull
    public List<OUT> all() {

        return mChannel.all();
    }

    @NotNull
    public OutputChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        return mChannel.allInto(results);
    }

    public boolean checkComplete() {

        return mChannel.checkComplete();
    }

    @NotNull
    public OutputChannel<OUT> eventuallyAbort() {

        return mChannel.eventuallyAbort();
    }

    @NotNull
    public OutputChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        return mChannel.eventuallyAbort(reason);
    }

    @NotNull
    public OutputChannel<OUT> eventuallyExit() {

        return mChannel.eventuallyExit();
    }

    @NotNull
    public OutputChannel<OUT> eventuallyThrow() {

        return mChannel.eventuallyThrow();
    }

    public boolean hasNext() {

        return mChannel.hasNext();
    }

    public OUT next() {

        return mChannel.next();
    }

    @NotNull
    public OutputChannel<OUT> immediately() {

        return mChannel.immediately();
    }

    public boolean isBound() {

        return mChannel.isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        return mChannel.next(count);
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL passTo(
            @NotNull final CHANNEL channel) {

        return mChannel.passTo(channel);
    }

    @NotNull
    public OutputChannel<OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        return mChannel.passTo(consumer);
    }

    @NotNull
    public OutputChannel<OUT> skip(final int count) {

        return mChannel.skip(count);
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return asyncMap(consumerFactory(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncCollect(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return asyncMap(functionFactory(function));
    }

    @NotNull
    public StreamOutputChannel<OUT> asyncFilter(@NotNull final Predicate<? super OUT> predicate) {

        return asyncMap(predicateFilter(predicate));
    }

    @NotNull
    public StreamOutputChannel<Void> asyncForEach(@NotNull final Consumer<? super OUT> consumer) {

        return asyncMap(new ConsumerInvocation<OUT>(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return asyncMap(new GenerateConsumerInvocation<OUT, AFTER>(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncGenerate(final long count,
            @NotNull final Supplier<AFTER> supplier) {

        return asyncMap(new GenerateSupplierInvocation<OUT, AFTER>(count, supplier));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncGenerate(
            @NotNull final Supplier<AFTER> supplier) {

        return asyncGenerate(1, supplier);
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<AFTER>> function) {

        return asyncMap(new LiftConsumer<OUT, AFTER>(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return asyncMap(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return asyncMap(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

        return asyncMap(
                JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return concat(routine.asyncInvoke());
    }

    @NotNull
    public StreamOutputChannel<OUT> asyncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return asyncMap(AccumulateInvocation.functionFactory(function));
    }

    @NotNull
    public StreamOutputChannel<OUT> parallelFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return parallelMap(predicateFilter(predicate));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelGenerate(final long count,
            @NotNull final Supplier<AFTER> supplier) {

        return parallelMap(new GenerateSupplierInvocation<OUT, AFTER>(count, supplier));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<AFTER>> function) {

        return parallelMap(new LiftConsumer<OUT, AFTER>(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return parallelMap(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return parallelMap(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

        return parallelMap(
                JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return concat(routine.parallelInvoke());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return syncMap(consumerFactory(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncCollect(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return syncMap(functionFactory(function));
    }

    @NotNull
    public StreamOutputChannel<OUT> syncFilter(@NotNull final Predicate<? super OUT> predicate) {

        return syncMap(predicateFilter(predicate));
    }

    @NotNull
    public StreamOutputChannel<Void> syncForEach(@NotNull final Consumer<? super OUT> consumer) {

        return syncMap(new ConsumerInvocation<OUT>(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return syncMap(new GenerateConsumerInvocation<OUT, AFTER>(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncGenerate(final long count,
            @NotNull final Supplier<AFTER> supplier) {

        return syncMap(new GenerateSupplierInvocation<OUT, AFTER>(count, supplier));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncGenerate(
            @NotNull final Supplier<AFTER> supplier) {

        return syncGenerate(1, supplier);
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<AFTER>> function) {

        return syncMap(new LiftConsumer<OUT, AFTER>(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return syncMap(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return syncMap(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final InvocationFactory<? super OUT, AFTER> factory) {

        return syncMap(
                JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return concat(routine.syncInvoke());
    }

    @NotNull
    public StreamOutputChannel<OUT> syncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return syncMap(AccumulateInvocation.functionFactory(function));
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamOutputChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
        mChannel.passTo(new TryCatchOutputConsumer<OUT>(consumer, ioChannel));
        return new DefaultStreamOutputChannel<OUT>(mConfiguration, ioChannel);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamOutputChannel<OUT> tryCatch(
            @NotNull final Consumer<? super RoutineException> consumer) {

        if (consumer == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        return tryCatch(new TryCatchBiConsumerConsumer<OUT>(consumer));
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamOutputChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        if (function == null) {

            throw new NullPointerException("the function instance must not be null");
        }

        return tryCatch(new TryCatchBiConsumerFunction<OUT>(function));
    }

    @NotNull
    public Builder<? extends StreamOutputChannel<OUT>> invocations() {

        return new Builder<StreamOutputChannel<OUT>>(this, mConfiguration);
    }

    public Iterator<OUT> iterator() {

        return mChannel.iterator();
    }

    public void remove() {

        mChannel.remove();
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamOutputChannel<OUT> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }

    @NotNull
    private <AFTER> StreamOutputChannel<AFTER> concat(
            @NotNull final InvocationChannel<? super OUT, AFTER> channel) {

        return new DefaultStreamOutputChannel<AFTER>(mConfiguration,
                                                     mChannel.passTo(channel).result());
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
    private static class GenerateConsumerInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
            implements Invocation<IN, OUT> {

        private final Consumer<? super ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        @SuppressWarnings("ConstantConditions")
        private GenerateConsumerInvocation(
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
     * Invocation implementation wrapping a supplier instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class GenerateSupplierInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
            implements Invocation<IN, OUT> {

        private final long mCount;

        private final Supplier<OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param count    the number of generated outputs.
         * @param supplier the supplier instance.
         * @throws java.lang.IllegalArgumentException if the specified count number is 0 or
         *                                            negative.
         */
        @SuppressWarnings("ConstantConditions")
        private GenerateSupplierInvocation(final long count,
                @NotNull final Supplier<OUT> supplier) {

            if (count <= 0) {

                throw new IllegalArgumentException("the count number must be positive: " + count);
            }

            if (supplier == null) {

                throw new NullPointerException("the supplier instance must not be null");
            }

            mCount = count;
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

            for (int i = 0; i < mCount; ++i) {

                result.pass(mSupplier.get());
            }
        }

        public void onTerminate() {

        }
    }

    // TODO: 12/23/15 javadoc
    private static class LiftConsumer<OUT, AFTER> implements BiConsumer<OUT, ResultChannel<AFTER>> {

        private final Function<? super OUT, ? extends OutputChannel<AFTER>> mFunction;

        @SuppressWarnings("ConstantConditions")
        private LiftConsumer(
                @NotNull final Function<? super OUT, ? extends OutputChannel<AFTER>> function) {

            if (function == null) {

                throw new NullPointerException("the function instance must not be null");
            }

            mFunction = function;
        }

        public void accept(final OUT out, final ResultChannel<AFTER> result) {

            final OutputChannel<AFTER> channel = mFunction.apply(out);

            if (channel != null) {

                channel.passTo(result);
            }
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
     * Try/catch output consumer implementation.
     *
     * @param <OUT> the output data type.
     */
    private static class TryCatchOutputConsumer<OUT> implements OutputConsumer<OUT> {

        private final BiConsumer<? super RoutineException, ? super InputChannel<OUT>> mConsumer;

        private final IOChannel<OUT> mOutputChannel;

        /**
         * Constructor.
         *
         * @param consumer      the consumer instance.
         * @param outputChannel the output channel.
         */
        private TryCatchOutputConsumer(
                @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                        consumer,
                @NotNull final IOChannel<OUT> outputChannel) {

            mConsumer = consumer;
            mOutputChannel = outputChannel;
        }

        public void onComplete() {

            mOutputChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            final IOChannel<OUT> channel = mOutputChannel;

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
    }
}
