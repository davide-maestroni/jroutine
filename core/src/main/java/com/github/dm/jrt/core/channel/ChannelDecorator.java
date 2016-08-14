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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Decorator of channel instances.
 * <p>
 * Created by davide-maestroni on 08/14/2016.
 */
public class ChannelDecorator<IN, OUT> implements Channel<IN, OUT> {

    private final Channel<IN, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped channel instance.
     */
    public ChannelDecorator(@NotNull final Channel<IN, OUT> wrapped) {
        mChannel = ConstantConditions.notNull("wrapped channel", wrapped);
    }

    public boolean abort() {
        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {
        return mChannel.abort(reason);
    }

    @NotNull
    public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        mChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> after(@NotNull final UnitDuration delay) {
        mChannel.after(delay);
        return this;
    }

    @NotNull
    public List<OUT> all() {
        return mChannel.all();
    }

    @NotNull
    public Channel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public Channel<? super OUT, ?> bind(@NotNull final Channel<? super OUT, ?> channel) {
        return mChannel.bind(channel);
    }

    @NotNull
    public Channel<IN, OUT> bind(@NotNull final ChannelConsumer<? super OUT> consumer) {
        mChannel.bind(consumer);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> close() {
        mChannel.close();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyAbort() {
        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        mChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyBreak() {
        mChannel.eventuallyBreak();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyFail() {
        mChannel.eventuallyFail();
        return this;
    }

    @NotNull
    public Iterator<OUT> expiringIterator() {
        return mChannel.expiringIterator();
    }

    @Nullable
    public RoutineException getError() {
        return mChannel.getError();
    }

    public boolean hasCompleted() {
        return mChannel.hasCompleted();
    }

    public boolean hasNext() {
        return mChannel.hasNext();
    }

    public OUT next() {
        return mChannel.next();
    }

    @NotNull
    public Channel<IN, OUT> immediately() {
        mChannel.immediately();
        return this;
    }

    public int inputCount() {
        return mChannel.inputCount();
    }

    public boolean isBound() {
        return mChannel.isBound();
    }

    public boolean isEmpty() {
        return mChannel.isEmpty();
    }

    public boolean isOpen() {
        return mChannel.isOpen();
    }

    @NotNull
    public List<OUT> next(final int count) {
        return mChannel.next(count);
    }

    public OUT nextOrElse(final OUT output) {
        return mChannel.nextOrElse(output);
    }

    public int outputCount() {
        return mChannel.outputCount();
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
        mChannel.pass(channel);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
        mChannel.pass(inputs);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final IN input) {
        mChannel.pass(input);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final IN... inputs) {
        mChannel.pass(inputs);
        return this;
    }

    public int size() {
        return mChannel.size();
    }

    @NotNull
    public Channel<IN, OUT> skipNext(final int count) {
        mChannel.skipNext(count);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> sortedByCall() {
        mChannel.sortedByCall();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> sortedByDelay() {
        mChannel.sortedByDelay();
        return this;
    }

    public void throwError() {
        mChannel.throwError();
    }

    public Iterator<OUT> iterator() {
        return mChannel.iterator();
    }

    public void remove() {
        mChannel.remove();
    }
}
