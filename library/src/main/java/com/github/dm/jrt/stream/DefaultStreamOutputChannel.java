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
import com.github.dm.jrt.function.Functions;
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
 * Default implementation of a stream output channel.
 * <p/>
 * Created by davide-maestroni on 12/23/2015.
 *
 * @param <OUT> the output data type.
 */
class DefaultStreamOutputChannel<OUT>
        implements StreamOutputChannel<OUT>, Configurable<StreamOutputChannel<OUT>> {

    private final OutputChannel<OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultStreamOutputChannel(@NotNull final OutputChannel<OUT> channel) {

        if (channel == null) {

            throw new NullPointerException("the output channel instance must not be null");
        }

        mChannel = channel;
    }

    /**
     * Constructor.
     *
     * @param configuration the initial invocation configuration.
     * @param channel       the wrapped output channel.
     */
    private DefaultStreamOutputChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final OutputChannel<OUT> channel) {

        this(channel);
        mConfiguration = configuration;
    }

    @NotNull
    private static <IN> RangeInvocation<IN, ? extends Number> range(@NotNull final Number start,
            @NotNull final Number end) {

        if ((start instanceof Double) || (end instanceof Double)) {

            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            return range(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Float) || (end instanceof Float)) {

            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            return range(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Long) || (end instanceof Long)) {

            final long startValue = start.longValue();
            final long endValue = end.longValue();
            return range(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Integer) || (end instanceof Integer)) {

            final int startValue = start.intValue();
            final int endValue = end.intValue();
            return range(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Short) || (end instanceof Short)) {

            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            return range(start, end, (short) ((startValue <= endValue) ? 1 : -1));

        } else if ((start instanceof Byte) || (end instanceof Byte)) {

            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            return range(start, end, (byte) ((startValue <= endValue) ? 1 : -1));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + "]");
    }

    @NotNull
    private static <IN> RangeInvocation<IN, ? extends Number> range(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        if ((start instanceof Double) || (end instanceof Double) || (increment instanceof Double)) {

            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            final double incValue = increment.doubleValue();
            final Function<Double, Double> function = new Function<Double, Double>() {

                public Double apply(final Double aDouble) {

                    return aDouble + incValue;
                }
            };

            return new RangeInvocation<IN, Double>(startValue, endValue, function);

        } else if ((start instanceof Float) || (end instanceof Float)
                || (increment instanceof Float)) {

            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            final float incValue = increment.floatValue();
            final Function<Float, Float> function = new Function<Float, Float>() {

                public Float apply(final Float aFloat) {

                    return aFloat + incValue;
                }
            };

            return new RangeInvocation<IN, Float>(startValue, endValue, function);

        } else if ((start instanceof Long) || (end instanceof Long)
                || (increment instanceof Long)) {

            final long startValue = start.longValue();
            final long endValue = end.longValue();
            final long incValue = increment.longValue();
            final Function<Long, Long> function = new Function<Long, Long>() {

                public Long apply(final Long aLong) {

                    return aLong + incValue;
                }
            };

            return new RangeInvocation<IN, Long>(startValue, endValue, function);

        } else if ((start instanceof Integer) || (end instanceof Integer)
                || (increment instanceof Integer)) {

            final int startValue = start.intValue();
            final int endValue = end.intValue();
            final int incValue = increment.intValue();
            final Function<Integer, Integer> function = new Function<Integer, Integer>() {

                public Integer apply(final Integer anInteger) {

                    return anInteger + incValue;
                }
            };

            return new RangeInvocation<IN, Integer>(startValue, endValue, function);

        } else if ((start instanceof Short) || (end instanceof Short)
                || (increment instanceof Short)) {

            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            final short incValue = increment.shortValue();
            final Function<Short, Short> function = new Function<Short, Short>() {

                public Short apply(final Short aShort) {

                    return (short) (aShort + incValue);
                }
            };

            return new RangeInvocation<IN, Short>(startValue, endValue, function);

        } else if ((start instanceof Byte) || (end instanceof Byte)
                || (increment instanceof Byte)) {

            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            final byte incValue = increment.byteValue();
            final Function<Byte, Byte> function = new Function<Byte, Byte>() {

                public Byte apply(final Byte aByte) {

                    return (byte) (aByte + incValue);
                }
            };

            return new RangeInvocation<IN, Byte>(startValue, endValue, function);
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + ", " + increment.getClass()
                                                                              .getCanonicalName()
                        + "]");
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
    public StreamOutputChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        mChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        mChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> eventuallyAbort() {

        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        mChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> eventuallyExit() {

        mChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> eventuallyThrow() {

        mChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> immediately() {

        mChannel.immediately();
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        mChannel.passTo(consumer);
        return this;
    }

    @NotNull
    public StreamOutputChannel<OUT> skip(final int count) {

        mChannel.skip(count);
        return this;
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return asyncMap(consumerFactory(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

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
            @NotNull final Supplier<? extends AFTER> supplier) {

        return asyncMap(new GenerateSupplierInvocation<OUT, AFTER>(count, supplier));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return asyncGenerate(1, supplier);
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return asyncMap(new LiftInvocation<OUT, AFTER>(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return asyncMap(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return asyncMap(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return asyncMap(
                JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> asyncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return concat(routine.asyncInvoke());
    }

    @NotNull
    public <AFTER extends Comparable<AFTER>> StreamOutputChannel<AFTER> asyncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return asyncMap(new RangeInvocation<OUT, AFTER>(start, end, increment));
    }

    @NotNull
    public StreamOutputChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return this.<Number>asyncMap(range(start, end));
    }

    @NotNull
    public StreamOutputChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return this.<Number>asyncMap(range(start, end, increment));
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
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        if (count <= 0) {

            throw new IllegalArgumentException("the count number must be positive: " + count);
        }

        return asyncRange(1, count).parallelMap(
                new GenerateConsumerInvocation<Number, AFTER>(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        if (count <= 0) {

            throw new IllegalArgumentException("the count number must be positive: " + count);
        }

        return asyncRange(1, count).parallelMap(
                new GenerateSupplierInvocation<Number, AFTER>(1, supplier));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return parallelMap(new LiftInvocation<OUT, AFTER>(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return parallelMap(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return parallelMap(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return parallelMap(
                JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> parallelMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return concat(routine.parallelInvoke());
    }

    @NotNull
    public <AFTER extends Comparable<AFTER>> StreamOutputChannel<AFTER> parallelRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return asyncRange(start, end, increment).parallelMap(Functions.<AFTER>identity());
    }

    @NotNull
    public StreamOutputChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end) {

        return asyncRange(start, end).parallelMap(Functions.<Number>identity());
    }

    @NotNull
    public StreamOutputChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return asyncRange(start, end, increment).parallelMap(Functions.<Number>identity());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return syncMap(consumerFactory(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

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
            @NotNull final Supplier<? extends AFTER> supplier) {

        return syncMap(new GenerateSupplierInvocation<OUT, AFTER>(count, supplier));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return syncGenerate(1, supplier);
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return syncMap(new LiftInvocation<OUT, AFTER>(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return syncMap(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return syncMap(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return syncMap(
                JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> StreamOutputChannel<AFTER> syncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return concat(routine.syncInvoke());
    }

    @NotNull
    public <AFTER extends Comparable<AFTER>> StreamOutputChannel<AFTER> syncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return syncMap(new RangeInvocation<OUT, AFTER>(start, end, increment));
    }

    @NotNull
    public StreamOutputChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return this.<Number>syncMap(range(start, end));
    }

    @NotNull
    public StreamOutputChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return this.<Number>syncMap(range(start, end, increment));
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
    public List<OUT> all() {

        return mChannel.all();
    }

    public boolean checkComplete() {

        return mChannel.checkComplete();
    }

    public boolean hasNext() {

        return mChannel.hasNext();
    }

    public OUT next() {

        return mChannel.next();
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
    @SuppressWarnings("unchecked")
    private <AFTER> StreamOutputChannel<AFTER> concat(
            @NotNull final InvocationChannel<? super OUT, ? extends AFTER> channel) {

        return new DefaultStreamOutputChannel<AFTER>(mConfiguration, (OutputChannel<AFTER>) mChannel
                .passTo(channel)
                .result());
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

        private final Supplier<? extends OUT> mSupplier;

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
                @NotNull final Supplier<? extends OUT> supplier) {

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

    /**
     * Filter invocation implementation wrapping a lifting function.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class LiftInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final Function<? super IN, ? extends OutputChannel<? extends OUT>> mFunction;

        /**
         * Constructor.
         *
         * @param function the lifting function.
         */
        @SuppressWarnings("ConstantConditions")
        private LiftInvocation(
                @NotNull final Function<? super IN, ? extends OutputChannel<? extends OUT>>
                        function) {

            if (function == null) {

                throw new NullPointerException("the function instance must not be null");
            }

            mFunction = function;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            final OutputChannel<? extends OUT> channel = mFunction.apply(input);

            if (channel != null) {

                channel.passTo(result);
            }
        }
    }

    /**
     * Invocation implementation generating a range of data.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class RangeInvocation<IN, OUT extends Comparable<OUT>>
            extends InvocationFactory<IN, OUT> implements Invocation<IN, OUT> {

        private final OUT mEnd;

        private final Function<OUT, OUT> mIncrement;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start     the first element of the range.
         * @param end       the last element of the range.
         * @param increment the function incrementing the current element.
         */
        @SuppressWarnings("ConstantConditions")
        private RangeInvocation(@NotNull final OUT start, @NotNull final OUT end,
                @NotNull final Function<OUT, OUT> increment) {

            if (start == null) {

                throw new NullPointerException("the start element must not be null");
            }

            if (end == null) {

                throw new NullPointerException("the end element must not be null");
            }

            if (increment == null) {

                throw new NullPointerException("the incrementing function must not be null");
            }

            mStart = start;
            mEnd = end;
            mIncrement = increment;
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

            final OUT start = mStart;
            final OUT end = mEnd;
            final Function<OUT, OUT> increment = mIncrement;
            OUT current = start;

            if (start.compareTo(end) <= 0) {

                while (current.compareTo(end) <= 0) {

                    result.pass(current);
                    current = increment.apply(current);
                }

            } else {

                while (current.compareTo(end) >= 0) {

                    result.pass(current);
                    current = increment.apply(current);
                }
            }
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
