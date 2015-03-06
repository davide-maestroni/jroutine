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
package com.gh.bmd.jrt.android.routine;

import android.content.Context;
import android.os.Looper;

import com.gh.bmd.jrt.android.invocation.AndroidInvocation;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runner;

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
public class ServiceRoutineBuilder<INPUT, OUTPUT> implements RoutineBuilder<INPUT, OUTPUT> {

    private final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> mClassToken;

    private final Context mContext;

    private RoutineConfiguration mConfiguration;

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
    }

    @Nonnull
    @Override
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new ServiceRoutine<INPUT, OUTPUT>(mContext, mServiceClass, mLooper, mClassToken,
                                                 RoutineConfiguration.notNull(mConfiguration),
                                                 mRunnerClass, mLogClass);
    }

    /**
     * Note that all the options related to the output and input channels size and timeout will be
     * ignored.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    public ServiceRoutineBuilder<INPUT, OUTPUT> withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
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
    public ServiceRoutineBuilder<INPUT, OUTPUT> dispatchingOn(@Nullable final Looper looper) {

        mLooper = looper;
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
}
