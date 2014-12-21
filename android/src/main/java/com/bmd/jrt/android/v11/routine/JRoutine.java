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

import com.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.Invocation;

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
public class JRoutine extends com.bmd.jrt.routine.JRoutine {

    /**
     * Returns a builder of routines linked to the specified context.
     * <p/>
     * Note that, waiting for the outputs of the built routine immediately after its invocation on
     * the main thread, will result in a deadlock. In fact the routine results will be always
     * dispatched in the main UI thread.
     *
     * @param activity   the activity instance.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     * @throws IllegalStateException if the specified activity is not initialized.
     * @throws NullPointerException  if any of the specified parameters is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> AndroidRoutineBuilder<INPUT, OUTPUT> from(
            @Nonnull final Activity activity,
            @Nonnull ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only with API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        if (!LoaderInvocation.isEnabled(activity)) {

            throw new IllegalStateException(
                    "routine creation is not enabled: be sure to call JRoutine.initContext(this) "
                            + "in activity onCreate() method");
        }

        return new DefaultAndroidRoutineBuilder<INPUT, OUTPUT>(activity, classToken);
    }

    /**
     * Returns a builder of routines linked to the specified context.
     * <p/>
     * Note that, waiting for the outputs of the built routine immediately after its invocation on
     * the main thread, will result in a deadlock. In fact the routine results will be always
     * dispatched in the main UI thread.
     *
     * @param fragment   the fragment instance.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     * @throws IllegalStateException if the specified activity is not initialized.
     * @throws NullPointerException  if any of the specified parameters is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> AndroidRoutineBuilder<INPUT, OUTPUT> from(
            @Nonnull final Fragment fragment,
            @Nonnull ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        if (!LoaderInvocation.isEnabled(fragment)) {

            throw new IllegalStateException(
                    "routine creation is not enabled: be sure to call JRoutine.initContext(this) "
                            + "in fragment onCreate() method");
        }

        return new DefaultAndroidRoutineBuilder<INPUT, OUTPUT>(fragment, classToken);
    }

    /**
     * Initializes the specified context so to enable the creation of routines linked to the context
     * lifecycle.<br/>
     * This method must be called in the activity <code>onCreate()</code> method.
     *
     * @param activity the activity instance.
     * @throws NullPointerException if the specified activity is null.
     */
    public static void initContext(@Nonnull final Activity activity) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only with API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        LoaderInvocation.initContext(activity);
    }

    /**
     * Initializes the specified context so to enable the creation of routines linked to the context
     * lifecycle.<br/>
     * This method must be called in the fragment <code>onCreate()</code> method.
     *
     * @param fragment the fragment instance.
     * @throws NullPointerException if the specified fragment is null.
     */
    public static void initContext(@Nonnull final Fragment fragment) {

        LoaderInvocation.initContext(fragment);
    }
}
