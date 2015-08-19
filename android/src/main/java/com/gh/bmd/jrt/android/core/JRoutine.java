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

import com.gh.bmd.jrt.android.builder.ServiceObjectRoutineBuilder;
import com.gh.bmd.jrt.android.builder.ServiceRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.log.Logs;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.util.ClassToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This utility class extends the base one in order to support additional routine builders specific
 * to the Android platform.<br/>
 * Routine invocations created through the returned builder will be executed inside a service
 * specified by the service context. Be aware, though, that the invocation results will be
 * dispatched into the configured looper, so that, waiting for the outputs on the very same looper
 * thread, immediately after its invocation, will result in a deadlock.<br/>
 * By default output results are dispatched in the main looper.<br/>
 * Note that the configuration of the maximum number of concurrent invocations will not be shared
 * among synchronous and asynchronous invocations, but the invocations created inside the service
 * and the synchronous will respect the same limit separately.
 * <p/>
 * It is up to the caller to properly declare the service in the manifest file. Note also that it is
 * possible to manage the service lifecycle starting it through the
 * {@link android.content.Context#startService(android.content.Intent)} method. Normally the service
 * will stay active only during a routine invocation. In fact, it is responsibility of the caller
 * to ensure that the started invocations have completed or have been aborted when the relative
 * context (for example the activity) is destroyed, so to avoid the leak of IPC connections.<br/>
 * The service can be also made run in a different process, however, in such case, the data passed
 * through the routine input and output channels, as well as the factory arguments, must comply with
 * the {@link android.os.Parcel#writeValue(Object)} method. Be aware though, that issues may arise
 * when employing {@link java.io.Serializable} objects on some OS versions, so, it is advisable to
 * use {@link android.os.Parcelable} objects instead.
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
 *                     JRoutine.on(serviceFrom(this), tokenOf(LoadResourceUri.class))
 *                             .buildRoutine();
 *             routine.asyncCall(RESOURCE_URI)
 *                    .passTo(new TemplateOutputConsumer&lt;MyResource&gt;() {
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
 * Created by davide-maestroni on 1/8/15.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class JRoutine extends com.gh.bmd.jrt.core.JRoutine {

    /**
     * Returns a builder of routines running in a service based on the specified context, wrapping
     * the specified object instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext FactoryContext} as the invocation
     * service.
     * <p/>
     * Note that the built routine results will be dispatched into the configured looper, thus,
     * waiting for the outputs on the very same looper thread, immediately after its invocation,
     * will result in a deadlock.<br/>
     * By default output results are dispatched in the main looper.
     *
     * @param context     the service context.
     * @param targetClass the wrapped object class.
     * @return the routine builder instance.
     */
    @Nonnull
    public static ServiceObjectRoutineBuilder on(@Nonnull final ServiceContext context,
            @Nonnull final Class<?> targetClass) {

        return on(context, targetClass, (Object[]) null);
    }

    /**
     * Returns a builder of routines running in a service based on the specified context, wrapping
     * the specified object instances.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.gh.bmd.jrt.android.builder.FactoryContext FactoryContext} as the invocation
     * service.
     * <p/>
     * Note that the built routine results will be dispatched into the configured looper, thus,
     * waiting for the outputs on the very same looper thread, immediately after its invocation,
     * will result in a deadlock.<br/>
     * By default output results are dispatched in the main looper.
     *
     * @param context     the service context.
     * @param targetClass the wrapped object class.
     * @param factoryArgs the object factory arguments.
     * @return the routine builder instance.
     */
    @Nonnull
    public static ServiceObjectRoutineBuilder on(@Nonnull final ServiceContext context,
            @Nonnull final Class<?> targetClass, @Nullable final Object... factoryArgs) {

        return new DefaultServiceObjectRoutineBuilder(context, targetClass, factoryArgs);
    }

    /**
     * Returns a builder of routines running in a service based on the specified context.<br/>
     * In order to customize the invocation creation, the caller must override the method
     * {@link com.gh.bmd.jrt.android.service.RoutineService#getInvocationFactory(Class, Object[])
     * getInvocationFactory(Class, Object[])} of the routine service.
     * <p/>
     * Note that the built routine results will be dispatched into the configured looper, thus,
     * waiting for the outputs on the very same looper thread, immediately after its invocation,
     * will result in a deadlock.<br/>
     * By default output results are dispatched in the main looper.
     *
     * @param context    the service context.
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine builder instance.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ServiceRoutineBuilder<INPUT, OUTPUT> on(
            @Nonnull final ServiceContext context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken) {

        return on(context, classToken, (Object[]) null);
    }

    /**
     * Returns a builder of routines running in a service based on the specified context.<br/>
     * In order to customize the invocation creation, the caller must override the method
     * {@link com.gh.bmd.jrt.android.service.RoutineService#getInvocationFactory(Class, Object[])
     * getInvocationFactory(Class, Object[])} of the routine service.
     * <p/>
     * Note that the built routine results will be dispatched into the configured looper, thus,
     * waiting for the outputs on the very same looper thread, immediately after its invocation,
     * will result in a deadlock.<br/>
     * By default output results are dispatched in the main looper.
     *
     * @param context     the service context.
     * @param classToken  the invocation class token.
     * @param factoryArgs the invocation factory arguments.
     * @param <INPUT>     the input data type.
     * @param <OUTPUT>    the output data type.
     * @return the routine builder instance.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ServiceRoutineBuilder<INPUT, OUTPUT> on(
            @Nonnull final ServiceContext context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken,
            @Nullable final Object... factoryArgs) {

        return new DefaultServiceRoutineBuilder<INPUT, OUTPUT>(context, classToken, factoryArgs);
    }

    static {

        // Sets the Android log as default
        Logger.setDefaultLog(Logs.androidLog());
    }
}
