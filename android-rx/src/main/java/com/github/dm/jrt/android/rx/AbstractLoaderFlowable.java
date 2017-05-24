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

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.config.FlowableConfiguration;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a Loader Flowable.
 * <p>
 * Created by davide-maestroni on 05/24/2017.
 */
public abstract class AbstractLoaderFlowable implements LoaderFlowable {

  private FlowableConfiguration mFlowableConfiguration =
      FlowableConfiguration.defaultConfiguration();

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  @NotNull
  @Override
  public LoaderFlowable withConfiguration(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderFlowable withConfiguration(@NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderFlowable withConfiguration(@NotNull final FlowableConfiguration configuration) {
    mFlowableConfiguration = ConstantConditions.notNull("Flowable configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public FlowableConfiguration.Builder<LoaderFlowable> withFlowable() {
    return new FlowableConfiguration.Builder<LoaderFlowable>(this, mFlowableConfiguration);
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends LoaderFlowable> withInvocation() {
    return new InvocationConfiguration.Builder<LoaderFlowable>(this, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderFlowable> withLoader() {
    return new LoaderConfiguration.Builder<LoaderFlowable>(this, mLoaderConfiguration);
  }

  /**
   * Returns the Flowable configuration.
   *
   * @return the Flowable configuration.
   */
  @NotNull
  protected FlowableConfiguration getFlowableConfiguration() {
    return mFlowableConfiguration;
  }

  /**
   * Returns the invocation configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected InvocationConfiguration getInvocationConfiguration() {
    return mInvocationConfiguration;
  }

  /**
   * Returns the Loader configuration.
   *
   * @return the Loader configuration.
   */
  @NotNull
  protected LoaderConfiguration getLoaderConfiguration() {
    return mLoaderConfiguration;
  }
}
