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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of a stream output channel.
 * <p>
 * Created by davide-maestroni on 12/23/2015.
 *
 * @param <OUT> the output data type.
 */
class DefaultStreamChannel<IN, OUT> extends AbstractStreamChannel<IN, OUT> {

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     * @param invoke  the invoke function.
     */
    DefaultStreamChannel(@NotNull final OutputChannel<IN> channel,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> invoke) {

        super(InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC, channel,
                invoke);
    }

    /**
     * Constructor.
     *
     * @param configuration  the initial invocation configuration.
     * @param invocationMode the invocation mode.
     * @param sourceChannel  the source output channel.
     * @param invoke         the invoke function.
     */
    private DefaultStreamChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<IN> sourceChannel,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> invoke) {

        super(configuration, invocationMode, sourceChannel, invoke);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> StreamChannel<AFTER> newChannel(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<BEFORE> sourceChannel,
            @NotNull final Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke) {

        return new DefaultStreamChannel<BEFORE, AFTER>(streamConfiguration, invocationMode,
                sourceChannel, invoke);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return JRoutineCore.on(factory)
                           .invocationConfiguration()
                           .with(configuration)
                           .apply()
                           .buildRoutine();
    }
}
