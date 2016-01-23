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
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of a stream output channel.
 * <p/>
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

        this(channel, null);
    }

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     * @param bind    the binding runnable.
     */
    DefaultStreamChannel(@NotNull final OutputChannel<OUT> channel, @Nullable final Runnable bind) {

        super(channel, InvocationConfiguration.DEFAULT_CONFIGURATION, DelegationType.ASYNC, bind);
    }

    /**
     * Constructor.
     *
     * @param channel        the wrapped output channel.
     * @param configuration  the initial invocation configuration.
     * @param delegationType the delegation type.
     * @param bind           the binding runnable.
     */
    private DefaultStreamChannel(@NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final DelegationType delegationType, @Nullable final Runnable bind) {

        super(channel, configuration, delegationType, bind);
    }

    @NotNull
    @Override
    protected <AFTER> StreamChannel<AFTER> newChannel(@NotNull final OutputChannel<AFTER> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final DelegationType delegationType, @Nullable final Runnable bind) {

        return new DefaultStreamChannel<AFTER>(channel, configuration, delegationType, bind);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return JRoutine.on(factory).withInvocations().with(configuration).set().buildRoutine();
    }
}
