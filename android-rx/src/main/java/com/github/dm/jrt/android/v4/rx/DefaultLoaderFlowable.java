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

import com.github.dm.jrt.android.rx.AbstractLoaderFlowable;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.JRoutineFlowable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;

/**
 * Default implementation of a Loader Flowable.
 * <p>
 * Created by davide-maestroni on 05/24/2017.
 */
class DefaultLoaderFlowable extends AbstractLoaderFlowable {

  private final LoaderSourceCompat mLoaderSource;

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultLoaderFlowable(@NotNull final LoaderSourceCompat loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
  }

  @NotNull
  @Override
  public <DATA> Flowable<DATA> observeOnLoader(@NotNull final Flowable<DATA> flowable) {
    final FlowableInvocationFactory<DATA> factory = new FlowableInvocationFactory<DATA>(flowable);
    return JRoutineFlowable.flowable()
                           .withConfiguration(getFlowableConfiguration())
                           .of(JRoutineLoaderCompat.routineOn(mLoaderSource)
                                                   .withConfiguration(getInvocationConfiguration())
                                                   .withConfiguration(getLoaderConfiguration())
                                                   .of(factory)
                                                   .invoke()
                                                   .close());
  }

  @NotNull
  @Override
  public <DATA> Flowable<DATA> subscribeOnLoader(@NotNull final Flowable<DATA> flowable) {
    return flowable.lift(
        new LoaderFlowableOperator<DATA>(mLoaderSource, getInvocationConfiguration(),
            getLoaderConfiguration()));
  }
}
