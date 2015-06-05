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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.builder.LoaderChannelBuilder;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderObjectRoutineBuilder;
import com.gh.bmd.jrt.android.builder.LoaderRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.ContextInvocations;
import com.gh.bmd.jrt.util.ClassToken;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This utility class extends the base one in order to support additional routine builders specific
 * to the Android platform.<br/>
 * Routine invocations created through the returned builders can be safely restored after a change
 * in the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will be dispatched on the configured looper thread, no matter the calling one
 * was, so that, waiting for the outputs right after the routine invocation, may result in a
 * deadlock.
 * <p/>
 * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the input parameter
 * objects and the invocation factory arguments, might be employed to check for clashing of
 * invocation instances or compute the loader ID.<br/>
 * In case the caller cannot guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or an input independent clash resolution should be used to
 * avoid unexpected results.
 * <p/>
 * The routine invocations will be identified by an ID number. In case a clash is detected, that is,
 * an already running loader with the same ID exists at the time the new invocation is executed,
 * the clash is resolved based on the strategy specified through the builder. When a clash cannot be
 * resolved, for example when invocations with different implementations share the same ID, the new
 * invocation is aborted with a {@link com.gh.bmd.jrt.android.invocation.InvocationTypeException}.
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
 *                        .passTo(new TemplateOutputConsumer&lt;MyResource&gt;() {
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
 *         public class LoadResource extends TemplateContextInvocation&lt;URI, MyResource&gt; {
 *
 *             private Routine&lt;URI, MyResource&gt; mRoutine;
 *
 *             &#64;Override
 *             public void onContext(&#64;Nonnull final Context context) {
 *
 *                 super.onContext(context);
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
 * See {@link com.gh.bmd.jrt.android.v4.core.JRoutine} for support of API levels less than
 * {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
 * <p/>
 * Created by davide-maestroni on 12/8/14.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
@TargetApi(VERSION_CODES.HONEYCOMB)
public class JRoutine extends com.gh.bmd.jrt.android.core.JRoutine {

    /**
     * Returns a builder of routines bound to the specified activity, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext} as application.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param activity the invocation activity context.
     * @param target   the wrapped object class.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderObjectRoutineBuilder onActivity(@Nonnull final Activity activity,
            @Nonnull final Class<?> target) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        return new DefaultLoaderObjectRoutineBuilder(activity, target);
    }

    /**
     * Returns a builder of routines bound to the specified activity.<br/>
     * In order to prevent undesired leaks, the class of the specified factory must be static.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param activity the invocation activity context.
     * @param factory  the invocation factory.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the class of the specified factory is not
     *                                            static.
     */
    @Nonnull
    public static <INPUT, OUTPUT> LoaderRoutineBuilder<INPUT, OUTPUT> onActivity(
            @Nonnull final Activity activity,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        return new DefaultLoaderRoutineBuilder<INPUT, OUTPUT>(activity, factory);
    }

    /**
     * Returns a builder of routines bound to the specified activity.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param activity the invocation activity context.
     * @param token    the invocation class token.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine builder instance.
     */
    @Nonnull
    public static <INPUT, OUTPUT> LoaderRoutineBuilder<INPUT, OUTPUT> onActivity(
            @Nonnull final Activity activity,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> token) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        return onActivity(activity, ContextInvocations.factoryOf(token));
    }

    /**
     * Returns a builder of an output channel bound to the loader identified by the specified ID.
     * <br/>
     * If no invocation with the specified ID is running at the time of the channel creation, the
     * output will be aborted with a
     * {@link com.gh.bmd.jrt.android.invocation.MissingInvocationException}.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param activity the invocation activity context.
     * @param id       the loader ID.
     * @return the channel builder instance.
     * @throws java.lang.IllegalArgumentException if the specified loader ID is equal to AUTO.
     */
    @Nonnull
    public static LoaderChannelBuilder onActivity(@Nonnull final Activity activity, final int id) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        if (id == LoaderConfiguration.AUTO) {

            throw new IllegalArgumentException("the loader ID must not be generated");
        }

        return new DefaultLoaderChannelBuilder(activity, id);
    }

    /**
     * Returns a builder of routines bound to the specified fragment, wrapping the specified object
     * instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext} as application.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param fragment the invocation fragment context.
     * @param target   the wrapped object class.
     * @return the routine builder instance.
     */
    @Nonnull
    public static LoaderObjectRoutineBuilder onFragment(@Nonnull final Fragment fragment,
            @Nonnull final Class<?> target) {

        return new DefaultLoaderObjectRoutineBuilder(fragment, target);
    }

    /**
     * Returns a builder of routines bound to the specified fragment.<br/>
     * In order to prevent undesired leaks, the class of the specified factory must be static.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param fragment the invocation fragment context.
     * @param factory  the invocation factory.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the class of the specified factory is not
     *                                            static.
     */
    @Nonnull
    public static <INPUT, OUTPUT> LoaderRoutineBuilder<INPUT, OUTPUT> onFragment(
            @Nonnull final Fragment fragment,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

        return new DefaultLoaderRoutineBuilder<INPUT, OUTPUT>(fragment, factory);
    }

    /**
     * Returns a builder of routines bound to the specified fragment.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param fragment the invocation fragment context.
     * @param token    the invocation class token.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine builder instance.
     */
    @Nonnull
    public static <INPUT, OUTPUT> LoaderRoutineBuilder<INPUT, OUTPUT> onFragment(
            @Nonnull final Fragment fragment,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> token) {

        return onFragment(fragment, ContextInvocations.factoryOf(token));
    }

    /**
     * Returns a builder of an output channel bound to the loader identified by the specified ID.
     * <br/>
     * If no invocation with the specified ID is running at the time of the channel creation, the
     * output will be aborted with a
     * {@link com.gh.bmd.jrt.android.invocation.MissingInvocationException}.<br/>
     * Note that the built routine results will be always dispatched on the configured looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param fragment the invocation fragment context.
     * @param id       the loader ID.
     * @return the channel builder instance.
     * @throws java.lang.IllegalArgumentException if the specified loader ID is equal to AUTO.
     */
    @Nonnull
    public static LoaderChannelBuilder onFragment(@Nonnull final Fragment fragment, final int id) {

        if (id == LoaderConfiguration.AUTO) {

            throw new IllegalArgumentException("the loader ID must not be generated");
        }

        return new DefaultLoaderChannelBuilder(fragment, id);
    }
}
