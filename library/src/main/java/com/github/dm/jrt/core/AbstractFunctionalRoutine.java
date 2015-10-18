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
public abstract class AbstractFunctionalRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
        implements FunctionalRoutine<IN, OUT>, Configurable<FunctionalRoutine<IN, OUT>> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     */
    protected AbstractFunctionalRoutine(@NotNull final InvocationConfiguration configuration) {

        super(configuration);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> andThenAccumulateAsync(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return andThenAccumulate(function, DelegationType.ASYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> andThenAccumulateSync(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return andThenAccumulate(function, DelegationType.SYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> andThenFilterAsync(
            @NotNull final Predicate<? super OUT> predicate) {

        return andThenFilter(predicate, DelegationType.ASYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> andThenFilterParallel(
            @NotNull final Predicate<? super OUT> predicate) {

        return andThenFilter(predicate, DelegationType.PARALLEL);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> andThenFilterSync(
            @NotNull final Predicate<? super OUT> predicate) {

        return andThenFilter(predicate, DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return andThenMapAsync(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return andThenMapAsync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull final Function<? super OUT, AFTER> function) {

        return andThenMapAsync(Functions.functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return andThenMapParallel(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return andThenMapParallel(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull final Function<? super OUT, AFTER> function) {

        return andThenMapParallel(Functions.functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return andThenMapSync(Functions.consumerFilter(consumer));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return andThenMapSync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull final Function<? super OUT, AFTER> function) {

        return andThenMapSync(Functions.functionFilter(function));
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceAsync(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return andThen(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceAsync(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return andThen(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceParallel(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return andThen(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceParallel(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return andThen(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceSync(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return andThen(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceSync(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return andThen(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.SYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> composeAccumulateAsync(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return composeAccumulate(function, DelegationType.ASYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> composeAccumulateSync(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return composeAccumulate(function, DelegationType.SYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> composeFilterAsync(
            @NotNull final Predicate<? super IN> predicate) {

        return composeFilter(predicate, DelegationType.ASYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> composeFilterParallel(
            @NotNull final Predicate<? super IN> predicate) {

        return composeFilter(predicate, DelegationType.PARALLEL);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> composeFilterSync(
            @NotNull final Predicate<? super IN> predicate) {

        return composeFilter(predicate, DelegationType.SYNC);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull final BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer) {

        return composeMapAsync(JRoutine.on(Functions.consumerFilter(consumer))
                                       .invocations()
                                       .with(mConfiguration)
                                       .set()
                                       .buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull final FilterInvocation<BEFORE, ? extends IN> invocation) {

        return composeMapAsync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull final Function<BEFORE, ? extends IN> function) {

        return composeMapAsync(JRoutine.on(Functions.functionFilter(function))
                                       .invocations()
                                       .with(mConfiguration)
                                       .set()
                                       .buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull final Routine<BEFORE, ? extends IN> routine) {

        return compose(routine, DelegationType.ASYNC);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull final BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer) {

        return composeMapParallel(JRoutine.on(Functions.consumerFilter(consumer))
                                          .invocations()
                                          .with(mConfiguration)
                                          .set()
                                          .buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull final FilterInvocation<BEFORE, ? extends IN> invocation) {

        return composeMapParallel(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull final Function<BEFORE, ? extends IN> function) {

        return composeMapParallel(JRoutine.on(Functions.functionFilter(function))
                                          .invocations()
                                          .with(mConfiguration)
                                          .set()
                                          .buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull final Routine<BEFORE, ? extends IN> routine) {

        return compose(routine, DelegationType.PARALLEL);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull final BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer) {

        return composeMapSync(JRoutine.on(Functions.consumerFilter(consumer))
                                      .invocations()
                                      .with(mConfiguration)
                                      .set()
                                      .buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull final FilterInvocation<BEFORE, ? extends IN> invocation) {

        return composeMapSync(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull final Function<BEFORE, ? extends IN> function) {

        return composeMapSync(JRoutine.on(Functions.functionFilter(function))
                                      .invocations()
                                      .with(mConfiguration)
                                      .set()
                                      .buildRoutine());
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull final Routine<BEFORE, ? extends IN> routine) {

        return compose(routine, DelegationType.SYNC);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceAsync(
            @NotNull final BiConsumer<? super List<? extends BEFORE>, ? super ResultChannel<IN>>
                    consumer) {

        return compose(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.ASYNC);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceAsync(
            @NotNull final Function<? super List<? extends BEFORE>, ? extends IN> function) {

        return compose(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.ASYNC);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceParallel(
            @NotNull final BiConsumer<? super List<? extends BEFORE>, ? super ResultChannel<IN>>
                    consumer) {

        return compose(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.PARALLEL);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceParallel(
            @NotNull final Function<? super List<? extends BEFORE>, ? extends IN> function) {

        return compose(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.PARALLEL);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceSync(
            @NotNull final BiConsumer<? super List<? extends BEFORE>, ? super ResultChannel<IN>>
                    consumer) {

        return compose(JRoutine.on(Functions.consumerFactory(consumer))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.SYNC);
    }

    @NotNull
    public <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceSync(
            @NotNull final Function<? super List<? extends BEFORE>, ? extends IN> function) {

        return compose(JRoutine.on(Functions.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), DelegationType.SYNC);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> liftAsync(
            @NotNull final Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function) {

        return lift(function, DelegationType.ASYNC);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> liftParallel(
            @NotNull final Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function) {

        return lift(function, DelegationType.PARALLEL);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> liftSync(
            @NotNull final Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function) {

        return lift(function, DelegationType.SYNC);
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

    @NotNull
    protected abstract <BEFORE> FunctionalRoutine<BEFORE, OUT> compose(
            @NotNull Routine<BEFORE, ? extends IN> routine, @NotNull DelegationType delegationType);

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
            @NotNull final Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function,
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
    private FunctionalRoutine<IN, OUT> andThenFilter(@NotNull Predicate<? super OUT> predicate,
            @NotNull DelegationType delegationType) {

        return andThen(JRoutine.on(Functions.predicateFilter(predicate))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), delegationType);
    }

    @NotNull
    private FunctionalRoutine<IN, OUT> composeAccumulate(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function,
            @NotNull final DelegationType delegationType) {

        return compose(JRoutine.on(AccumulateInvocation.functionFactory(function))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), delegationType);
    }

    @NotNull
    private FunctionalRoutine<IN, OUT> composeFilter(@NotNull final Predicate<? super IN> predicate,
            @NotNull final DelegationType delegationType) {

        return compose(JRoutine.on(Functions.predicateFilter(predicate))
                               .invocations()
                               .with(mConfiguration)
                               .set()
                               .buildRoutine(), delegationType);
    }

}
