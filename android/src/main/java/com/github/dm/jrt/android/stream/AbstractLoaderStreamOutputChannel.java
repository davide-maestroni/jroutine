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
package com.github.dm.jrt.android.stream;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.Configurable;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.stream.AbstractStreamOutputChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of a loader stream output channel.
 * <p/>
 * This class provides a default implementation of all the stream channel features. The inheriting
 * class just needs to create routine and channel instances when required.
 * <p/>
 * Created by davide-maestroni on 01/12/2016.
 *
 * @param <OUT> the output data type.
 */
public abstract class AbstractLoaderStreamOutputChannel<OUT>
        extends AbstractStreamOutputChannel<OUT>
        implements LoaderStreamOutputChannel<OUT>, Configurable<LoaderStreamOutputChannel<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamOutputChannel<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamOutputChannel<OUT>>() {

                @NotNull
                public LoaderStreamOutputChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final AbstractLoaderStreamOutputChannel<OUT> outer =
                            AbstractLoaderStreamOutputChannel.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamOutputChannel<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamOutputChannel<OUT>>() {

                @NotNull
                public LoaderStreamOutputChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final AbstractLoaderStreamOutputChannel<OUT> outer =
                            AbstractLoaderStreamOutputChannel.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamOutputChannel<OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamOutputChannel<OUT>>() {

                @NotNull
                public LoaderStreamOutputChannel<OUT> setConfiguration(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return AbstractLoaderStreamOutputChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param channel                 the wrapped output channel.
     */
    @SuppressWarnings("ConstantConditions")
    protected AbstractLoaderStreamOutputChannel(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final OutputChannel<OUT> channel) {

        super(invocationConfiguration, channel);

        if (loaderConfiguration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mStreamConfiguration = loaderConfiguration;
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        return (LoaderStreamOutputChannel<OUT>) super.afterMax(timeout);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> afterMax(final long timeout,
            @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamOutputChannel<OUT>) super.afterMax(timeout, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        return (LoaderStreamOutputChannel<OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> eventuallyAbort() {

        return (LoaderStreamOutputChannel<OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        return (LoaderStreamOutputChannel<OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> eventuallyExit() {

        return (LoaderStreamOutputChannel<OUT>) super.eventuallyExit();
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> eventuallyThrow() {

        return (LoaderStreamOutputChannel<OUT>) super.eventuallyThrow();
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> immediately() {

        return (LoaderStreamOutputChannel<OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> passTo(
            @NotNull final OutputConsumer<? super OUT> consumer) {

        return (LoaderStreamOutputChannel<OUT>) super.passTo(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> skip(final int count) {

        return (LoaderStreamOutputChannel<OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncCollect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncCollect(function);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> asyncFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamOutputChannel<OUT>) super.asyncFilter(predicate);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Void> asyncForEach(
            @NotNull final Consumer<? super OUT> consumer) {

        return (LoaderStreamOutputChannel<Void>) super.asyncForEach(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncGenerate(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncGenerate(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamOutputChannel<AFTER> asyncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamOutputChannel<AFTER>) super.asyncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamOutputChannel<Number>) super.asyncRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamOutputChannel<Number>) super.asyncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> asyncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return (LoaderStreamOutputChannel<OUT>) super.asyncReduce(function);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamOutputChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay,
                                                                     timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        return (LoaderStreamOutputChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> maxParallelInvocations(final int maxInvocations) {

        return (LoaderStreamOutputChannel<OUT>) super.maxParallelInvocations(maxInvocations);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamOutputChannel<OUT>) super.ordered(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> parallelFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamOutputChannel<OUT>) super.parallelFilter(predicate);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelGenerate(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelGenerate(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamOutputChannel<AFTER> parallelRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamOutputChannel<AFTER>) super.parallelRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamOutputChannel<Number>) super.parallelRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamOutputChannel<Number>) super.parallelRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> runOn(@Nullable final Runner runner) {

        return (LoaderStreamOutputChannel<OUT>) super.runOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> runOnShared() {

        return (LoaderStreamOutputChannel<OUT>) super.runOnShared();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncCollect(
            @NotNull final BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncCollect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncCollect(
            @NotNull final Function<? super List<? extends OUT>, ? extends AFTER> function) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncCollect(function);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> syncFilter(
            @NotNull final Predicate<? super OUT> predicate) {

        return (LoaderStreamOutputChannel<OUT>) super.syncFilter(predicate);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Void> syncForEach(
            @NotNull final Consumer<? super OUT> consumer) {

        return (LoaderStreamOutputChannel<Void>) super.syncForEach(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncGenerate(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncGenerate(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncGenerate(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncGenerate(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncGenerate(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncGenerate(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncLift(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncLift(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncMap(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncMap(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncMap(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncMap(routine);
    }

    @NotNull
    @Override
    public <AFTER extends Comparable<AFTER>> LoaderStreamOutputChannel<AFTER> syncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return (LoaderStreamOutputChannel<AFTER>) super.syncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end) {

        return (LoaderStreamOutputChannel<Number>) super.syncRange(start, end);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment) {

        return (LoaderStreamOutputChannel<Number>) super.syncRange(start, end, increment);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> syncReduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return (LoaderStreamOutputChannel<OUT>) super.syncReduce(function);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        return (LoaderStreamOutputChannel<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> tryCatch(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return (LoaderStreamOutputChannel<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamOutputChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return (LoaderStreamOutputChannel<OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>>
    withInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamOutputChannel<OUT>>(
                mInvocationConfigurable, getConfiguration());
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>>
    withStreamInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamOutputChannel<OUT>>(
                mStreamInvocationConfigurable, getStreamConfiguration());
    }

    @NotNull
    @Override
    protected <AFTER> LoaderStreamOutputChannel<AFTER> newChannel(
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
    public LoaderStreamOutputChannel<OUT> loadersId(final int loaderId) {

        return withLoaders().withId(loaderId).set();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>> withLoaders() {

        final LoaderConfiguration configuration = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamOutputChannel<OUT>>(this, configuration);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>>
    withStreamLoaders() {

        final LoaderConfiguration configuration = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamOutputChannel<OUT>>(mStreamConfigurable,
                                                                               configuration);
    }

    /**
     * Creates a new channel instance.
     *
     * @param invocationConfiguration the stream invocation configuration.
     * @param loaderConfiguration     the stream loader configuration.
     * @param channel                 the wrapped output channel.
     * @param <AFTER>                 the concatenation output type.
     * @return the newly created channel instance.
     */
    @NotNull
    protected abstract <AFTER> LoaderStreamOutputChannel<AFTER> newChannel(
            @NotNull InvocationConfiguration invocationConfiguration,
            @NotNull LoaderConfiguration loaderConfiguration,
            @NotNull OutputChannel<AFTER> channel);

    /**
     * Creates a new routine instance based on the specified factory.
     *
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the loader configuration.
     * @param factory                 the invocation factory.
     * @param <AFTER>                 the concatenation output type.
     * @return the newly created routine instance.
     */
    @NotNull
    protected abstract <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull InvocationConfiguration invocationConfiguration,
            @NotNull LoaderConfiguration loaderConfiguration,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderStreamOutputChannel<OUT> setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
