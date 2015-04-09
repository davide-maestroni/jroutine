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
package com.gh.bmd.jrt.android.builder;

import android.os.Looper;

import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.runner.Runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routine whose invocation runs in a dedicated service.
 * <p/>
 * The local context of the invocations will be the specific service instance.
 * <p/>
 * Created by davide on 3/29/15.
 */
public interface ServiceRoutineBuilder {

    /**
     * Sets the looper on which the results from the service are dispatched. A null value means that
     * results will be dispatched on the main thread (as by default).
     *
     * @param looper the looper instance.
     * @return this builder.
     */
    @Nonnull
    ServiceRoutineBuilder dispatchingOn(@Nullable Looper looper);

    /**
     * Sets the log class. A null value means that it is up to the framework to choose a default
     * implementation.
     *
     * @param logClass the log class.
     * @return this builder.
     */
    @Nonnull
    ServiceRoutineBuilder withLogClass(@Nullable Class<? extends Log> logClass);

    /**
     * Sets the runner class. A null value means that it is up to the framework to choose a default
     * implementation.
     *
     * @param runnerClass the runner class.
     * @return this builder.
     */
    @Nonnull
    ServiceRoutineBuilder withRunnerClass(@Nullable Class<? extends Runner> runnerClass);

    /**
     * Sets the class of the service executing the built routine. A null value means that it is up
     * to the framework to choose the default service class.
     *
     * @param serviceClass the service class.
     * @return this builder.
     */
    @Nonnull
    ServiceRoutineBuilder withServiceClass(@Nullable Class<? extends RoutineService> serviceClass);
}
