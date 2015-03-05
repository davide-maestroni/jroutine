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
package com.gh.bmd.jrt.runner;

import com.gh.bmd.jrt.common.InvocationInterruptedException;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a sequential synchronous runner.
 * <p/>
 * The runner simply runs the executions as soon as they are invoked.<br/>
 * While it is less memory and CPU consuming than the queued implementation, it might greatly
 * increase the depth of the call stack, and blocks execution of the calling thread during delayed
 * executions.
 * <p/>
 * Created by davide on 9/9/14.
 *
 * @see com.gh.bmd.jrt.runner.QueuedRunner
 */
class SequentialRunner implements Runner {

    @Override
    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        try {

            fromUnit(delay, timeUnit).sleepAtLeast();
            execution.run();

        } catch (final InterruptedException e) {

            throw InvocationInterruptedException.interrupt(e);
        }
    }
}
