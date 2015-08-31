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
package com.github.dm.jrt.android.builder;

import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.RoutineBuilder;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routine objects executed in a dedicated service.<br/>
 * Note that the configuration of the maximum number of concurrent invocations will not be shared
 * among synchronous and asynchronous invocations, but the invocations created inside the service
 * and the synchronous will respect the same limit separately.<br/>
 * Note also that it is responsibility of the caller to ensure that the started invocations have
 * completed or have been aborted when the relative context (for example the activity) is destroyed,
 * so to avoid the leak of IPC connections.
 * <p/>
 * The local context of the invocations will be the specific service instance.
 * <p/>
 * Created by davide-maestroni on 3/7/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface ServiceRoutineBuilder<INPUT, OUTPUT> extends RoutineBuilder<INPUT, OUTPUT>,
        ServiceConfigurableBuilder<ServiceRoutineBuilder<INPUT, OUTPUT>> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    Builder<? extends ServiceRoutineBuilder<INPUT, OUTPUT>> invocations();
}
