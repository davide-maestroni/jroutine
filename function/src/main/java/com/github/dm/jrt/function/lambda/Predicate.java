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

package com.github.dm.jrt.function.lambda;

/**
 * Interface representing a predicate (boolean-valued function) of one argument.
 * <p>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN> the input data type.
 */
public interface Predicate<IN> {

  /**
   * Evaluates this predicate on the given argument.
   *
   * @param in the input argument.
   * @return true if the input argument matches the predicate, otherwise false.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  boolean test(IN in) throws Exception;
}
