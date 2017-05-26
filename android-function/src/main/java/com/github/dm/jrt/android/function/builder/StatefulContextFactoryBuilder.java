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
 * Builder of stateful invocation factories based on functions handling the invocation lifecycle.
 * <br>
 * The function instances must have a static scope in order to avoid undesired leaks.
 * <br>
 * Be also aware that the state instance will not be retained between invocations, since a Loader
 * routine destroys its invocations as soon as they complete.
 * <p>
 * The state object is created when the invocation starts and modified during the execution.
 * <br>
 * The last instance returned by the finalization function is retained and re-used during the next
 * invocation execution, unless null, in which case a new instance is created.
 * <p>
 * By default, the same state is retained through the whole invocation lifecycle and automatically
 * nulled during the finalization step. Hence, it is advisable to customize the finalization
 * function, in order to be able to re-use the same state instances through successive invocation
 * executions.
 * <br>
 * Note, however, that the state object should be reset on finalization in order to avoid
 * unpredictable behaviors during different invocations.
 * <p>
 * For example, a factory concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * builder.onCreate(StringBuilder::new)
 *        .onNextState(StringBuilder::append)
 *        .onCompleteOutput(StringBuilder::toString)
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
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
public interface StatefulContextFactoryBuilder<IN, OUT, STATE>
    extends StatefulContextBuilder<IN, OUT, STATE, StatefulContextFactoryBuilder<IN, OUT, STATE>> {

  /**
   * Builds a new factory instance based on the set functions.
   *
   * @return the Context invocation factory instance.
   */
  @NotNull
  ContextInvocationFactory<IN, OUT> create();
}
