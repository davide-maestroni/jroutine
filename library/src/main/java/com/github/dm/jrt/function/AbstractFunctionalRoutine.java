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
package com.github.dm.jrt.function;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.predicateFilter;

/**
 * Abstract implementation of a functional routine.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
abstract class AbstractFunctionalRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
        implements FunctionalRoutine<IN, OUT>, Configurable<FunctionalRoutine<IN, OUT>> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     */
    AbstractFunctionalRoutine() {

        super(InvocationConfiguration.DEFAULT_CONFIGURATION);
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
    public FunctionalRoutine<IN, OUT> thenAsyncAccumulate(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.ASYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> thenAsyncFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return fromFactory(predicateFilter(predicate), DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return fromFactory(consumerFilter(consumer), DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return fromFactory(invocation, DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return fromFactory(functionFilter(function), DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncReduce(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return fromFactory(consumerFactory(consumer), DelegationType.ASYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenAsyncReduce(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return fromFactory(functionFactory(function), DelegationType.ASYNC);
    }

    @NotNull
    public <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> thenFlatLift(
            @NotNull final Function<? super FunctionalRoutine<IN, OUT>, ? extends
                    FunctionalRoutine<BEFORE, AFTER>> function) {

        return function.apply(this);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> thenParallelFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return fromFactory(predicateFilter(predicate), DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return fromFactory(consumerFilter(consumer), DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return fromFactory(invocation, DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return fromFactory(functionFilter(function), DelegationType.PARALLEL);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenParallelMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.PARALLEL);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> thenSyncAccumulate(
            @NotNull final BiFunction<? super OUT, ? super OUT, ?
                    extends OUT> function) {

        return fromFactory(AccumulateInvocation.functionFactory(function), DelegationType.SYNC);
    }

    @NotNull
    public FunctionalRoutine<IN, OUT> thenSyncFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return fromFactory(predicateFilter(predicate), DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return fromFactory(consumerFilter(consumer), DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
            @NotNull final FilterInvocation<? super OUT, AFTER> invocation) {

        return fromFactory(invocation, DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
            @NotNull final Function<? super OUT, AFTER> function) {

        return fromFactory(functionFilter(function), DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncMap(
            @NotNull final Routine<? super OUT, AFTER> routine) {

        return andThen(routine, DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncReduce(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return fromFactory(consumerFactory(consumer), DelegationType.SYNC);
    }

    @NotNull
    public <AFTER> FunctionalRoutine<IN, AFTER> thenSyncReduce(
            @NotNull final Function<? super List<? extends OUT>, AFTER> function) {

        return fromFactory(functionFactory(function), DelegationType.SYNC);
    }

    /**
     * Concatenates a functional routine based on the specified instance to this one.
     *
     * @param routine        the routine instance.
     * @param delegationType the delegation type.
     * @param <AFTER>        the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    protected abstract <AFTER> FunctionalRoutine<IN, AFTER> andThen(
            @NotNull Routine<? super OUT, AFTER> routine, @NotNull DelegationType delegationType);

    @NotNull
    private <AFTER> FunctionalRoutine<IN, AFTER> fromFactory(
            @NotNull final InvocationFactory<? super OUT, AFTER> factory,
            @NotNull final DelegationType delegationType) {

        return andThen(JRoutine.on(factory).invocations().with(mConfiguration).set().buildRoutine(),
                       delegationType);
    }
}
