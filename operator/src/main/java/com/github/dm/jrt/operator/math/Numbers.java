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

package com.github.dm.jrt.operator.math;

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility class handling {@code Number} objects.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
public class Numbers {

  /**
   * Avoid explicit instantiation.
   */
  protected Numbers() {
    ConstantConditions.avoid();
  }

  /**
   * Adds the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instance is of an unsupported type, the sum will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the sum or null.
   */
  @Nullable
  public static Number add(@NotNull final Number n1, @NotNull final Number n2) {
    if ((n1 instanceof BigDecimal) || (n2 instanceof BigDecimal)) {
      final BigDecimal big1 = toBigDecimal(n1);
      final BigDecimal big2 = toBigDecimal(n2);
      if ((big1 == null) || (big2 == null)) {
        return null;
      }

      return big1.add(big2);

    } else if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {
      final BigDecimal big1 = toBigDecimal(n1);
      final BigDecimal big2 = toBigDecimal(n2);
      if ((big1 == null) || (big2 == null)) {
        return null;
      }

      final BigDecimal decimal = big1.add(big2);
      return (decimal.scale() > 0) ? decimal : decimal.toBigInteger();

    } else if ((n1 instanceof Double) || (n2 instanceof Double)) {
      return n1.doubleValue() + n2.doubleValue();

    } else if ((n1 instanceof Float) || (n2 instanceof Float)) {
      return n1.floatValue() + n2.floatValue();

    } else if ((n1 instanceof Long) || (n2 instanceof Long)) {
      return n1.longValue() + n2.longValue();

    } else if ((n1 instanceof Integer) || (n2 instanceof Integer)) {
      return n1.intValue() + n2.intValue();

    } else if ((n1 instanceof Short) || (n2 instanceof Short)) {
      return (short) (n1.shortValue() + n2.shortValue());

    } else if ((n1 instanceof Byte) || (n2 instanceof Byte)) {
      return (byte) (n1.byteValue() + n2.byteValue());
    }

    return null;
  }

  /**
   * Adds the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instance is of an unsupported type, the double value will be employed in
   * the computation of the sum.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the sum.
   */
  @NotNull
  @SuppressWarnings("ConstantConditions")
  public static Number addOptimistic(@NotNull final Number n1, @NotNull final Number n2) {
    final Number number = add(n1, n2);
    if (number == null) {
      final Number number1 = isSupported(n1.getClass()) ? n1 : n1.doubleValue();
      final Number number2 = isSupported(n2.getClass()) ? n2 : n2.doubleValue();
      return add(number1, number2);
    }

    return number;
  }

  /**
   * Adds the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the sum.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  @SuppressWarnings("ConstantConditions")
  public static Number addSafe(@NotNull final Number n1, @NotNull final Number n2) {
    if (!isSupported(n1.getClass()) || !isSupported(n2.getClass())) {
      throw new IllegalArgumentException(
          "unsupported Number class: [" + n1.getClass().getCanonicalName() + ", " + n1.getClass()
                                                                                      .getCanonicalName()
              + "]");
    }

    return add(n1, n2);
  }

  /**
   * Checks if the specified type is supported.
   *
   * @param type the number type.
   * @return whether the type is supported.
   */
  public static boolean isSupported(@NotNull final Class<? extends Number> type) {
    return Integer.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)
        || Float.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)
        || Short.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)
        || BigInteger.class.isAssignableFrom(type) || BigDecimal.class.isAssignableFrom(type);
  }

  /**
   * Converts the specified number into a {@code BigDecimal}.
   * <br>
   * If the number instance is of an unsupported type, the result will be null.
   *
   * @param n the number to convert.
   * @return the {@code BigDecimal} instance or null.
   */
  @Nullable
  public static BigDecimal toBigDecimal(@NotNull final Number n) {
    if (n instanceof Double) {
      return new BigDecimal(n.doubleValue());

    } else if (n instanceof Float) {
      return new BigDecimal(n.floatValue());

    } else if (n instanceof Long) {
      return new BigDecimal(n.longValue());

    } else if (n instanceof Integer) {
      return new BigDecimal(n.intValue());

    } else if (n instanceof Short) {
      return new BigDecimal(n.shortValue());

    } else if (n instanceof Byte) {
      return new BigDecimal(n.byteValue());

    } else if (n instanceof BigInteger) {
      return new BigDecimal(((BigInteger) n));

    } else if (n instanceof BigDecimal) {
      return (BigDecimal) n;
    }

    return null;
  }

  /**
   * Converts the specified number into a {@code BigDecimal}.
   * <br>
   * If the number instance is of an unsupported type, the double value will be employed in the
   * conversion.
   *
   * @param n the number to convert.
   * @return the {@code BigDecimal} instance.
   */
  @NotNull
  public static BigDecimal toBigDecimalOptimistic(@NotNull final Number n) {
    final BigDecimal bigDecimal = toBigDecimal(n);
    return (bigDecimal == null) ? new BigDecimal(n.doubleValue()) : bigDecimal;
  }

  /**
   * Converts the specified number into a {@code BigDecimal}.
   *
   * @param n the number to convert.
   * @return the {@code BigDecimal} instance.
   * @throws java.lang.IllegalArgumentException if the number instance is of an unsupported type.
   */
  @NotNull
  public static BigDecimal toBigDecimalSafe(@NotNull final Number n) {
    final BigDecimal bigDecimal = toBigDecimal(n);
    if (bigDecimal == null) {
      throw new IllegalArgumentException(
          "unsupported Number class: [" + n.getClass().getCanonicalName() + "]");
    }

    return bigDecimal;
  }
}
