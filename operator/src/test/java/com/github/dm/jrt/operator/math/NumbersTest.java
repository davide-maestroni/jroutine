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

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Numbers unit tests.
 * <p>
 * Created by davide-maestroni on 05/05/2016.
 */
public class NumbersTest {

  @Test
  public void testAbs() {
    assertThat(Numbers.abs(-3)).isEqualTo(3);
    assertThat(Numbers.abs((byte) 2)).isEqualTo((byte) 2);
    assertThat(Numbers.abs((short) -1)).isEqualTo((short) 1);
    assertThat(Numbers.abs(-1L)).isEqualTo(1L);
    assertThat(Numbers.abs(2.5f)).isEqualTo(2.5f);
    assertThat(Numbers.abs(-2.5)).isEqualTo(2.5);
    assertThat(Numbers.abs(BigDecimal.ZERO)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.abs(BigInteger.ONE)).isEqualTo(BigInteger.ONE);
    assertThat(Numbers.abs(new MyNumber())).isNull();
  }

  @Test
  public void testAbsSafe() {
    assertThat(Numbers.absSafe(-3)).isEqualTo(3);
    assertThat(Numbers.absSafe((byte) 2)).isEqualTo((byte) 2);
    assertThat(Numbers.absSafe((short) -1)).isEqualTo((short) 1);
    assertThat(Numbers.absSafe(-1L)).isEqualTo(1L);
    assertThat(Numbers.absSafe(2.5f)).isEqualTo(2.5f);
    assertThat(Numbers.absSafe(-2.5)).isEqualTo(2.5);
    assertThat(Numbers.absSafe(BigDecimal.ZERO)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.absSafe(BigInteger.ONE)).isEqualTo(BigInteger.ONE);
    try {
      assertThat(Numbers.absSafe(new MyNumber()));

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testAdd() {
    assertThat(Numbers.add(1, 2)).isEqualTo(3);
    assertThat(Numbers.add(1, (byte) 2)).isEqualTo(3);
    assertThat(Numbers.add((short) -1, 2.5f)).isEqualTo(1.5f);
    assertThat(Numbers.add(-1L, 2.5)).isEqualTo(1.5);
    assertThat(Numbers.add(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(2.5));
    assertThat(Numbers.add(BigDecimal.ONE, new MyNumber())).isNull();
    assertThat(Numbers.add(new MyNumber(), BigDecimal.ONE)).isNull();
    assertThat(Numbers.add(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(3.5));
    assertThat(Numbers.add(BigInteger.ONE, -1)).isEqualTo(BigInteger.ZERO);
    assertThat(Numbers.add(BigInteger.ONE, new MyNumber())).isNull();
    assertThat(Numbers.add(new MyNumber(), BigInteger.ONE)).isNull();
    assertThat(Numbers.add(new MyNumber(), new MyNumber())).isNull();
  }

  @Test
  public void testAddSafe() {
    assertThat(Numbers.addSafe(1, 2)).isEqualTo(3);
    assertThat(Numbers.addSafe(1, (byte) 2)).isEqualTo(3);
    assertThat(Numbers.addSafe((short) -1, 2.5f)).isEqualTo(1.5f);
    assertThat(Numbers.addSafe(-1L, 2.5)).isEqualTo(1.5);
    assertThat(Numbers.addSafe(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(2.5));
    try {
      Numbers.addSafe(BigDecimal.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.addSafe(new MyNumber(), BigDecimal.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.addSafe(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(3.5));
    assertThat(Numbers.addSafe(BigInteger.ONE, -1)).isEqualTo(BigInteger.ZERO);
    try {
      Numbers.addSafe(BigInteger.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.addSafe(new MyNumber(), BigInteger.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.addSafe(new MyNumber(), new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testCompare() {
    assertThat(Numbers.compare(1, 2)).isEqualTo(-1);
    assertThat(Numbers.compare(1, (byte) 2)).isEqualTo(-1);
    assertThat(Numbers.compare((short) -1, 2.5f)).isEqualTo(-1);
    assertThat(Numbers.compare(-1L, 2.5)).isEqualTo(-1);
    assertThat(Numbers.compare(-1.5, -1.5f)).isEqualTo(0);
    assertThat(Numbers.compare(3.5f, 3.5f)).isEqualTo(0);
    assertThat(Numbers.compare(BigDecimal.ZERO, 2.5)).isEqualTo(-1);
    assertThat(Numbers.compare(BigDecimal.ONE, BigDecimal.ONE)).isEqualTo(0);
    assertThat(Numbers.compare(BigDecimal.ONE, new MyNumber())).isNull();
    assertThat(Numbers.compare(new MyNumber(), BigDecimal.ONE)).isNull();
    assertThat(Numbers.compare(BigInteger.ONE, 2.5)).isEqualTo(-1);
    assertThat(Numbers.compare(BigInteger.ONE, -1)).isEqualTo(1);
    assertThat(Numbers.compare(BigInteger.ONE, new MyNumber())).isNull();
    assertThat(Numbers.compare(new MyNumber(), BigInteger.ONE)).isNull();
    assertThat(Numbers.compare(new MyNumber(), new MyNumber())).isNull();
  }

  @Test
  public void testCompareSafe() {
    assertThat(Numbers.compareSafe(1, 2)).isEqualTo(-1);
    assertThat(Numbers.compareSafe(1, (byte) 2)).isEqualTo(-1);
    assertThat(Numbers.compareSafe((short) -1, 2.5f)).isEqualTo(-1);
    assertThat(Numbers.compareSafe(-1L, 2.5)).isEqualTo(-1);
    assertThat(Numbers.compareSafe(-1.5, -1.5f)).isEqualTo(0);
    assertThat(Numbers.compareSafe(BigDecimal.ZERO, 2.5)).isEqualTo(-1);
    assertThat(Numbers.compareSafe(BigDecimal.ONE, BigDecimal.ONE)).isEqualTo(0);
    try {
      Numbers.compareSafe(BigDecimal.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.compareSafe(new MyNumber(), BigDecimal.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.compareSafe(BigInteger.ONE, 2.5)).isEqualTo(-1);
    assertThat(Numbers.compareSafe(BigInteger.ONE, -1)).isEqualTo(1);
    try {
      Numbers.compareSafe(BigInteger.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.compareSafe(new MyNumber(), BigInteger.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.compareSafe(new MyNumber(), new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testConstructor() {
    boolean failed = false;
    try {
      new Numbers();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testConversion() {
    assertThat(Numbers.convertTo(Byte.class, 3)).isEqualTo((byte) 3);
    assertThat(Numbers.convertTo(Float.class, (byte) 2)).isEqualTo(2f);
    assertThat(Numbers.convertTo(Double.class, (short) -1)).isEqualTo(-1d);
    assertThat(Numbers.convertTo(Short.class, -1L)).isEqualTo((short) -1);
    assertThat(Numbers.convertTo(BigDecimal.class, (short) 3)).isEqualTo(new BigDecimal(3));
    assertThat(Numbers.convertTo(BigDecimal.class, 2.5f)).isEqualTo(new BigDecimal(2.5f));
    assertThat(Numbers.convertTo(BigDecimal.class, -7L)).isEqualTo(new BigDecimal(-7));
    assertThat(Numbers.convertTo(BigInteger.class, 2.5)).isEqualTo(BigInteger.valueOf(2));
    assertThat(Numbers.convertTo(BigInteger.class, (short) 3)).isEqualTo(BigInteger.valueOf(3));
    assertThat(Numbers.convertTo(BigInteger.class, 2.5f)).isEqualTo(BigInteger.valueOf(2));
    assertThat(Numbers.convertTo(BigInteger.class, new BigDecimal(2.5))).isEqualTo(
        BigInteger.valueOf(2));
    assertThat(Numbers.convertTo(Long.class, BigDecimal.ZERO)).isEqualTo(0L);
    assertThat(Numbers.convertTo(Integer.class, BigInteger.ONE)).isEqualTo(1);
    assertThat(Numbers.convertTo(Integer.class, new MyNumber())).isEqualTo(0);
    assertThat(Numbers.convertTo(MyNumber.class, 0)).isNull();
  }

  @Test
  public void testConversionSafe() {
    assertThat(Numbers.convertToSafe(Byte.class, 3)).isEqualTo((byte) 3);
    assertThat(Numbers.convertToSafe(Float.class, (byte) 2)).isEqualTo(2f);
    assertThat(Numbers.convertToSafe(Double.class, (short) -1)).isEqualTo(-1d);
    assertThat(Numbers.convertToSafe(Short.class, -1L)).isEqualTo((short) -1);
    assertThat(Numbers.convertToSafe(BigDecimal.class, 2.5f)).isEqualTo(new BigDecimal(2.5f));
    assertThat(Numbers.convertToSafe(BigInteger.class, 2.5)).isEqualTo(BigInteger.valueOf(2));
    assertThat(Numbers.convertToSafe(Long.class, BigDecimal.ZERO)).isEqualTo(0L);
    assertThat(Numbers.convertToSafe(Integer.class, BigInteger.ONE)).isEqualTo(1);
    assertThat(Numbers.convertToSafe(Integer.class, new MyNumber())).isEqualTo(0);

    try {
      assertThat(Numbers.convertToSafe(BigInteger.class, new MyNumber()));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      assertThat(Numbers.convertToSafe(BigDecimal.class, new MyNumber()));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      assertThat(Numbers.convertToSafe(MyNumber.class, 0)).isEqualTo(0d);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testDivide() {
    assertThat(Numbers.divide(1, 2)).isEqualTo(0);
    assertThat(Numbers.divide(1, (byte) 2)).isEqualTo(0);
    assertThat(Numbers.divide((byte) 2, (byte) 2)).isEqualTo((byte) 1);
    assertThat(Numbers.divide((short) -1, 2.5f)).isEqualTo(-0.4f);
    assertThat(Numbers.divide((short) 5, (byte) 2)).isEqualTo((short) 2);
    assertThat(Numbers.divide(-1L, 2.5)).isEqualTo(-0.4);
    assertThat(Numbers.divide(BigDecimal.ZERO, 2.5)).isEqualTo(
        BigDecimal.ZERO.setScale(-1, BigDecimal.ROUND_UNNECESSARY));
    assertThat(Numbers.divide(BigDecimal.ONE, new MyNumber())).isNull();
    assertThat(Numbers.divide(new MyNumber(), BigDecimal.ONE)).isNull();
    assertThat(Numbers.divide(BigInteger.ONE, 2.5)).isEqualTo(
        new BigDecimal(0.4).setScale(1, BigDecimal.ROUND_HALF_UP));
    assertThat(Numbers.divide(BigInteger.ONE, -1)).isEqualTo(BigInteger.valueOf(-1));
    assertThat(Numbers.divide(BigInteger.ONE, new MyNumber())).isNull();
    assertThat(Numbers.divide(new MyNumber(), BigInteger.ONE)).isNull();
    assertThat(Numbers.divide(new MyNumber(), new MyNumber())).isNull();
  }

  @Test
  public void testDivideSafe() {
    assertThat(Numbers.divideSafe(1, 2)).isEqualTo(0);
    assertThat(Numbers.divideSafe(1, (byte) 2)).isEqualTo(0);
    assertThat(Numbers.divideSafe((short) -1, 2.5f)).isEqualTo(-0.4f);
    assertThat(Numbers.divideSafe(-1L, 2.5)).isEqualTo(-0.4);
    assertThat(Numbers.divideSafe(BigDecimal.ZERO, 2.5)).isEqualTo(
        BigDecimal.ZERO.setScale(-1, BigDecimal.ROUND_UNNECESSARY));
    try {
      Numbers.divideSafe(BigDecimal.ONE, new MyNumber() {

        @Override
        public double doubleValue() {
          return 1;
        }
      });
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.divideSafe(new MyNumber(), BigDecimal.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.divideSafe(BigInteger.ONE, 2.5)).isEqualTo(
        new BigDecimal(0.4).setScale(1, BigDecimal.ROUND_HALF_UP));
    assertThat(Numbers.divideSafe(BigInteger.ONE, -1)).isEqualTo(BigInteger.valueOf(-1));
    try {
      Numbers.divideSafe(BigInteger.ONE, new MyNumber() {

        @Override
        public double doubleValue() {
          return 1;
        }
      });
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.divideSafe(new MyNumber(), BigInteger.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.divideSafe(new MyNumber(), new MyNumber() {

        @Override
        public double doubleValue() {
          return 1;
        }
      });
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testMultiply() {
    assertThat(Numbers.multiply(1, 2)).isEqualTo(2);
    assertThat(Numbers.multiply(1, (byte) 2)).isEqualTo(2);
    assertThat(Numbers.multiply((byte) 2, (byte) 2)).isEqualTo((byte) 4);
    assertThat(Numbers.multiply((short) -1, 2.5f)).isEqualTo(-2.5f);
    assertThat(Numbers.multiply((short) 2, (byte) 2)).isEqualTo((short) 4);
    assertThat(Numbers.multiply(-1L, 2.5)).isEqualTo(-2.5);
    assertThat(Numbers.multiply(-1L, 2)).isEqualTo(-2L);
    assertThat(Numbers.multiply(BigDecimal.ZERO, 2.5)).isEqualTo(
        BigDecimal.ZERO.setScale(1, BigDecimal.ROUND_HALF_UP));
    assertThat(Numbers.multiply(BigDecimal.ONE, new MyNumber())).isNull();
    assertThat(Numbers.multiply(new MyNumber(), BigDecimal.ONE)).isNull();
    assertThat(Numbers.multiply(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(2.5));
    assertThat(Numbers.multiply(BigInteger.ONE, -1)).isEqualTo(BigInteger.valueOf(-1));
    assertThat(Numbers.multiply(BigInteger.ONE, new MyNumber())).isNull();
    assertThat(Numbers.multiply(new MyNumber(), BigInteger.ONE)).isNull();
    assertThat(Numbers.multiply(new MyNumber(), new MyNumber())).isNull();
  }

  @Test
  public void testMultiplySafe() {
    assertThat(Numbers.multiplySafe(1, 2)).isEqualTo(2);
    assertThat(Numbers.multiplySafe(1, (byte) 2)).isEqualTo(2);
    assertThat(Numbers.multiplySafe((short) -1, 2.5f)).isEqualTo(-2.5f);
    assertThat(Numbers.multiplySafe(-1L, 2.5)).isEqualTo(-2.5);
    assertThat(Numbers.multiplySafe(BigDecimal.ZERO, 2.5)).isEqualTo(
        BigDecimal.ZERO.setScale(1, BigDecimal.ROUND_HALF_UP));
    try {
      Numbers.multiplySafe(BigDecimal.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.multiplySafe(new MyNumber(), BigDecimal.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.multiplySafe(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(2.5));
    assertThat(Numbers.multiplySafe(BigInteger.ONE, -1)).isEqualTo(BigInteger.valueOf(-1));
    try {
      Numbers.multiplySafe(BigInteger.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.multiplySafe(new MyNumber(), BigInteger.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.multiplySafe(new MyNumber(), new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testNegate() {
    assertThat(Numbers.negate(-3)).isEqualTo(3);
    assertThat(Numbers.negate((byte) 2)).isEqualTo((byte) -2);
    assertThat(Numbers.negate((short) -1)).isEqualTo((short) 1);
    assertThat(Numbers.negate(-1L)).isEqualTo(1L);
    assertThat(Numbers.negate(2.5f)).isEqualTo(-2.5f);
    assertThat(Numbers.negate(-2.5)).isEqualTo(2.5);
    assertThat(Numbers.negate(BigDecimal.ZERO)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.negate(BigInteger.ONE)).isEqualTo(BigInteger.valueOf(-1));
    assertThat(Numbers.negate(new MyNumber())).isNull();
  }

  @Test
  public void testNegateSafe() {
    assertThat(Numbers.negateSafe(-3)).isEqualTo(3);
    assertThat(Numbers.negateSafe((byte) 2)).isEqualTo((byte) -2);
    assertThat(Numbers.negateSafe((short) -1)).isEqualTo((short) 1);
    assertThat(Numbers.negateSafe(-1L)).isEqualTo(1L);
    assertThat(Numbers.negateSafe(2.5f)).isEqualTo(-2.5f);
    assertThat(Numbers.negateSafe(-2.5)).isEqualTo(2.5);
    assertThat(Numbers.negateSafe(BigDecimal.ZERO)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.negateSafe(BigInteger.ONE)).isEqualTo(BigInteger.valueOf(-1));
    try {
      assertThat(Numbers.negateSafe(new MyNumber()));

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testOperation() {
    assertThat(Numbers.getOperation(MyNumber.class)).isNull();
    assertThat(Numbers.getOperation(BigDecimal.class)).isNotNull();
    try {
      Numbers.getOperationSafe(MyNumber.class);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.getOperationSafe(BigDecimal.class).convert(0)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.getHigherPrecisionOperationSafe(BigInteger.class, Float.class)
                      .convert(0)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.getHigherPrecisionOperationSafe(Double.class, BigInteger.class)
                      .convert(0)).isEqualTo(BigDecimal.ZERO);
    assertThat(
        Numbers.getHigherPrecisionOperationSafe(BigDecimal.class, Long.class).convert(0)).isEqualTo(
        BigDecimal.ZERO);
    assertThat(Numbers.getHigherPrecisionOperation(BigDecimal.class, MyNumber.class)).isNull();
    try {
      Numbers.getHigherPrecisionOperationSafe(MyNumber.class, BigDecimal.class);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(
        Numbers.getLowerPrecisionOperationSafe(Float.class, Long.class).convert(0)).isEqualTo(0L);
    assertThat(
        Numbers.getLowerPrecisionOperationSafe(Float.class, BigInteger.class).convert(0)).isEqualTo(
        0f);
    assertThat(Numbers.getLowerPrecisionOperationSafe(BigDecimal.class, BigInteger.class)
                      .convert(0)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.getLowerPrecisionOperationSafe(BigInteger.class, BigDecimal.class)
                      .convert(0)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.getLowerPrecisionOperation(MyNumber.class, Long.class)).isNull();
    try {
      Numbers.getLowerPrecisionOperationSafe(MyNumber.class, BigDecimal.class);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testRemainder() {
    assertThat(Numbers.remainder(1, 2)).isEqualTo(1);
    assertThat(Numbers.remainder(1, (byte) 2)).isEqualTo(1);
    assertThat(Numbers.remainder((byte) 5, (byte) 2)).isEqualTo((byte) 1);
    assertThat(Numbers.remainder((short) -1, 2.5f)).isEqualTo(-1f);
    assertThat(Numbers.remainder((byte) 5, (short) 2)).isEqualTo((short) 1);
    assertThat(Numbers.remainder(-1L, 2.5)).isEqualTo(-1d);
    assertThat(Numbers.remainder(-1, 2L)).isEqualTo(-1L);
    assertThat(Numbers.remainder(BigDecimal.ZERO, 2.5)).isEqualTo(BigDecimal.ZERO);
    assertThat(Numbers.remainder(BigDecimal.ONE, new MyNumber())).isNull();
    assertThat(Numbers.remainder(new MyNumber(), BigDecimal.ONE)).isNull();
    assertThat(Numbers.remainder(BigInteger.ONE, 2.5)).isEqualTo(BigDecimal.ONE);
    assertThat(Numbers.remainder(BigInteger.ONE, 1)).isEqualTo(BigInteger.ZERO);
    assertThat(Numbers.remainder(BigInteger.ONE, new MyNumber())).isNull();
    assertThat(Numbers.remainder(new MyNumber(), BigInteger.ONE)).isNull();
    assertThat(Numbers.remainder(new MyNumber(), new MyNumber())).isNull();
  }

  @Test
  public void testRemainderSafe() {
    assertThat(Numbers.remainderSafe(1, 2)).isEqualTo(1);
    assertThat(Numbers.remainderSafe(1, (byte) 2)).isEqualTo(1);
    assertThat(Numbers.remainderSafe((short) -1, 2.5f)).isEqualTo(-1f);
    assertThat(Numbers.remainderSafe(-1L, 2.5)).isEqualTo(-1d);
    assertThat(Numbers.remainderSafe(BigDecimal.ZERO, 2.5)).isEqualTo(BigDecimal.ZERO);
    try {
      Numbers.remainderSafe(BigDecimal.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.remainderSafe(new MyNumber(), BigDecimal.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.remainderSafe(BigInteger.ONE, 2.5)).isEqualTo(BigDecimal.ONE);
    assertThat(Numbers.remainderSafe(BigInteger.ONE, 1)).isEqualTo(BigInteger.ZERO);
    try {
      Numbers.remainderSafe(BigInteger.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.remainderSafe(new MyNumber(), BigInteger.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.remainderSafe(new MyNumber(), new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testSubtract() {
    assertThat(Numbers.subtract(1, 2)).isEqualTo(-1);
    assertThat(Numbers.subtract(1, (byte) 2)).isEqualTo(-1);
    assertThat(Numbers.subtract((byte) 1, (byte) 2)).isEqualTo((byte) -1);
    assertThat(Numbers.subtract((short) -1, 2.5f)).isEqualTo(-3.5f);
    assertThat(Numbers.subtract((byte) 1, (short) 2)).isEqualTo((short) -1);
    assertThat(Numbers.subtract(-1L, 2.5)).isEqualTo(-3.5);
    assertThat(Numbers.subtract(-1L, -2)).isEqualTo(1L);
    assertThat(Numbers.subtract(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(-2.5));
    assertThat(Numbers.subtract(BigDecimal.ONE, new MyNumber())).isNull();
    assertThat(Numbers.subtract(new MyNumber(), BigDecimal.ONE)).isNull();
    assertThat(Numbers.subtract(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(-1.5));
    assertThat(Numbers.subtract(BigInteger.ONE, 1)).isEqualTo(BigInteger.ZERO);
    assertThat(Numbers.subtract(BigInteger.ONE, new MyNumber())).isNull();
    assertThat(Numbers.subtract(new MyNumber(), BigInteger.ONE)).isNull();
    assertThat(Numbers.subtract(new MyNumber(), new MyNumber())).isNull();
  }

  @Test
  public void testSubtractSafe() {
    assertThat(Numbers.subtractSafe(1, 2)).isEqualTo(-1);
    assertThat(Numbers.subtractSafe(1, (byte) 2)).isEqualTo(-1);
    assertThat(Numbers.subtractSafe((short) -1, 2.5f)).isEqualTo(-3.5f);
    assertThat(Numbers.subtractSafe(-1L, 2.5)).isEqualTo(-3.5);
    assertThat(Numbers.subtractSafe(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(-2.5));
    try {
      Numbers.subtractSafe(BigDecimal.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.subtractSafe(new MyNumber(), BigDecimal.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    assertThat(Numbers.subtractSafe(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(-1.5));
    assertThat(Numbers.subtractSafe(BigInteger.ONE, 1)).isEqualTo(BigInteger.ZERO);
    try {
      Numbers.subtractSafe(BigInteger.ONE, new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.subtractSafe(new MyNumber(), BigInteger.ONE);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Numbers.subtractSafe(new MyNumber(), new MyNumber());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  private static class MyNumber extends Number {

    public int intValue() {
      return 0;
    }

    public long longValue() {
      return 0;
    }

    public float floatValue() {
      return 0;
    }

    public double doubleValue() {
      return 0;
    }
  }
}
