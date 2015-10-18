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
    public <IN> FunctionalRoutine<IN, IN> accumulateAsync(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return mapAsync(JRoutine.on(AccumulateInvocation.functionFactory(function))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> accumulateSync(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return mapSync(JRoutine.on(AccumulateInvocation.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> filterAsync(
            @NotNull final Predicate<? super IN> predicate) {

        return mapAsync(JRoutine.on(Functions.predicateFilter(predicate))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> filterParallel(
            @NotNull final Predicate<? super IN> predicate) {

        return mapParallel(JRoutine.on(Functions.predicateFilter(predicate))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <IN> FunctionalRoutine<IN, IN> filterSync(
            @NotNull final Predicate<? super IN> predicate) {

        return mapSync(JRoutine.on(Functions.predicateFilter(predicate))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapAsync(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return mapAsync(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> mapAsync(
            @NotNull final CommandInvocation<OUT> invocation) {

        return mapAsync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapAsync(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return mapAsync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapAsync(
            @NotNull final Function<IN, OUT> function) {

        return mapAsync(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapAsync(@NotNull final Routine<IN, OUT> routine) {

        return map(routine, DelegationType.ASYNC);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> mapAsync(@NotNull final Supplier<OUT> supplier) {

        return mapAsync(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapParallel(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return mapParallel(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> mapParallel(
            @NotNull final CommandInvocation<OUT> invocation) {

        return mapParallel(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapParallel(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return mapParallel(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapParallel(
            @NotNull final Function<IN, OUT> function) {

        return mapParallel(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapParallel(
            @NotNull final Routine<IN, OUT> routine) {

        return map(routine, DelegationType.PARALLEL);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> mapParallel(@NotNull final Supplier<OUT> supplier) {

        return mapParallel(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapSync(
            @NotNull final BiConsumer<IN, ? super ResultChannel<OUT>> consumer) {

        return mapSync(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> mapSync(
            @NotNull final CommandInvocation<OUT> invocation) {

        return mapSync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapSync(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return mapSync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapSync(@NotNull final Function<IN, OUT> function) {

        return mapSync(Functions.functionFilter(function));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> mapSync(@NotNull final Routine<IN, OUT> routine) {

        return map(routine, DelegationType.SYNC);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> mapSync(@NotNull final Supplier<OUT> supplier) {

        return mapSync(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduceAsync(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return mapAsync(JRoutine.on(Functions.consumerFactory(consumer))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduceAsync(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return mapAsync(JRoutine.on(Functions.functionFactory(function))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduceParallel(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return mapParallel(JRoutine.on(Functions.consumerFactory(consumer))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduceParallel(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return mapParallel(JRoutine.on(Functions.functionFactory(function))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduceSync(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return mapSync(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <IN, OUT> FunctionalRoutine<IN, OUT> reduceSync(
            @NotNull final Function<? super List<? extends IN>, OUT> function) {

        return mapSync(JRoutine.on(Functions.functionFactory(function))
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
