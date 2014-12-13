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
package com.bmd.jrt.android.runner;

import android.os.HandlerThread;
import android.os.Looper;

import com.bmd.jrt.runner.Runner;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for creating and sharing runner instances, employing specific Android classes.
 * <p/>
 * Created by davide on 9/28/14.
 */
public class Runners extends com.bmd.jrt.runner.Runners {

    /**
     * Returns a runner employing in the specified looper.
     *
     * @param looper           the looper instance.
     * @param sameThreadRunner the runner to be used when the specified looper is called on its own
     *                         thread. If null, the invocation will be posted on the specified
     *                         looper.
     * @return the runner instance.
     * @throws NullPointerException if the specified looper is null.
     */
    @Nonnull
    public static Runner looperRunner(@Nonnull final Looper looper,
            @Nullable final Runner sameThreadRunner) {

        return new LooperRunner(looper, sameThreadRunner);
    }

    /**
     * Returns a runner employing the main thread looper.
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
     * Returns a runner employing the calling thread looper.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner myRunner() {

        return looperRunner(Looper.myLooper(), queuedRunner());
    }

    /**
     * Returns a runner employing default async tasks.
     * <p/>
     * Beware of the caveats of using
     * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AyncTask<a/>s
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
     * @throws NullPointerException if the specified thread is null.
     */
    @Nonnull
    public static Runner threadRunner(@Nonnull final HandlerThread thread) {

        if (!thread.isAlive()) {

            thread.start();
        }

        return looperRunner(thread.getLooper(), null);
    }
}