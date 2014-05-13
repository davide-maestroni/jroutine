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

import android.os.Looper;

import com.bmd.wtf.flw.Flow;

/**
 * Utility class for {@link com.bmd.wtf.flw.Flow} instances, employing specific Android classes.
 * <p/>
 * Created by davide on 3/5/14.
 */
public class AndroidFlows {

    private static LooperFlow sMain;

    /**
     * Avoid direct instantiation.
     */
    private AndroidFlows() {

    }

    /**
     * Creates a flow running in the current thread looper.
     *
     * @return The newly created flow.
     */
    public static Flow currentThread() {

        return new LooperFlow(Looper.myLooper());
    }

    /**
     * Creates a flow running in the specified looper.
     *
     * @return The newly created flow.
     */
    public static Flow looper(final Looper looper) {

        return new LooperFlow(looper);
    }

    /**
     * Returns the default flow implementation running in the main looper.
     *
     * @return The default flow instance.
     */
    public static Flow mainLooper() {

        if (sMain == null) {

            sMain = new LooperFlow(Looper.getMainLooper());
        }

        return sMain;
    }
}