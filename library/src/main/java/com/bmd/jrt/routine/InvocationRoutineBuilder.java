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
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class InvocationRoutineBuilder<INPUT, OUTPUT> implements RoutineChannelBuilder {

    private final DefaultConfigurationBuilder mBuilder;

    private final Class<? extends Invocation<INPUT, OUTPUT>> mInvocationClass;

    private Object[] mArgs = NO_ARGS;

    /**
     * Constructor.
     *
     * @param classToken the invocation class token.
     * @throws NullPointerException if the class token is null.
     */
    InvocationRoutineBuilder(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        mInvocationClass = classToken.getRawClass();
        mBuilder = new DefaultConfigurationBuilder();
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
    public InvocationRoutineBuilder<INPUT, OUTPUT> availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.availableTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> availableTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.availableTimeout(timeout);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> logLevel(@Nonnull final LogLevel level) {

        mBuilder.logLevel(level);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> loggedWith(@Nullable final Log log) {

        mBuilder.loggedWith(log);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> maxRetained(final int maxRetainedInstances) {

        mBuilder.maxRetained(maxRetainedInstances);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> maxRunning(final int maxRunningInstances) {

        mBuilder.maxRunning(maxRunningInstances);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> runBy(@Nullable final Runner runner) {

        mBuilder.runBy(runner);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> syncRunner(@Nonnull final RunnerType type) {

        mBuilder.syncRunner(type);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> inputOrder(@Nonnull final DataOrder order) {

        mBuilder.inputOrder(order);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> inputSize(final int inputMaxSize) {

        mBuilder.inputSize(inputMaxSize);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> inputTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.inputTimeout(timeout);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> inputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.inputTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> outputOrder(@Nonnull final DataOrder order) {

        mBuilder.outputOrder(order);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> outputSize(final int outputMaxSize) {

        mBuilder.outputSize(outputMaxSize);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> outputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.outputTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> outputTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.outputTimeout(timeout);

        return this;
    }

    /**
     * Builds and returns the routine instance.
     *
     * @return the newly created routine.
     */
    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        final RoutineConfiguration configuration = mBuilder.buildConfiguration();
        final Runner syncRunner = (configuration.getSyncRunner(null) == RunnerType.SEQUENTIAL)
                ? Runners.sequentialRunner() : Runners.queuedRunner();

        return new DefaultRoutine<INPUT, OUTPUT>(configuration, syncRunner, mInvocationClass,
                                                 mArgs);
    }

    /**
     * Sets the arguments to be passed to the invocation constructor.
     *
     * @param args the arguments.
     * @return this builder.
     * @throws NullPointerException if the specified arguments array is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> withArgs(@Nonnull final Object... args) {

        if (args == null) {

            throw new NullPointerException("the arguments array must not be null");
        }

        mArgs = args;

        return this;
    }
}
