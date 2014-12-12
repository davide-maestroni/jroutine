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
package com.bmd.jrt.android.v4.routine;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.bmd.jrt.android.invocator.RoutineInvocator;

import javax.annotation.Nonnull;

/**
 * This utility class extends the base Java routine in order to support additional paradigms
 * specific to the Android platform.
 * <p/>
 * TODO: example
 * <p/>
 * Created by davide on 12/8/14.
 */
public class JRoutine extends com.bmd.jrt.routine.JRoutine {

    /**
     * Returns an invocator operating in the specified context.
     *
     * @param activity the activity instance.
     * @return the invocator instance.
     * @throws NullPointerException if the specified activity is null.
     */
    @Nonnull
    public static RoutineInvocator in(@Nonnull final FragmentActivity activity) {

        return new DefaultRoutineInvocator(activity);
    }

    /**
     * Returns an invocator operating in the specified context.
     *
     * @param fragment the fragment instance.
     * @return the invocator instance.
     * @throws NullPointerException if the specified fragment is null.
     */
    @Nonnull
    public static RoutineInvocator in(@Nonnull final Fragment fragment) {

        return new DefaultRoutineInvocator(fragment);
    }
}
