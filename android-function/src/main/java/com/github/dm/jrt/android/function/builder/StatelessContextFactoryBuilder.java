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

import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;

import org.jetbrains.annotations.NotNull;

/**
 * Builder of stateless invocation factories based on functions handling the invocation lifecycle.
 * <br>
 * The function instances must have a static scope in order to avoid undesired leaks.
 * <p>
 * For example, a factory switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * builder.onNextOutput(String::toUpperCase)
 *        .create();
 * </code></pre>
 * <p>
 * Note that the passed instances are expected to behave like a function, that is, they must not
 * retain a mutable internal state.
 * <br>
 * Note also that any external object used inside the function must be synchronized in order to
 * avoid concurrency issues.
 * <p>
 * Created by davide-maestroni on 05/25/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StatelessContextFactoryBuilder<IN, OUT>
    extends StatelessContextBuilder<IN, OUT, StatelessContextFactoryBuilder<IN, OUT>> {

  /**
   * Builds a new factory instance based on the set functions.
   *
   * @return the Context invocation factory instance.
   */
  @NotNull
  ContextInvocationFactory<IN, OUT> create();
}
