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

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Operator enabling an Observable to dispatch data to a dedicated Loader.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <DATA> the data type.
 */
class LoaderObservableOperator<DATA> implements ObservableOperator<DATA, DATA> {

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
  LoaderObservableOperator(@NotNull final LoaderContext context,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final LoaderConfiguration loaderConfiguration) {
    mContext = ConstantConditions.notNull("Loader context", context);
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", invocationConfiguration);
    mLoaderConfiguration = ConstantConditions.notNull("loader configuration", loaderConfiguration);
  }

  @Override
  public Observer<? super DATA> apply(final Observer<? super DATA> observer) {
    final ObserverInvocationFactory<DATA> factory = new ObserverInvocationFactory<DATA>(observer);
    return new LoaderObserver<DATA>(observer, JRoutineLoader.on(mContext)
                                                            .with(factory)
                                                            .apply(mInvocationConfiguration)
                                                            .apply(mLoaderConfiguration)
                                                            .buildRoutine());
  }

  /**
   * Observer dispatching data to a dedicated Loader invocation.
   *
   * @param <DATA> the data type.
   */
  private static class LoaderObserver<DATA> implements Observer<DATA> {

    private final Observer<? super DATA> mObserver;

    private final LoaderRoutine<DATA, Void> mRoutine;

    private Channel<DATA, Void> mChannel;

    /**
     * Constructor.
     *
     * @param observer the wrapped observer.
     * @param routine  the Loader routine.
     */
    private LoaderObserver(@NotNull final Observer<? super DATA> observer,
        @NotNull final LoaderRoutine<DATA, Void> routine) {
      mObserver = ConstantConditions.notNull("observer instance", observer);
      mRoutine = routine;
    }

    @Override
    public void onSubscribe(final Disposable d) {
      mChannel = mRoutine.call();
      mObserver.onSubscribe(d);
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

    @Override
    public void onContext(@NotNull final Context context) {
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
}
