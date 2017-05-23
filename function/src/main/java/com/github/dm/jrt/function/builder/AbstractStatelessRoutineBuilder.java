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

package com.github.dm.jrt.function.builder;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a stateless routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>   the input data type.
 * @param <OUT>  the output data type.
 * @param <TYPE> the type of the class extending this one.
 */
public abstract class AbstractStatelessRoutineBuilder<IN, OUT, TYPE extends
    StatelessRoutineBuilder<IN, OUT>>
    extends AbstractStatelessBuilder<IN, OUT, StatelessRoutineBuilder<IN, OUT>>
    implements StatelessRoutineBuilder<IN, OUT> {

  private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

  private final Configurable<TYPE> mConfigurable = new Configurable<TYPE>() {

    @NotNull
    public TYPE withConfiguration(@NotNull final InvocationConfiguration configuration) {
      return AbstractStatelessRoutineBuilder.this.withConfiguration(configuration);
    }
  };

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE withConfiguration(@NotNull final InvocationConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    return (TYPE) this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Builder<? extends TYPE> withInvocation() {
    return new Builder<TYPE>(mConfigurable, mConfiguration);
  }

  /**
   * Returns the current invocation configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected InvocationConfiguration getConfiguration() {
    return mConfiguration;
  }
}
