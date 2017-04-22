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

package com.github.dm.jrt.android.core.executor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Utility class for creating and sharing executor instances, employing specific Android classes.
 * <p>
 * Created by davide-maestroni on 09/28/2014.
 */
@SuppressWarnings("WeakerAccess")
public class AndroidExecutors {

  private static final ScheduledExecutor sMainExecutor = new MainExecutor();

  /**
   * Avoid explicit instantiation.
   */
  protected AndroidExecutors() {
    ConstantConditions.avoid();
  }

  /**
   * Returns an executor employing the specified Handler.
   *
   * @param handler the Handler.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor handlerExecutor(@NotNull final Handler handler) {
    return new HandlerExecutor(handler);
  }

  /**
   * Returns an executor employing the specified Handler thread.
   *
   * @param thread the thread.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor handlerExecutor(@NotNull final HandlerThread thread) {
    if (!thread.isAlive()) {
      thread.start();
    }

    return looperExecutor(thread.getLooper());
  }

  /**
   * Returns an executor employing the specified Looper.
   *
   * @param looper the Looper instance.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor looperExecutor(@NotNull final Looper looper) {
    return new HandlerExecutor(new Handler(looper));
  }

  /**
   * Returns the shared executor employing the main thread Looper.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor mainExecutor() {
    return sMainExecutor;
  }

  /**
   * Returns an executor employing the calling thread Looper.
   *
   * @return the executor instance.
   */
  @NotNull
  @SuppressWarnings("ConstantConditions")
  public static ScheduledExecutor myExecutor() {
    return looperExecutor(Looper.myLooper());
  }

  /**
   * Returns an executor employing async tasks.
   * <p>
   * Beware of the caveats of using
   * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AsyncTask</a>s
   * especially on some platform versions.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor taskExecutor() {
    return taskExecutor(null);
  }

  /**
   * Returns an executor employing async tasks running on the specified {@code Executor}.
   * <p>
   * Beware of the caveats of using
   * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AsyncTask</a>s
   * especially on some platform versions.
   * <p>
   * Note also that the executor instance will be ignored on platforms with API level lower than
   * {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
   *
   * @param executor the executor.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor taskExecutor(@Nullable final Executor executor) {
    return new AsyncTaskExecutor(executor);
  }
}
