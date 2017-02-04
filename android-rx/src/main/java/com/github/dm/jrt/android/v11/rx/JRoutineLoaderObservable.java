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

package com.github.dm.jrt.android.v11.rx;

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
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.JRoutineObservable;

import org.jetbrains.annotations.NotNull;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Utility class integrating the JRoutine Android classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to make the computation happen in a dedicated Loader:
 * <pre><code>
 * JRoutineLoaderObservable.with(myObservable)
 *                         .applyLoaderConfiguration()
 *                         .withInvocationId(INVOCATION_ID)
 *                         .configured()
 *                         .observeOn(loaderFrom(activity))
 *                         .subscribe(getAction());
 * </code></pre>
 * Note that the Loader ID, by default, will only depend on the inputs, so, in order to avoid
 * clashing, it is advisable to explicitly set one through the configuration.
 * <p>
 * See {@link com.github.dm.jrt.android.v4.rx.JRoutineLoaderObservableCompat
 * JRoutineLoaderObservableCompat} for support of API levels lower than
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 12/02/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineLoaderObservable {

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
   * Class wrapping an Observable so to enable it to dispatch data to a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  public static class LoaderObservable<DATA>
      implements InvocationConfigurable<LoaderObservable<DATA>>,
      LoaderConfigurable<LoaderObservable<DATA>> {

    private final Observable<DATA> mObservable;

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param observable the Observable.
     */
    private LoaderObservable(@NotNull final Observable<DATA> observable) {
      mObservable = ConstantConditions.notNull("Observable", observable);
    }

    @NotNull
    @Override
    public LoaderObservable<DATA> apply(@NotNull final LoaderConfiguration configuration) {
      mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
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
    public InvocationConfiguration.Builder<? extends LoaderObservable<DATA>>
    applyInvocationConfiguration() {
      return new InvocationConfiguration.Builder<LoaderObservable<DATA>>(this,
          mInvocationConfiguration);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderObservable<DATA>> applyLoaderConfiguration
        () {
      return new LoaderConfiguration.Builder<LoaderObservable<DATA>>(this, mLoaderConfiguration);
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
      return Observable.create(
          new OnSubscribeLoader<DATA>(context, mInvocationConfiguration, mLoaderConfiguration,
              mObservable));
    }

    /**
     * Returns an Observable asynchronously subscribing Observers in a dedicated Loader.
     *
     * @param context the Loader context.
     * @return the Observable.
     */
    @NotNull
    public Observable<DATA> subscribeOn(@NotNull final LoaderContext context) {
      return mObservable.lift(
          new LoaderOperator<DATA>(context, mInvocationConfiguration, mLoaderConfiguration));
    }
  }

  /**
   * Operator enabling an Observable to dispatch data to a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  public static class LoaderOperator<DATA> implements Operator<DATA, DATA> {

    private final LoaderContext mContext;

    private final InvocationConfiguration mInvocationConfiguration;

    private final LoaderConfiguration mLoaderConfiguration;

    /**
     * Constructor.
     *
     * @param context                 the Loader context.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the loader configuration.
     */
    private LoaderOperator(@NotNull final LoaderContext context,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration) {
      mContext = ConstantConditions.notNull("loader context", context);
      mInvocationConfiguration = invocationConfiguration;
      mLoaderConfiguration = loaderConfiguration;
    }

    @Override
    public Subscriber<? super DATA> call(final Subscriber<? super DATA> subscriber) {
      return new LoaderSubscriber<DATA>(JRoutineLoader.on(mContext)
                                                      .with(new SubscriberInvocationFactory<DATA>(
                                                          subscriber))
                                                      .apply(mInvocationConfiguration)
                                                      .apply(mLoaderConfiguration)
                                                      .buildRoutine());
    }
  }

  /**
   * Subscriber dispatching data to a dedicated Loader invocation.
   *
   * @param <DATA> the data type.
   */
  private static class LoaderSubscriber<DATA> extends Subscriber<DATA> {

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
    public void onCompleted() {
      mChannel.close();
    }

    @Override
    public void onError(final Throwable e) {
      mChannel.abort(e);
    }

    @Override
    public void onNext(final DATA data) {
      mChannel.pass(data);
    }

    @Override
    public void onStart() {
      super.onStart();
      mChannel = mRoutine.call();
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
      JRoutineObservable.with(mObservable).buildChannel().bind(result);
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
    public ContextInvocation<Void, DATA> newInvocation() throws Exception {
      return new ObservableInvocation<DATA>(mObservable);
    }
  }

  /**
   * Subscription listener calling the subscriber in a dedicated Loader.
   *
   * @param <DATA> the data type.
   */
  private static class OnSubscribeLoader<DATA> implements OnSubscribe<DATA> {

    private final LoaderContext mContext;

    private final InvocationConfiguration mInvocationConfiguration;

    private final LoaderConfiguration mLoaderConfiguration;

    private final Observable<DATA> mObservable;

    /**
     * Constructor.
     *
     * @param context                 the Loader context.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the loader configuration.
     * @param observable              the Observable instance.
     */
    private OnSubscribeLoader(@NotNull final LoaderContext context,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration,
        @NotNull final Observable<DATA> observable) {
      mContext = ConstantConditions.notNull("loader context", context);
      mInvocationConfiguration = invocationConfiguration;
      mLoaderConfiguration = loaderConfiguration;
      mObservable = observable;
    }

    @Override
    public void call(final Subscriber<? super DATA> subscriber) {
      JRoutineLoader.on(mContext)
                    .with(new ObservableInvocationFactory<DATA>(mObservable))
                    .apply(mInvocationConfiguration)
                    .apply(mLoaderConfiguration)
                    .call()
                    .bind(new SubscriberConsumer<DATA>(subscriber))
                    .close();
    }
  }

  /**
   * Channel consumer passing data to a subscriber.
   *
   * @param <DATA> the data type.
   */
  private static class SubscriberConsumer<DATA> implements ChannelConsumer<DATA> {

    private final Subscriber<? super DATA> mSubscriber;

    /**
     * Constructor.
     *
     * @param subscriber the subscriber instance.
     */
    private SubscriberConsumer(@NotNull final Subscriber<? super DATA> subscriber) {
      mSubscriber = subscriber;
    }

    @Override
    public void onComplete() {
      final Subscriber<? super DATA> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onCompleted();
      }
    }

    @Override
    public void onError(@NotNull final RoutineException error) {
      final Subscriber<? super DATA> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onError(error.getCause());
      }
    }

    @Override
    public void onOutput(final DATA output) {
      final Subscriber<? super DATA> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onNext(output);
      }
    }
  }

  /**
   * Context invocation passing data to a subscriber.
   *
   * @param <DATA> the data type.
   */
  private static class SubscriberInvocation<DATA> implements ContextInvocation<DATA, Void> {

    private final Subscriber<? super DATA> mSubscriber;

    /**
     * Constructor.
     *
     * @param subscriber the subscriber instance.
     */
    private SubscriberInvocation(@NotNull final Subscriber<? super DATA> subscriber) {
      mSubscriber = subscriber;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      final Subscriber<? super DATA> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onError(reason);
      }
    }

    @Override
    public void onComplete(@NotNull final Channel<Void, ?> result) {
      final Subscriber<? super DATA> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onCompleted();
      }
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<Void, ?> result) {
      final Subscriber<? super DATA> subscriber = mSubscriber;
      if (!subscriber.isUnsubscribed()) {
        subscriber.onNext(input);
      }
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }

    @Override
    public void onRestart() {
    }

    @Override
    public void onContext(@NotNull final Context context) {
    }
  }

  /**
   * Factory of context invocation passing data to a subscriber.
   *
   * @param <DATA> the data type.
   */
  private static class SubscriberInvocationFactory<DATA>
      extends ContextInvocationFactory<DATA, Void> {

    private final Subscriber<? super DATA> mSubscriber;

    /**
     * Constructor.
     *
     * @param subscriber the subscriber instance.
     */
    private SubscriberInvocationFactory(@NotNull final Subscriber<? super DATA> subscriber) {
      super(null);
      mSubscriber = ConstantConditions.notNull("subscriber", subscriber);
    }

    @NotNull
    @Override
    public ContextInvocation<DATA, Void> newInvocation() {
      return new SubscriberInvocation<DATA>(mSubscriber);
    }
  }
}
