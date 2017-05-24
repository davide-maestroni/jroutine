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

package com.github.dm.jrt.android.stream.builder;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of lifting functions based on Loader instances.
 * <p>
 * Created by davide-maestroni on 01/30/2017.
 */
public interface LoaderStreamLifter
    extends InvocationConfigurable<LoaderStreamLifter>, LoaderConfigurable<LoaderStreamLifter> {

  /**
   * Returns a lifting function making the stream run on the lifter Loader.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> runOnLoader();
}
