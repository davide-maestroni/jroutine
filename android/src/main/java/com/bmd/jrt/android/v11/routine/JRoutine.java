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
package com.bmd.jrt.android.v11.routine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.bmd.jrt.android.invocator.RoutineInvocator;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This utility class extends the base Java routine in order to support additional paradigms
 * specific to the Android platform.<br/>
 * See {@link com.bmd.jrt.android.v4.routine.JRoutine} for pre HONEYCOMB support.
 * <p/>
 * TODO: example
 * <p/>
 * Created by davide on 12/8/14.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
                    justification = "utility class extending functionalities of another utility "
                            + "class")
@TargetApi(VERSION_CODES.HONEYCOMB)
public class JRoutine extends com.bmd.jrt.android.v4.routine.JRoutine {

    /**
     * Enables routine invocation for the specified activity.<br/>
     * This method must be called in the activity <code>onCreate()</code> method.
     *
     * @param activity the activity instance.
     */
    public static void enable(@Nonnull final Activity activity) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only with API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        LoaderInvocation.enable(activity);
    }

    /**
     * Enables routine invocation for the specified fragment.<br/>
     * This method must be called in the fragment <code>onCreate()</code> method.
     *
     * @param fragment the fragment instance.
     */
    public static void enable(@Nonnull final Fragment fragment) {

        LoaderInvocation.enable(fragment);
    }

    /**
     * Returns an invocator operating in the specified context.
     *
     * @param activity the activity instance.
     * @return the invocator instance.
     * @throws IllegalStateException if the specified activity is not enabled.
     * @throws NullPointerException if the specified activity is null.
     */
    @Nonnull
    public static RoutineInvocator in(@Nonnull final Activity activity) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only with API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        if (!LoaderInvocation.isEnabled(activity)) {

            throw new IllegalStateException(
                    "routine invocation is not enabled: be sure to call RoutineInvocator.enable"
                            + "(this) in activity onCreate() method");
        }

        return new DefaultRoutineInvocator(activity);
    }

    /**
     * Returns an invocator operating in the specified context.
     *
     * @param fragment the fragment instance.
     * @return the invocator instance.
     * @throws IllegalStateException if the specified fragment is not enabled.
     * @throws NullPointerException if the specified fragment is null.
     */
    @Nonnull
    public static RoutineInvocator in(@Nonnull final Fragment fragment) {

        if (!LoaderInvocation.isEnabled(fragment)) {

            throw new IllegalStateException(
                    "routine invocation is not enabled: be sure to call RoutineInvocator.enable"
                            + "(this) in fragment onCreate() method");
        }

        return new DefaultRoutineInvocator(fragment);
    }
}
