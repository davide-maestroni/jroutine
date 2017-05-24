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

package com.github.dm.jrt.android.rx;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.rx.config.FlowableConfigurable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;

/**
 * Interface defining a class lifting a Flowable so to make it run in a dedicated Loader.
 * <p>
 * Created by davide-maestroni on 05/24/2017.
 */
public interface LoaderFlowable
    extends FlowableConfigurable<LoaderFlowable>, InvocationConfigurable<LoaderFlowable>,
    LoaderConfigurable<LoaderFlowable> {

  /**
   * Returns a Flowable performing its emissions and notifications in a dedicated Loader.
   * <br>
   * The returned Flowable will asynchronously subscribe Observers on the main thread.
   *
   * @param flowable the Flowable to lift.
   * @return the lifted Flowable.
   */
  @NotNull
  <DATA> Flowable<DATA> observeOnLoader(@NotNull Flowable<DATA> flowable);

  /**
   * Returns a Flowable asynchronously subscribing Observers in a dedicated Loader.
   *
   * @param flowable the Flowable to lift.
   * @return the lifted Flowable.
   */
  @NotNull
  <DATA> Flowable<DATA> subscribeOnLoader(@NotNull Flowable<DATA> flowable);
}
