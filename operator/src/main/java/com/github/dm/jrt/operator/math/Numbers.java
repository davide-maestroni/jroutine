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
import java.math.MathContext;
import java.util.HashMap;

/**
 * Utility class handling {@code Number} objects.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
@SuppressWarnings("WeakerAccess")
public class Numbers {

  private static final HashMap<Class<? extends Number>, ExtendedOperation<?>> sOperations =
      new HashMap<Class<? extends Number>, ExtendedOperation<?>>() {{
        put(Byte.class, new ExtendedOperation<Byte>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public Byte abs(@NotNull final Number n) {
            return (byte) Math.abs(n.byteValue());
          }

          @NotNull
          public Byte add(@NotNull final Number n1, @NotNull final Number n2) {
            return (byte) (n1.byteValue() + n2.byteValue());
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.byteValue() - n2.byteValue();
          }

          @NotNull
          public Byte convert(@NotNull final Number n) {
            return n.byteValue();
          }

          @NotNull
          public Byte divide(@NotNull final Number n1, @NotNull final Number n2) {
            return (byte) (n1.byteValue() / n2.byteValue());
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.BYTE;
          }

          @NotNull
          public Byte multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return (byte) (n1.byteValue() * n2.byteValue());
          }

          @NotNull
          public Byte negate(@NotNull final Number n) {
            return (byte) -n.byteValue();
          }

          @NotNull
          public Byte remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return (byte) (n1.byteValue() % n2.byteValue());
          }

          @NotNull
          public Byte subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return (byte) (n1.byteValue() - n2.byteValue());
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final Byte n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final Byte n) {
            return BigInteger.valueOf(n);
          }
        });
        put(Short.class, new ExtendedOperation<Short>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public Short abs(@NotNull final Number n) {
            return (short) Math.abs(n.shortValue());
          }

          @NotNull
          public Short add(@NotNull final Number n1, @NotNull final Number n2) {
            return (short) (n1.shortValue() + n2.shortValue());
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.shortValue() - n2.shortValue();
          }

          @NotNull
          public Short convert(@NotNull final Number n) {
            return n.shortValue();
          }

          @NotNull
          public Short divide(@NotNull final Number n1, @NotNull final Number n2) {
            return (short) (n1.shortValue() / n2.shortValue());
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.SHORT;
          }

          @NotNull
          public Short multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return (short) (n1.shortValue() * n2.shortValue());
          }

          @NotNull
          public Short negate(@NotNull final Number n) {
            return (short) -n.shortValue();
          }

          @NotNull
          public Short remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return (short) (n1.shortValue() % n2.shortValue());
          }

          @NotNull
          public Short subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return (short) (n1.shortValue() - n2.shortValue());
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final Short n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final Short n) {
            return BigInteger.valueOf(n);
          }
        });
        put(Integer.class, new ExtendedOperation<Integer>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public Integer abs(@NotNull final Number n) {
            return Math.abs(n.intValue());
          }

          @NotNull
          public Integer add(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.intValue() + n2.intValue();
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            final int v1 = n1.intValue();
            final int v2 = n2.intValue();
            return (v1 < v2) ? -1 : ((v1 == v2) ? 0 : 1);
          }

          @NotNull
          public Integer convert(@NotNull final Number n) {
            return n.intValue();
          }

          @NotNull
          public Integer divide(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.intValue() / n2.intValue();
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.INTEGER;
          }

          @NotNull
          public Integer multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.intValue() * n2.intValue();
          }

          @NotNull
          public Integer negate(@NotNull final Number n) {
            return -n.intValue();
          }

          @NotNull
          public Integer remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.intValue() % n2.intValue();
          }

          @NotNull
          public Integer subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.intValue() - n2.intValue();
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final Integer n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final Integer n) {
            return BigInteger.valueOf(n);
          }
        });
        put(Long.class, new ExtendedOperation<Long>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public Long abs(@NotNull final Number n) {
            return Math.abs(n.longValue());
          }

          @NotNull
          public Long add(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.longValue() + n2.longValue();
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            final long v1 = n1.longValue();
            final long v2 = n2.longValue();
            return (v1 < v2) ? -1 : ((v1 == v2) ? 0 : 1);
          }

          @NotNull
          public Long convert(@NotNull final Number n) {
            return n.longValue();
          }

          @NotNull
          public Long divide(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.longValue() / n2.longValue();
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.LONG;
          }

          @NotNull
          public Long multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.longValue() * n2.longValue();
          }

          @NotNull
          public Long negate(@NotNull final Number n) {
            return -n.longValue();
          }

          @NotNull
          public Long remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.longValue() % n2.longValue();
          }

          @NotNull
          public Long subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.longValue() - n2.longValue();
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final Long n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final Long n) {
            return BigInteger.valueOf(n);
          }
        });
        put(Float.class, new ExtendedOperation<Float>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public Float abs(@NotNull final Number n) {
            return Math.abs(n.floatValue());
          }

          @NotNull
          public Float add(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.floatValue() + n2.floatValue();
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            final float v1 = n1.floatValue();
            final float v2 = n2.floatValue();
            if (v1 < v2) {
              return -1;

            } else if (v1 > v2) {
              return 1;
            }

            final int i1 = Float.floatToIntBits(v1);
            final int i2 = Float.floatToIntBits(v2);
            return (i1 < i2) ? -1 : ((i1 == i2) ? 0 : 1);
          }

          @NotNull
          public Float convert(@NotNull final Number n) {
            return n.floatValue();
          }

          @NotNull
          public Float divide(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.floatValue() / n2.floatValue();
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.FLOAT;
          }

          @NotNull
          public Float multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.floatValue() * n2.floatValue();
          }

          @NotNull
          public Float negate(@NotNull final Number n) {
            return -n.floatValue();
          }

          @NotNull
          public Float remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.floatValue() % n2.floatValue();
          }

          @NotNull
          public Float subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.floatValue() - n2.floatValue();
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final Float n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final Float n) {
            return BigInteger.valueOf(n.longValue());
          }
        });
        put(Double.class, new ExtendedOperation<Double>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public Double abs(@NotNull final Number n) {
            return Math.abs(n.doubleValue());
          }

          @NotNull
          public Double add(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.doubleValue() + n2.doubleValue();
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            final double v1 = n1.doubleValue();
            final double v2 = n2.doubleValue();
            if (v1 < v2) {
              return -1;

            } else if (v1 > v2) {
              return 1;
            }

            final long l1 = Double.doubleToLongBits(v1);
            final long l2 = Double.doubleToLongBits(v2);
            return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
          }

          @NotNull
          public Double convert(@NotNull final Number n) {
            return n.doubleValue();
          }

          @NotNull
          public Double divide(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.doubleValue() / n2.doubleValue();
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.DOUBLE;
          }

          @NotNull
          public Double multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.doubleValue() * n2.doubleValue();
          }

          @NotNull
          public Double negate(@NotNull final Number n) {
            return -n.doubleValue();
          }

          @NotNull
          public Double remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.doubleValue() % n2.doubleValue();
          }

          @NotNull
          public Double subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return n1.doubleValue() - n2.doubleValue();
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final Double n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final Double n) {
            return BigInteger.valueOf(n.longValue());
          }
        });
        put(BigInteger.class, new ExtendedOperation<BigInteger>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return op.getPrecision().isFloatingPoint();
          }

          @NotNull
          public BigInteger abs(@NotNull final Number n) {
            return convert(n).abs();
          }

          @NotNull
          public BigInteger add(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).add(convert(n2));
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).compareTo(convert(n2));
          }

          @NotNull
          @SuppressWarnings("unchecked")
          public BigInteger convert(@NotNull final Number n) {
            final ExtendedOperation<?> operation = sOperations.get(n.getClass());
            if (operation == null) {
              throw unsupportedException(n.getClass());
            }

            return ((ExtendedOperation<Number>) operation).toBigInteger(n);
          }

          @NotNull
          public BigInteger divide(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).divide(convert(n2));
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.BIG_INTEGER;
          }

          @NotNull
          public BigInteger multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).multiply(convert(n2));
          }

          @NotNull
          public BigInteger negate(@NotNull final Number n) {
            return convert(n).negate();
          }

          @NotNull
          public BigInteger remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).remainder(convert(n2));
          }

          @NotNull
          public BigInteger subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).subtract(convert(n2));
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final BigInteger n) {
            return new BigDecimal(n);
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final BigInteger n) {
            return n;
          }
        });
        put(BigDecimal.class, new ExtendedOperation<BigDecimal>() {

          public boolean replaceWithBigDecimal(@NotNull final ExtendedOperation<?> op) {
            return false;
          }

          @NotNull
          public BigDecimal abs(@NotNull final Number n) {
            return convert(n).abs();
          }

          @NotNull
          public BigDecimal add(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).add(convert(n2));
          }

          public int compare(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).compareTo(convert(n2));
          }

          @NotNull
          @SuppressWarnings("unchecked")
          public BigDecimal convert(@NotNull final Number n) {
            final ExtendedOperation<?> operation = sOperations.get(n.getClass());
            if (operation == null) {
              throw unsupportedException(n.getClass());
            }

            return ((ExtendedOperation<Number>) operation).toBigDecimal(n);
          }

          @NotNull
          public BigDecimal divide(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).divide(convert(n2), MathContext.UNLIMITED);
          }

          @NotNull
          public Precision getPrecision() {
            return Precision.BIG_DECIMAL;
          }

          @NotNull
          public BigDecimal multiply(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).multiply(convert(n2));
          }

          @NotNull
          public BigDecimal negate(@NotNull final Number n) {
            return convert(n).negate();
          }

          @NotNull
          public BigDecimal remainder(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).remainder(convert(n2));
          }

          @NotNull
          public BigDecimal subtract(@NotNull final Number n1, @NotNull final Number n2) {
            return convert(n1).subtract(convert(n2));
          }

          @NotNull
          public BigDecimal toBigDecimal(@NotNull final BigDecimal n) {
            return n;
          }

          @NotNull
          public BigInteger toBigInteger(@NotNull final BigDecimal n) {
            return n.toBigInteger();
          }
        });
      }};

  private static final ExtendedOperation<?> sBigDecimalOperation =
      sOperations.get(BigDecimal.class);

  /**
   * Avoid explicit instantiation.
   */
  protected Numbers() {
    ConstantConditions.avoid();
  }

  /**
   * Computes the absolute value of the specified number.
   *
   * @param n the number.
   * @return the absolute value.
   * @throws java.lang.IllegalArgumentException if the number instance is of an unsupported type.
   */
  @NotNull
  public static Number abs(@NotNull final Number n) {
    return getOperation(n.getClass()).abs(n);
  }

  /**
   * Computes the absolute value of the specified number.
   * <br>
   * If the number instance is of an unsupported type, the result will be null.
   *
   * @param n the number.
   * @return the absolute value or null.
   */
  @Nullable
  public static Number absSafe(@Nullable final Number n) {
    if (n == null) {
      return null;
    }

    final ExtendedOperation<?> operation = sOperations.get(n.getClass());
    if (operation != null) {
      return operation.abs(n);
    }

    return null;
  }

  /**
   * Adds the specified numbers.
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
  public static Number add(@NotNull final Number n1, @NotNull final Number n2) {
    return getHigherPrecisionOperation(n1.getClass(), n2.getClass()).add(n1, n2);
  }

  /**
   * Adds the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instances is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the sum or null.
   */
  @Nullable
  public static Number addSafe(@Nullable final Number n1, @Nullable final Number n2) {
    if ((n1 == null) || (n2 == null)) {
      return null;
    }

    final Operation<?> operation = getHigherPrecisionOperationSafe(n1.getClass(), n2.getClass());
    if (operation != null) {
      return operation.add(n1, n2);
    }

    return null;
  }

  /**
   * Compares the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the comparison result.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  public static Integer compare(@NotNull final Number n1, @NotNull final Number n2) {
    return getHigherPrecisionOperation(n1.getClass(), n2.getClass()).compare(n1, n2);
  }

  /**
   * Compares the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instances is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the comparison result or null.
   */
  @Nullable
  public static Integer compareSafe(@Nullable final Number n1, @Nullable final Number n2) {
    if ((n1 == null) || (n2 == null) || !isSupported(n1.getClass()) || !isSupported(
        n2.getClass())) {
      return null;
    }

    return compare(n1, n2);
  }

  /**
   * Converts a number into the specified type.
   *
   * @param type the type.
   * @param n    the number to convert.
   * @param <N>  the number type.
   * @return the converted number.
   * @throws java.lang.IllegalArgumentException if the specified type or the number instance is is
   *                                            not supported.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <N extends Number> N convertTo(@NotNull final Class<N> type,
      @NotNull final Number n) {
    return (N) getOperation(type).convert(n);
  }

  /**
   * Converts a number into the specified type.
   * <br>
   * If the number instance is of an unsupported type, the result will be null.
   *
   * @param type the type.
   * @param n    the number to convert.
   * @param <N>  the number type.
   * @return the converted number or null.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <N extends Number> N convertToSafe(@Nullable final Class<N> type,
      @Nullable final Number n) {
    if (n == null) {
      return null;
    }

    final Operation<?> operation = sOperations.get(type);
    if (operation != null) {
      return (N) operation.convert(n);
    }

    return null;
  }

  /**
   * Divides the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the quotient.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  public static Number divide(@NotNull final Number n1, @NotNull final Number n2) {
    return getHigherPrecisionOperation(n1.getClass(), n2.getClass()).divide(n1, n2);
  }

  /**
   * Divides the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instances is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the quotient or null.
   */
  @Nullable
  public static Number divideSafe(@Nullable final Number n1, @Nullable final Number n2) {
    if ((n1 == null) || (n2 == null)) {
      return null;
    }

    final Operation<?> operation = getHigherPrecisionOperationSafe(n1.getClass(), n2.getClass());
    if (operation != null) {
      return operation.divide(n1, n2);
    }

    return null;
  }

  /**
   * Gets the operation with the higher precision, choosing between the specified number types.
   *
   * @param type1 the first number type.
   * @param type2 the second number type.
   * @return the operation.
   * @throws java.lang.IllegalArgumentException if one of the two types is not supported.
   */
  @NotNull
  public static Operation<?> getHigherPrecisionOperation(
      @NotNull final Class<? extends Number> type1, @NotNull final Class<? extends Number> type2) {
    final Operation<?> operation = getHigherPrecisionOperationSafe(type1, type2);
    if (operation == null) {
      throw unsupportedException(type1, type2);
    }

    return operation;
  }

  /**
   * Gets the operation with the higher precision, choosing between the specified number types.
   * <br>
   * If one of the two types is not supported, the result will be null.
   *
   * @param type1 the first number type.
   * @param type2 the second number type.
   * @return the operation or null.
   */
  @Nullable
  public static Operation<?> getHigherPrecisionOperationSafe(
      @Nullable final Class<? extends Number> type1,
      @Nullable final Class<? extends Number> type2) {
    final HashMap<Class<? extends Number>, ExtendedOperation<?>> operations = sOperations;
    ExtendedOperation<?> op1 = operations.get(type1);
    if (op1 != null) {
      ExtendedOperation<?> op2 = operations.get(type2);
      if (op2 != null) {
        if (op1.replaceWithBigDecimal(op2)) {
          op1 = sBigDecimalOperation;

        } else if (op2.replaceWithBigDecimal(op1)) {
          op2 = sBigDecimalOperation;
        }

        return (op1.getPrecision().ordinal() > op2.getPrecision().ordinal()) ? op1 : op2;
      }
    }

    return null;
  }

  /**
   * Gets the operation with the lower precision, choosing between the specified number types.
   *
   * @param type1 the first number type.
   * @param type2 the second number type.
   * @return the operation.
   * @throws java.lang.IllegalArgumentException if one of the two types is not supported.
   */
  @NotNull
  public static Operation<?> getLowerPrecisionOperation(
      @NotNull final Class<? extends Number> type1, @NotNull final Class<? extends Number> type2) {
    final Operation<?> operation = getLowerPrecisionOperationSafe(type1, type2);
    if (operation == null) {
      throw unsupportedException(type1, type2);
    }

    return operation;
  }

  /**
   * Gets the operation with the lower precision, choosing between the specified number types.
   * <br>
   * If one of the two types is not supported, the result will be null.
   *
   * @param type1 the first number type.
   * @param type2 the second number type.
   * @return the operation or null.
   */
  @Nullable
  public static Operation<?> getLowerPrecisionOperationSafe(
      @Nullable final Class<? extends Number> type1,
      @Nullable final Class<? extends Number> type2) {
    final HashMap<Class<? extends Number>, ExtendedOperation<?>> operations = sOperations;
    ExtendedOperation<?> op1 = operations.get(type1);
    if (op1 != null) {
      ExtendedOperation<?> op2 = operations.get(type2);
      if (op2 != null) {
        if (op1.replaceWithBigDecimal(op2)) {
          op1 = sBigDecimalOperation;

        } else if (op2.replaceWithBigDecimal(op1)) {
          op2 = sBigDecimalOperation;
        }

        return (op1.getPrecision().ordinal() < op2.getPrecision().ordinal()) ? op1 : op2;
      }
    }

    return null;
  }

  /**
   * Gets the operation relative to the specified number type.
   *
   * @param type the number type.
   * @return the operation.
   * @throws java.lang.IllegalArgumentException if the type is not supported.
   */
  @NotNull
  public static Operation<?> getOperation(@NotNull final Class<? extends Number> type) {
    final ExtendedOperation<?> operation = sOperations.get(type);
    if (operation == null) {
      throw unsupportedException(type);
    }

    return operation;
  }

  /**
   * Gets the operation relative to the specified number type.
   * <br>
   * If the the type is not supported, the result will be null.
   *
   * @param type the number type.
   * @return the operation or null.
   */
  @Nullable
  public static Operation<?> getOperationSafe(@Nullable final Class<? extends Number> type) {
    return sOperations.get(type);
  }

  /**
   * Checks if the specified type is supported.
   *
   * @param type the number type.
   * @return whether the type is supported.
   */
  public static boolean isSupported(@Nullable final Class<? extends Number> type) {
    return (sOperations.get(type) != null);
  }

  /**
   * Multiplies the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the product.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  public static Number multiply(@NotNull final Number n1, @NotNull final Number n2) {
    return getHigherPrecisionOperation(n1.getClass(), n2.getClass()).multiply(n1, n2);
  }

  /**
   * Multiplies the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instances is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the product or null.
   */
  @Nullable
  public static Number multiplySafe(@Nullable final Number n1, @Nullable final Number n2) {
    if ((n1 == null) || (n2 == null)) {
      return null;
    }

    final Operation<?> operation = getHigherPrecisionOperationSafe(n1.getClass(), n2.getClass());
    if (operation != null) {
      return operation.multiply(n1, n2);
    }

    return null;
  }

  /**
   * Negates the specified number.
   *
   * @param n the number.
   * @return the negated value.
   * @throws java.lang.IllegalArgumentException if the number instance is of an unsupported type.
   */
  @NotNull
  public static Number negate(@NotNull final Number n) {
    return getOperation(n.getClass()).negate(n);
  }

  /**
   * Negates the specified number.
   * <br>
   * If the number instance is of an unsupported type, the result will be null.
   *
   * @param n the number.
   * @return the negated value or null.
   */
  @Nullable
  public static Number negateSafe(@Nullable final Number n) {
    if (n == null) {
      return null;
    }

    final ExtendedOperation<?> operation = sOperations.get(n.getClass());
    if (operation != null) {
      return operation.negate(n);
    }

    return null;
  }

  /**
   * Computes the remainder of the division of the specified numbers. Note that the remainder is not
   * the same as the modulo, since the result can be negative.
   * <br>
   * The result type will match the input with the highest precision.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the remainder.
   * @throws java.lang.IllegalArgumentException if one of the two instances is of an unsupported
   *                                            type.
   */
  @NotNull
  public static Number remainder(@NotNull final Number n1, @NotNull final Number n2) {
    return getHigherPrecisionOperation(n1.getClass(), n2.getClass()).remainder(n1, n2);
  }

  /**
   * Computes the remainder of the division of the specified numbers. Note that the remainder is not
   * the same as the modulo, since the result can be negative.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instances is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the remainder or null.
   */
  @Nullable
  public static Number remainderSafe(@Nullable final Number n1, @Nullable final Number n2) {
    if ((n1 == null) || (n2 == null)) {
      return null;
    }

    final Operation<?> operation = getHigherPrecisionOperationSafe(n1.getClass(), n2.getClass());
    if (operation != null) {
      return operation.remainder(n1, n2);
    }

    return null;
  }

  /**
   * Subtracts the specified numbers.
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
  public static Number subtract(@NotNull final Number n1, @NotNull final Number n2) {
    return getHigherPrecisionOperation(n1.getClass(), n2.getClass()).subtract(n1, n2);
  }

  /**
   * Subtracts the specified numbers.
   * <br>
   * The result type will match the input with the highest precision.
   * <br>
   * If one of the two instances is of an unsupported type, the result will be null.
   *
   * @param n1 the first number.
   * @param n2 the second number.
   * @return the difference or null.
   */
  @Nullable
  public static Number subtractSafe(@Nullable final Number n1, @Nullable final Number n2) {
    if ((n1 == null) || (n2 == null)) {
      return null;
    }

    final Operation<?> operation = getHigherPrecisionOperationSafe(n1.getClass(), n2.getClass());
    if (operation != null) {
      return operation.subtract(n1, n2);
    }

    return null;
  }

  @NotNull
  private static IllegalArgumentException unsupportedException(
      @Nullable final Class<? extends Number> type) {
    return new IllegalArgumentException(
        "unsupported Number class: [" + ((type != null) ? type.getCanonicalName() : null) + "]");
  }

  @NotNull
  private static IllegalArgumentException unsupportedException(
      @Nullable final Class<? extends Number> type1,
      @Nullable final Class<? extends Number> type2) {
    if ((type1 == null) || !isSupported(type1)) {
      if ((type2 == null) || !isSupported(type2)) {
        return new IllegalArgumentException(
            "unsupported Number classes: [" + ((type1 != null) ? type1.getCanonicalName() : null)
                + ", " + ((type2 != null) ? type2.getCanonicalName() : null) + "]");
      }

      return unsupportedException(type1);
    }

    return unsupportedException(type2);
  }

  /**
   * Enumeration used to order number precisions.
   */
  private enum Precision {

    BYTE(false),
    SHORT(false),
    INTEGER(false),
    LONG(false),
    FLOAT(true),
    DOUBLE(true),
    BIG_INTEGER(false),
    BIG_DECIMAL(true);

    private final boolean mIsFloating;

    /**
     * Constructor.
     *
     * @param isFloating whether this is a floating point operation.
     */
    Precision(final boolean isFloating) {
      mIsFloating = isFloating;
    }

    /**
     * Checks if this is a floating point operation.
     *
     * @return whether this is a floating point operation.
     */
    boolean isFloatingPoint() {
      return mIsFloating;
    }
  }

  /**
   * Extended operation interface.
   *
   * @param <N> the number type.
   */
  private interface ExtendedOperation<N extends Number> extends Operation<N> {

    /**
     * Returns the result precision.
     *
     * @return the precision.
     */
    @NotNull
    Precision getPrecision();

    /**
     * Checks if this operation needs to be replaced with the BigDecimal one when compared to the
     * specified one.
     *
     * @param op the other operation.
     * @return whether this operation needs to be replaced with the BigDecimal one.
     */
    boolean replaceWithBigDecimal(@NotNull ExtendedOperation<?> op);

    /**
     * Converts the specified number into a {@code BigDecimal}.
     *
     * @param n the number to convert.
     * @return the {@code BigDecimal} instance or null.
     */
    @NotNull
    BigDecimal toBigDecimal(@NotNull N n);

    /**
     * Converts the specified number into a {@code BigInteger}.
     *
     * @param n the number to convert.
     * @return the {@code BigInteger} instance or null.
     */
    @NotNull
    BigInteger toBigInteger(@NotNull N n);
  }
}
