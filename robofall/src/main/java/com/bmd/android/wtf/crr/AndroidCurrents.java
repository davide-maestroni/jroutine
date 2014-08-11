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
package com.bmd.android.wtf.crr;

import android.os.HandlerThread;
import android.os.Looper;

import com.bmd.wtf.crr.Current;

import java.util.concurrent.Executor;

/**
 * Utility class for {@link com.bmd.wtf.crr.Current} instances, employing specific Android classes.
 * <p/>
 * Created by davide on 3/5/14.
 */
public class AndroidCurrents {

    private static LooperCurrent sMain;

    /**
     * Avoid direct instantiation.
     */
    private AndroidCurrents() {

    }

    /**
     * Creates a current running inside async tasks employing the specified executor.
     *
     * @param executor the executor.
     * @return the newly created current.
     */
    public static Current currentFrom(final Executor executor) {

        return new AsyncTaskCurrent(executor);
    }

    /**
     * Creates a current running in the specified handler thread.
     * <p/>
     * Note that the thread might be started as a result of this call.
     *
     * @param thread the handler thread instance.
     * @return the newly created current.
     */
    public static Current currentFrom(final HandlerThread thread) {

        if (!thread.isAlive()) {

            thread.start();
        }

        return new LooperCurrent(thread.getLooper());
    }

    /**
     * Creates a current running in the specified looper.
     *
     * @param looper the looper instance.
     * @return the newly created current.
     */
    public static Current currentFrom(final Looper looper) {

        return new LooperCurrent(looper);
    }

    /**
     * Creates a current running inside async tasks.
     * <p/>
     * Beware of the caveats of using
     * <a href="http://developer.android.com/reference/android/os/AsyncTask.html">AyncTask<a/>s
     * especially on some platform versions.
     *
     * @return the newly created current.
     */
    public static Current currentFromAsync() {

        return new AsyncTaskCurrent(null);
    }

    /**
     * Returns the default current implementation running in the main looper.
     *
     * @return the default current instance.
     */
    public static Current mainLooperCurrent() {

        if (sMain == null) {

            sMain = new LooperCurrent(Looper.getMainLooper());
        }

        return sMain;
    }

    /**
     * Creates a current running in the calling thread looper.
     *
     * @return the newly created current.
     */
    public static Current thisLooperCurrent() {

        return new LooperCurrent(Looper.myLooper());
    }
}