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
package com.github.dm.jrt.android.v11.stream;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.v11.core.JRoutine;
import com.github.dm.jrt.android.v11.core.JRoutine.ContextBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
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
public class DefaultLoaderStreamChannel<OUT> extends AbstractStreamChannel<OUT>
        implements LoaderStreamChannel<OUT>, Configurable<LoaderStreamChannel<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final DefaultLoaderStreamChannel<OUT> outer = DefaultLoaderStreamChannel.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final DefaultLoaderStreamChannel<OUT> outer = DefaultLoaderStreamChannel.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ContextBuilder mContextBuilder;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamChannel<OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return DefaultLoaderStreamChannel.this;
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
    DefaultLoaderStreamChannel(@Nullable final ContextBuilder builder,
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
    DefaultLoaderStreamChannel(@Nullable final ContextBuilder builder,
            @NotNull final OutputChannel<OUT> channel) {

        this(builder, InvocationConfiguration.DEFAULT_CONFIGURATION,
             LoaderConfiguration.DEFAULT_CONFIGURATION, channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        return (LoaderStreamChannel<OUT>) super.afterMax(timeout);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannel<OUT>) super.afterMax(timeout, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        return (LoaderStreamChannel<OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyAbort() {

        return (LoaderStreamChannel<OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        return (LoaderStreamChannel<OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyExit() {

        return (LoaderStreamChannel<OUT>) super.eventuallyExit();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyThrow() {

        return (LoaderStreamChannel<OUT>) super.eventuallyThrow();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> immediately() {

        return (LoaderStreamChannel<OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> passTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        return (LoaderStreamChannel<OUT>) super.passTo(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> skip(final int count) {

        return (LoaderStreamChannel<OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return (LoaderStreamChannel<AFTER>) super.asyncCollect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return (LoaderStreamChannel<AFTER>) super.asyncCollect(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> asyncFilter(@NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamChannel<OUT>) super.asyncFilter(predicate);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Void> asyncForEach(@NotNull final Consumer<? super OUT> consumer) {

        return (LoaderStreamChannel<Void>) super.asyncForEach(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannel<AFTER>) super.asyncGenerate(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannel<AFTER>) super.asyncGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannel<AFTER>) super.asyncGenerate(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamChannel<AFTER>) super.asyncLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannel<AFTER>) super.asyncMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamChannel<AFTER>) super.asyncMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamChannel<AFTER>) super.asyncMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> asyncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannel<AFTER>) super.asyncMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamChannel<AFTER> asyncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamChannel<AFTER>) super.asyncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamChannel<Number>) super.asyncRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamChannel<Number>) super.asyncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> asyncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return (LoaderStreamChannel<OUT>) super.asyncReduce(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay,
                                                               timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        return (LoaderStreamChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> maxParallelInvocations(final int maxInvocations) {

        return (LoaderStreamChannel<OUT>) super.maxParallelInvocations(maxInvocations);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamChannel<OUT>) super.ordered(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> parallelFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamChannel<OUT>) super.parallelFilter(predicate);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelGenerate(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannel<AFTER>) super.parallelGenerate(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannel<AFTER>) super.parallelGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamChannel<AFTER>) super.parallelLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannel<AFTER>) super.parallelMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamChannel<AFTER>) super.parallelMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamChannel<AFTER>) super.parallelMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> parallelMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannel<AFTER>) super.parallelMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamChannel<AFTER> parallelRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamChannel<AFTER>) super.parallelRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamChannel<Number>) super.parallelRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamChannel<Number>) super.parallelRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> runOn(@Nullable final Runner runner) {

        return (LoaderStreamChannel<OUT>) super.runOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> runOnShared() {

        return (LoaderStreamChannel<OUT>) super.runOnShared();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return (LoaderStreamChannel<AFTER>) super.syncCollect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return (LoaderStreamChannel<AFTER>) super.syncCollect(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> syncFilter(@NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamChannel<OUT>) super.syncFilter(predicate);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Void> syncForEach(@NotNull final Consumer<? super OUT> consumer) {

        return (LoaderStreamChannel<Void>) super.syncForEach(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannel<AFTER>) super.syncGenerate(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannel<AFTER>) super.syncGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamChannel<AFTER>) super.syncGenerate(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamChannel<AFTER>) super.syncLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamChannel<AFTER>) super.syncMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamChannel<AFTER>) super.syncMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamChannel<AFTER>) super.syncMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> syncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannel<AFTER>) super.syncMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamChannel<AFTER> syncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamChannel<AFTER>) super.syncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamChannel<Number>) super.syncRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamChannel<Number>) super.syncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> syncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return (LoaderStreamChannel<OUT>) super.syncReduce(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        return (LoaderStreamChannel<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryCatch(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return (LoaderStreamChannel<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return (LoaderStreamChannel<OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mInvocationConfigurable, getConfiguration());
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    withStreamInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mStreamInvocationConfigurable, getStreamConfiguration());
    }

    @NotNull
    @Override
    protected <AFTER> LoaderStreamChannel<AFTER> newChannel(
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
    public LoaderStreamChannel<OUT> loadersId(final int loaderId) {

        return withLoaders().withId(loaderId).set();
    }

    @NotNull
    public LoaderStreamChannel<OUT> with(@Nullable final LoaderContext context) {

        mContextBuilder = (context != null) ? JRoutine.with(context) : null;
        return this;
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withLoaders() {

        final LoaderConfiguration configuration = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(this, configuration);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withStreamLoaders() {

        final LoaderConfiguration configuration = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(mStreamConfigurable,
                                                                         configuration);
    }

    @NotNull
    private <AFTER> LoaderStreamChannel<AFTER> newChannel(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final OutputChannel<AFTER> channel) {

        return new DefaultLoaderStreamChannel<AFTER>(mContextBuilder, invocationConfiguration,
                                                     loaderConfiguration, channel);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final ContextBuilder contextBuilder = mContextBuilder;

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
    public LoaderStreamChannel<OUT> setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
