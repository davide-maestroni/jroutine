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
import com.bmd.jrt.builder.DefaultConfigurationBuilder;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
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

    private final DefaultConfigurationBuilder mBuilder;

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
     * @throws NullPointerException if the class token is null.
     */
    ServiceRoutineBuilder(@Nonnull final Context context,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        mContext = context;
        mClassToken = classToken;
        mBuilder = new DefaultConfigurationBuilder();
    }

    /**
     * Sets the timeout for an invocation instance to become available.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws NullPointerException     if the specified time unit is null.
     * @throws IllegalArgumentException if the specified timeout is negative.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.availableTimeout(timeout, timeUnit);
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
    public ServiceRoutineBuilder<INPUT, OUTPUT> availableTimeout(
            @Nullable final TimeDuration timeout) {

        mBuilder.availableTimeout(timeout);
        return this;
    }

    /**
     * Builds and returns the routine.
     *
     * @return the newly created routine instance.
     */
    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        final RoutineConfiguration configuration = mBuilder.buildConfiguration();
        return new ServiceRoutine<INPUT, OUTPUT>(mContext, mServiceClass, mLooper, mClassToken,
                                                 configuration, mRunnerClass, mLogClass);
    }

    /**
     * Sets the order in which input data are collected from the input channel.
     *
     * @param order the order type.
     * @return this builder.
     * @throws NullPointerException if the specified order type is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> inputOrder(@Nonnull final DataOrder order) {

        mBuilder.inputOrder(order);
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
    public ServiceRoutineBuilder<INPUT, OUTPUT> logClass(
            @Nullable final Class<? extends Log> logClass) {

        mLogClass = logClass;
        return this;
    }

    /**
     * Sets the log level.
     *
     * @param level the log level.
     * @return this builder.
     * @throws NullPointerException if the log level is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> logLevel(@Nonnull final LogLevel level) {

        mBuilder.logLevel(level);
        return this;
    }

    /**
     * Sets the max number of retained instances. A DEFAULT value means that it is up to the
     * framework to chose a default number.
     *
     * @param maxRetainedInstances the max number of instances.
     * @return this builder.
     * @throws IllegalArgumentException if the number is negative.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> maxRetained(final int maxRetainedInstances) {

        mBuilder.maxRetained(maxRetainedInstances);
        return this;
    }

    /**
     * Sets the max number of concurrently running instances.A DEFAULT value means that it is up
     * to the framework to chose a default number.
     *
     * @param maxRunningInstances the max number of instances.
     * @return this builder.
     * @throws IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> maxRunning(final int maxRunningInstances) {

        mBuilder.maxRunning(maxRunningInstances);
        return this;
    }

    /**
     * Sets the order in which output data are collected from the result channel.
     *
     * @param order the order type.
     * @return this builder.
     * @throws NullPointerException if the specified order type is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> outputOrder(@Nonnull final DataOrder order) {

        mBuilder.outputOrder(order);
        return this;
    }

    /**
     * Sets the looper on which the results from the service are dispatched. A null value means that
     * results will be dispatched on the invocation thread.
     *
     * @param looper the looper instance.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> resultOn(@Nullable final Looper looper) {

        mLooper = looper;
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
    public ServiceRoutineBuilder<INPUT, OUTPUT> runnerClass(
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
    public ServiceRoutineBuilder<INPUT, OUTPUT> serviceClass(
            @Nullable final Class<? extends RoutineService> serviceClass) {

        mServiceClass = serviceClass;
        return this;
    }


    /**
     * Sets the type of the synchronous runner to be used by the routine.
     *
     * @param type the runner type.
     * @return this builder.
     * @throws NullPointerException if the specified type is null.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> syncRunner(@Nonnull RunnerType type) {

        mBuilder.syncRunner(type);
        return this;
    }
}
