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

package com.github.dm.jrt.android.core.invocation;

import com.github.dm.jrt.core.error.RoutineException;

/**
 * Base exception indicating that an unrecoverable error occurred during a Loader invocation
 * execution.
 * <p>
 * Created by davide-maestroni on 06/03/2015.
 */
@SuppressWarnings("WeakerAccess")
public class LoaderInvocationException extends RoutineException {

  private final int mId;

  /**
   * Constructor.
   */
  public LoaderInvocationException() {
    this(-1);
  }

  /**
   * Constructor.
   *
   * @param id the Loader ID.
   */
  public LoaderInvocationException(final int id) {
    mId = id;
  }

  /**
   * Returns the Loader ID.
   *
   * @return the Loader ID.
   */
  public int getId() {
    return mId;
  }
}
