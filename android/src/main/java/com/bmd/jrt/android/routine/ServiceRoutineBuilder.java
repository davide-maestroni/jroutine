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
package com.bmd.jrt.android.routine;

import android.content.Context;
import android.os.Looper;

import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.android.service.RoutineService;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineBuilder.TimeoutAction;
import com.bmd.jrt.builder.RoutineChannelBuilder.OrderBy;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
 * <p/>
 * Created by davide on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class ServiceRoutineBuilder<INPUT, OUTPUT> {

    private final RoutineConfigurationBuilder mBuilder;

    private final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> mClassToken;

    private final Context mContext;

    private Class<? extends Log> mLogClass;

    private Looper mLooper;

    private Class<? extends Runner> mRunnerClass;

    private Class<? extends RoutineService> mServiceClass;

    /**
     * Constructor.
     *
     * @param context    the routine context.
     * @param classToken the invocation class token.
     * @throws java.lang.NullPointerException if the context or the class token are null.
     */
    @SuppressWarnings("ConstantConditions")
    ServiceRoutineBuilder(@Nonnull final Context context,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (classToken == null) {

            throw new NullPointerException("the invocation class token must not be null");
        }

        mContext = context;
        mClassToken = classToken;
        mBuilder = new RoutineConfigurationBuilder();
    }

    /**
     * Applies the specified configuration to this builder.
     *
     * @param configuration the configuration.
     * @return this builder.
     * @throws java.lang.NullPointerException if the specified configuration is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> apply(
            @Nonnull final RoutineConfiguration configuration) {

        mBuilder.apply(configuration);
        return this;
    }

    /**
     * Builds and returns the routine.
     *
     * @return the newly created routine instance.
     */
    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new ServiceRoutine<INPUT, OUTPUT>(mContext, mServiceClass, mLooper, mClassToken,
                                                 mBuilder.buildConfiguration(), mRunnerClass,
                                                 mLogClass);
    }

    /**
     * Sets the looper on which the results from the service are dispatched. A null value means that
     * results will be dispatched on the invocation thread.
     *
     * @param looper the looper instance.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> dispatchTo(@Nullable final Looper looper) {

        mLooper = looper;
        return this;
    }

    /**
     * Sets the action to be taken if the timeout elapses before a result can be read from the
     * output channel.
     *
     * @param action the action type.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> onReadTimeout(
            @Nullable final TimeoutAction action) {

        mBuilder.onReadTimeout(action);
        return this;
    }

    /**
     * Sets the timeout for an invocation instance to become available.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withAvailableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.withAvailableTimeout(timeout, timeUnit);
        return this;
    }

    /**
     * Sets the timeout for an invocation instance to become available. A null value means that
     * it is up to the framework to chose a default duration.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withAvailableTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.withAvailableTimeout(timeout);
        return this;
    }

    /**
     * Sets the max number of core invocation instances. A {@link RoutineConfiguration#DEFAULT}
     * value means that it is up to the framework to chose a default number.
     *
     * @param coreInstances the max number of instances.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is negative.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withCoreInvocations(final int coreInstances) {

        mBuilder.withCoreInvocations(coreInstances);
        return this;
    }

    /**
     * Sets the order in which input data are collected from the input channel. A null value means
     * that it is up to the framework to chose a default order type.
     *
     * @param order the order type.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withInputOrder(@Nullable final OrderBy order) {

        mBuilder.withInputOrder(order);
        return this;
    }

    /**
     * Sets the log class. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param logClass the log class.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withLogClass(
            @Nullable final Class<? extends Log> logClass) {

        mLogClass = logClass;
        return this;

    }

    /**
     * Sets the log level. A null value means that it is up to the framework to chose a default
     * level.
     *
     * @param level the log level.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withLogLevel(@Nullable final LogLevel level) {

        mBuilder.withLogLevel(level);
        return this;
    }

    /**
     * Sets the max number of concurrently running invocation instances. A
     * {@link RoutineConfiguration#DEFAULT} value means that it is up to the framework to chose a
     * default number.
     *
     * @param maxInstances the max number of instances.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withMaxInvocations(final int maxInstances) {

        mBuilder.withMaxInvocations(maxInstances);
        return this;
    }

    /**
     * Sets the order in which output data are collected from the result channel. A null value means
     * that it is up to the framework to chose a default order type.
     *
     * @param order the order type.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withOutputOrder(@Nullable final OrderBy order) {

        mBuilder.withOutputOrder(order);
        return this;
    }

    /**
     * Sets the timeout for an invocation instance to produce a readable result.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withReadTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.withReadTimeout(timeout, timeUnit);
        return this;
    }

    /**
     * Sets the timeout for an invocation instance to produce a readable result. A null value means
     * that it is up to the framework to chose a default duration.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withReadTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.withReadTimeout(timeout);
        return this;
    }

    /**
     * Sets the runner class. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param runnerClass the runner class.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withRunnerClass(
            @Nullable final Class<? extends Runner> runnerClass) {

        mRunnerClass = runnerClass;
        return this;
    }

    /**
     * Sets the class of the service executing the built routine. A null value means that it is up
     * to the framework to chose the default service class.
     *
     * @param serviceClass the service class.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withServiceClass(
            @Nullable final Class<? extends RoutineService> serviceClass) {

        mServiceClass = serviceClass;
        return this;
    }

    /**
     * Sets the type of the synchronous runner to be used by the routine. A null value means that it
     * is up to the framework to chose a default order type.
     *
     * @param type the runner type.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withSyncRunner(@Nullable RunnerType type) {

        mBuilder.withSyncRunner(type);
        return this;
    }
}
