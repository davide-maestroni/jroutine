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

package com.github.dm.jrt.method;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
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
 * Channel implementation acting as input of a routine method.
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 *
 * @see com.github.dm.jrt.method.RoutineMethod RoutineMethod
 */
public final class InputChannel<IN> implements Channel<IN, IN> {

    private final Channel<IN, IN> mChannel;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped channel.
     */
    InputChannel(@NotNull final Channel<IN, IN> wrapped) {
        mChannel = ConstantConditions.notNull("wrapped channel", wrapped);
    }

    public boolean abort() {
        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {
        return mChannel.abort(reason);
    }

    @NotNull
    public InputChannel<IN> after(final long delay, @NotNull final TimeUnit timeUnit) {
        mChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public InputChannel<IN> after(@NotNull final UnitDuration delay) {
        mChannel.after(delay);
        return this;
    }

    @NotNull
    public List<IN> all() {
        return mChannel.all();
    }

    @NotNull
    public InputChannel<IN> allInto(@NotNull final Collection<? super IN> results) {
        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public Channel<? super IN, ?> bind(@NotNull final Channel<? super IN, ?> channel) {
        return mChannel.bind(channel);
    }

    @NotNull
    public InputChannel<IN> bind(@NotNull final ChannelConsumer<? super IN> consumer) {
        mChannel.bind(consumer);
        return this;
    }

    @NotNull
    public InputChannel<IN> close() {
        mChannel.close();
        return this;
    }

    @NotNull
    public InputChannel<IN> eventuallyAbort() {
        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public InputChannel<IN> eventuallyAbort(@Nullable final Throwable reason) {
        mChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public InputChannel<IN> eventuallyContinue() {
        mChannel.eventuallyContinue();
        return this;
    }

    @NotNull
    public InputChannel<IN> eventuallyFail() {
        mChannel.eventuallyFail();
        return this;
    }

    @NotNull
    public Iterator<IN> expiringIterator() {
        return mChannel.expiringIterator();
    }

    public boolean getComplete() {
        return mChannel.getComplete();
    }

    @Nullable
    public RoutineException getError() {
        return mChannel.getError();
    }

    public boolean hasNext() {
        return mChannel.hasNext();
    }

    public IN next() {
        return mChannel.next();
    }

    @NotNull
    public InputChannel<IN> immediately() {
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
    public List<IN> next(final int count) {
        return mChannel.next(count);
    }

    public IN nextOrElse(final IN output) {
        return mChannel.nextOrElse(output);
    }

    public int outputCount() {
        return mChannel.outputCount();
    }

    @NotNull
    public InputChannel<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
        mChannel.pass(channel);
        return this;
    }

    @NotNull
    public InputChannel<IN> pass(@Nullable final Iterable<? extends IN> inputs) {
        mChannel.pass(inputs);
        return this;
    }

    @NotNull
    public InputChannel<IN> pass(@Nullable final IN input) {
        mChannel.pass(input);
        return this;
    }

    @NotNull
    public InputChannel<IN> pass(@Nullable final IN... inputs) {
        mChannel.pass(inputs);
        return this;
    }

    public int size() {
        return mChannel.size();
    }

    @NotNull
    public InputChannel<IN> skipNext(final int count) {
        mChannel.skipNext(count);
        return this;
    }

    @NotNull
    public InputChannel<IN> sortedByCall() {
        mChannel.sortedByCall();
        return this;
    }

    @NotNull
    public InputChannel<IN> sortedByDelay() {
        mChannel.sortedByDelay();
        return this;
    }

    public void throwError() {
        mChannel.throwError();
    }

    public Iterator<IN> iterator() {
        return mChannel.iterator();
    }

    public void remove() {
        mChannel.remove();
    }
}
