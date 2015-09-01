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

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * The runner interface defines an object responsible for executing routine invocations inside
 * specifically managed threads.
 * <p/>
 * The implementation can both be synchronous or asynchronous, it can allocate specific threads or
 * share a pool of them between different instances.<br/>
 * The only requirement is that the specified execution is called each time a run method is invoked.
 * <br/>
 * Note that, a proper asynchronous runner implementation will never synchronously run an execution,
 * no matter the delay, unless it employs a single thread. While, a proper synchronous runner, will
 * always run executions in the very same caller thread.
 * <br/>
 * Note also that the runner methods can be called from different threads, so, it is up to the
 * implementing class to ensure synchronization when required.
 * <p/>
 * The implementing class can optionally support the cancellation of executions not yet run (
 * waiting, for example, in a consuming queue).
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 */
public interface Runner {

    /**
     * Cancels the specified execution if not already run.<br/>
     * Note that the method will have no effect in case the runner does not maintain a queue or the
     * specified execution has been already processed at the moment of the call.<br/>
     * Note also that, in case the same execution has been added more than one time to the runner
     * queue, when the method returns, the queue will not contain the execution instance anymore,
     * with the consequence that the {@link Execution#run()} method will never be called.
     * <p/>
     * The implementation of this method is optional, still, it may greatly increase the performance
     * by avoiding to start invocations which are already canceled. The specific implementation can
     * safely ignore all that executions whose method {@link Execution#mayBeCanceled()} returns
     * false.
     *
     * @param execution the execution.
     */
    void cancel(@Nonnull Execution execution);

    /**
     * Checks if the calling thread belongs to the ones employed by the object to run executions.
     * <p/>
     * The implementation of this method is not strictly mandatory, even if, the classes always
     * returning false effectively prevent the correct detection of possible deadlocks.<br/>
     * A synchronous runner implementation will always return true.
     *
     * @return whether the calling thread is managed by the runner.
     */
    boolean isExecutionThread();

    /**
     * Runs the specified execution (that is, it calls the {@link Execution#run()} method inside the
     * runner thread).
     *
     * @param execution the execution.
     * @param delay     the execution delay.
     * @param timeUnit  the delay time unit.
     */
    void run(@Nonnull Execution execution, long delay, @Nonnull TimeUnit timeUnit);
}
