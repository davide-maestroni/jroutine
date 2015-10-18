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
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.routine.FunctionalRoutine;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by davide-maestroni on 10/18/2015.
 */
class DefaultFunctionalRoutineBuilder
        implements FunctionalRoutineBuilder, Configurable<FunctionalRoutineBuilder> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> asyncAccumulate(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return asyncMap(JRoutine.on(AccumulateInvocation.functionFactory(function))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> asyncFilter(
            @NotNull final Predicate<? super IN> predicate) {

        return asyncMap(JRoutine.on(Functions.predicateFilter(predicate))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return asyncMap(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> asyncMap(
            @NotNull final CommandInvocation<OUT> invocation) {

        return asyncMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return asyncMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
            @NotNull final Function<IN, OUT> function) {

        return asyncMap(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull final Routine<IN, OUT> routine) {

        return map(routine, DelegationType.ASYNC);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> asyncMap(@NotNull final Supplier<OUT> supplier) {

        return asyncMap(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return asyncMap(JRoutine.on(Functions.consumerFactory(consumer))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return asyncMap(JRoutine.on(Functions.functionFactory(function))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> parallelFilter(
            @NotNull final Predicate<? super IN> predicate) {

        return parallelMap(JRoutine.on(Functions.predicateFilter(predicate))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return parallelMap(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> parallelMap(
            @NotNull final CommandInvocation<OUT> invocation) {

        return parallelMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return parallelMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
            @NotNull final Function<IN, OUT> function) {

        return parallelMap(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
            @NotNull final Routine<IN, OUT> routine) {

        return map(routine, DelegationType.PARALLEL);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> parallelMap(@NotNull final Supplier<OUT> supplier) {

        return parallelMap(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> parallelReduce(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return parallelMap(JRoutine.on(Functions.consumerFactory(consumer))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> parallelReduce(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return parallelMap(JRoutine.on(Functions.functionFactory(function))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> syncAccumulate(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return syncMap(JRoutine.on(AccumulateInvocation.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> syncFilter(
            @NotNull final Predicate<? super IN> predicate) {

        return syncMap(JRoutine.on(Functions.predicateFilter(predicate))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return syncMap(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> syncMap(
            @NotNull final CommandInvocation<OUT> invocation) {

        return syncMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return syncMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull final Function<IN, OUT> function) {

        return syncMap(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull final Routine<IN, OUT> routine) {

        return map(routine, DelegationType.SYNC);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> syncMap(@NotNull final Supplier<OUT> supplier) {

        return syncMap(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return syncMap(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return syncMap(JRoutine.on(Functions.functionFactory(function))
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

    @NotNull
    private <IN, OUT> FunctionalRoutine<IN, OUT> map(@NotNull final Routine<IN, OUT> routine,
            @NotNull final DelegationType delegationType) {

        return new DefaultFunctionalRoutine<IN, OUT>(mConfiguration, routine, delegationType);
    }
}
