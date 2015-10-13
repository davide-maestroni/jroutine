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
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.FunctionalChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.Channels.asyncStream;
import static com.github.dm.jrt.core.Channels.parallelStream;
import static com.github.dm.jrt.core.Channels.syncStream;
import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;

/**
 * Default implementation of a functional channel.
 * <p/>
 * Created by davide-maestroni on 10/13/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultFunctionalChannel<IN, OUT>
        implements FunctionalChannel<IN, OUT>, Configurable<FunctionalChannel<IN, OUT>> {

    private final StreamingChannel<IN, OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param configuration the initial configuration.
     * @param channel       the backing streaming channel.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultFunctionalChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final StreamingChannel<IN, OUT> channel) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        if (channel == null) {

            throw new NullPointerException("the streaming channel must not be null");
        }

        mConfiguration = configuration;
        mChannel = channel;
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
    public FunctionalChannel<IN, OUT> after(@NotNull final TimeDuration delay) {

        mChannel.after(delay);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        mChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> now() {

        mChannel.now();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> orderByCall() {

        mChannel.orderByCall();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> orderByChance() {

        mChannel.orderByChance();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> orderByDelay() {

        mChannel.orderByDelay();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> pass(@Nullable final OutputChannel<? extends IN> channel) {

        mChannel.pass(channel);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

        mChannel.pass(inputs);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> pass(@Nullable final IN input) {

        mChannel.pass(input);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> pass(@Nullable final IN... inputs) {

        mChannel.pass(inputs);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> afterMax(@NotNull final TimeDuration timeout) {

        mChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> afterMax(final long timeout,
            @NotNull final TimeUnit timeUnit) {

        mChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> eventuallyAbort() {

        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> eventuallyExit() {

        mChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> eventuallyThrow() {

        mChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> immediately() {

        mChannel.immediately();
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        mChannel.passTo(consumer);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> skip(final int count) {

        mChannel.skip(count);
        return this;
    }

    @NotNull
    public FunctionalChannel<IN, OUT> close() {

        mChannel.close();
        return this;
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMap(
            @NotNull final IOChannel<? super OUT, AFTER> channel) {

        return new DefaultFunctionalChannel<IN, AFTER>(mConfiguration, mChannel.concat(channel));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapAsync(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return andThenMapAsync(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapAsync(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return andThenMap(
                asyncStream(JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapAsync(
            @NotNull final Function<? super OUT, AFTER> function) {

        return andThenMapAsync(functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapParallel(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return andThenMapParallel(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapParallel(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return andThenMap(
                parallelStream(JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapParallel(
            @NotNull final Function<? super OUT, AFTER> function) {

        return andThenMapParallel(functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenReduceAsync(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return andThenMap(asyncStream(
                JRoutine.on(consumerFactory(consumer)).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenReduceAsync(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return andThenMap(asyncStream(
                JRoutine.on(functionFactory(function)).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenReduceSync(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return andThenMap(syncStream(
                JRoutine.on(consumerFactory(consumer)).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenReduceSync(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return andThenMap(syncStream(
                JRoutine.on(functionFactory(function)).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapSync(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return andThenMapSync(consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapSync(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return andThenMap(
                syncStream(JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <AFTER> FunctionalChannel<IN, AFTER> andThenMapSync(
            @NotNull final Function<? super OUT, AFTER> function) {

        return andThenMapSync(functionFilter(function));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> compose(
            @NotNull final IOChannel<BEFORE, ? extends IN> channel) {

        return new DefaultFunctionalChannel<BEFORE, OUT>(mConfiguration, mChannel.combine(channel));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeAsync(
            @NotNull final FilterInvocation<BEFORE, ? extends IN> invocation) {

        return compose(
                asyncStream(JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeAsync(
            @NotNull final BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer) {

        return composeAsync(consumerFilter(consumer));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeAsync(
            @NotNull final Function<BEFORE, ? extends IN> function) {

        return composeAsync(functionFilter(function));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeParallel(
            @NotNull final FilterInvocation<BEFORE, ? extends IN> invocation) {

        return compose(
                parallelStream(JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeParallel(
            @NotNull final BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer) {

        return composeParallel(consumerFilter(consumer));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeParallel(
            @NotNull final Function<BEFORE, ? extends IN> function) {

        return composeParallel(functionFilter(function));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeSync(
            @NotNull final FilterInvocation<BEFORE, ? extends IN> invocation) {

        return compose(
                syncStream(JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeSync(
            @NotNull final BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer) {

        return composeSync(consumerFilter(consumer));
    }

    @NotNull
    public <BEFORE> FunctionalChannel<BEFORE, OUT> composeSync(
            @NotNull final Function<BEFORE, ? extends IN> function) {

        return composeSync(functionFilter(function));
    }

    @NotNull
    public Builder<? extends FunctionalChannel<IN, OUT>> invocations() {

        return new Builder<FunctionalChannel<IN, OUT>>(this, mConfiguration);
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
    public InputChannel<IN> asInput() {

        return this;
    }

    @NotNull
    public OutputChannel<OUT> asOutput() {

        return this;
    }

    public Iterator<OUT> iterator() {

        return mChannel.iterator();
    }

    public void remove() {

        mChannel.remove();
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public FunctionalChannel<IN, OUT> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
