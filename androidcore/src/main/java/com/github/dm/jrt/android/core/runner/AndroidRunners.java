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

package com.github.dm.jrt.android.core.runner;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Utility class for creating and sharing runner instances, employing specific Android classes.
 * <p>
 * Created by davide-maestroni on 09/28/2014.
 */
public class AndroidRunners {

    private static final Runner sMainRunner = new MainRunner();

    /**
     * Avoid explicit instantiation.
     */
    protected AndroidRunners() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a runner employing the specified handler.
     *
     * @param handler the handler.
     * @return the runner instance.
     */
    @NotNull
    public static Runner handlerRunner(@NotNull final Handler handler) {
        return new HandlerRunner(handler);
    }

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

        return looperRunner(thread.getLooper());
    }

    /**
     * Returns a runner employing the specified looper.
     *
     * @param looper the looper instance.
     * @return the runner instance.
     */
    @NotNull
    public static Runner looperRunner(@NotNull final Looper looper) {
        return new HandlerRunner(new Handler(looper));
    }

    /**
     * Returns the shared runner employing the main thread looper.
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
        return looperRunner(Looper.myLooper());
    }

    /**
     * Returns a runner employing async tasks.
     * <p>
     * Beware of the caveats of using
     * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AsyncTask</a>s
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
     * <p>
     * Beware of the caveats of using
     * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AsyncTask</a>s
     * especially on some platform versions.
     * <p>
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
