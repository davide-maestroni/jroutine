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

package com.github.dm.jrt.android.function.builder;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a stateful Loader routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 * @param <TYPE>  the type of the class extending this one.
 */
public abstract class AbstractStatefulLoaderRoutineBuilder<IN, OUT, STATE, TYPE extends
    StatefulLoaderRoutineBuilder<IN, OUT, STATE>>
    extends AbstractStatefulContextBuilder<IN, OUT, STATE, StatefulLoaderRoutineBuilder<IN, OUT, STATE>>
    implements StatefulLoaderRoutineBuilder<IN, OUT, STATE> {

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private final InvocationConfiguration.Configurable<TYPE> mInvocationConfigurable =
      new InvocationConfiguration.Configurable<TYPE>() {

        @NotNull
        public TYPE withConfiguration(@NotNull final InvocationConfiguration configuration) {
          return AbstractStatefulLoaderRoutineBuilder.this.withConfiguration(configuration);
        }
      };

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  private final Configurable<TYPE> mLoaderConfigurable = new Configurable<TYPE>() {

    @NotNull
    public TYPE withConfiguration(@NotNull final LoaderConfiguration configuration) {
      return AbstractStatefulLoaderRoutineBuilder.this.withConfiguration(configuration);
    }
  };

  @NotNull
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends TYPE> withInvocation() {
    return new InvocationConfiguration.Builder<TYPE>(mInvocationConfigurable,
        mInvocationConfiguration);
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends TYPE> withLoader() {
    return new LoaderConfiguration.Builder<TYPE>(mLoaderConfigurable, mLoaderConfiguration);
  }

  /**
   * Returns the current invocation configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected InvocationConfiguration getInvocationConfiguration() {
    return mInvocationConfiguration;
  }

  /**
   * Returns the current Loader configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected LoaderConfiguration getLoaderConfiguration() {
    return mLoaderConfiguration;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE withConfiguration(@NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE withConfiguration(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return (TYPE) this;
  }
}
