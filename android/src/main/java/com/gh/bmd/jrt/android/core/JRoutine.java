/*
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
package com.gh.bmd.jrt.android.core;

import android.content.Context;

import com.gh.bmd.jrt.android.builder.InvocationServiceRoutineBuilder;
import com.gh.bmd.jrt.android.builder.ObjectServiceRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.common.ClassToken;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This utility class extends the base Java routine in order to support additional routine builders
 * specific to the Android platform.<br/>
 * Routine invocations created through the returned builder will be execute inside a service
 * specified through the routine builder. Be aware, though, that the invocation results will be
 * dispatched in the specified looper, so that, waiting for the outputs right after the routine
 * invocation in the looper thread, will result in a deadlock.
 * <p/>
 * Note that it is up to the caller to properly declare the service in the manifest file. Note also
 * that it is possible to manage the service lifecycle starting it through the
 * {@link Context#startService(android.content.Intent)} method. Normally the service will stay
 * active only during a routine invocation.<br/>
 * The service can be also made run in a different process, however, in such case, the data passed
 * through the routine input and output channels must comply with the
 * {@link android.os.Parcel#writeValue(Object)} method. Be aware though, that issues may arise when
 * employing {@link java.io.Serializable} objects on the Lollipop OS version, so, it is advisable
 * to use {@link android.os.Parcelable} objects instead.
 * <p/>
 * For example, in order to get a resource from the network, needed to fill an activity UI:
 * <pre>
 *     <code>
 *
 *         &#64;Override
 *         protected void onCreate(final Bundle savedInstanceState) {
 *
 *             super.onCreate(savedInstanceState);
 *             setContentView(R.layout.my_activity_layout);
 *
 *             final Routine&lt;URI, MyResource&gt; routine =
 *                     JRoutine.onService(this, ClassToken.tokenOf(LoadResourceUri.class))
 *                             .buildRoutine();
 *             routine.callAsync(RESOURCE_URI)
 *                    .bind(new TemplateOutputConsumer&lt;MyResource&gt;() {
 *
 *                        &#64;Override
 *                        public void onError(&#64;Nullable final Throwable error) {
 *
 *                            displayError(error);
 *                        }
 *
 *                        &#64;Override
 *                        public void onOutput(final MyResource resource) {
 *
 *                            displayResource(resource);
 *                        }
 *                    });
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 1/8/15.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending functionalities of another utility class")
public class JRoutine extends com.gh.bmd.jrt.core.JRoutine {

    /**
     * Returns a builder of routines running in a service based on the specified context.
     * <p/>
     * Note that the built routine results will be dispatched in the looper specified through the
     * builder, thus, waiting for the outputs immediately after its invocation in the looper thread,
     * will result in a deadlock.
     *
     * @param context    the routine context.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if any of the specified parameters is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationServiceRoutineBuilder<INPUT, OUTPUT> onService(
            @Nonnull final Context context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken) {

        return new DefaultInvocationServiceRoutineBuilder<INPUT, OUTPUT>(context, classToken);
    }

    /**
     * Returns a builder of routines running in a service based on the specified context, wrapping
     * the specified object instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext} as the invocation service.
     *
     * @param context     the routine context.
     * @param targetClass the wrapped object class.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if any of the specified parameters is null.
     */
    @Nonnull
    public static ObjectServiceRoutineBuilder onService(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass) {

        return new DefaultObjectServiceRoutineBuilder(context, targetClass);
    }
}
