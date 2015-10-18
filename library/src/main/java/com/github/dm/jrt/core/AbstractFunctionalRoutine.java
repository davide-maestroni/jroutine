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

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.functional.BiConsumer;
import com.github.dm.jrt.functional.BiFunction;
import com.github.dm.jrt.functional.Function;
import com.github.dm.jrt.functional.Functions;
import com.github.dm.jrt.functional.Predicate;
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.routine.FunctionalRoutine;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by davide-maestroni on 10/16/2015.
 */
abstract class AbstractFunctionalRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
        implements FunctionalRoutine<IN, OUT>, Configurable<FunctionalRoutine<IN, OUT>> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     */
    AbstractFunctionalRoutine(@NotNull final InvocationConfiguration configuration) {

        super(configuration);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> asyncAccumulate(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return andThenAccumulate(function, DelegationType.ASYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> asyncFilter(@NotNull final Predicate<? super OUT> predicate) {

        return andThenFilter(predicate, DelegationType.ASYNC);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> asyncLift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function) {

        return lift(function, DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return asyncMap(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return asyncMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return asyncMap(Functions.functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return asyncMap(JRoutine.on(Functions.consumerFactory(consumer))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return asyncMap(JRoutine.on(Functions.functionFactory(function))
                                .invocations()
                                .with(mConfiguration)
                                .set()
                                .buildRoutine());
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> lift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends
                    FunctionalRoutine<BEFORE, AFTER>> function) {

        return function.apply(this);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> parallelFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return andThenFilter(predicate, DelegationType.PARALLEL);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> parallelLift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function) {

        return lift(function, DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return parallelMap(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return parallelMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return parallelMap(Functions.functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> parallelReduce(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return parallelMap(JRoutine.on(Functions.consumerFactory(consumer))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> parallelReduce(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return parallelMap(JRoutine.on(Functions.functionFactory(function))
                                   .invocations()
                                   .with(mConfiguration)
                                   .set()
                                   .buildRoutine());
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> syncAccumulate(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return andThenAccumulate(function, DelegationType.SYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> syncFilter(@NotNull final Predicate<? super OUT> predicate) {

        return andThenFilter(predicate, DelegationType.SYNC);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> syncLift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function) {

        return lift(function, DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return syncMap(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return syncMap(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return syncMap(Functions.functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return syncMap(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return syncMap(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine());
    }

    @NotNull
    public Builder<? extends FunctionalRoutine<IN, OUT>> invocations() {

        return new Builder<FunctionalRoutine<IN, OUT>>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public FunctionalRoutine<IN, OUT> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }

    @NotNull
    protected abstract <AFTER> FunctionalRoutine<IN, AFTER> andThen(
            @NotNull Routine<? super OUT, AFTER> routine, @NotNull DelegationType delegationType);

    /**
     * Returns the builder invocation configuration.
     *
     * @return the invocation configuration.
     */
    @NotNull
    protected InvocationConfiguration getBuilderConfiguration() {

        return mConfiguration;
    }

    @NotNull
    protected abstract <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> lift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function,
            @NotNull final DelegationType delegationType);

    @NotNull
    private FunctionalRoutine<IN, OUT> andThenAccumulate(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function,
            @NotNull final DelegationType delegationType) {

        return andThen(JRoutine.on(AccumulateInvocation.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), delegationType);
    }

    @NotNull
    private FunctionalRoutine<IN, OUT> andThenFilter(
            @NotNull final Predicate<? super OUT> predicate,
            @NotNull final DelegationType delegationType) {

        return andThen(JRoutine.on(Functions.predicateFilter(predicate))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), delegationType);
    }
}
