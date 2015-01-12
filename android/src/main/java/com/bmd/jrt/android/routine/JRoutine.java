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
package com.bmd.jrt.android.routine;

import android.content.Context;

import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.common.ClassToken;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This utility class extends the base Java routine in order to support additional routine builders
 * specific to the Android platform.<br/>
 * Routine invocations created through the returned builder TODO: Parcel.writeValue() + service
 * <p/>
 * Created by davide on 1/8/15.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
                    justification = "utility class extending functionalities of another utility "
                            + "class")
public class JRoutine extends com.bmd.jrt.routine.JRoutine {

    /**
     * Returns a builder of routines running in a service based on the specified context.
     * <p/>
     * Note that, waiting for the outputs of the built routine immediately after its invocation on
     * the main thread, will result in a deadlock. In fact the routine results will be always
     * dispatched in the main UI thread.
     *
     * @param context    the routine context.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     * @throws NullPointerException if any of the specified parameters is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ServiceRoutineBuilder<INPUT, OUTPUT> onService(
            @Nonnull final Context context,
            @Nonnull ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        return new ServiceRoutineBuilder<INPUT, OUTPUT>(context, classToken);
    }
}
