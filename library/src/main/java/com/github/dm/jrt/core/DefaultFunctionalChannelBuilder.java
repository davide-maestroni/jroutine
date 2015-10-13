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

import com.github.dm.jrt.builder.FunctionalChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.FunctionalChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.Channels.asyncStream;
import static com.github.dm.jrt.core.Channels.parallelStream;
import static com.github.dm.jrt.core.Channels.syncStream;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.supplierCommand;

/**
 * Default implementation of a functional channel builder.
 * <p/>
 * Created by davide-maestroni on 10/13/2015.
 */
class DefaultFunctionalChannelBuilder
        implements FunctionalChannelBuilder, Configurable<FunctionalChannelBuilder> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> async(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return async(consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalChannel<Void, OUT> async(
            @NotNull final CommandInvocation<OUT> invocation) {

        return new DefaultFunctionalChannel<Void, OUT>(mConfiguration, asyncStream(
                JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> async(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return new DefaultFunctionalChannel<IN, OUT>(mConfiguration, asyncStream(
                JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> async(@NotNull final Function<IN, OUT> function) {

        return async(functionFilter(function));
    }

    @NotNull
    public <OUT> FunctionalChannel<Void, OUT> async(@NotNull final Supplier<OUT> supplier) {

        return async(supplierCommand(supplier));
    }

    @NotNull
    public Builder<? extends FunctionalChannelBuilder> invocations() {

        return new Builder<FunctionalChannelBuilder>(this, mConfiguration);
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> parallel(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return parallel(consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalChannel<Void, OUT> parallel(
            @NotNull final CommandInvocation<OUT> invocation) {

        return new DefaultFunctionalChannel<Void, OUT>(mConfiguration, parallelStream(
                JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> parallel(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return new DefaultFunctionalChannel<IN, OUT>(mConfiguration, parallelStream(
                JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> parallel(
            @NotNull final Function<IN, OUT> function) {

        return parallel(functionFilter(function));
    }

    @NotNull
    public <OUT> FunctionalChannel<Void, OUT> parallel(@NotNull final Supplier<OUT> supplier) {

        return parallel(supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> sync(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return sync(consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalChannel<Void, OUT> sync(
            @NotNull final CommandInvocation<OUT> invocation) {

        return new DefaultFunctionalChannel<Void, OUT>(mConfiguration, syncStream(
                JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> sync(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return new DefaultFunctionalChannel<IN, OUT>(mConfiguration, syncStream(
                JRoutine.on(invocation).invocations().with(mConfiguration).set()));
    }

    @NotNull
    public <IN, OUT> FunctionalChannel<IN, OUT> sync(@NotNull final Function<IN, OUT> function) {

        return sync(functionFilter(function));
    }

    @NotNull
    public <OUT> FunctionalChannel<Void, OUT> sync(@NotNull final Supplier<OUT> supplier) {

        return sync(supplierCommand(supplier));
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public FunctionalChannelBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
