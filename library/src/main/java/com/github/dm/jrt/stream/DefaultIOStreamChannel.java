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
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.Channels.Selectable;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 02/01/2016.
 */
public class DefaultIOStreamChannel<IN, OUT> implements IOStreamChannel<IN, OUT> {

    private final IOChannel<IN> mInputChannel;

    private final StreamChannel<OUT> mStreamChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    private final Configurable<IOStreamChannel<IN, OUT>> mConfigurable =
            new Configurable<IOStreamChannel<IN, OUT>>() {

                @NotNull
                public IOStreamChannel<IN, OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    mConfiguration = configuration;
                    mStreamChannel.withInvocations().with(null).with(configuration).set();
                    return DefaultIOStreamChannel.this;
                }
            };

    private InvocationConfiguration mStreamConfiguration;

    private final Configurable<IOStreamChannel<IN, OUT>> mStreamConfigurable =
            new Configurable<IOStreamChannel<IN, OUT>>() {

                @NotNull
                public IOStreamChannel<IN, OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    mStreamChannel.withStreamInvocations().with(null).with(configuration).set();
                    return DefaultIOStreamChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param input   the input I/O channel.
     * @param channel the wrapped stream channel.
     */
    DefaultIOStreamChannel(@NotNull final IOChannel<IN> input,
            @NotNull final StreamChannel<OUT> channel) {

        this(InvocationConfiguration.DEFAULT_CONFIGURATION, input, channel);
    }

    private DefaultIOStreamChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final IOChannel<IN> input, @NotNull final StreamChannel<OUT> channel) {

        mStreamConfiguration = configuration;
        mInputChannel = input;
        mStreamChannel = channel;
    }

    public boolean abort() {

        return mInputChannel.abort() || mStreamChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return mInputChannel.abort(reason) || mStreamChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mInputChannel.isEmpty() && mStreamChannel.isEmpty();
    }

    public boolean isOpen() {

        return mInputChannel.isOpen();
    }

    @NotNull
    public IOStreamChannel<IN, OUT> after(@NotNull final TimeDuration delay) {

        mInputChannel.after(delay);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        mInputChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> now() {

        mInputChannel.now();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> orderByCall() {

        mInputChannel.orderByCall();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> orderByDelay() {

        mInputChannel.orderByDelay();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> pass(@Nullable final OutputChannel<? extends IN> channel) {

        mInputChannel.pass(channel);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> pass(@Nullable final IN input) {

        mInputChannel.pass(input);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> pass(@Nullable final IN... inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> afterMax(@NotNull final TimeDuration timeout) {

        mStreamChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        mStreamChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mStreamChannel.allInto(results);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> eventuallyAbort() {

        mStreamChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {

        mStreamChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> eventuallyExit() {

        mStreamChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> eventuallyThrow() {

        mInputChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> immediately() {

        mStreamChannel.immediately();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        mStreamChannel.passTo(consumer);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> skip(final int count) {

        mStreamChannel.skip(count);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> async() {

        mStreamChannel.async();
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        mStreamChannel.backPressureOn(runner, maxInputs, maxDelay, timeUnit);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        mStreamChannel.backPressureOn(runner, maxInputs, maxDelay);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> bind() {

        mStreamChannel.bind();
        return this;
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> collect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.collect(consumer));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> collect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.collect(function));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> filter(@NotNull final Predicate<? super OUT> predicate) {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.filter(predicate));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.flatMap(function));
    }

    @NotNull
    public IOStreamChannel<IN, Void> forEach(@NotNull final Consumer<? super OUT> consumer) {

        return new DefaultIOStreamChannel<IN, Void>(mStreamConfiguration, mInputChannel,
                                                    mStreamChannel.forEach(consumer));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(@Nullable final AFTER output) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(output));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(@Nullable final AFTER... outputs) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(outputs));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(
            @Nullable final Iterable<? extends AFTER> outputs) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(outputs));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(consumer));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(consumer));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(supplier));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> generate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.generate(supplier));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.map(consumer));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.map(function));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.map(factory));
    }

    @NotNull
    public <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.map(routine));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> maxParallelInvocations(final int maxInvocations) {

        mStreamChannel.maxParallelInvocations(maxInvocations);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> ordered(@Nullable final OrderType orderType) {

        mStreamChannel.ordered(orderType);
        return this;
    }

    @NotNull
    public IOStreamChannel<IN, OUT> parallel() {

        mStreamChannel.parallel();
        return this;
    }

    @NotNull
    public <AFTER extends Comparable<AFTER>> IOStreamChannel<IN, AFTER> range(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return new DefaultIOStreamChannel<IN, AFTER>(mStreamConfiguration, mInputChannel,
                                                     mStreamChannel.range(start, end, increment));
    }

    @NotNull
    public IOStreamChannel<IN, Number> range(@NotNull final Number start,
            @NotNull final Number end) {

        return new DefaultIOStreamChannel<IN, Number>(mStreamConfiguration, mInputChannel,
                                                      mStreamChannel.range(start, end));
    }

    @NotNull
    public IOStreamChannel<IN, Number> range(@NotNull final Number start, @NotNull final Number end,
            @NotNull final Number increment) {

        return new DefaultIOStreamChannel<IN, Number>(mStreamConfiguration, mInputChannel,
                                                      mStreamChannel.range(start, end, increment));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.reduce(function));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> repeat() {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.repeat());
    }

    @NotNull
    public IOStreamChannel<IN, OUT> runOn(@Nullable final Runner runner) {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.runOn(runner));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> runOnShared() {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.runOnShared());
    }

    @NotNull
    public IOStreamChannel<IN, OUT> sync() {

        mStreamChannel.sync();
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public IOStreamChannel<IN, ? extends Selectable<OUT>> toSelectable(final int index) {

        return new DefaultIOStreamChannel<IN, Selectable<OUT>>(mStreamConfiguration, mInputChannel,
                                                               (StreamChannel<Selectable<OUT>>)
                                                                       mStreamChannel
                                                                       .toSelectable(index));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.tryCatch(consumer));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> tryCatch(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.tryCatch(consumer));
    }

    @NotNull
    public IOStreamChannel<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return new DefaultIOStreamChannel<IN, OUT>(mStreamConfiguration, mInputChannel,
                                                   mStreamChannel.tryCatch(function));
    }

    @NotNull
    public Builder<? extends IOStreamChannel<IN, OUT>> withInvocations() {

        return new Builder<IOStreamChannel<IN, OUT>>(mConfigurable, mConfiguration);
    }

    @NotNull
    public Builder<? extends IOStreamChannel<IN, OUT>> withStreamInvocations() {

        return new Builder<IOStreamChannel<IN, OUT>>(mStreamConfigurable, mStreamConfiguration);
    }

    @NotNull
    public IOStreamChannel<IN, OUT> close() {

        mInputChannel.close();
        return this;
    }

    @NotNull
    public List<OUT> all() {

        return mStreamChannel.all();
    }

    public boolean checkDone() {

        return mStreamChannel.checkDone();
    }

    @Nullable
    public RoutineException getError() {

        return mStreamChannel.getError();
    }

    public boolean hasNext() {

        return mStreamChannel.hasNext();
    }

    public OUT next() {

        return mStreamChannel.next();
    }

    public boolean isBound() {

        return mStreamChannel.isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        return mStreamChannel.next(count);
    }

    public OUT nextOr(final OUT output) {

        return mStreamChannel.nextOr(output);
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL passTo(
            @NotNull final CHANNEL channel) {

        return mStreamChannel.passTo(channel);
    }

    public void throwError() {

        mStreamChannel.throwError();
    }

    @NotNull
    public InputChannel<IN> asInput() {

        return this;
    }

    @NotNull
    public OutputChannel<OUT> asOutput() {

        return this;
    }

    public Iterator<OUT> iterator() {

        return mStreamChannel.iterator();
    }

    public void remove() {

        mStreamChannel.remove();
    }
}
