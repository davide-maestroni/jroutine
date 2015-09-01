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
package com.github.dm.jrt.channel;

import com.github.dm.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining an invocation input channel, that is the channel used to pass input data to
 * the routine invocation.
 * <p/>
 * Created by davide-maestroni on 09/15/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface InvocationChannel<IN, OUT> extends InputChannel<IN> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> after(@Nonnull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> after(long delay, @Nonnull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> now();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> pass(@Nullable OutputChannel<? extends IN> channel);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * Closes the input channel and returns the output one.
     *
     * @return the routine output channel.
     * @throws java.lang.IllegalStateException if this method has been already called.
     */
    @Nonnull
    OutputChannel<OUT> result();
}
