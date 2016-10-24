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

package com.github.dm.jrt.stream.builder;

import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception indicating that an unrecoverable error occurred while building a stream channel.
 * <p>
 * Created by davide-maestroni on 05/10/2016.
 */
public class StreamBuildingException extends RoutineException {

  /**
   * Constructor.
   *
   * @param cause the wrapped exception.
   */
  private StreamBuildingException(@Nullable final Throwable cause) {
    super(cause);
  }

  /**
   * Wraps the specified throwable.
   * <br>
   * If the cause is an instance of
   * {@link com.github.dm.jrt.core.common.RoutineException RoutineException}, its cause will be
   * wrapped instead.
   *
   * @param cause the throwable to wrap.
   * @return the throwable or a stream exception wrapping it.
   */
  @NotNull
  public static StreamBuildingException wrapIfNeeded(@Nullable final Throwable cause) {
    if (cause instanceof StreamBuildingException) {
      return (StreamBuildingException) cause;
    }

    // Unwrap routine exception to get the original cause
    return new StreamBuildingException(
        (cause instanceof RoutineException) ? cause.getCause() : cause);
  }
}
