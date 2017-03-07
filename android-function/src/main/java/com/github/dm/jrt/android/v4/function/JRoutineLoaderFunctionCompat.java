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

package com.github.dm.jrt.android.v4.function;

import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide-maestroni on 03/07/2017.
 */
public class JRoutineLoaderFunctionCompat {

  private static final BiConsumerDecorator<? extends List<?>, ?> sListConsumer =
      BiConsumerDecorator.decorate(new BiConsumer<List<Object>, Object>() {

        public void accept(final List<Object> list, final Object input) {
          list.add(input);
        }
      });

  private static final SupplierDecorator<? extends List<?>> sListSupplier =
      SupplierDecorator.decorate(new Supplier<List<?>>() {

        public List<?> get() {
          return new ArrayList<Object>();
        }
      });

  private JRoutineLoaderFunctionCompat() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static <IN, OUT, STATE> StatefulLoaderRoutineBuilder<IN, OUT, STATE> stateful(
      @NotNull final LoaderContextCompat loaderContext, final int invocationId) {
    return new DefaultStatefulLoaderRoutineBuilderCompat<IN, OUT, STATE>(
        loaderContext).loaderConfiguration().withInvocationId(invocationId).apply();
  }

  @NotNull
  public static <IN, OUT> StatefulLoaderRoutineBuilder<IN, OUT, ? extends List<IN>> statefulList(
      @NotNull final LoaderContextCompat loaderContext, final int invocationId) {
    final Supplier<? extends List<IN>> onCreate = listSupplier();
    final BiConsumer<? super List<IN>, ? super IN> onNext = listConsumer();
    final DefaultStatefulLoaderRoutineBuilderCompat<IN, OUT, List<IN>> builder =
        new DefaultStatefulLoaderRoutineBuilderCompat<IN, OUT, List<IN>>(loaderContext);
    return builder.onCreate(onCreate)
                  .onNextConsume(onNext)
                  .loaderConfiguration()
                  .withInvocationId(invocationId)
                  .apply();
  }

  @NotNull
  public static <IN, OUT> StatelessLoaderRoutineBuilder<IN, OUT> stateless(
      @NotNull final LoaderContextCompat loaderContext, final int invocationId) {
    return new DefaultStatelessLoaderRoutineBuilderCompat<IN, OUT>(
        loaderContext).loaderConfiguration().withInvocationId(invocationId).apply();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private static <IN> BiConsumer<? super List<IN>, ? super IN> listConsumer() {
    return (BiConsumer<? super List<IN>, ? super IN>) sListConsumer;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private static <IN> Supplier<? extends List<IN>> listSupplier() {
    return (Supplier<? extends List<IN>>) sListSupplier;
  }
}
