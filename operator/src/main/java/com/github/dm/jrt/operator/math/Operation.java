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

package com.github.dm.jrt.operator.math;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining the operations applicable to a number object.
 * <p>
 * Created by davide-maestroni on 01/25/2017.
 *
 * @param <N> the number type.
 */
public interface Operation<N extends Number> {

  /**
   * Computes the absolute value of the specified number.
   *
   * @param n the number.
   * @return the absolute value.
   * @throws java.lang.IllegalArgumentException if the number instance is of an unsupported type.
   */
  @NotNull
  N abs(@NotNull Number n);

  /**
   * Adds the specified numbers.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the sum.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  N add(@NotNull Number n1, @NotNull Number n2);

  /**
   * Compares the specified numbers.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the comparison result.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  int compare(@NotNull Number n1, @NotNull Number n2);

  /**
   * Converts the specified number.
   *
   * @param n the number to convert.
   * @return the converted number.
   * @throws java.lang.IllegalArgumentException if the number instance is of an unsupported type.
   */
  @NotNull
  N convert(@NotNull Number n);

  /**
   * Divides the specified numbers.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the quotient.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  N divide(@NotNull Number n1, @NotNull Number n2);

  /**
   * Multiplies the specified numbers.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the product.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  N multiply(@NotNull Number n1, @NotNull Number n2);

  /**
   * Negates the specified number.
   *
   * @param n the number.
   * @return the negated value.
   * @throws java.lang.IllegalArgumentException if the number instance is of an unsupported type.
   */
  @NotNull
  N negate(@NotNull Number n);

  /**
   * Computes the remainder of the division of the specified numbers. Note that the remainder is not
   * the same as the modulo, since the result can be negative.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the remainder.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  N remainder(@NotNull Number n1, @NotNull Number n2);

  /**
   * Subtracts the specified numbers.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the difference.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  N subtract(@NotNull Number n1, @NotNull Number n2);
}
