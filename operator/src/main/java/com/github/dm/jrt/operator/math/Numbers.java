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
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * Utility class handling {@code Number} objects.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
@SuppressWarnings("WeakerAccess")
public class Numbers {

  private static final HashMap<Class<? extends Number>, ToBigDecimalFunction> sBigDecimalFunctions =
      new HashMap<Class<? extends Number>, ToBigDecimalFunction>() {{
        put(Byte.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal(n.byteValue());
          }
        });
        put(Short.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal(n.shortValue());
          }
        });
        put(Integer.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal(n.intValue());
          }
        });
        put(Long.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal(n.longValue());
          }
        });
        put(Float.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal(n.floatValue());
          }
        });
        put(Double.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal(n.doubleValue());
          }
        });
        put(BigInteger.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return new BigDecimal((BigInteger) n);
          }
        });
        put(BigDecimal.class, new ToBigDecimalFunction() {

          public BigDecimal apply(final Number n) {
            return (BigDecimal) n;
          }
        });
      }};

  private static final HashMap<Class<? extends Number>, PrecisionFunction> sAddFunctions =
      new HashMap<Class<? extends Number>, PrecisionFunction>() {{
        final int[] precision = new int[1];
        put(Byte.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return (byte) (n1.byteValue() + n2.byteValue());
          }
        });
        put(Short.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return (short) (n1.shortValue() + n2.shortValue());
          }
        });
        put(Integer.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.intValue() + n2.intValue();
          }
        });
        put(Long.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.longValue() + n2.longValue();
          }
        });
        put(Float.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.floatValue() + n2.floatValue();
          }
        });
        put(Double.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.doubleValue() + n2.doubleValue();
          }
        });
        put(BigInteger.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            final BigDecimal big1 = toBigDecimal(n1);
            final BigDecimal big2 = toBigDecimal(n2);
            if ((big1 == null) || (big2 == null)) {
              return null;
            }

            final BigDecimal decimal = big1.add(big2);
            return (decimal.scale() > 0) ? decimal : decimal.toBigInteger();
          }
        });
        put(BigDecimal.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            final BigDecimal big1 = toBigDecimal(n1);
            final BigDecimal big2 = toBigDecimal(n2);
            if ((big1 == null) || (big2 == null)) {
              return null;
            }

            return big1.add(big2);
          }
        });
      }};

  private static final HashMap<Class<? extends Number>, PrecisionFunction> sSubtractFunctions =
      new HashMap<Class<? extends Number>, PrecisionFunction>() {{
        final int[] precision = new int[1];
        put(Byte.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return (byte) (n1.byteValue() - n2.byteValue());
          }
        });
        put(Short.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return (short) (n1.shortValue() - n2.shortValue());
          }
        });
        put(Integer.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.intValue() - n2.intValue();
          }
        });
        put(Long.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.longValue() - n2.longValue();
          }
        });
        put(Float.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.floatValue() - n2.floatValue();
          }
        });
        put(Double.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            return n1.doubleValue() - n2.doubleValue();
          }
        });
        put(BigInteger.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            final BigDecimal big1 = toBigDecimal(n1);
            final BigDecimal big2 = toBigDecimal(n2);
            if ((big1 == null) || (big2 == null)) {
              return null;
            }

            final BigDecimal decimal = big1.subtract(big2);
            return (decimal.scale() > 0) ? decimal : decimal.toBigInteger();
          }
        });
        put(BigDecimal.class, new PrecisionFunction() {

          private int mPrecision = precision[0]++;

          public int getPrecision() {
            return mPrecision;
          }

          public Number apply(final Number n1, final Number n2) {
            final BigDecimal big1 = toBigDecimal(n1);
            final BigDecimal big2 = toBigDecimal(n2);
            if ((big1 == null) || (big2 == null)) {
              return null;
            }

            return big1.subtract(big2);
          }
        });
      }};

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
   * If one of the two instance is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the sum or null.
   */
  @Nullable
  public static Number add(@NotNull final Number n1, @NotNull final Number n2) {
    final HashMap<Class<? extends Number>, PrecisionFunction> addFunctions = sAddFunctions;
    final PrecisionFunction add = getFunction(addFunctions, n1, n2);
    if (add != null) {
      return add.apply(n1, n2);
    }

    if ((n1 instanceof BigDecimal) || (n2 instanceof BigDecimal)) {
      return addFunctions.get(BigDecimal.class).apply(n1, n2);

    } else if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {
      return addFunctions.get(BigInteger.class).apply(n1, n2);
    }

    return null;
  }

  /**
   * Adds the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instance is of an unsupported type, the double value will be employed in
   * the computation.
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
    final Number number = add(n1, n2);
    if (number == null) {
      if (!isSupported(n1.getClass())) {
        if (!isSupported(n2.getClass())) {
          throw new IllegalArgumentException(
              "unsupported Number classes: [" + n1.getClass().getCanonicalName() + ", "
                  + n2.getClass().getCanonicalName() + "]");
        }

        throw new IllegalArgumentException(
            "unsupported Number class: [" + n1.getClass().getCanonicalName() + "]");
      }

      throw new IllegalArgumentException(
          "unsupported Number class: [" + n2.getClass().getCanonicalName() + "]");
    }

    return number;
  }

  /**
   * Checks if the specified type is supported.
   *
   * @param type the number type.
   * @return whether the type is supported.
   */
  public static boolean isSupported(@NotNull final Class<? extends Number> type) {
    return (sAddFunctions.get(type) != null) || BigInteger.class.isAssignableFrom(type)
        || BigDecimal.class.isAssignableFrom(type);
  }

  /**
   * Subtracts the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instance is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the difference or null.
   */
  @Nullable
  public static Number subtract(@NotNull final Number n1, @NotNull final Number n2) {
    final HashMap<Class<? extends Number>, PrecisionFunction> subtractFunctions =
        sSubtractFunctions;
    final PrecisionFunction add = getFunction(subtractFunctions, n1, n2);
    if (add != null) {
      return add.apply(n1, n2);
    }

    if ((n1 instanceof BigDecimal) || (n2 instanceof BigDecimal)) {
      return subtractFunctions.get(BigDecimal.class).apply(n1, n2);

    } else if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {
      return subtractFunctions.get(BigInteger.class).apply(n1, n2);
    }

    return null;
  }

  /**
   * Subtracts the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instance is of an unsupported type, the double value will be employed in
   * the computation.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the difference.
   */
  @NotNull
  @SuppressWarnings("ConstantConditions")
  public static Number subtractOptimistic(@NotNull final Number n1, @NotNull final Number n2) {
    final Number number = subtract(n1, n2);
    if (number == null) {
      final Number number1 = isSupported(n1.getClass()) ? n1 : n1.doubleValue();
      final Number number2 = isSupported(n2.getClass()) ? n2 : n2.doubleValue();
      return subtract(number1, number2);
    }

    return number;
  }

  /**
   * Subtracts the specified number objects.
   * <br>
   * The result type will match the input with the highest precision.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the difference.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  @SuppressWarnings("ConstantConditions")
  public static Number subtractSafe(@NotNull final Number n1, @NotNull final Number n2) {
    final Number number = subtract(n1, n2);
    if (number == null) {
      if (!isSupported(n1.getClass())) {
        if (!isSupported(n2.getClass())) {
          throw new IllegalArgumentException(
              "unsupported Number classes: [" + n1.getClass().getCanonicalName() + ", "
                  + n2.getClass().getCanonicalName() + "]");
        }

        throw new IllegalArgumentException(
            "unsupported Number class: [" + n1.getClass().getCanonicalName() + "]");
      }

      throw new IllegalArgumentException(
          "unsupported Number class: [" + n2.getClass().getCanonicalName() + "]");
    }

    return number;
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
    final HashMap<Class<? extends Number>, ToBigDecimalFunction> bigDecimalFunctions =
        sBigDecimalFunctions;
    final ToBigDecimalFunction function = bigDecimalFunctions.get(n.getClass());
    if (function != null) {
      return function.apply(n);
    }

    if (n instanceof BigInteger) {
      return bigDecimalFunctions.get(BigInteger.class).apply(n);

    } else if (n instanceof BigDecimal) {
      return bigDecimalFunctions.get(BigDecimal.class).apply(n);
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

  @Nullable
  private static PrecisionFunction getFunction(
      @NotNull final HashMap<Class<? extends Number>, PrecisionFunction> functions,
      @NotNull final Number n1, @NotNull final Number n2) {
    final PrecisionFunction f1 = functions.get(n1.getClass());
    if (f1 != null) {
      final PrecisionFunction f2 = functions.get(n2.getClass());
      if (f2 != null) {
        return (f1.getPrecision() > f2.getPrecision()) ? f1 : f2;
      }
    }

    return null;
  }

  /**
   * Function storing also the operand precision.
   */
  private interface PrecisionFunction extends BiFunction<Number, Number, Number> {

    /**
     * {@inheritDoc}
     */
    Number apply(Number n1, Number n2);

    /**
     * Returns the operand precision.
     *
     * @return the precision.
     */
    int getPrecision();
  }

  /**
   * Function converting a number into a BigDecimal object.
   */
  private interface ToBigDecimalFunction extends Function<Number, BigDecimal> {

    /**
     * {@inheritDoc}
     */
    BigDecimal apply(Number number);
  }
}
