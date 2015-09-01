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
package com.github.dm.jrt.runner;

import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Class implementing a sequential synchronous runner.
 * <p/>
 * The runner simply runs the executions as soon as they are invoked.<br/>
 * While it is less memory and CPU consuming than the queued implementation, it might greatly
 * increase the depth of the call stack, and blocks execution of the calling thread during delayed
 * executions.
 * <p/>
 * Created by davide-maestroni on 09/09/14.
 *
 * @see com.github.dm.jrt.runner.QueuedRunner QueuedRunner
 */
class SequentialRunner implements Runner {

    public void cancel(@Nonnull final Execution execution) {

    }

    public boolean isExecutionThread() {

        return true;
    }

    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        try {

            if (delay != 0) {

                TimeDuration.fromUnit(delay, timeUnit).sleepAtLeast();
            }

            execution.run();

        } catch (final InterruptedException e) {

            throw new InvocationInterruptedException(e);
        }
    }
}
