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
package com.github.dm.jrt.android.v4.stream;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.v11.core.JRoutine;
import com.github.dm.jrt.android.v4.core.JRoutineCompat;
import com.github.dm.jrt.android.v4.core.JRoutineCompat.ContextBuilderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.stream.AbstractStreamChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;

/**
 * Default implementation of a loader stream output channel.
 * <p/>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
public class DefaultLoaderStreamChannelCompat<OUT> extends AbstractStreamChannel<OUT>
        implements LoaderStreamChannelCompat<OUT>, Configurable<LoaderStreamChannelCompat<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final DefaultLoaderStreamChannelCompat<OUT> outer =
                            DefaultLoaderStreamChannelCompat.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final DefaultLoaderStreamChannelCompat<OUT> outer =
                            DefaultLoaderStreamChannelCompat.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ContextBuilderCompat mContextBuilder;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamChannelCompat<OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannelCompat<OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<OUT> setConfiguration(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return DefaultLoaderStreamChannelCompat.this;
                }
            };

    /**
     * Constructor.
     *
     * @param builder                 the context builder.
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param channel                 the wrapped output channel.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderStreamChannelCompat(@Nullable final ContextBuilderCompat builder,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final OutputChannel<OUT> channel) {

        super(invocationConfiguration, channel);

        if (loaderConfiguration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mContextBuilder = builder;
        mStreamConfiguration = loaderConfiguration;
    }

    /**
     * Constructor.
     *
     * @param builder the context builder.
     * @param channel the wrapped output channel.
     */
    DefaultLoaderStreamChannelCompat(@Nullable final ContextBuilderCompat builder,
            @NotNull final OutputChannel<OUT> channel) {

        this(builder, InvocationConfiguration.DEFAULT_CONFIGURATION,
             LoaderConfiguration.DEFAULT_CONFIGURATION, channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> afterMax(@NotNull final TimeDuration timeout) {

        return (LoaderStreamChannelCompat<OUT>) super.afterMax(timeout);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> afterMax(final long timeout,
            @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannelCompat<OUT>) super.afterMax(timeout, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        return (LoaderStreamChannelCompat<OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyAbort() {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyExit() {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyExit();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyThrow() {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyThrow();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> immediately() {

        return (LoaderStreamChannelCompat<OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> passTo(
            @NotNull final OutputConsumer<? super OUT> consumer) {

        return (LoaderStreamChannelCompat<OUT>) super.passTo(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> skip(final int count) {

        return (LoaderStreamChannelCompat<OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncCollect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncCollect(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> asyncFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamChannelCompat<OUT>) super.asyncFilter(predicate);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Void> asyncForEach(
            @NotNull final Consumer<? super OUT> consumer) {

        return (LoaderStreamChannelCompat<Void>) super.asyncForEach(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncGenerate(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncGenerate(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> asyncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamChannelCompat<AFTER> asyncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamChannelCompat<AFTER>) super.asyncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamChannelCompat<Number>) super.asyncRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamChannelCompat<Number>) super.asyncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> asyncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return (LoaderStreamChannelCompat<OUT>) super.asyncReduce(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannelCompat<OUT>) super.backPressureOn(runner, maxInputs, maxDelay,
                                                                     timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        return (LoaderStreamChannelCompat<OUT>) super.backPressureOn(runner, maxInputs, maxDelay);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> maxParallelInvocations(final int maxInvocations) {

        return (LoaderStreamChannelCompat<OUT>) super.maxParallelInvocations(maxInvocations);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamChannelCompat<OUT>) super.ordered(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> parallelFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamChannelCompat<OUT>) super.parallelFilter(predicate);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelGenerate(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelGenerate(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> parallelMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamChannelCompat<AFTER> parallelRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamChannelCompat<AFTER>) super.parallelRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamChannelCompat<Number>) super.parallelRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamChannelCompat<Number>) super.parallelRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> runOn(@Nullable final Runner runner) {

        return (LoaderStreamChannelCompat<OUT>) super.runOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> runOnShared() {

        return (LoaderStreamChannelCompat<OUT>) super.runOnShared();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncCollect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncCollect(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> syncFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamChannelCompat<OUT>) super.syncFilter(predicate);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Void> syncForEach(
            @NotNull final Consumer<? super OUT> consumer) {

        return (LoaderStreamChannelCompat<Void>) super.syncForEach(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncGenerate(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncGenerate(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> syncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamChannelCompat<AFTER> syncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamChannelCompat<AFTER>) super.syncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamChannelCompat<Number>) super.syncRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamChannelCompat<Number>) super.syncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> syncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return (LoaderStreamChannelCompat<OUT>) super.syncReduce(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        return (LoaderStreamChannelCompat<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return (LoaderStreamChannelCompat<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return (LoaderStreamChannelCompat<OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    withInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(
                mInvocationConfigurable, getConfiguration());
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    withStreamInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(
                mStreamInvocationConfigurable, getStreamConfiguration());
    }

    @NotNull
    @Override
    protected <AFTER> LoaderStreamChannelCompat<AFTER> newChannel(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final OutputChannel<AFTER> channel) {

        return newChannel(configuration, mConfiguration, channel);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(configuration,
                          mStreamConfiguration.builderFrom().with(mConfiguration).set(), factory);
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> loaderId(final int loaderId) {

        return withLoaders().withId(loaderId).set();
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> with(@Nullable final LoaderContextCompat context) {

        mContextBuilder = (context != null) ? JRoutineCompat.with(context) : null;
        return this;
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>> withLoaders() {

        final LoaderConfiguration configuration = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(this, configuration);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    withStreamLoaders() {

        final LoaderConfiguration configuration = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(mStreamConfigurable,
                                                                               configuration);
    }

    @NotNull
    private <AFTER> LoaderStreamChannelCompat<AFTER> newChannel(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final OutputChannel<AFTER> channel) {

        return new DefaultLoaderStreamChannelCompat<AFTER>(mContextBuilder, invocationConfiguration,
                                                           loaderConfiguration, channel);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final ContextBuilderCompat contextBuilder = mContextBuilder;

        if (contextBuilder == null) {

            return JRoutine.on(factory)
                           .withInvocations()
                           .with(invocationConfiguration)
                           .set()
                           .buildRoutine();
        }

        final FunctionContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutine.on(factory).buildRoutine(), factory.hashCode(),
                            DelegationType.SYNC);
        return contextBuilder.on(invocationFactory)
                             .withInvocations()
                             .with(invocationConfiguration)
                             .set()
                             .withLoaders()
                             .with(loaderConfiguration)
                             .set()
                             .buildRoutine();
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderStreamChannelCompat<OUT> setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
