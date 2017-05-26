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

package com.github.dm.jrt.android.v4.rx;

import android.content.Context;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.FlowableOperator;

/**
 * Operator enabling a Flowable to dispatch data to a dedicated Loader.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <DATA> the data type.
 */
class LoaderFlowableOperatorCompat<DATA> implements FlowableOperator<DATA, DATA> {

  private final InvocationConfiguration mInvocationConfiguration;

  private final LoaderConfiguration mLoaderConfiguration;

  private final LoaderSourceCompat mLoaderSource;

  /**
   * Constructor.
   *
   * @param loaderSource            the Loader source.
   * @param invocationConfiguration the invocation configuration.
   * @param loaderConfiguration     the Loader configuration.
   */
  LoaderFlowableOperatorCompat(@NotNull final LoaderSourceCompat loaderSource,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final LoaderConfiguration loaderConfiguration) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", invocationConfiguration);
    mLoaderConfiguration = ConstantConditions.notNull("loader configuration", loaderConfiguration);
  }

  @Override
  public Subscriber<? super DATA> apply(final Subscriber<? super DATA> subscriber) {
    final SubscriberInvocationFactory<DATA> factory =
        new SubscriberInvocationFactory<DATA>(subscriber);
    return new LoaderSubscriber<DATA>(subscriber, JRoutineLoaderCompat.routineOn(mLoaderSource)
                                                                      .withConfiguration(
                                                                          mInvocationConfiguration)
                                                                      .withConfiguration(
                                                                          mLoaderConfiguration)
                                                                      .of(factory));
  }

  /**
   * Subscriber dispatching data to a dedicated Loader invocation.
   *
   * @param <DATA> the data type.
   */
  private static class LoaderSubscriber<DATA> implements Subscriber<DATA> {

    private final LoaderRoutine<DATA, Void> mRoutine;

    private final Subscriber<? super DATA> mSubscriber;

    private Channel<DATA, Void> mChannel;

    /**
     * Constructor.
     *
     * @param subscriber the wrapped subscriber.
     * @param routine    the Loader routine.
     */
    private LoaderSubscriber(@NotNull final Subscriber<? super DATA> subscriber,
        @NotNull final LoaderRoutine<DATA, Void> routine) {
      mSubscriber = ConstantConditions.notNull("subscriber instance", subscriber);
      mRoutine = routine;
    }

    @Override
    public void onSubscribe(final Subscription s) {
      mChannel = mRoutine.invoke();
      mSubscriber.onSubscribe(s);
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
    public void onComplete(@NotNull final Channel<Void, ?> result) {
      mSubscriber.onComplete();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<Void, ?> result) {
      mSubscriber.onNext(input);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onContext(@NotNull final Context context) {
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
