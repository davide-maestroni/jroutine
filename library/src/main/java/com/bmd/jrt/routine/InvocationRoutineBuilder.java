/**
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
package com.bmd.jrt.routine;

import com.bmd.jrt.builder.RoutineChannelBuilder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.invocation.InvocationFactory;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class InvocationRoutineBuilder<INPUT, OUTPUT> implements RoutineChannelBuilder {

    private final RoutineConfigurationBuilder mBuilder;

    private final InvocationFactory<INPUT, OUTPUT> mFactory;

    /**
     * Constructor.
     *
     * @param factory the invocation factory.
     * @throws java.lang.NullPointerException if the class token is null.
     */
    @SuppressWarnings("ConstantConditions")
    InvocationRoutineBuilder(@Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

        if (factory == null) {

            throw new NullPointerException("the invocation factory must not be null");
        }

        mFactory = factory;
        mBuilder = new RoutineConfigurationBuilder();
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> apply(
            @Nonnull final RoutineConfiguration configuration) {

        mBuilder.apply(configuration);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> onReadTimeout(
            @Nullable final TimeoutAction action) {

        mBuilder.onReadTimeout(action);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withAvailableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.withAvailableTimeout(timeout, timeUnit);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withAvailableTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.withAvailableTimeout(timeout);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withCoreInvocations(final int coreInvocations) {

        mBuilder.withCoreInvocations(coreInvocations);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withLog(@Nullable final Log log) {

        mBuilder.withLog(log);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withLogLevel(@Nullable final LogLevel level) {

        mBuilder.withLogLevel(level);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withMaxInvocations(final int maxInvocations) {

        mBuilder.withMaxInvocations(maxInvocations);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withReadTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.withReadTimeout(timeout, timeUnit);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withReadTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.withReadTimeout(timeout);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withRunner(@Nullable final Runner runner) {

        mBuilder.withRunner(runner);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withSyncRunner(@Nullable final RunnerType type) {

        mBuilder.withSyncRunner(type);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withInputOrder(@Nullable final OrderBy order) {

        mBuilder.withInputOrder(order);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withInputSize(final int inputMaxSize) {

        mBuilder.withInputSize(inputMaxSize);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withInputTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.withInputTimeout(timeout);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withInputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.withInputTimeout(timeout, timeUnit);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withOutputOrder(@Nullable final OrderBy order) {

        mBuilder.withOutputOrder(order);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withOutputSize(final int outputMaxSize) {

        mBuilder.withOutputSize(outputMaxSize);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withOutputTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.withOutputTimeout(timeout);
        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withOutputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.withOutputTimeout(timeout, timeUnit);
        return this;
    }

    /**
     * Builds and returns the routine.
     *
     * @return the newly created routine instance.
     */
    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new DefaultRoutine<INPUT, OUTPUT>(mBuilder.buildConfiguration(), mFactory);
    }
}
