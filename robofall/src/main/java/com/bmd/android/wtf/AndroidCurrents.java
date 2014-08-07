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
package com.bmd.android.wtf;

import android.os.HandlerThread;
import android.os.Looper;

import com.bmd.wtf.crr.Current;

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
     * Creates a current running in the specified handler thread.
     * <p/>
     * Note that the thread might be started as a result of this call.
     *
     * @param thread the handler thread instance.
     * @return the newly created current.
     */
    public static Current currentOf(final HandlerThread thread) {

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
    public static Current currentOf(final Looper looper) {

        return new LooperCurrent(looper);
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
     * Creates a current running in the current thread looper.
     *
     * @return the newly created current.
     */
    public static Current thisLooperCurrent() {

        return new LooperCurrent(Looper.myLooper());
    }
}