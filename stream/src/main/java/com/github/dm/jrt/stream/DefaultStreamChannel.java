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

import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.stream.AbstractStreamChannel.Binder.binderOf;

/**
 * Default implementation of a stream output channel.
 * <p>
 * Created by davide-maestroni on 12/23/2015.
 *
 * @param <OUT> the output data type.
 */
class DefaultStreamChannel<OUT> extends AbstractStreamChannel<OUT> {

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     */
    DefaultStreamChannel(@NotNull final OutputChannel<OUT> channel) {

        this(channel, (Binder) null);
    }

    /**
     * Constructor.
     *
     * @param input  the channel returning the inputs.
     * @param output the channel consuming them.
     */
    DefaultStreamChannel(@NotNull final OutputChannel<OUT> input,
            @NotNull final IOChannel<OUT> output) {

        this(output, binderOf(input, output));
    }

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     * @param binder  the binder instance.
     */
    private DefaultStreamChannel(@NotNull final OutputChannel<OUT> channel,
            @Nullable final Binder binder) {

        super(channel, InvocationConfiguration.defaultConfiguration(), DelegationType.ASYNC,
                binder);
    }

    /**
     * Constructor.
     *
     * @param channel        the wrapped output channel.
     * @param configuration  the initial invocation configuration.
     * @param delegationType the delegation type.
     * @param binder         the binder instance.
     */
    private DefaultStreamChannel(@NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        super(channel, configuration, delegationType, binder);
    }

    @NotNull
    @Override
    protected <AFTER> StreamChannel<AFTER> newChannel(@NotNull final OutputChannel<AFTER> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        return new DefaultStreamChannel<AFTER>(channel, configuration, delegationType, binder);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return JRoutineCore.on(factory)
                           .getInvocationConfiguration()
                           .with(configuration)
                           .setConfiguration()
                           .buildRoutine();
    }
}
