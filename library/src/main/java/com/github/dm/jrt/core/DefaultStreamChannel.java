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
import com.github.dm.jrt.channel.StreamChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 09/24/2015.
 */
public class DefaultStreamChannel<IN, OUT> implements StreamChannel<IN, OUT> {

    private final IOChannel<IN, ?> mInputChannel;

    private final OutputChannel<OUT> mOutputChannel;

    @SuppressWarnings("ConstantConditions")
    DefaultStreamChannel(@NotNull final IOChannel<IN, ?> inputChannel,
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
    public StreamChannel<IN, OUT> after(@NotNull final TimeDuration delay) {

        mInputChannel.after(delay);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        mInputChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> now() {

        mInputChannel.now();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> orderByCall() {

        mInputChannel.orderByCall();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> orderByChance() {

        mInputChannel.orderByChance();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> orderByDelay() {

        mInputChannel.orderByDelay();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final OutputChannel<? extends IN> channel) {

        mInputChannel.pass(channel);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final IN input) {

        mInputChannel.pass(input);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final IN... inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> afterMax(@NotNull final TimeDuration timeout) {

        mOutputChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        mOutputChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mOutputChannel.allInto(results);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventually() {

        mOutputChannel.eventually();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyAbort() {

        mOutputChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyExit() {

        mOutputChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyThrow() {

        mOutputChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> immediately() {

        mOutputChannel.immediately();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        mOutputChannel.passTo(consumer);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> close() {

        mInputChannel.close();
        return this;
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> concat(
            @NotNull final IOChannel<? super OUT, AFTER> after) {

        mOutputChannel.passTo(after);
        return new DefaultStreamChannel<IN, AFTER>(mInputChannel, after.close());
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

    public boolean hasDelays() {

        return mInputChannel.hasDelays();
    }

    public Iterator<OUT> iterator() {

        return mOutputChannel.iterator();
    }

    public void remove() {

        mOutputChannel.remove();
    }
}
