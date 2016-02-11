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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.Channels;
import com.github.dm.jrt.core.Channels.Selectable;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.ConsumerWrapper;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.function.SupplierWrapper;
import com.github.dm.jrt.invocation.ComparableFilterInvocation;
import com.github.dm.jrt.invocation.ComparableInvocationFactory;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.wrapConsumer;
import static com.github.dm.jrt.function.Functions.wrapFunction;
import static com.github.dm.jrt.function.Functions.wrapSupplier;
import static com.github.dm.jrt.util.Reflection.asArgs;
import static com.github.dm.jrt.util.TimeDuration.fromUnit;

/**
 * Abstract implementation of a stream output channel.
 * <p/>
 * This class provides a default implementation of all the stream channel features. The inheriting
 * class just needs to create routine and channel instances when required.
 * <p/>
 * Created by davide-maestroni on 01/12/2016.
 *
 * @param <OUT> the output data type.
 */
public abstract class AbstractStreamChannel<OUT>
        implements StreamChannel<OUT>, Configurable<StreamChannel<OUT>> {

    private static final Binder NO_OP = new Binder() {

        public void bind() {

        }
    };

    private final Binder mBinder;

    private final OutputChannel<OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    private DelegationType mDelegationType;

    private InvocationConfiguration mStreamConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private final Configurable<StreamChannel<OUT>> mStreamConfigurable =
            new Configurable<StreamChannel<OUT>>() {

                @NotNull
                public StreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return AbstractStreamChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param channel        the wrapped output channel.
     * @param configuration  the initial invocation configuration.
     * @param delegationType the delegation type.
     * @param binder         the binding runnable.
     */
    @SuppressWarnings("ConstantConditions")
    protected AbstractStreamChannel(@NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        if (configuration == null) {
            throw new NullPointerException("the configuration must not be null");
        }

        if (delegationType == null) {
            throw new NullPointerException("the delegation type must not be null");
        }

        if (channel == null) {
            throw new NullPointerException("the output channel instance must not be null");
        }

        mStreamConfiguration = configuration;
        mDelegationType = delegationType;
        mChannel = channel;
        mBinder = (binder != null) ? binder : NO_OP;
    }

    @NotNull
    private static RangeInvocation<? extends Number> numberRange(@NotNull final Number start,
            @NotNull final Number end) {

        if ((start instanceof Double) || (end instanceof Double)) {
            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Float) || (end instanceof Float)) {
            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Long) || (end instanceof Long)) {
            final long startValue = start.longValue();
            final long endValue = end.longValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Integer) || (end instanceof Integer)) {
            final int startValue = start.intValue();
            final int endValue = end.intValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Short) || (end instanceof Short)) {
            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            return numberRange(start, end, (short) ((startValue <= endValue) ? 1 : -1));

        } else if ((start instanceof Byte) || (end instanceof Byte)) {
            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            return numberRange(start, end, (byte) ((startValue <= endValue) ? 1 : -1));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + "]");
    }

    @NotNull
    private static RangeInvocation<? extends Number> numberRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        if ((start instanceof Double) || (end instanceof Double) || (increment instanceof Double)) {
            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            final double incValue = increment.doubleValue();
            return new RangeInvocation<Double>(startValue, endValue, new DoubleInc(incValue));

        } else if ((start instanceof Float) || (end instanceof Float)
                || (increment instanceof Float)) {
            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            final float incValue = increment.floatValue();
            return new RangeInvocation<Float>(startValue, endValue, new FloatInc(incValue));

        } else if ((start instanceof Long) || (end instanceof Long)
                || (increment instanceof Long)) {
            final long startValue = start.longValue();
            final long endValue = end.longValue();
            final long incValue = increment.longValue();
            return new RangeInvocation<Long>(startValue, endValue, new LongInc(incValue));

        } else if ((start instanceof Integer) || (end instanceof Integer)
                || (increment instanceof Integer)) {
            final int startValue = start.intValue();
            final int endValue = end.intValue();
            final int incValue = increment.intValue();
            return new RangeInvocation<Integer>(startValue, endValue, new IntegerInc(incValue));

        } else if ((start instanceof Short) || (end instanceof Short)
                || (increment instanceof Short)) {
            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            final short incValue = increment.shortValue();
            return new RangeInvocation<Short>(startValue, endValue, new ShortInc(incValue));

        } else if ((start instanceof Byte) || (end instanceof Byte)
                || (increment instanceof Byte)) {
            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            final byte incValue = increment.byteValue();
            return new RangeInvocation<Byte>(startValue, endValue, new ByteInc(incValue));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + ", " + increment.getClass()
                                                                              .getCanonicalName()
                        + "]");
    }

    public boolean abort() {

        mBinder.bind();
        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        mBinder.bind();
        return mChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mChannel.isEmpty();
    }

    public boolean isOpen() {

        return mChannel.isOpen();
    }

    @NotNull
    public StreamChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        mChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        mChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mBinder.bind();
        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyAbort() {

        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        mChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyExit() {

        mChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyThrow() {

        mChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> immediately() {

        mChannel.immediately();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        mBinder.bind();
        mChannel.passTo(consumer);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> skip(final int count) {

        mBinder.bind();
        mChannel.skip(count);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> async() {

        mDelegationType = DelegationType.ASYNC;
        return this;
    }

    @NotNull
    public StreamChannel<OUT> backPressureOn(@Nullable final Runner runner, final int maxInputs,
            final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return backPressureOn(runner, maxInputs, fromUnit(maxDelay, timeUnit));
    }

    @NotNull
    public StreamChannel<OUT> backPressureOn(@Nullable final Runner runner, final int maxInputs,
            @Nullable final TimeDuration maxDelay) {

        return withInvocations().withRunner(runner)
                                .withInputLimit(maxInputs)
                                .withInputMaxDelay(maxDelay)
                                .set();
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> collect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return map(consumerFactory(consumer));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> collect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return map(functionFactory(function));
    }

    @NotNull
    public StreamChannel<Void> consume(@NotNull final Consumer<? super OUT> consumer) {

        return map(new ConsumerInvocation<OUT>(wrapConsumer(consumer)));
    }

    @NotNull
    public StreamChannel<OUT> filter(@NotNull final Predicate<? super OUT> predicate) {

        return map(predicateFilter(predicate));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return map(new MapInvocation<OUT, AFTER>(wrapFunction(function)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(@Nullable final AFTER output) {

        return map(new GenerateOutputInvocation<AFTER>(Collections.singletonList(output)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(@Nullable final AFTER... outputs) {

        final List<AFTER> list;
        if (outputs != null) {
            list = new ArrayList<AFTER>();
            Collections.addAll(list, outputs);

        } else {
            list = Collections.emptyList();
        }

        final GenerateOutputInvocation<AFTER> factory = new GenerateOutputInvocation<AFTER>(list);
        final DelegationType delegationType = mDelegationType;
        if (delegationType == DelegationType.ASYNC) {
            return sync().map(factory).async().map(PassingInvocation.<AFTER>factoryOf());

        } else if (delegationType == DelegationType.PARALLEL) {
            return async().map(factory)
                          .async()
                          .parallel()
                          .map(PassingInvocation.<AFTER>factoryOf());
        }

        return map(factory);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(
            @Nullable final Iterable<? extends AFTER> outputs) {

        final List<AFTER> list;
        if (outputs != null) {
            list = new ArrayList<AFTER>();
            for (final AFTER output : outputs) {
                list.add(output);
            }

        } else {
            list = Collections.emptyList();
        }

        final GenerateOutputInvocation<AFTER> factory = new GenerateOutputInvocation<AFTER>(list);
        final DelegationType delegationType = mDelegationType;
        if (delegationType == DelegationType.ASYNC) {
            return sync().map(factory).async().map(PassingInvocation.<AFTER>factoryOf());

        } else if (delegationType == DelegationType.PARALLEL) {
            return async().map(factory)
                          .async()
                          .parallel()
                          .map(PassingInvocation.<AFTER>factoryOf());
        }

        return map(factory);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return generate(count, new GenerateConsumerInvocation<AFTER>(wrapConsumer(consumer)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return generate(1, consumer);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return generate(count, new GenerateSupplierInvocation<AFTER>(wrapSupplier(supplier)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> generate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return generate(1, supplier);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return map(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return map(functionFilter(function));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return map(buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        final DelegationType delegationType = mDelegationType;
        final InvocationChannel<? super OUT, ? extends AFTER> channel;
        if (delegationType == DelegationType.ASYNC) {
            channel = routine.asyncInvoke();

        } else if (delegationType == DelegationType.PARALLEL) {
            channel = routine.parallelInvoke();

        } else {
            channel = routine.syncInvoke();
        }

        return concatRoutine(channel);
    }

    @NotNull
    public StreamChannel<OUT> maxParallelInvocations(final int maxInvocations) {

        return withInvocations().withMaxInstances(maxInvocations).set();
    }

    @NotNull
    public StreamChannel<OUT> ordered(@Nullable final OrderType orderType) {

        return withStreamInvocations().withOutputOrder(orderType).set();
    }

    @NotNull
    public StreamChannel<OUT> parallel() {

        mDelegationType = DelegationType.PARALLEL;
        return this;
    }

    @NotNull
    public <AFTER extends Comparable<AFTER>> StreamChannel<AFTER> range(@NotNull final AFTER start,
            @NotNull final AFTER end, @NotNull final Function<AFTER, AFTER> increment) {

        return map(new RangeInvocation<AFTER>(start, end, wrapFunction(increment)));
    }

    @NotNull
    public StreamChannel<Number> range(@NotNull final Number start, @NotNull final Number end) {

        return this.<Number>map(numberRange(start, end));
    }

    @NotNull
    public StreamChannel<Number> range(@NotNull final Number start, @NotNull final Number end,
            @NotNull final Number increment) {

        return this.<Number>map(numberRange(start, end, increment));
    }

    @NotNull
    public StreamChannel<OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return map(AccumulateInvocation.functionFactory(function));
    }

    @NotNull
    public StreamChannel<OUT> repeat() {

        return newChannel(Channels.repeat(this), getStreamConfiguration(), mDelegationType,
                          mBinder);
    }

    @NotNull
    public StreamChannel<OUT> runOn(@Nullable final Runner runner) {

        final DelegationType delegationType = mDelegationType;
        final StreamChannel<OUT> channel = withStreamInvocations().withRunner(runner)
                                                                  .set()
                                                                  .async()
                                                                  .map(PassingInvocation
                                                                               .<OUT>factoryOf());
        if (delegationType == DelegationType.ASYNC) {
            return channel.async();
        }

        if (delegationType == DelegationType.PARALLEL) {
            return channel.parallel();
        }

        return channel.sync();
    }

    @NotNull
    public StreamChannel<OUT> runOnShared() {

        return runOn(null);
    }

    @NotNull
    public StreamChannel<OUT> sync() {

        mDelegationType = DelegationType.SYNC;
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public StreamChannel<? extends Selectable<OUT>> toSelectable(final int index) {

        return newChannel(Channels.toSelectable(this, index), getStreamConfiguration(),
                          mDelegationType, mBinder);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        if (consumer == null) {
            throw new NullPointerException("the consumer instance must not be null");
        }

        final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
        mChannel.passTo(new TryCatchOutputConsumer<OUT>(consumer, ioChannel));
        return newChannel(ioChannel, getStreamConfiguration(), mDelegationType, mBinder);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamChannel<OUT> tryCatch(@NotNull final Consumer<? super RoutineException> consumer) {

        if (consumer == null) {
            throw new NullPointerException("the consumer instance must not be null");
        }

        return tryCatch(new TryCatchBiConsumerConsumer<OUT>(consumer));
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        if (function == null) {
            throw new NullPointerException("the function instance must not be null");
        }

        return tryCatch(new TryCatchBiConsumerFunction<OUT>(function));
    }

    @NotNull
    public Builder<? extends StreamChannel<OUT>> withInvocations() {

        return new Builder<StreamChannel<OUT>>(this, mConfiguration);
    }

    @NotNull
    public Builder<? extends StreamChannel<OUT>> withStreamInvocations() {

        return new Builder<StreamChannel<OUT>>(mStreamConfigurable, getStreamConfiguration());
    }

    @NotNull
    public List<OUT> all() {

        mBinder.bind();
        return mChannel.all();
    }

    @Nullable
    public RoutineException getError() {

        mBinder.bind();
        return mChannel.getError();
    }

    public boolean hasCompleted() {

        mBinder.bind();
        return mChannel.hasCompleted();
    }

    public boolean hasNext() {

        mBinder.bind();
        return mChannel.hasNext();
    }

    public OUT next() {

        mBinder.bind();
        return mChannel.next();
    }

    public boolean isBound() {

        return mChannel.isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        mBinder.bind();
        return mChannel.next(count);
    }

    public OUT nextOr(final OUT output) {

        mBinder.bind();
        return mChannel.nextOr(output);
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL passTo(
            @NotNull final CHANNEL channel) {

        mBinder.bind();
        return mChannel.passTo(channel);
    }

    public void throwError() {

        mBinder.bind();
        mChannel.throwError();
    }

    public Iterator<OUT> iterator() {

        mBinder.bind();
        return mChannel.iterator();
    }

    public void remove() {

        mBinder.bind();
        mChannel.remove();
    }

    /**
     * Returns the binder instance.
     *
     * @return the binder.
     */
    @NotNull
    protected Binder getBinder() {

        return mBinder;
    }

    /**
     * Returns the configuration which will be used by the next routine concatenated to the stream.
     *
     * @return the configuration.
     */
    @NotNull
    protected InvocationConfiguration getConfiguration() {

        return mConfiguration;
    }

    /**
     * Returns the delegation type used by all the routines concatenated to the stream.
     *
     * @return the delegation type.
     */
    @NotNull
    protected DelegationType getDelegationType() {

        return mDelegationType;
    }

    /**
     * Returns the configuration used by all the routines concatenated to the stream.
     *
     * @return the configuration.
     */
    @NotNull
    protected InvocationConfiguration getStreamConfiguration() {

        return mStreamConfiguration;
    }

    /**
     * Creates a new channel instance.
     *
     * @param <AFTER>        the concatenation output type.
     * @param channel        the wrapped output channel.
     * @param configuration  the stream configuration.
     * @param delegationType the delegation type.
     * @param binder         the binder instance.
     * @return the newly created channel instance.
     */
    @NotNull
    protected abstract <AFTER> StreamChannel<AFTER> newChannel(
            @NotNull OutputChannel<AFTER> channel, @NotNull InvocationConfiguration configuration,
            @NotNull DelegationType delegationType, @Nullable Binder binder);

    /**
     * Creates a new routine instance based on the specified factory.
     *
     * @param <AFTER>       the concatenation output type.
     * @param configuration the routine configuration.
     * @param factory       the invocation factory.
     * @return the newly created routine instance.
     */
    @NotNull
    protected abstract <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull InvocationConfiguration configuration,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(getStreamConfiguration().builderFrom().with(mConfiguration).set(),
                          factory);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <AFTER> StreamChannel<AFTER> concatRoutine(
            @NotNull final InvocationChannel<? super OUT, ? extends AFTER> channel) {

        return newChannel((OutputChannel<AFTER>) mChannel.passTo(channel).result(),
                          getStreamConfiguration(), mDelegationType, mBinder);
    }

    @NotNull
    private <AFTER> StreamChannel<AFTER> generate(final long count,
            @NotNull final InvocationFactory<Object, AFTER> factory) {

        if (count <= 0) {
            throw new IllegalArgumentException("the count number must be positive: " + count);
        }

        final DelegationType delegationType = mDelegationType;
        if (delegationType == DelegationType.ASYNC) {
            return sync().range(1, count).async().map(factory);

        } else if (delegationType == DelegationType.PARALLEL) {
            return async().range(1, count).parallel().map(factory);
        }

        return range(1, count).map(factory);
    }

    /**
     * Class binding two channels together.
     */
    protected static abstract class Binder {

        /**
         * Avoid instantiation.
         */
        private Binder() {

        }

        /**
         * Returns a new binder of the two specified channels.
         *
         * @param input  the channel returning the inputs.
         * @param output the channel consuming them.
         * @param <DATA> the data type.
         * @return the binder instance.
         */
        @SuppressWarnings("ConstantConditions")
        public static <DATA> Binder binderOf(@NotNull final OutputChannel<DATA> input,
                @NotNull final IOChannel<DATA> output) {

            if (input == null) {
                throw new NullPointerException("the input channel must not be null");
            }

            if (output == null) {
                throw new NullPointerException("the output channel must not be null");
            }

            return new InputBinder<DATA>(input, output);
        }

        /**
         * Binds the two channel.<br/>
         * The call will have no effect if the method has been already invoked at least once.
         */
        public abstract void bind();

        /**
         * Default implementation of a binder.
         *
         * @param <DATA> the data type.
         */
        private static class InputBinder<DATA> extends Binder {

            private final OutputChannel<DATA> mInput;

            private final AtomicBoolean mIsBound = new AtomicBoolean();

            private final IOChannel<DATA> mOutput;

            /**
             * Constructor.
             *
             * @param input  the channel returning the inputs.
             * @param output the channel consuming them.
             */
            private InputBinder(@NotNull final OutputChannel<DATA> input,
                    @NotNull final IOChannel<DATA> output) {

                mInput = input;
                mOutput = output;
            }

            public void bind() {

                if (!mIsBound.getAndSet(true)) {
                    mInput.passTo(mOutput).close();
                }
            }
        }
    }

    /**
     * Function incrementing a short of a specific value.
     */
    private static class ByteInc extends NumberInc<Byte> {

        private final byte mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private ByteInc(final byte incValue) {

            super(incValue);
            mIncValue = incValue;
        }

        public Byte apply(final Byte aByte) {

            return (byte) (aByte + mIncValue);
        }
    }

    /**
     * Invocation implementation wrapping a consumer accepting output data.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerInvocation<OUT> extends ComparableFilterInvocation<OUT, Void> {

        private final ConsumerWrapper<? super OUT> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerInvocation(@NotNull final ConsumerWrapper<? super OUT> consumer) {

            super(asArgs(consumer));
            mConsumer = consumer;
        }

        public void onInput(final OUT input, @NotNull final ResultChannel<Void> result) {

            mConsumer.accept(input);
        }
    }

    /**
     * Function incrementing a double of a specific value.
     */
    private static class DoubleInc extends NumberInc<Double> {

        private final double mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private DoubleInc(final double incValue) {

            super(incValue);
            mIncValue = incValue;
        }

        public Double apply(final Double aDouble) {

            return aDouble + mIncValue;
        }
    }

    /**
     * Function incrementing a float of a specific value.
     */
    private static class FloatInc extends NumberInc<Float> {

        private final float mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private FloatInc(final float incValue) {

            super(incValue);
            mIncValue = incValue;
        }

        public Float apply(final Float aFloat) {

            return aFloat + mIncValue;
        }
    }

    /**
     * Invocation implementation wrapping a consumer instance.
     *
     * @param <OUT> the output data type.
     */
    private static class GenerateConsumerInvocation<OUT>
            extends ComparableFilterInvocation<Object, OUT> {

        private final ConsumerWrapper<? super ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         * @throws java.lang.IllegalArgumentException if the specified count number is 0 or
         *                                            negative.
         */
        private GenerateConsumerInvocation(
                @NotNull final ConsumerWrapper<? super ResultChannel<OUT>> consumer) {

            super(asArgs(consumer));
            mConsumer = consumer;
        }

        public void onInput(final Object input, @NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(result);
        }
    }

    /**
     * Base abstract implementation of an invocation generating output data.
     *
     * @param <OUT> the output data type.
     */
    private abstract static class GenerateInvocation<OUT>
            extends ComparableInvocationFactory<Object, OUT> implements Invocation<Object, OUT> {

        /**
         * Constructor.
         *
         * @param args the constructor arguments.
         */
        private GenerateInvocation(@Nullable final Object[] args) {

            super(args);
        }

        @NotNull
        @Override
        public final Invocation<Object, OUT> newInvocation() {

            return this;
        }

        public final void onAbort(@NotNull final RoutineException reason) {

        }

        public final void onDestroy() {

        }

        public final void onInitialize() {

        }

        public final void onInput(final Object input, @NotNull final ResultChannel<OUT> result) {

        }

        public final void onTerminate() {

        }
    }

    /**
     * Invocation implementation generating a list of outputs.
     *
     * @param <OUT> the output data type.
     */
    private static class GenerateOutputInvocation<OUT> extends GenerateInvocation<OUT> {

        private final List<OUT> mOutputs;

        /**
         * Constructor.
         *
         * @param outputs the list of outputs.
         */
        private GenerateOutputInvocation(@NotNull final List<OUT> outputs) {

            super(asArgs(outputs));
            mOutputs = outputs;
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            result.pass(mOutputs);
        }
    }

    /**
     * Invocation implementation wrapping a supplier instance.
     *
     * @param <OUT> the output data type.
     */
    private static class GenerateSupplierInvocation<OUT>
            extends ComparableFilterInvocation<Object, OUT> {

        private final SupplierWrapper<? extends OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier instance.
         * @throws java.lang.IllegalArgumentException if the specified count number is 0 or
         *                                            negative.
         */
        private GenerateSupplierInvocation(@NotNull final SupplierWrapper<? extends OUT> supplier) {

            super(asArgs(supplier));
            mSupplier = supplier;
        }

        public void onInput(final Object input, @NotNull final ResultChannel<OUT> result) {

            result.pass(mSupplier.get());
        }
    }

    /**
     * Function incrementing an integer of a specific value.
     */
    private static class IntegerInc extends NumberInc<Integer> {

        private final int mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private IntegerInc(final int incValue) {

            super(incValue);
            mIncValue = incValue;
        }

        public Integer apply(final Integer integer) {

            return integer + mIncValue;
        }
    }

    /**
     * Function incrementing a long of a specific value.
     */
    private static class LongInc extends NumberInc<Long> {

        private final long mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private LongInc(final long incValue) {

            super(incValue);
            mIncValue = incValue;
        }

        public Long apply(final Long aLong) {

            return aLong + mIncValue;
        }
    }

    /**
     * Filter invocation implementation wrapping a map function.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MapInvocation<IN, OUT> extends ComparableFilterInvocation<IN, OUT> {

        private final FunctionWrapper<? super IN, ? extends OutputChannel<? extends OUT>> mFunction;

        /**
         * Constructor.
         *
         * @param function the lifting function.
         */
        private MapInvocation(
                @NotNull final FunctionWrapper<? super IN, ? extends OutputChannel<? extends
                        OUT>> function) {

            super(asArgs(function));
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
     * Base abstract function incrementing a number of a specific value.<br/>
     * It provides an implementation for {@code equals()} and {@code hashCode()} methods.
     */
    private static abstract class NumberInc<N extends Number> implements Function<N, N> {

        private final N mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private NumberInc(@NotNull final N incValue) {

            mIncValue = incValue;
        }

        @Override
        public int hashCode() {

            return mIncValue.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof NumberInc)) {
                return false;
            }

            final NumberInc<?> numberInc = (NumberInc<?>) o;
            return mIncValue.equals(numberInc.mIncValue);
        }
    }

    /**
     * Invocation implementation generating a range of data.
     *
     * @param <OUT> the output data type.
     */
    private static class RangeInvocation<OUT extends Comparable<OUT>>
            extends GenerateInvocation<OUT> {

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

            super(asArgs(start, end, increment));
            if (start == null) {
                throw new NullPointerException("the start element must not be null");
            }

            if (end == null) {
                throw new NullPointerException("the end element must not be null");
            }

            mStart = start;
            mEnd = end;
            mIncrement = increment;
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
    }

    /**
     * Function incrementing a short of a specific value.
     */
    private static class ShortInc extends NumberInc<Short> {

        private final short mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private ShortInc(final short incValue) {

            super(incValue);
            mIncValue = incValue;
        }

        public Short apply(final Short aShort) {

            return (short) (aShort + mIncValue);
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

        public void onError(@NotNull final RoutineException error) {

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

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public StreamChannel<OUT> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
