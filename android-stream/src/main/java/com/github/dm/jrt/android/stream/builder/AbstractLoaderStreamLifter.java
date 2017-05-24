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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a builder of lifting functions based on Loader instances.
 * <p>
 * Created by davide-maestroni on 01/30/2017.
 */
public abstract class AbstractLoaderStreamLifter implements LoaderStreamLifter {

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  @NotNull
  @Override
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> runOnLoader() {
    return new LiftOnLoader<IN, OUT>(mInvocationConfiguration, mLoaderConfiguration);
  }

  @NotNull
  @Override
  public LoaderStreamLifter withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderStreamLifter withConfiguration(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends LoaderStreamLifter> withInvocation() {
    return new InvocationConfiguration.Builder<LoaderStreamLifter>(this, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderStreamLifter> withLoader() {
    return new LoaderConfiguration.Builder<LoaderStreamLifter>(this, mLoaderConfiguration);
  }

  /**
   * Lifts the specified supplier.
   *
   * @param invocationConfiguration the invocation configuration.
   * @param loaderConfiguration     the Loader configuration.
   * @param supplier                the channel supplier.
   * @return the lifted supplier.
   */
  @NotNull
  protected abstract <IN, OUT> Supplier<? extends Channel<IN, OUT>> lift(
      @NotNull InvocationConfiguration invocationConfiguration,
      @NotNull LoaderConfiguration loaderConfiguration,
      @NotNull Supplier<? extends Channel<IN, OUT>> supplier);

  /**
   * Lifting function implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private class LiftOnLoader<IN, OUT> implements LiftingFunction<IN, OUT, IN, OUT> {

    private final InvocationConfiguration mInvocationConfiguration;

    private final LoaderConfiguration mLoaderConfiguration;

    /**
     * Constructor.
     *
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the Loader configuration.
     */
    private LiftOnLoader(@NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration) {
      mInvocationConfiguration = invocationConfiguration;
      mLoaderConfiguration = loaderConfiguration;
    }

    @Override
    public Supplier<? extends Channel<IN, OUT>> apply(
        final Supplier<? extends Channel<IN, OUT>> supplier) throws Exception {
      return lift(mInvocationConfiguration, mLoaderConfiguration, supplier);
    }
  }
}
