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

package com.github.dm.jrt.stream.routine;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a routine which can be transformed through dedicated methods.
 * <p>
 * Created by davide-maestroni on 04/28/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamRoutine<IN, OUT> extends Routine<IN, OUT> {

  /**
   * Transforms the routine by modifying the invocation function.
   *
   * @param liftingFunction the function modifying the invocation channel.
   * @param <BEFORE>        the input type of the resulting routine.
   * @param <AFTER>         the output type of the resulting routine.
   * @return the new routine.
   * @throws java.lang.IllegalArgumentException if an unexpected error occurred.
   */
  @NotNull
  <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> lift(
      @NotNull Function<? super Supplier<? extends Channel<IN, OUT>>, ? extends Supplier<?
          extends Channel<BEFORE, AFTER>>> liftingFunction);

  // TODO: 07/05/2017 javadoc
  @NotNull
  <AFTER> StreamRoutine<IN, AFTER> map(
      @NotNull Invocation<? super OUT, ? extends AFTER> invocation);

  /**
   * Concatenates the invocations returned by the specified factory to the stream.
   * <br>
   * The invocations will be executed synchronously on the same thread as the last routine
   * invocation.
   *
   * @param factory the invocation factory instance.
   * @param <AFTER> the output type of the resulting routine.
   * @return the new routine.
   */
  @NotNull
  <AFTER> StreamRoutine<IN, AFTER> map(
      @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

  /**
   * Concatenates the specified routine mapping this one outputs.
   *
   * @param routine the routine instance.
   * @param <AFTER> the concatenation output type.
   * @return the new routine.
   */
  @NotNull
  <AFTER> StreamRoutine<IN, AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

  /**
   * Transforms the routine by applying the specified function.
   *
   * @param transformingFunction the function modifying the routine.
   * @param <BEFORE>             the input type of the resulting routine.
   * @param <AFTER>              the output type of the resulting routine.
   * @return the new routine.
   * @throws java.lang.IllegalArgumentException if an unexpected error occurred.
   */
  @NotNull
  <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> transform(
      @NotNull Function<? super StreamRoutine<IN, OUT>, ? extends StreamRoutine<BEFORE, AFTER>>
          transformingFunction);
}
