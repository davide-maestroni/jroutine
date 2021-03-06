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

package com.github.dm.jrt.android.v4.core;

import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class supporting the creation of routine builders specific to the Android platform.
 * <br>
 * Routine invocations created through the returned builders can be safely restored after a change
 * in the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will be dispatched on the configured Looper thread, no matter the calling one
 * was, so that, waiting for the outputs on the very same Looper thread right after the routine
 * invocation, will result in a deadlock.
 * <br>
 * Note that the configuration of the maximum number of concurrent invocations might not work as
 * expected. In fact, the number of running loaders cannot be computed.
 * <br>
 * Note also that the input data will be cached, and the results will be produced only after the
 * invocation channel is closed, so be sure to avoid streaming inputs in order to prevent starvation
 * or out of memory errors.
 * <p>
 * The {@code equals()} and {@code hashCode()} methods of the input parameter objects and the
 * invocation factory, might be employed to check for clashing of invocation instances or compute
 * the Loader ID.
 * <br>
 * In case the caller could not guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or an input independent clash resolution should be used in
 * order to avoid unexpected results.
 * <p>
 * The routine invocations will be identified by the Loader ID. In case a clash is detected, that
 * is, an already running Loader with the same ID exists at the time the new invocation is executed,
 * the clash is resolved based on the strategy specified through the builder. When a clash cannot be
 * resolved, for example when loaders with different implementations share the same ID, the new
 * invocation is aborted with a
 * {@link com.github.dm.jrt.android.core.invocation.TypeClashException TypeClashException}.
 * <p>
 * For example, in order to get a resource from the network, needed to fill an Activity UI:
 * <pre>
 *     <code>
 *
 *         &#64;Override
 *         protected void onCreate(final Bundle savedInstanceState) {
 *             super.onCreate(savedInstanceState);
 *             setContentView(R.layout.my_activity_layout);
 *             if (savedInstanceState != null) {
 *                 mResource = savedInstanceState.getParcelable(RESOURCE_KEY);
 *             }
 *
 *             if (mResource != null) {
 *                 displayResource(mResource);
 *
 *             } else {
 *                 final Routine&lt;URI, MyResource&gt; routine =
 *                         JRoutineLoaderCompat.on(loaderFrom(this))
 *                                             .with(factoryOf(LoadResource.class))
 *                                             .buildRoutine();
 *                 routine.call(RESOURCE_URI)
 *                        .bind(new TemplateChannelConsumer&lt;MyResource&gt;() {
 *
 *                            &#64;Override
 *                            public void onError(&#64;NotNull final RoutineException error) {
 *                                displayError(error);
 *                            }
 *
 *                            &#64;Override
 *                            public void onOutput(final MyResource resource) {
 *                                mResource = resource;
 *                                displayResource(resource);
 *                            }
 *                        });
 *             }
 *         }
 *
 *         &#64;Override
 *         protected void onSaveInstanceState(final Bundle outState) {
 *             super.onSaveInstanceState(outState);
 *             outState.putParcelable(RESOURCE_KEY, mResource);
 *         }
 *     </code>
 * </pre>
 * The above code will ensure that the loading process survives any configuration change and the
 * resulting resource is dispatched only once.
 * <p>
 * Note that the invocation may be implemented so to run in a dedicated Service:
 * <pre>
 *     <code>
 *
 *         public class LoadResource extends TemplateContextInvocation&lt;URI, MyResource&gt; {
 *
 *             private Routine&lt;URI, MyResource&gt; mRoutine;
 *
 *             &#64;Override
 *             public void onContext(&#64;Nonnull final Context context) {
 *                 mRoutine = JRoutineService.on(serviceFrom(context))
 *                                           .with(factoryOf(LoadResourceUri.class))
 *                                           .buildRoutine();
 *             }
 *
 *             &#64;Override
 *             protected void onCall(final List&lt;? extends URI&gt; uris,
 *                     &#64;Nonnull final Channel&lt;MyResource, ?&gt; result) {
 *                 result.pass(mRoutine.call(uris));
 *             }
 *         }
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 12/08/2014.
 */
public class JRoutineLoaderCompat {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineLoaderCompat() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a Context based builder of Loader routine builders.
   *
   * @param context the Loader context.
   * @return the Context based builder.
   */
  @NotNull
  public static LoaderBuilderCompat on(@NotNull final LoaderContextCompat context) {
    return new LoaderBuilderCompat(context);
  }

  /**
   * Context based builder of Loader routine builders.
   */
  @SuppressWarnings("WeakerAccess")
  public static class LoaderBuilderCompat {

    private final LoaderContextCompat mContext;

    /**
     * Constructor.
     *
     * @param context the Loader context.
     */
    private LoaderBuilderCompat(@NotNull final LoaderContextCompat context) {
      mContext = ConstantConditions.notNull("Loader context", context);
    }

    /**
     * Returns a builder of routines bound to the builder context.
     * <br>
     * In order to prevent undesired leaks, the class of the specified factory must have a static
     * scope.
     * <p>
     * Note that the built routine results will be always dispatched on the configured Looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     * <br>
     * Note also that the input data passed to the invocation channel will be cached, and the
     * results will be produced only after the invocation channel is closed, so be sure to avoid
     * streaming inputs in order to prevent starvation or out of memory errors.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the class of the specified factory has not
     *                                            a static scope.
     */
    @NotNull
    public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
        @NotNull final ContextInvocationFactory<IN, OUT> factory) {
      return new DefaultLoaderRoutineBuilder<IN, OUT>(mContext, factory);
    }

    /**
     * Returns a builder of channels bound to the Loader identified by the specified ID.
     * <br>
     * If no Loader with the specified ID is running at the time of the channel creation, the
     * output will be aborted with a
     * {@link com.github.dm.jrt.android.core.invocation.MissingLoaderException
     * MissingLoaderException}.
     * <p>
     * Note that the built routine results will be always dispatched on the configured Looper
     * thread, thus waiting for the outputs immediately after its invocation may result in a
     * deadlock.
     *
     * @param loaderId the Loader ID.
     * @return the channel builder instance.
     */
    @NotNull
    public LoaderChannelBuilder withId(final int loaderId) {
      return new DefaultLoaderChannelBuilder(mContext).applyLoaderConfiguration()
                                                      .withLoaderId(loaderId)
                                                      .configured();
    }
  }
}
