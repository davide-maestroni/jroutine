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
package com.gh.bmd.jrt.runner;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * The runner interface defines an object responsible for executing routine invocations inside
 * specifically managed threads.
 * <p/>
 * The implementation can both be synchronous or asynchronous, it can allocate specific threads or
 * share a pool of them between different instances.<br/>
 * The only requirements is that the specified execution is called each time a run method is
 * invoked.<br/>
 * Note also that the runner methods can be called from different threads, so, it is up to the
 * implementing class to ensure synchronization when needed.
 * <p/>
 * Created by davide-maestroni on 9/7/14.
 */
public interface Runner {

    /**
     * Checks if the calling thread belongs to the runner ones.<br/>
     * A synchronous runner will always return <code>false</code>.
     *
     * @return whether the calling thread is managed by the runner.
     */
    boolean isRunnerThread();

    /**
     * Runs the specified execution (that is, it calls the execution <b><code>run()</code></b>
     * method inside the runner thread).
     *
     * @param execution the execution.
     * @param delay     the execution delay.
     * @param timeUnit  the delay time unit.
     */
    void run(@Nonnull Execution execution, long delay, @Nonnull TimeUnit timeUnit);
}
