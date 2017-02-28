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

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.lambda.Function;
import com.github.dm.jrt.stream.builder.StreamConfiguration;
import com.github.dm.jrt.stream.transform.LiftFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a builder of lifting functions based on Loader instances.
 * <p>
 * Created by davide-maestroni on 01/30/2017.
 *
 * @param <IN>     the input data type.
 * @param <OUT>    the output data type.
 * @param <BEFORE> the input type after the lifting.
 * @param <AFTER>  the output type after the lifting.
 */
public abstract class AbstractLoaderTransformationBuilder<IN, OUT, BEFORE, AFTER>
    implements LoaderTransformationBuilder<IN, OUT, BEFORE, AFTER> {

  private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

  @NotNull
  @Override
  public LoaderTransformationBuilder<IN, OUT, BEFORE, AFTER> apply(
      @NotNull final LoaderConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LiftFunction<IN, OUT, BEFORE, AFTER> buildFunction() {
    return new BuilderLiftFunction(mConfiguration);
  }

  @NotNull
  @Override
  public Builder<? extends LoaderTransformationBuilder<IN, OUT, BEFORE, AFTER>>
  loaderConfiguration() {
    return new Builder<LoaderTransformationBuilder<IN, OUT, BEFORE, AFTER>>(this, mConfiguration);
  }

  /**
   * Lifts the specified function.
   *
   * @param loaderConfiguration the Loader configuration.
   * @param streamConfiguration the stream configuration.
   * @param function            the function to lift.
   * @return the lifted function.
   */
  @NotNull
  protected abstract Function<Channel<?, BEFORE>, Channel<?, AFTER>> liftFunction(
      @NotNull LoaderConfiguration loaderConfiguration,
      @NotNull StreamConfiguration streamConfiguration,
      @NotNull Function<Channel<?, IN>, Channel<?, OUT>> function);

  /**
   * Lifting function implementation.
   */
  private class BuilderLiftFunction implements LiftFunction<IN, OUT, BEFORE, AFTER> {

    private final LoaderConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param configuration the Loader configuration.
     */
    private BuilderLiftFunction(@NotNull final LoaderConfiguration configuration) {
      mConfiguration = configuration;
    }

    @Override
    public Function<Channel<?, BEFORE>, Channel<?, AFTER>> apply(
        final StreamConfiguration streamConfiguration,
        final Function<Channel<?, IN>, Channel<?, OUT>> function) throws Exception {
      return liftFunction(mConfiguration, streamConfiguration, function);
    }
  }
}
