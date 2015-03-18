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
package com.gh.bmd.jrt.android.runner;

import android.os.HandlerThread;
import android.os.Looper;

import com.gh.bmd.jrt.runner.Runner;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for creating and sharing runner instances, employing specific Android classes.
 * <p/>
 * Created by davide on 9/28/14.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending functionalities of another utility class")
public class Runners extends com.gh.bmd.jrt.runner.Runners {

    private static volatile Runner sMainRunner;

    /**
     * Returns a runner employing the specified looper.<br/>
     * Note that, based on the choice of the runner to be used when the invocation runs in the
     * looper thread, waiting for results in the very same thread may result in a deadlock
     * exception.
     *
     * @param looper           the looper instance.
     * @param sameThreadRunner the runner to be used when the specified looper is called on its own
     *                         thread. If null, the invocation will be posted on the specified
     *                         looper.
     * @return the runner instance.
     * @throws java.lang.NullPointerException if the specified looper is null.
     */
    @Nonnull
    public static Runner looperRunner(@Nonnull final Looper looper,
            @Nullable final Runner sameThreadRunner) {

        return new LooperRunner(looper, sameThreadRunner);
    }

    /**
     * Returns a runner employing the main thread looper.<br/>
     * Note that, based on the choice of the runner to be used when the invocation runs in the
     * looper thread, waiting for results in the very same thread may result in a deadlock
     * exception.
     *
     * @param sameThreadRunner the runner to be used when the main looper is called on its own
     *                         thread. If null, the invocation will be posted on the main looper.
     * @return the runner instance.
     */
    @Nonnull
    public static Runner mainRunner(@Nullable final Runner sameThreadRunner) {

        return looperRunner(Looper.getMainLooper(), sameThreadRunner);
    }

    /**
     * Returns the shared runner employing the main thread looper.<br/>
     * Note that, when the invocation runs in the main thread, the executions with a delay of 0 will
     * be performed synchronously, while the ones with a positive delay will be posted on the UI
     * thread.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner mainRunner() {

        if (sMainRunner == null) {

            sMainRunner = new MainRunner();
        }

        return sMainRunner;
    }

    /**
     * Returns a runner employing the calling thread looper.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner myRunner() {

        return looperRunner(Looper.myLooper(), queuedRunner());
    }

    /**
     * Returns a runner employing async tasks.
     * <p/>
     * Beware of the caveats of using
     * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AsyncTask<a/>s
     * especially on some platform versions.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner taskRunner() {

        return taskRunner(null);
    }

    /**
     * Returns a runner employing async tasks running on the specified executor.
     * <p/>
     * Beware of the caveats of using
     * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AsyncTask<a/>s
     * especially on some platform versions.
     * <p/>
     * Note also that the executor instance will be ignored on platforms with API level < 11.
     *
     * @param executor the executor.
     * @return the runner instance.
     */
    @Nonnull
    public static Runner taskRunner(@Nullable final Executor executor) {

        return new AsyncTaskRunner(executor);
    }

    /**
     * Returns a runner employing the specified handler thread.
     *
     * @param thread the thread.
     * @return the runner instance.
     * @throws java.lang.NullPointerException if the specified thread is null.
     */
    @Nonnull
    public static Runner threadRunner(@Nonnull final HandlerThread thread) {

        if (!thread.isAlive()) {

            thread.start();
        }

        return looperRunner(thread.getLooper(), null);
    }
}
