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
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.runner.Runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TODO
 * <p/>
 * Created by davide on 3/29/15.
 */
public interface ServiceObjectRoutineBuilder extends ServiceBuilder, ObjectRoutineBuilder {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceObjectRoutineBuilder dispatchingOn(@Nullable Looper looper);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceObjectRoutineBuilder withLogClass(@Nullable Class<? extends Log> logClass);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceObjectRoutineBuilder withRunnerClass(@Nullable Class<? extends Runner> runnerClass);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceObjectRoutineBuilder withServiceClass(
            @Nullable Class<? extends RoutineService> serviceClass);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceObjectRoutineBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceObjectRoutineBuilder withShareGroup(@Nullable String group);
}
