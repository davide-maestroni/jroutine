/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.core;

import com.github.dm.jrt.android.core.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

/**
 * Utility class extending the base one in order to support additional routine builders specific to
 * the Android platform.
 * <br>
 * Routine invocations created through the returned builder will be executed inside a service
 * specified by the service context.
 * <p>
 * It is up to the caller to properly declare the service in the manifest file. Note also that it is
 * possible to manage the service lifecycle starting it through the
 * {@link android.content.Context#startService(android.content.Intent)} method. Normally the service
 * will stay active only during a routine invocation. In fact, it is responsibility of the caller
 * to ensure that the started invocations have completed or have been aborted when the relative
 * context (for example the activity) is destroyed, so to avoid the leak of IPC connections.
 * <br>
 * The service can be also made run in a different process, however, in such case, the data passed
 * through the routine input and output channels, as well as the factory arguments, must comply with
 * the {@link android.os.Parcel#writeValue(Object)} method. Be aware though, that issues may arise
 * when employing {@link java.io.Serializable} objects on some OS versions, so, it is advisable to
 * use {@link android.os.Parcelable} objects instead.
 * <p>
 * For example, in order to get a resource from the network, needed to fill an activity UI:
 * <pre>
 *     <code>
 *
 *         &#64;Override
 *         protected void onCreate(final Bundle savedInstanceState) {
 *
 *             super.onCreate(savedInstanceState);
 *             setContentView(R.layout.my_activity_layout);
 *             final Routine&lt;URI, MyResource&gt; routine =
 *                     JRoutineService.on(serviceFrom(this))
 *                                    .with(factoryOf(LoadResourceUri.class))
 *                                    .buildRoutine();
 *             routine.async(RESOURCE_URI)
 *                    .bind(new TemplateOutputConsumer&lt;MyResource&gt;() {
 *
 *                        &#64;Override
 *                        public void onError(&#64;NotNull final RoutineException error) {
 *                            displayError(error);
 *                        }
 *
 *                        &#64;Override
 *                        public void onOutput(final MyResource resource) {
 *                            displayResource(resource);
 *                        }
 *                    });
 *         }
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 01/08/2015.
 */
public class JRoutineService {

    private static final WeakHashMap<ServiceContext, ServiceBuilder> sBuilders =
            new WeakHashMap<ServiceContext, ServiceBuilder>();

    /**
     * Avoid explicit instantiation.
     */
    protected JRoutineService() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a context based builder of service routine builders.
     *
     * @param context the service context.
     * @return the context based builder.
     */
    @NotNull
    public static ServiceBuilder on(@NotNull final ServiceContext context) {
        synchronized (sBuilders) {
            final WeakHashMap<ServiceContext, ServiceBuilder> builders = sBuilders;
            ServiceBuilder builder = builders.get(context);
            if (builder == null) {
                builder = new ServiceBuilder(context);
                builders.put(context, builder);
            }

            return builder;
        }
    }

    /**
     * Context based builder of service proxy routine builders.
     */
    public static class ServiceBuilder {

        private final ServiceContext mContext;

        /**
         * Constructor.
         *
         * @param context the service context.
         */
        private ServiceBuilder(@NotNull final ServiceContext context) {
            mContext = ConstantConditions.notNull("service context", context);
        }

        /**
         * Returns a builder of routines running in a service based on the builder context.
         * <br>
         * In order to customize the invocation creation, the caller must override the method
         * {@link com.github.dm.jrt.android.core.service.InvocationService#getInvocationFactory(
         *Class, Object...) getInvocationFactory(Class, Object...)} of the routine service.
         * <p>
         * Note that the built routine results will be dispatched into the configured looper, thus,
         * waiting for the outputs on the very same looper thread, immediately after its invocation,
         * will result in a deadlock. By default output results are dispatched in the main looper.
         *
         * @param target the invocation target.
         * @param <IN>   the input data type.
         * @param <OUT>  the output data type.
         * @return the routine builder instance.
         */
        @NotNull
        public <IN, OUT> ServiceRoutineBuilder<IN, OUT> with(
                @NotNull final TargetInvocationFactory<IN, OUT> target) {
            return new DefaultServiceRoutineBuilder<IN, OUT>(mContext, target);
        }
    }

    static {
        // Sets the Android log as default
        Logger.setDefaultLog(AndroidLogs.androidLog());
    }
}
