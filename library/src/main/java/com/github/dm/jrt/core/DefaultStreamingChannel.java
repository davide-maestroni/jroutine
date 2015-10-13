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

import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of a streaming channel.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStreamingChannel<IN, OUT> implements StreamingChannel<IN, OUT> {

    private final IOChannel<IN, ?> mInputChannel;

    private final OutputChannel<OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param inputChannel  the input channel.
     * @param outputChannel the output channel.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultStreamingChannel(@NotNull final IOChannel<IN, ?> inputChannel,
            @NotNull final OutputChannel<OUT> outputChannel) {

        if (inputChannel == null) {

            throw new NullPointerException("the input channel must not be null");
        }

        if (outputChannel == null) {

            throw new NullPointerException("the output channel must not be null");
        }

        mInputChannel = inputChannel;
        mOutputChannel = outputChannel;
    }

    public boolean abort() {

        return mInputChannel.abort() || mOutputChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return mInputChannel.abort(reason) || mOutputChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mInputChannel.isEmpty() && mOutputChannel.isEmpty();
    }

    public boolean isOpen() {

        return mInputChannel.isOpen();
    }

    @NotNull
    public StreamingChannel<IN, OUT> after(@NotNull final TimeDuration delay) {

        mInputChannel.after(delay);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        mInputChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> now() {

        mInputChannel.now();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> orderByCall() {

        mInputChannel.orderByCall();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> orderByChance() {

        mInputChannel.orderByChance();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> orderByDelay() {

        mInputChannel.orderByDelay();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> pass(@Nullable final OutputChannel<? extends IN> channel) {

        mInputChannel.pass(channel);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> pass(@Nullable final IN input) {

        mInputChannel.pass(input);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> pass(@Nullable final IN... inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> afterMax(@NotNull final TimeDuration timeout) {

        mOutputChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> afterMax(final long timeout,
            @NotNull final TimeUnit timeUnit) {

        mOutputChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mOutputChannel.allInto(results);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> eventuallyAbort() {

        mOutputChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> eventuallyExit() {

        mOutputChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> eventuallyThrow() {

        mOutputChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> immediately() {

        mOutputChannel.immediately();
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        mOutputChannel.passTo(consumer);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> skip(final int count) {

        mOutputChannel.skip(count);
        return this;
    }

    @NotNull
    public StreamingChannel<IN, OUT> close() {

        mInputChannel.close();
        return this;
    }

    @NotNull
    public <BEFORE> StreamingChannel<BEFORE, OUT> combine(
            @NotNull final IOChannel<BEFORE, ? extends IN> channel) {

        mInputChannel.pass(channel).close();
        return new DefaultStreamingChannel<BEFORE, OUT>(channel, mOutputChannel);
    }

    @NotNull
    public <AFTER> StreamingChannel<IN, AFTER> concat(
            @NotNull final IOChannel<? super OUT, AFTER> channel) {

        mOutputChannel.passTo(channel);
        return new DefaultStreamingChannel<IN, AFTER>(mInputChannel, channel.close());
    }

    @NotNull
    public List<OUT> all() {

        return mOutputChannel.all();
    }

    public boolean checkComplete() {

        return mOutputChannel.checkComplete();
    }

    public boolean hasNext() {

        return mOutputChannel.hasNext();
    }

    public OUT next() {

        return mOutputChannel.next();
    }

    public boolean isBound() {

        return mOutputChannel.isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        return mOutputChannel.next(count);
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL passTo(
            @NotNull final CHANNEL channel) {

        return mOutputChannel.passTo(channel);
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

        return mOutputChannel.iterator();
    }

    public void remove() {

        mOutputChannel.remove();
    }
}
