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

package com.github.dm.jrt.core.util;

import org.jetbrains.annotations.Nullable;

/**
 * Thread local with an initial immutable value.
 * <p>
 * Created by davide-maestroni on 06/11/2016.
 *
 * @param <T> the value type.
 */
public class LocalValue<T> extends ThreadLocal<T> {

  private final T mInitialValue;

  /**
   * Constructor.
   *
   * @param initialValue the initial value.
   */
  public LocalValue(@Nullable final T initialValue) {
    mInitialValue = initialValue;
  }

  @Override
  protected T initialValue() {
    return mInitialValue;
  }
}
