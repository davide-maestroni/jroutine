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

package com.github.dm.jrt.function.util;

/**
 * Interface defining a decorator of functions.
 * <p>
 * Created by davide-maestroni on 02/13/2016.
 */
public interface Decorator {

  /**
   * Checks if the class of the wrapped functions are static or top level.
   *
   * @return whether this decorator has a static scope.
   */
  boolean hasStaticScope();
}
