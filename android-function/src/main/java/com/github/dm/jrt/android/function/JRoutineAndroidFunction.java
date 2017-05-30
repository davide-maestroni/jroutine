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

package com.github.dm.jrt.android.function;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * This utility class provides a few ways to easily implement Context invocation factories through
 * functional interfaces.
 * <p>
 * Created by davide-maestroni on 05/29/2017.
 */
public class JRoutineAndroidFunction {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineAndroidFunction() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a new Context invocation factory based on the specified supplier instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the Context invocation factory.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope.
   */
  @NotNull
  public static <IN, OUT> ContextInvocationFactory<IN, OUT> contextFactoryOf(
      @NotNull final Supplier<? extends ContextInvocation<? super IN, ? extends OUT>> supplier) {
    if (!wrapSupplier(supplier).hasStaticScope()) {
      throw new IllegalArgumentException("the specified function must have a static scope");
    }

    return new SupplierContextInvocationFactory<IN, OUT>(supplier);
  }

  /**
   * Implementation of an invocation factory based on a supplier function.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SupplierContextInvocationFactory<IN, OUT>
      extends ContextInvocationFactory<IN, OUT> {

    private final Supplier<? extends ContextInvocation<? super IN, ? extends OUT>> mSupplier;

    /**
     * Constructor.
     *
     * @param supplier the supplier function.
     */
    private SupplierContextInvocationFactory(
        @NotNull final Supplier<? extends ContextInvocation<? super IN, ? extends OUT>> supplier) {
      super(asArgs(wrapSupplier(supplier)));
      mSupplier = supplier;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public ContextInvocation<IN, OUT> newInvocation() throws Exception {
      return (ContextInvocation<IN, OUT>) mSupplier.get();
    }
  }
}
