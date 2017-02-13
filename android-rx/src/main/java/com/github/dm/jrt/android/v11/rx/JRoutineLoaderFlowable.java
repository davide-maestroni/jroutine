/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.android.v11.rx;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.JRoutineFlowable;
import com.github.dm.jrt.rx.config.FlowableConfigurable;
import com.github.dm.jrt.rx.config.FlowableConfiguration;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;

/**
 * Utility class integrating the JRoutine Android classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to make the computation happen in a dedicated Loader:
 * <pre><code>
 * JRoutineLoaderFlowable.with(myFlowable)
 *                       .loaderConfiguration()
 *                       .withInvocationId(INVOCATION_ID)
 *                       .apply()
 *                       .observeOn(loaderFrom(activity))
 *                       .subscribe(getConsumer());
 * </code></pre>
 * Note that the Loader ID, by default, will only depend on the inputs, so, in order to avoid
 * clashing, it is advisable to explicitly set one through the configuration.
 * <p>
 * See {@link com.github.dm.jrt.android.v4.rx.JRoutineLoaderFlowableCompat
 * JRoutineLoaderObservableCompat} for support of API levels lower than
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineLoaderFlowable {

  /**
   * Returns a Loader Flowable instance wrapping the specified one.
   *
   * @param flowable the Flowable.
   * @param <DATA>   the data type.
   * @return the Loader Flowable.
   */
  @NotNull
  public static <DATA> LoaderFlowable<DATA> with(@NotNull final Flowable<DATA> flowable) {
    return new LoaderFlowable<DATA>(flowable);
  }

  /**
   * Class wrapping a Flowable so to enable it to dispatch data to a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  public static class LoaderFlowable<DATA>
      implements FlowableConfigurable<Void, LoaderFlowable<DATA>>,
      InvocationConfigurable<LoaderFlowable<DATA>>, LoaderConfigurable<LoaderFlowable<DATA>> {

    private final Flowable<DATA> mFlowable;

    private FlowableConfiguration<Void> mFlowableConfiguration =
        FlowableConfiguration.defaultConfiguration();

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param flowable the Flowable.
     */
    private LoaderFlowable(@NotNull final Flowable<DATA> flowable) {
      mFlowable = ConstantConditions.notNull("Flowable instance", flowable);
    }

    @NotNull
    @Override
    public LoaderFlowable<DATA> apply(@NotNull final LoaderConfiguration configuration) {
      mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public LoaderFlowable<DATA> apply(@NotNull final InvocationConfiguration configuration) {
      mInvocationConfiguration =
          ConstantConditions.notNull("invocation configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public LoaderFlowable<DATA> apply(@NotNull final FlowableConfiguration<Void> configuration) {
      mFlowableConfiguration = ConstantConditions.notNull("Flowable configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public FlowableConfiguration.Builder<Void, LoaderFlowable<DATA>> flowableConfiguration() {
      return new FlowableConfiguration.Builder<Void, LoaderFlowable<DATA>>(this,
          mFlowableConfiguration);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderFlowable<DATA>>
    invocationConfiguration() {
      return new InvocationConfiguration.Builder<LoaderFlowable<DATA>>(this,
          mInvocationConfiguration);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderFlowable<DATA>> loaderConfiguration() {
      return new LoaderConfiguration.Builder<LoaderFlowable<DATA>>(this, mLoaderConfiguration);
    }

    /**
     * Returns a Flowable performing its emissions and notifications in a dedicated Loader.
     * <br>
     * The returned Flowable will asynchronously subscribe Observers on the main thread.
     *
     * @param context the Loader context.
     * @return the Observable.
     */
    @NotNull
    public Flowable<DATA> observeOn(@NotNull final LoaderContext context) {
      final FlowableInvocationFactory<DATA> factory =
          new FlowableInvocationFactory<DATA>(mFlowable);
      return JRoutineFlowable.from(JRoutineLoader.on(context)
                                                 .with(factory)
                                                 .apply(mInvocationConfiguration)
                                                 .apply(mLoaderConfiguration))
                             .apply(mFlowableConfiguration)
                             .buildFlowable();
    }

    /**
     * Returns a Flowable asynchronously subscribing Observers in a dedicated Loader.
     *
     * @param context the Loader context.
     * @return the Observable.
     */
    @NotNull
    public Flowable<DATA> subscribeOn(@NotNull final LoaderContext context) {
      return mFlowable.lift(new LoaderFlowableOperator<DATA>(context, mInvocationConfiguration,
          mLoaderConfiguration));
    }
  }
}
