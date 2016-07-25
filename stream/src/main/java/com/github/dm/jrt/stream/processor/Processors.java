/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.stream.processor;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
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
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Backoffs.afterCount;
import static com.github.dm.jrt.function.Functions.decorate;

/**
 * Utility class providing several transformation functions to be applied to a routine stream.
 * <p>
 * Created by davide-maestroni on 07/06/2016.
 */
public class Processors {

    /**
     * Avoid explicit instantiation.
     */
    protected Processors() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a function applying the configuration:
     * {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(backoff).configured()}.
     * <br>
     * This method is useful to easily apply a configuration which will slow down the thread
     * feeding the next routine concatenated to the stream, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner  the configured runner.
     * @param backoff the backoff policy to apply to the feeding thread.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified limit is negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> backoffOn(
            @Nullable final Runner runner, @NotNull final Backoff backoff) {
        ConstantConditions.notNull("backoff instance", backoff);
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.invocationConfiguration()
                              .withRunner(runner)
                              .withInputBackoff(backoff)
                              .configured();
            }
        };
    }

    /**
     * Returns a function applying the configuration:
     * {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(delay, timeUnit).configured()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will slow down the thread feeding it, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner   the configured runner.
     * @param limit    the maximum number of buffered inputs before starting to slow down the
     *                 feeding thread.
     * @param delay    the constant delay to apply to the feeding thread.
     * @param timeUnit the delay time unit.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified limit or the specified delay are
     *                                            negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> backoffOn(
            @Nullable final Runner runner, final int limit, final long delay,
            @NotNull final TimeUnit timeUnit) {
        ConstantConditions.notNull("time unit", timeUnit);
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.invocationConfiguration()
                              .withRunner(runner)
                              .withInputBackoff(afterCount(limit).constantDelay(delay, timeUnit))
                              .configured();
            }
        };
    }

    /**
     * Returns a function applying the configuration:
     * {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(delay).configured()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will slow down the thread feeding it, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner the configured runner.
     * @param limit  the maximum number of buffered inputs before starting to slow down the
     *               feeding thread.
     * @param delay  the constant delay to apply to the feeding thread.
     * @param <IN>   the input data type.
     * @param <OUT>  the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified limit is negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> backoffOn(
            @Nullable final Runner runner, final int limit, @NotNull final UnitDuration delay) {
        ConstantConditions.notNull("delay value", delay);
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.invocationConfiguration()
                              .withRunner(runner)
                              .withInputBackoff(afterCount(limit).constantDelay(delay))
                              .configured();
            }
        };
    }

    /**
     * Returns a function adding a delay at the end of the stream, so that any data, exception or
     * completion notification will be dispatched to the next concatenated routine after the
     * specified time.
     *
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified delay is negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> delay(
            final long delay, @NotNull final TimeUnit timeUnit) {
        ConstantConditions.notNull("time unit", timeUnit);
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

    /**
     * Returns a function adding a delay at the end of the stream, so that any data, exception or
     * completion notification will be dispatched to the next concatenated routine after the
     * specified time.
     *
     * @param delay the delay.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> delay(
            @NotNull final UnitDuration delay) {
        return delay(delay.value, delay.unit);
    }

    /**
     * Returns a function adding a delay at the beginning of the stream, so that any data, exception
     * or completion notification coming from the source will be dispatched to this stream after the
     * specified time.
     *
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified delay is negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> lag(
            final long delay, @NotNull final TimeUnit timeUnit) {
        ConstantConditions.notNull("time unit", timeUnit);
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

    /**
     * Returns a function adding a delay at the beginning of the stream, so that any data, exception
     * or completion notification coming from the source will be dispatched to this stream after the
     * specified time.
     *
     * @param delay the delay.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> lag(
            @NotNull final UnitDuration delay) {
        return lag(delay.value, delay.unit);
    }

    /**
     * Returns a function making the stream generate the specified output in place of the invocation
     * ones.
     *
     * @param output the output.
     * @param <IN>   the input data type.
     * @param <OUT>  the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> output(
            @Nullable final OUT output) {
        return output(JRoutineCore.io().of(output));
    }

    /**
     * Returns a function making the stream generate the specified outputs in place of the
     * invocation ones.
     *
     * @param outputs the outputs.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> output(
            @Nullable final OUT... outputs) {
        return output(JRoutineCore.io().of(outputs));
    }

    /**
     * Returns a function making the stream generate the outputs returned by the specified iterable
     * in place of the invocation ones.
     *
     * @param outputs the iterable returning the output data.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> output(
            @Nullable final Iterable<? extends OUT> outputs) {
        return output(JRoutineCore.io().of(outputs));
    }

    /**
     * Returns a function making the stream generate the outputs returned by the specified channel
     * in place of the invocation ones.
     *
     * @param channel the output channel.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> output(
            @Nullable final Channel<?, ? extends OUT> channel) {
        return new Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>>() {

            @SuppressWarnings("unchecked")
            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, ?> builder) {
                return ((StreamBuilder<IN, Object>) builder).liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, Object>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            Object>> function) {
                                return decorate(function).andThen(new BindOutput<OUT>(channel));
                            }
                        });
            }
        };
    }

    /**
     * Returns a function making the stream generate the outputs returned by the specified consumer
     * in place of the invocation ones.
     * <br>
     * The result channel will be passed to the consumer, so that multiple or no results may be
     * generated.
     * <br>
     * The consumer will be called {@code count} number of times. The count number must be positive.
     *
     * @param count           the number of generated outputs.
     * @param outputsConsumer the consumer instance.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> outputAccept(
            final long count, @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        ConstantConditions.positive("count number", count);
        ConstantConditions.notNull("consumer instance", outputsConsumer);
        return new Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>>() {

            @SuppressWarnings("unchecked")
            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, ?> builder) {
                return ((StreamBuilder<IN, Object>) builder).liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, Object>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            Object>> function) {
                                return decorate(function).andThen(new BindOutputConsumer<OUT>(
                                        streamConfiguration.asInvocationConfiguration(),
                                        streamConfiguration.getInvocationMode(), count,
                                        outputsConsumer));
                            }
                        });
            }
        };
    }

    /**
     * Returns a function making the stream generate the outputs returned by the specified consumer
     * in place of the invocation ones.
     * <br>
     * The result channel will be passed to the consumer, so that multiple or no results may be
     * generated.
     *
     * @param outputsConsumer the consumer instance.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> outputAccept(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return outputAccept(1, outputsConsumer);
    }

    /**
     * Returns a function making the stream generate the outputs returned by the specified supplier
     * in place of the invocation ones.
     * <br>
     * The supplier will be called {@code count} number of times. The count number must be positive.
     *
     * @param count          the number of generated outputs.
     * @param outputSupplier the supplier instance.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> outputGet(
            final long count, @NotNull final Supplier<? extends OUT> outputSupplier) {
        ConstantConditions.positive("count number", count);
        ConstantConditions.notNull("supplier instance", outputSupplier);
        return new Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>>() {

            @SuppressWarnings("unchecked")
            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, ?> builder) {
                return ((StreamBuilder<IN, Object>) builder).liftWithConfig(
                        new BiFunction<StreamConfiguration, Function<? super Channel<?, IN>, ?
                                extends Channel<?, Object>>, Function<? super Channel<?, IN>, ?
                                extends Channel<?, OUT>>>() {

                            public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                            apply(
                                    final StreamConfiguration streamConfiguration,
                                    final Function<? super Channel<?, IN>, ? extends Channel<?,
                                            Object>> function) {
                                return decorate(function).andThen(new BindOutputSupplier<OUT>(
                                        streamConfiguration.asInvocationConfiguration(),
                                        streamConfiguration.getInvocationMode(), count,
                                        outputSupplier));
                            }
                        });
            }
        };
    }

    /**
     * Returns a function making the stream generate the outputs returned by the specified supplier
     * in place of the invocation ones.
     *
     * @param outputSupplier the supplier instance.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, ?>, StreamBuilder<IN, OUT>> outputGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        return outputGet(1, outputSupplier);
    }

    /**
     * Returns a function applying the configuration:
     * {@code parallel().invocationConfiguration().withMaxInstances(maxInvocations).configured()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will limit the maximum number of concurrent invocations to the specified value.
     *
     * @param maxInvocations the maximum number of concurrent invocations.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> parallel(
            final int maxInvocations) {
        return new Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>>() {

            public StreamBuilder<IN, OUT> apply(final StreamBuilder<IN, OUT> builder) {
                return builder.parallel()
                              .invocationConfiguration()
                              .withMaxInstances(maxInvocations)
                              .configured();
            }
        };
    }

    /**
     * Returns a function splitting the outputs produced by the stream, so that each group will be
     * processed by a different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     *
     * @param count   the number of groups.
     * @param routine the processing routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @param <AFTER> the new output type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallel(
            final int count, @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        ConstantConditions.notNull("routine instance", routine);
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

    /**
     * Returns a function splitting the outputs produced by the stream, so that each group will be
     * processed by a different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     *
     * @param count   the number of groups.
     * @param builder the builder of processing routine instances.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @param <AFTER> the new output type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallel(
            final int count, @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, builder.buildRoutine());
    }

    /**
     * Returns a function splitting the outputs produced by the stream, so that each group will be
     * processed by a different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param routine     the processing routine instance
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @param <AFTER>     the new output type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        ConstantConditions.notNull("function instance", keyFunction);
        ConstantConditions.notNull("routine instance", routine);
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

    /**
     * Returns a function splitting the outputs produced by the stream, so that each group will be
     * processed by a different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param builder     the builder of processing routine instances.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @param <AFTER>     the new output type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT, AFTER> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, AFTER>>
    parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallelBy(keyFunction, builder.buildRoutine());
    }

    /**
     * Returns a function making the stream retry the whole flow of data at maximum for the
     * specified number of times.
     *
     * @param count the maximum number of retries.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> retry(
            final int count) {
        return retry(count, Backoffs.noDelay());
    }

    /**
     * Returns a function making the stream retry the whole flow of data at maximum for the
     * specified number of times.
     * <br>
     * For each retry the specified backoff policy will be applied before re-starting the flow.
     *
     * @param count   the maximum number of retries.
     * @param backoff the backoff policy.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the transformation function.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> retry(
            final int count, @NotNull final Backoff backoff) {
        return retry(new RetryBackoff(count, backoff));
    }

    /**
     * Returns a function making the stream retry the whole flow of data until the specified
     * function does not return a null value.
     * <br>
     * For each retry the function is called passing the retry count (starting from 1) and the error
     * which caused the failure. If the function returns a non-null value, it will represent the
     * number of milliseconds to wait before a further retry. While, in case the function returns
     * null, the flow of data will be aborted with the passed error as reason.
     * <p>
     * Note that no retry will be attempted in case of an explicit abortion, that is, if the error
     * is an instance of {@link com.github.dm.jrt.core.channel.AbortException}.
     *
     * @param backoffFunction the retry function.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        ConstantConditions.notNull("function instance", backoffFunction);
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

    /**
     * Returns a function concatenating to the stream a consumer handling invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     *
     * @param catchFunction the function instance.
     * @param <IN>          the input data type.
     * @param <OUT>         the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return tryCatchAccept(new TryCatchBiConsumerFunction<OUT>(catchFunction));
    }

    /**
     * Returns a function concatenating to the stream a consumer handling invocation exceptions.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The errors will not be automatically further propagated.
     *
     * @param catchConsumer the bi-consumer instance.
     * @param <IN>          the input data type.
     * @param <OUT>         the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> tryCatchAccept(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        ConstantConditions.notNull("consumer instance", catchConsumer);
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

    /**
     * Returns a function concatenating to the stream an action always performed when outputs
     * complete, even if an error occurred.
     * <br>
     * Both outputs and errors will be automatically passed on.
     *
     * @param finallyAction the action instance.
     * @param <IN>          the input data type.
     * @param <OUT>         the output data type.
     * @return the transformation function.
     */
    @NotNull
    public static <IN, OUT> Function<StreamBuilder<IN, OUT>, StreamBuilder<IN, OUT>> tryFinally(
            @NotNull final Action finallyAction) {
        ConstantConditions.notNull("action instance", finallyAction);
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
