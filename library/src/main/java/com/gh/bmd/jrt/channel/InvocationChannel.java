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
package com.gh.bmd.jrt.channel;

import com.gh.bmd.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining an invocation input channel, that is the channel used to pass input data to
 * the routine invocation.
 * <p/>
 * Created by davide-maestroni on 9/15/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface InvocationChannel<INPUT, OUTPUT> extends InputChannel<INPUT> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> after(@Nonnull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> after(long delay, @Nonnull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> now();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> pass(@Nullable OutputChannel<? extends INPUT> channel);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> pass(@Nullable Iterable<? extends INPUT> inputs);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> pass(@Nullable INPUT input);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationChannel<INPUT, OUTPUT> pass(@Nullable INPUT... inputs);

    /**
     * Closes the input channel and returns the output one.
     *
     * @return the routine output channel.
     * @throws java.lang.IllegalStateException if this method has been already called.
     */
    @Nonnull
    OutputChannel<OUTPUT> result();
}
