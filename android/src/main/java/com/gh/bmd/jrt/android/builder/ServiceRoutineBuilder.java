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
package com.gh.bmd.jrt.android.builder;

import android.os.Looper;

import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.runner.Runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routine objects based on an invocation class token.
 * <p/>
 * The context of the invocations will be the specific service instance.
 * <p/>
 * Created by davide on 3/7/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface ServiceRoutineBuilder<INPUT, OUTPUT> extends RoutineBuilder<INPUT, OUTPUT> {

    /**
     * Sets the looper on which the results from the service are dispatched. A null value means that
     * results will be dispatched on the invocation thread.
     *
     * @param looper the looper instance.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> dispatchingOn(@Nullable Looper looper);

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
            @Nullable RoutineConfiguration configuration);

    /**
     * Sets the log class. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param logClass the log class.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withLogClass(
            @Nullable Class<? extends Log> logClass);

    /**
     * Sets the runner class. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param runnerClass the runner class.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withRunnerClass(
            @Nullable Class<? extends Runner> runnerClass);

    /**
     * Sets the class of the service executing the built routine. A null value means that it is up
     * to the framework to chose the default service class.
     *
     * @param serviceClass the service class.
     * @return this builder.
     */
    @Nonnull
    public ServiceRoutineBuilder<INPUT, OUTPUT> withServiceClass(
            @Nullable Class<? extends RoutineService> serviceClass);
}
