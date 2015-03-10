/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.v11.routine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.builder.AndroidChannelBuilder;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.AndroidInvocation;
import com.gh.bmd.jrt.common.ClassToken;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This utility class extends the base Java routine in order to support additional routine builders
 * specific to the Android platform.<br/>
 * Routine invocations created through the returned builders can be safely restored after a change
 * in the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will always be dispatched in the main thread, no matter the calling one was,
 * so that waiting for the outputs right after the routine invocation will result in a deadlock.
 * <p/>
 * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the input parameter
 * objects might be employed to check for clashing of invocations or compute the invocation ID.<br/>
 * In case the caller cannot guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or an input independent clash resolution should be used to
 * avoid unexpected results.
 * <p/>
 * The routine invocations will be identified by an ID number. In case a clash is detected, that is,
 * an already running invocation with the same ID exists at the time the new invocation is executed,
 * the clash is resolved based on the strategy specified through the builder. When a clash cannot be
 * resolved, for example when invocations with different implementations share the same ID, the new
 * invocation is aborted with a {@link com.gh.bmd.jrt.android.builder.InvocationClashException}.
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
 *             if (savedInstanceState != null) {
 *
 *                 mResource = savedInstanceState.getParcelable(RESOURCE_KEY);
 *             }
 *
 *             if (mResource != null) {
 *
 *                 displayResource(mResource);
 *
 *             } else {
 *
 *                 final Routine&lt;URI, MyResource&gt; routine =
 *                         JRoutine.onActivity(this, ClassToken.tokenOf(LoadResource.class))
 *                                 .buildRoutine();
 *                 routine.callAsync(RESOURCE_URI)
 *                        .bind(new TemplateOutputConsumer&lt;MyResource&gt;() {
 *
 *                            &#64;Override
 *                            public void onError(&#64;Nullable final Throwable error) {
 *
 *                                displayError(error);
 *                            }
 *
 *                            &#64;Override
 *                            public void onOutput(final MyResource resource) {
 *
 *                                mResource = resource;
 *                                displayResource(resource);
 *                            }
 *                        });
 *             }
 *         }
 *
 *         &#64;Override
 *         protected void onSaveInstanceState(final Bundle outState) {
 *
 *             super.onSaveInstanceState(outState);
 *             outState.putParcelable(RESOURCE_KEY, mResource);
 *         }
 *     </code>
 * </pre>
 * The above code will ensure that the loading process survives any configuration change and the
 * resulting resource is dispatched only once.
 * <p/>
 * Note that the invocation may be implemented so to run in a separate service:
 * <pre>
 *     <code>
 *
 *         public class LoadResource extends AndroidTemplateInvocation&lt;URI, MyResource&gt; {
 *
 *             private Routine&lt;URI, MyResource&gt; mRoutine;
 *
 *             &#64;Override
 *             public void onContext(&#64;Nonnull final Context context) {
 *
 *                 super.onContext(context);
 *
 *                 mRoutine = JRoutine.onService(context, ClassToken.tokenOf(LoadResourceUri.class))
 *                                    .buildRoutine();
 *             }
 *
 *             &#64;Override
 *             public void onInput(final URI uri,
 *                     &#64;Nonnull final ResultChannel&lt;MyResource&gt; result) {
 *
 *                 result.pass(mRoutine.callAsync(uri));
 *             }
 *         }
 *     </code>
 * </pre>
 * <p/>
 * See {@link com.gh.bmd.jrt.android.v4.routine.JRoutine} for pre-HONEYCOMB support.
 * <p/>
 * Created by davide on 12/8/14.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending functionalities of another utility " + "class")
@TargetApi(VERSION_CODES.HONEYCOMB)
public class JRoutine extends com.gh.bmd.jrt.android.routine.JRoutine {

    /**
     * Returns a builder of routines bound to the specified activity.<br/>
     * Note that the specified invocation class must be static and have a default constructor.<br/>
     * Note that also the built routine results will be always dispatched in the main UI thread,
     * thus waiting for the outputs immediately after its invocation in the main thread will result
     * in a deadlock.
     *
     * @param activity   the activity instance.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the specified invocation has no default
     *                                            constructor.
     * @throws java.lang.NullPointerException     if the specified activity or class token are null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> AndroidRoutineBuilder<INPUT, OUTPUT> onActivity(
            @Nonnull final Activity activity,
            @Nonnull ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only with API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        return new DefaultAndroidRoutineBuilder<INPUT, OUTPUT>(activity, classToken);
    }

    /**
     * Returns a builder of an output channel bound to the invocation identified by the specified
     * ID.<br/>
     * Note that, if no invocation with the specified ID is running at the time of the channel
     * creation, the output will be aborted with a
     * {@link com.gh.bmd.jrt.android.builder.InvocationMissingException}.
     *
     * @param activity the invocation activity context.
     * @param id       the invocation ID.
     * @return the channel builder instance.
     * @throws java.lang.IllegalArgumentException if the specified invocation ID is equal to AUTO.
     * @throws java.lang.NullPointerException     if the specified activity is null.
     */
    @Nonnull
    public static AndroidChannelBuilder onActivity(@Nonnull final Activity activity, final int id) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only with API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        if (id == AndroidRoutineBuilder.AUTO) {

            throw new IllegalArgumentException("the invocation ID must not be generated");
        }

        return new DefaultAndroidChannelBuilder(activity, id);
    }

    /**
     * Returns a builder of routines bound to the specified fragment.<br/>
     * Note that the specified invocation class must be static and have a default constructor.<br/>
     * Note that also the built routine results will be always dispatched in the main UI thread,
     * thus waiting for the outputs immediately after its invocation in the main thread will result
     * in a deadlock.
     *
     * @param fragment   the fragment instance.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if the specified fragment or class token are null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> AndroidRoutineBuilder<INPUT, OUTPUT> onFragment(
            @Nonnull final Fragment fragment,
            @Nonnull ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> classToken) {

        return new DefaultAndroidRoutineBuilder<INPUT, OUTPUT>(fragment, classToken);
    }

    /**
     * Returns a builder of an output channel bound to the invocation identified by the specified
     * ID.<br/>
     * Note that, if no invocation with the specified ID is running at the time of the channel
     * creation, the output will be aborted with a
     * {@link com.gh.bmd.jrt.android.builder.InvocationMissingException}.
     *
     * @param fragment the invocation fragment context.
     * @param id       the invocation ID.
     * @return the channel builder instance.
     * @throws java.lang.IllegalArgumentException if the specified invocation ID is equal to AUTO.
     * @throws java.lang.NullPointerException     if the specified fragment is null.
     */
    @Nonnull
    public static AndroidChannelBuilder onFragment(@Nonnull final Fragment fragment, final int id) {

        if (id == AndroidRoutineBuilder.AUTO) {

            throw new IllegalArgumentException("the invocation ID must not be generated");
        }

        return new DefaultAndroidChannelBuilder(fragment, id);
    }
}
