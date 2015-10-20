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

import com.github.dm.jrt.builder.FunctionalRoutineBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.functional.BiConsumer;
import com.github.dm.jrt.functional.BiFunction;
import com.github.dm.jrt.functional.Function;
import com.github.dm.jrt.functional.Functions;
import com.github.dm.jrt.functional.Predicate;
import com.github.dm.jrt.functional.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.routine.FunctionalRoutine;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Default implementation of a functional routine builder.
 * <p/>
 * Created by davide-maestroni on 10/18/2015.
 */
class DefaultFunctionalRoutineBuilder
        implements FunctionalRoutineBuilder, Configurable<FunctionalRoutineBuilder> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> accumulate(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return map(JRoutine.on(AccumulateInvocation.functionFactory(function))
                           .invocations()
                           .with(mConfiguration)
                           .set()
                           .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> filter(@NotNull final Predicate<? super IN> predicate) {

        return map(JRoutine.on(Functions.predicateFilter(predicate))
                           .invocations()
                           .with(mConfiguration)
                           .set()
                           .buildRoutine());
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> from(
            @NotNull final CommandInvocation<OUT> invocation) {

        return map(JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> from(@NotNull final Supplier<OUT> supplier) {

        return from(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> map(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return map(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> map(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return map(JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> map(@NotNull final Function<IN, OUT> function) {

        return map(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> map(@NotNull final Routine<IN, OUT> routine) {

        return new DefaultFunctionalRoutine<IN, OUT>(mConfiguration, routine);
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduce(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return map(JRoutine.on(Functions.consumerFactory(consumer))
                           .invocations()
                           .with(mConfiguration)
                           .set()
                           .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduce(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return map(JRoutine.on(Functions.functionFactory(function))
                           .invocations()
                           .with(mConfiguration)
                           .set()
                           .buildRoutine());
    }

    @NotNull
    public Builder<? extends FunctionalRoutineBuilder> invocations() {

        return new Builder<FunctionalRoutineBuilder>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public FunctionalRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
