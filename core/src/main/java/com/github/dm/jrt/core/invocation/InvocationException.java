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

package com.github.dm.jrt.core.invocation;

import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception wrapping any throwable escaping an invocation execution.
 * <p>
 * Created by davide-maestroni on 09/08/2014.
 */
public class InvocationException extends RoutineException {

  /**
   * Constructor.
   *
   * @param cause the wrapped exception.
   */
  public InvocationException(@Nullable final Throwable cause) {
    super(cause);
  }

  /**
   * Wraps the specified throwable only if it is not an instance of
   * {@link com.github.dm.jrt.core.common.RoutineException RoutineException}.
   *
   * @param cause the throwable to wrap.
   * @return the throwable or an invocation exception wrapping it.
   */
  @NotNull
  public static RoutineException wrapIfNeeded(@Nullable final Throwable cause) {
    return (cause instanceof RoutineException) ? (RoutineException) cause
        : new InvocationException(cause);
  }
}
