package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.function.Functions.decorate;

/**
 * Created by davide-maestroni on 07/06/2016.
 */
// TODO: 7/7/16 everything
public class Transformations {

    protected Transformations() {
        ConstantConditions.avoid();
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> backoffOn(
            @Nullable final Runner runner, final int limit, @NotNull final Backoff backoff) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.invocationConfiguration()
                              .withRunner(runner)
                              .withInputLimit(limit)
                              .withInputBackoff(backoff)
                              .applied();
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> backoffOn(
            @Nullable final Runner runner, final int limit, final long delay,
            @NotNull final TimeUnit timeUnit) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.invocationConfiguration()
                              .withRunner(runner)
                              .withInputLimit(limit)
                              .withInputBackoff(delay, timeUnit)
                              .applied();
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> backoffOn(
            @Nullable final Runner runner, final int limit, @Nullable final UnitDuration delay) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.invocationConfiguration()
                              .withRunner(runner)
                              .withInputLimit(limit)
                              .withInputBackoff(delay)
                              .applied();
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> delay(
            final long delay, @NotNull final TimeUnit timeUnit) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindDelay<OUT>(
                                        streamConfiguration.asChannelConfiguration(), delay,
                                        timeUnit));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> delay(
            @NotNull final UnitDuration delay) {
        return delay(delay.value, delay.unit);
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> lag(
            final long delay, @NotNull final TimeUnit timeUnit) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).<Channel<?, IN>>compose(new BindDelay<IN>(
                                        streamConfiguration.asChannelConfiguration(), delay,
                                        timeUnit));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> lag(
            @NotNull final UnitDuration delay) {
        return lag(delay.value, delay.unit);
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, Void>> onComplete(
            @NotNull final Action completeAction) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, Void>>() {

            public StreamBuilder<IN, Void> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, Void>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, Void>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindCompleteConsumer<OUT>(
                                        streamConfiguration.asChannelConfiguration(),
                                        completeAction));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return tryCatchWith(new TryCatchBiConsumerConsumer<OUT>(errorConsumer));
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, Void>> onOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, Void>>() {

            public StreamBuilder<IN, Void> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, Void>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, Void>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindOutputConsumer<OUT>(
                                        streamConfiguration.asChannelConfiguration(),
                                        outputConsumer));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> order(
            @Nullable final OrderType orderType) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.parallel()
                              .invocationConfiguration()
                              .withOutputOrder(orderType)
                              .applied();
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> parallel(
            final int maxInvocations) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.parallel()
                              .invocationConfiguration()
                              .withMaxInstances(maxInvocations)
                              .applied();
            }
        };
    }

    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallel(
            final int count, @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>() {

            public StreamBuilder<IN, AFTER> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, AFTER>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, AFTER>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindParallelCount<OUT, AFTER>(
                                        streamConfiguration.asChannelConfiguration(), count,
                                        routine, streamConfiguration.getInvocationMode()));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallel(
            final int count, @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, builder.buildRoutine());
    }

    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>() {

            public StreamBuilder<IN, AFTER> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, AFTER>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, AFTER>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindParallelKey<OUT, AFTER>(
                                        streamConfiguration.asChannelConfiguration(), keyFunction,
                                        routine, streamConfiguration.getInvocationMode()));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallelBy(keyFunction, builder.buildRoutine());
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> retry(
            final int count) {
        return retry(count, Backoffs.zeroDelay());
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> retry(
            final int count, @NotNull final Backoff backoff) {
        return retry(new RetryBackoff(count, backoff));
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            @SuppressWarnings("unchecked")
                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return new BindRetry<IN, OUT>(
                                        streamConfiguration.asChannelConfiguration(),
                                        (Function<Channel<?, IN>, Channel<?, OUT>>) function,
                                        backoffFunction);
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return tryCatchWith(new TryCatchBiConsumerFunction<OUT>(catchFunction));
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> tryCatchWith(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            @SuppressWarnings("unchecked")
                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindTryCatch<OUT>(
                                        streamConfiguration.asChannelConfiguration(),
                                        catchConsumer));
                            }
                        });
            }
        };
    }

    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> tryFinally(
            @NotNull final Action finallyAction) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            @SuppressWarnings("unchecked")
                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            OUT>> function) {
                                return decorate(function).andThen(new BindTryFinally<OUT>(
                                        streamConfiguration.asChannelConfiguration(),
                                        finallyAction));
                            }
                        });
            }
        };
    }
}
