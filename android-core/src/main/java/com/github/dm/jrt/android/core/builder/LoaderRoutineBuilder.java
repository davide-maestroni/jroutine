/*
 * Copyright 2016 Davide Maestroni
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

package com.github.dm.jrt.android.core.builder;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines running in dedicated Loaders.
 * <p>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will be dispatched on the configured Looper thread, no matter the calling one
 * was. If the invocation and the result Looper are the same, waiting for the outputs right after
 * the routine invocation, may result in a deadlock.
 * <br>
 * Note that the configuration of the maximum number of concurrent invocations might not work as
 * expected, since the number of running Loaders cannot be computed.
 * <br>
 * Note also that the input data will be cached, so be sure to avoid streaming inputs in order to
 * prevent out of memory errors.
 * <br>
 * The local Context of the invocations will always be the application one.
 * <p>
 * Created by davide-maestroni on 12/09/2014.
 */
public interface LoaderRoutineBuilder
    extends RoutineBuilder, LoaderConfigurable<LoaderRoutineBuilder> {

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified factory has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  <IN, OUT> LoaderRoutine<IN, OUT> of(@NotNull InvocationFactory<IN, OUT> factory);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified factory has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  <IN, OUT> LoaderRoutine<IN, OUT> ofSingleton(@NotNull Invocation<IN, OUT> invocation);

  // TODO: 13/05/2017 Javadoc
  <IN, OUT> LoaderRoutine<IN, OUT> of(@NotNull ContextInvocationFactory<IN, OUT> factory);

  <IN, OUT> LoaderRoutine<IN, OUT> ofSingleton(@NotNull ContextInvocation<IN, OUT> invocation);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderRoutineBuilder withConfiguration(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  Builder<? extends LoaderRoutineBuilder> withInvocation();
}
