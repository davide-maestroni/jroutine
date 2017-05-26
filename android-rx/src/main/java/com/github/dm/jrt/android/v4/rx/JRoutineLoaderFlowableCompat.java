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

import com.github.dm.jrt.android.rx.LoaderFlowable;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class integrating the JRoutine Android classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to make the computation happen in a dedicated Loader:
 * <pre><code>
 * JRoutineLoaderFlowableCompat.flowableOn(loaderOf(activity))
 *                             .withLoader()
 *                             .withInvocationId(INVOCATION_ID)
 *                             .configuration()
 *                             .observeOnLoader(myFlowable)
 *                             .subscribe(getConsumer());
 * </code></pre>
 * Note that the Loader ID, by default, will only depend on the inputs, so, in order to avoid
 * clashing, it is advisable to explicitly set one through the configuration.
 * <p>
 * Created by davide-maestroni on 02/10/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineLoaderFlowableCompat {

  /**
   * Returns a Loader Flowable instance based on the specified source.
   *
   * @param loaderSource the Loader source.
   * @return the Loader Flowable.
   */
  @NotNull
  public static LoaderFlowable flowableOn(@NotNull final LoaderSourceCompat loaderSource) {
    return new DefaultLoaderFlowableCompat(loaderSource);
  }
}
