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

package com.github.dm.jrt.android.core.builder;

import com.github.dm.jrt.android.core.config.ServiceConfigurable;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routine objects executed in a dedicated Service.
 * <br>
 * Note that the configuration of the maximum number of concurrent invocations will not be shared
 * among synchronous and asynchronous invocations, but the invocations created inside the Service
 * and the synchronous ones will respect the same limit separately.
 * <br>
 * Note also that, it is responsibility of the caller to ensure that the started invocations have
 * completed or have been aborted when the relative Context (for example the Activity) is destroyed,
 * so to avoid the leak of IPC connections.
 * <p>
 * The local Context of the invocations will be the specific Service instance.
 * <p>
 * Created by davide-maestroni on 03/07/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface ServiceRoutineBuilder<IN, OUT>
        extends RoutineBuilder<IN, OUT>, ServiceConfigurable<ServiceRoutineBuilder<IN, OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    ServiceRoutineBuilder<IN, OUT> apply(@NotNull InvocationConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    Builder<? extends ServiceRoutineBuilder<IN, OUT>> applyInvocationConfiguration();
}
