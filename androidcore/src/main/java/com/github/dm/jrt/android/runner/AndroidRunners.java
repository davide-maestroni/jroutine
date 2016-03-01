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

package com.github.dm.jrt.android.runner;

import android.os.HandlerThread;
import android.os.Looper;

import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Utility class for creating and sharing runner instances, employing specific Android classes.
 * <p/>
 * Created by davide-maestroni on 09/28/2014.
 */
public class AndroidRunners {

    private static final Runner sMainRunner = new MainRunner();

    /**
     * Returns a runner employing the specified handler thread.
     *
     * @param thread the thread.
     * @return the runner instance.
     */
    @NotNull
    public static Runner handlerRunner(@NotNull final HandlerThread thread) {

        if (!thread.isAlive()) {
            thread.start();
        }

        return looperRunner(thread.getLooper(), Runners.syncRunner());
    }

    /**
     * Returns a runner employing the specified looper.<br/>
     * Note that, when the invocation runs in the looper thread, the executions with a delay of 0
     * will be performed synchronously, while the ones with a positive delay will be posted on the
     * same thread.
     *
     * @param looper the looper instance.
     * @return the runner instance.
     */
    @NotNull
    public static Runner looperRunner(@NotNull final Looper looper) {

        return new LooperRunner(looper);
    }

    /**
     * Returns a runner employing the specified looper.<br/>
     * Note that, based on the choice of the runner to be used when the invocation runs in the
     * looper thread, waiting for results in the very same thread may result in a deadlock
     * exception.
     *
     * @param looper           the looper instance.
     * @param sameThreadRunner the runner to be used when the specified looper is called on its own
     *                         thread. If null, the invocation will be posted on the same looper.
     * @return the runner instance.
     */
    @NotNull
    public static Runner looperRunner(@NotNull final Looper looper,
            @Nullable final Runner sameThreadRunner) {

        return new LooperRunner(looper, sameThreadRunner);
    }

    /**
     * Returns the shared runner employing the main thread looper.<br/>
     * Note that, when the invocation runs in the main thread, the executions with a delay of 0 will
     * be performed synchronously, while the ones with a positive delay will be posted on the main
     * thread.
     *
     * @return the runner instance.
     */
    @NotNull
    public static Runner mainRunner() {

        return sMainRunner;
    }

    /**
     * Returns a runner employing the calling thread looper.
     *
     * @return the runner instance.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static Runner myRunner() {

        return looperRunner(Looper.myLooper(), Runners.syncRunner());
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
    @NotNull
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
     * Note also that the executor instance will be ignored on platforms with API level lower than
     * {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
     *
     * @param executor the executor.
     * @return the runner instance.
     */
    @NotNull
    public static Runner taskRunner(@Nullable final Executor executor) {

        return new AsyncTaskRunner(executor);
    }
}
