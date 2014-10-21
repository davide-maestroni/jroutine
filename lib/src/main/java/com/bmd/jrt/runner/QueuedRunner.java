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
package com.bmd.jrt.runner;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Class implementing a queued synchronous runner.
 * <p/>
 * The runner maintains an internal buffer of executions that are consumed only when the last one
 * completes, thus avoiding overflowing the call stack because of nested calls to other routines.
 * <br/>
 * While it is more memory and CPU consuming than the sequential implementation, it avoids
 * overflows of the call stack, and tries to prevent blocking the execution of the calling thread
 * by reordering delayed executions inside the queue.
 * <p/>
 * Created by davide on 9/18/14.
 *
 * @see SequentialRunner
 */
class QueuedRunner implements Runner {

    @Override
    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        LocalQueue.run(execution, delay, timeUnit);
    }
}