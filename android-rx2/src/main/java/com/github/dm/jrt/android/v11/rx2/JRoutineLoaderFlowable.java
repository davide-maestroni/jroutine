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

package com.github.dm.jrt.android.v11.rx2;

import android.content.Context;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx2.JRoutineFlowable;
import com.github.dm.jrt.rx2.config.FlowableConfigurable;
import com.github.dm.jrt.rx2.config.FlowableConfiguration;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.FlowableOperator;
import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Utility class integrating the JRoutine Android classes with RxJava2 ones.
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
 * See {@link com.github.dm.jrt.android.v4.rx2.JRoutineLoaderFlowableCompat
 * JRoutineLoaderObservableCompat} for support of API levels lower than
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineLoaderFlowable {

  /**
   * Returns a Loader Observable instance wrapping the specified one.
   *
   * @param observable the Observable.
   * @param <DATA>     the data type.
   * @return the Loader Observable.
   */
  @NotNull
  public static <DATA> LoaderObservable<DATA> with(@NotNull final Observable<DATA> observable) {
    return new LoaderObservable<DATA>(observable);
  }

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
      return JRoutineFlowable.from(JRoutineLoader.on(context)
                                                 .with(
                                                     new FlowableInvocationFactory<DATA>(mFlowable))
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

  /**
   * Operator enabling a Flowable to dispatch data to a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  public static class LoaderFlowableOperator<DATA> implements FlowableOperator<DATA, DATA> {

    private final LoaderContext mContext;

    private final InvocationConfiguration mInvocationConfiguration;

    private final LoaderConfiguration mLoaderConfiguration;

    /**
     * Constructor.
     *
     * @param context                 the Loader context.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the Loader configuration.
     */
    private LoaderFlowableOperator(@NotNull final LoaderContext context,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration) {
      mContext = ConstantConditions.notNull("Loader context", context);
      mInvocationConfiguration = invocationConfiguration;
      mLoaderConfiguration = loaderConfiguration;
    }

    @Override
    public Subscriber<? super DATA> apply(final Subscriber<? super DATA> subscriber) {
      return new LoaderSubscriber<DATA>(JRoutineLoader.on(mContext)
                                                      .with(new SubscriberInvocationFactory<DATA>(
                                                          subscriber))
                                                      .apply(mInvocationConfiguration)
                                                      .apply(mLoaderConfiguration)
                                                      .buildRoutine());
    }
  }

  /**
   * Class wrapping an Observable so to enable it to dispatch data to a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  public static class LoaderObservable<DATA>
      implements FlowableConfigurable<Void, LoaderObservable<DATA>>,
      InvocationConfigurable<LoaderObservable<DATA>>, LoaderConfigurable<LoaderObservable<DATA>> {

    private final Observable<DATA> mObservable;

    private FlowableConfiguration<Void> mFlowableConfiguration =
        FlowableConfiguration.defaultConfiguration();

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param observable the Observable.
     */
    private LoaderObservable(@NotNull final Observable<DATA> observable) {
      mObservable = ConstantConditions.notNull("Observable instance", observable);
    }

    /**
     * Returns an Observable performing its emissions and notifications in a dedicated Loader.
     * <br>
     * The returned Observable will asynchronously subscribe Observers on the main thread.
     *
     * @param context the Loader context.
     * @return the Observable.
     */
    @NotNull
    public Observable<DATA> observeOn(@NotNull final LoaderContext context) {
      return JRoutineFlowable.from(JRoutineLoader.on(context)
                                                 .with(new ObservableInvocationFactory<DATA>(
                                                     mObservable))
                                                 .apply(mInvocationConfiguration)
                                                 .apply(mLoaderConfiguration))
                             .apply(mFlowableConfiguration)
                             .buildFlowable()
                             .toObservable();
    }

    /**
     * Returns an Observable asynchronously subscribing Observers in a dedicated Loader.
     *
     * @param context the Loader context.
     * @return the Observable.
     */
    @NotNull
    public Observable<DATA> subscribeOn(@NotNull final LoaderContext context) {
      return mObservable.lift(new LoaderObservableOperator<DATA>(context, mInvocationConfiguration,
          mLoaderConfiguration));
    }

    @NotNull
    @Override
    public LoaderObservable<DATA> apply(@NotNull final LoaderConfiguration configuration) {
      mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public LoaderObservable<DATA> apply(@NotNull final InvocationConfiguration configuration) {
      mInvocationConfiguration =
          ConstantConditions.notNull("invocation configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public LoaderObservable<DATA> apply(@NotNull final FlowableConfiguration<Void> configuration) {
      mFlowableConfiguration = ConstantConditions.notNull("Flowable configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public FlowableConfiguration.Builder<Void, LoaderObservable<DATA>> flowableConfiguration() {
      return new FlowableConfiguration.Builder<Void, LoaderObservable<DATA>>(this,
          mFlowableConfiguration);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderObservable<DATA>>
    invocationConfiguration() {
      return new InvocationConfiguration.Builder<LoaderObservable<DATA>>(this,
          mInvocationConfiguration);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderObservable<DATA>> loaderConfiguration() {
      return new LoaderConfiguration.Builder<LoaderObservable<DATA>>(this, mLoaderConfiguration);
    }
  }

  /**
   * Operator enabling an Observable to dispatch data to a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  public static class LoaderObservableOperator<DATA> implements ObservableOperator<DATA, DATA> {

    private final LoaderContext mContext;

    private final InvocationConfiguration mInvocationConfiguration;

    private final LoaderConfiguration mLoaderConfiguration;

    /**
     * Constructor.
     *
     * @param context                 the Loader context.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the Loader configuration.
     */
    private LoaderObservableOperator(@NotNull final LoaderContext context,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration) {
      mContext = ConstantConditions.notNull("Loader context", context);
      mInvocationConfiguration = invocationConfiguration;
      mLoaderConfiguration = loaderConfiguration;
    }

    @Override
    public Observer<? super DATA> apply(final Observer<? super DATA> observer) {
      return new LoaderObserver<DATA>(JRoutineLoader.on(mContext)
                                                    .with(new ObserverInvocationFactory<DATA>(
                                                        observer))
                                                    .apply(mInvocationConfiguration)
                                                    .apply(mLoaderConfiguration)
                                                    .buildRoutine());
    }
  }

  /**
   * Context invocation passing Flowable data to the result channel.
   *
   * @param <DATA> the data type.
   */
  private static class FlowableInvocation<DATA> extends TemplateContextInvocation<Void, DATA> {

    private final Flowable<DATA> mFlowable;

    /**
     * Constructor.
     *
     * @param flowable the Flowable instance.
     */
    private FlowableInvocation(final Flowable<DATA> flowable) {
      mFlowable = flowable;
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) {
      JRoutineFlowable.with(mFlowable).buildChannel().bind(result);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }

  /**
   * Factory of context invocations passing Flowable data to the result channel.
   *
   * @param <DATA> the data type.
   */
  private static class FlowableInvocationFactory<DATA>
      extends ContextInvocationFactory<Void, DATA> {

    private final Flowable<DATA> mFlowable;

    /**
     * Constructor.
     *
     * @param flowable the Flowable instance.
     */
    private FlowableInvocationFactory(@NotNull final Flowable<DATA> flowable) {
      super(asArgs(flowable));
      mFlowable = flowable;
    }

    @NotNull
    @Override
    public ContextInvocation<Void, DATA> newInvocation() {
      return new FlowableInvocation<DATA>(mFlowable);
    }
  }

  /**
   * Observer dispatching data to a dedicated Loader invocation.
   *
   * @param <DATA> the data type.
   */
  private static class LoaderObserver<DATA> implements Observer<DATA> {

    private final LoaderRoutine<DATA, Void> mRoutine;

    private Channel<DATA, Void> mChannel;

    /**
     * Constructor.
     *
     * @param routine the Loader routine.
     */
    private LoaderObserver(@NotNull final LoaderRoutine<DATA, Void> routine) {
      mRoutine = routine;
    }

    @Override
    public void onSubscribe(final Disposable d) {
      mChannel = mRoutine.call();
    }

    @Override
    public void onNext(final DATA data) {
      mChannel.pass(data);
    }

    @Override
    public void onError(final Throwable e) {
      mChannel.abort(e);
    }

    @Override
    public void onComplete() {
      mChannel.close();
    }
  }

  /**
   * Subscriber dispatching data to a dedicated Loader invocation.
   *
   * @param <DATA> the data type.
   */
  private static class LoaderSubscriber<DATA> implements Subscriber<DATA> {

    private final LoaderRoutine<DATA, Void> mRoutine;

    private Channel<DATA, Void> mChannel;

    /**
     * Constructor.
     *
     * @param routine the Loader routine.
     */
    private LoaderSubscriber(@NotNull final LoaderRoutine<DATA, Void> routine) {
      mRoutine = routine;
    }

    @Override
    public void onSubscribe(final Subscription s) {
      mChannel = mRoutine.call();
      s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(final DATA data) {
      mChannel.pass(data);
    }

    @Override
    public void onError(final Throwable e) {
      mChannel.abort(e);
    }

    @Override
    public void onComplete() {
      mChannel.close();
    }
  }

  /**
   * Context invocation passing Observable data to the result channel.
   *
   * @param <DATA> the data type.
   */
  private static class ObservableInvocation<DATA> extends TemplateContextInvocation<Void, DATA> {

    private final Observable<DATA> mObservable;

    /**
     * Constructor.
     *
     * @param observable the Observable instance.
     */
    private ObservableInvocation(final Observable<DATA> observable) {
      mObservable = observable;
    }

    @Override
    public void onComplete(@NotNull final Channel<DATA, ?> result) {
      JRoutineFlowable.with(mObservable).buildChannel().bind(result);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }

  /**
   * Factory of context invocations passing Observable data to the result channel.
   *
   * @param <DATA> the data type.
   */
  private static class ObservableInvocationFactory<DATA>
      extends ContextInvocationFactory<Void, DATA> {

    private final Observable<DATA> mObservable;

    /**
     * Constructor.
     *
     * @param observable the Observable instance.
     */
    private ObservableInvocationFactory(@NotNull final Observable<DATA> observable) {
      super(asArgs(observable));
      mObservable = observable;
    }

    @NotNull
    @Override
    public ContextInvocation<Void, DATA> newInvocation() {
      return new ObservableInvocation<DATA>(mObservable);
    }
  }

  /**
   * Context invocation passing data to an Observer.
   *
   * @param <DATA> the data type.
   */
  private static class ObserverInvocation<DATA> implements ContextInvocation<DATA, Void> {

    private final Observer<? super DATA> mObserver;

    /**
     * Constructor.
     *
     * @param observer the Observer instance.
     */
    private ObserverInvocation(@NotNull final Observer<? super DATA> observer) {
      mObserver = observer;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mObserver.onError(reason);
    }

    @Override
    public void onContext(@NotNull final Context context) {
    }

    @Override
    public void onComplete(@NotNull final Channel<Void, ?> result) {
      mObserver.onComplete();
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<Void, ?> result) {
      mObserver.onNext(input);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }

    @Override
    public void onRestart() {
    }
  }

  /**
   * Factory of context invocation passing data to an Observer.
   *
   * @param <DATA> the data type.
   */
  private static class ObserverInvocationFactory<DATA>
      extends ContextInvocationFactory<DATA, Void> {

    private final Observer<? super DATA> mObserver;

    /**
     * Constructor.
     *
     * @param observer the Observer instance.
     */
    private ObserverInvocationFactory(@NotNull final Observer<? super DATA> observer) {
      super(null);
      mObserver = ConstantConditions.notNull("Observer instance", observer);
    }

    @NotNull
    @Override
    public ContextInvocation<DATA, Void> newInvocation() {
      return new ObserverInvocation<DATA>(mObserver);
    }
  }

  /**
   * Context invocation passing data to a Subscriber.
   *
   * @param <DATA> the data type.
   */
  private static class SubscriberInvocation<DATA> implements ContextInvocation<DATA, Void> {

    private final Subscriber<? super DATA> mSubscriber;

    /**
     * Constructor.
     *
     * @param subscriber the Subscriber instance.
     */
    private SubscriberInvocation(@NotNull final Subscriber<? super DATA> subscriber) {
      mSubscriber = subscriber;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mSubscriber.onError(reason);
    }

    @Override
    public void onContext(@NotNull final Context context) {
    }

    @Override
    public void onComplete(@NotNull final Channel<Void, ?> result) {
      mSubscriber.onComplete();
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<Void, ?> result) {
      mSubscriber.onNext(input);
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }

    @Override
    public void onRestart() {
    }
  }

  /**
   * Factory of context invocation passing data to a Subscriber.
   *
   * @param <DATA> the data type.
   */
  private static class SubscriberInvocationFactory<DATA>
      extends ContextInvocationFactory<DATA, Void> {

    private final Subscriber<? super DATA> mSubscriber;

    /**
     * Constructor.
     *
     * @param subscriber the Subscriber instance.
     */
    private SubscriberInvocationFactory(@NotNull final Subscriber<? super DATA> subscriber) {
      super(null);
      mSubscriber = ConstantConditions.notNull("Subscriber instance", subscriber);
    }

    @NotNull
    @Override
    public ContextInvocation<DATA, Void> newInvocation() {
      return new SubscriberInvocation<DATA>(mSubscriber);
    }
  }
}
