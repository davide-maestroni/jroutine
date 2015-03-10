/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a configurable builder.
 * <p/>
 * The configuration has a synchronous and an asynchronous runner associated. The synchronous
 * implementation can only be chosen between queued (the default one) and sequential.<br/>
 * The queued one maintains an internal buffer of executions that are consumed only when the
 * last one completes, thus avoiding overflowing the call stack because of nested calls to other
 * routines.<br/>
 * The sequential one simply runs the executions as soon as they are invoked.<br/>
 * While the latter is less memory and CPU consuming, it might greatly increase the depth of the
 * call stack, and blocks execution of the calling thread during delayed executions.<br/>
 * In both cases the executions are run inside the calling thread.<br/>
 * The default asynchronous runner is shared among all the routines, but a custom one can be set
 * through the builder.
 * <p/>
 * Additionally, a recycling mechanism is provided so that, when an invocation successfully
 * completes, the instance is retained for future executions. Moreover, the maximum running
 * invocation instances at one time can be limited by calling the specific builder method. When the
 * limit is reached and an additional instance is requires, the call is blocked until one becomes
 * available or the timeout set through the builder elapses.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * In case the timeout elapses before an invocation instance becomes available, a
 * {@link com.gh.bmd.jrt.routine.RoutineDeadlockException} will be thrown.
 * <p/>
 * Finally, the number of input and output data buffered in the corresponding channel can be
 * limited in order to avoid excessive memory consumption. In case the maximum number is reached
 * when passing an input or output, the call blocks until enough data are consumed or the specified
 * timeout elapses. In the latter case a {@link com.gh.bmd.jrt.common.DeadlockException} will be
 * thrown.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks, and the order of input and
 * output data is not guaranteed. Nevertheless, it is possible to force data to be delivered in
 * the same order as they are passed to the channels, at the cost of a slightly increased memory
 * usage and computation, by the proper options.
 * <p/>
 * Created by davide on 3/6/15.
 */
public interface ConfigurableBuilder {

    /**
     * Sets the specified configuration to this builder by replacing any configuration already set.
     * <br/>
     * Note that the configuration options not supported by the builder implementation may be
     * ignored.
     *
     * @param configuration the configuration.
     * @return this builder.
     */
    @Nonnull
    ConfigurableBuilder withConfiguration(@Nullable RoutineConfiguration configuration);
}
