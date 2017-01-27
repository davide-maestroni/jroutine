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

package com.github.dm.jrt.operator.sequence;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.operator.sequence.Sequences.range;
import static com.github.dm.jrt.operator.sequence.Sequences.sequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Sequences unit tests.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 */
public class SequencesTest {

  @Test
  public void testRange() {
    assertThat(
        JRoutineCore.with(consumerCommand(range('a', 'e', new Function<Character, Character>() {

          public Character apply(final Character character) {
            return (char) (character + 1);
          }
        }))).close().in(seconds(3)).all()).containsExactly('a', 'b', 'c', 'd', 'e');
    assertThat(
        JRoutineCore.with(consumerCommand(range('e', 'a', new Function<Character, Character>() {

          public Character apply(final Character character) {
            return (char) (character - 1);
          }
        }))).close().in(seconds(3)).all()).containsExactly('e', 'd', 'c', 'b', 'a');
    assertThat(JRoutineCore.with(consumerCommand(range(0, 2, new BigDecimal(0.7))))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(
        Arrays.asList(0, new BigDecimal(0.7), new BigDecimal(0.7).add(new BigDecimal(0.7))));
    assertThat(JRoutineCore.with(consumerCommand(range(0, -10, BigInteger.valueOf(-2))))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(
        Arrays.asList(0, BigInteger.valueOf(-2), BigInteger.valueOf(-4), BigInteger.valueOf(-6),
            BigInteger.valueOf(-8), -10));
    assertThat(JRoutineCore.with(consumerCommand(range(0, BigInteger.valueOf(2), 0.7)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, 0.7, 1.4));
    assertThat(JRoutineCore.with(consumerCommand(range(0, -10, -2)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, -2, -4, -6, -8, -10));
    assertThat(JRoutineCore.with(consumerCommand(range(0, 2, 0.7)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, 0.7d, 1.4d));
    assertThat(JRoutineCore.with(consumerCommand(range(0, 2, 0.7f)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, 0.7f, 1.4f));
    assertThat(JRoutineCore.with(consumerCommand(range(0L, -9, -2)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0L, -2L, -4L, -6L, -8L));
    assertThat(JRoutineCore.with(consumerCommand(range(0, (short) 9, 2)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, 2, 4, 6, 8));
    assertThat(JRoutineCore.with(consumerCommand(range((byte) 0, (short) 9, (byte) 2)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(
        Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8));
    assertThat(JRoutineCore.with(consumerCommand(range((byte) 0, (byte) 10, (byte) 2)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(
        Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
    assertThat(JRoutineCore.with(consumerCommand(range(0, new BigDecimal(2))))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, 1, new BigDecimal(2)));
    assertThat(JRoutineCore.with(consumerCommand(range(0, BigInteger.valueOf(-2))))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0, -1, BigInteger.valueOf(-2)));
    assertThat(JRoutineCore.with(consumerCommand(range(0.1, BigInteger.valueOf(2))))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(Arrays.asList(0.1, 1.1));
    assertThat(
        JRoutineCore.with(consumerCommand(range(0, -5))).close().in(seconds(3)).all()).isEqualTo(
        Arrays.asList(0, -1, -2, -3, -4, -5));
    assertThat(
        JRoutineCore.with(consumerCommand(range(0, 2.1))).close().in(seconds(3)).all()).isEqualTo(
        Arrays.asList(0, 1, 2));
    assertThat(
        JRoutineCore.with(consumerCommand(range(0, 1.9f))).close().in(seconds(3)).all()).isEqualTo(
        Arrays.asList(0, 1));
    assertThat(
        JRoutineCore.with(consumerCommand(range(0L, -4))).close().in(seconds(3)).all()).isEqualTo(
        Arrays.asList(0L, -1L, -2L, -3L, -4));
    assertThat(JRoutineCore.with(consumerCommand(range(0, (short) 4))).close().in(seconds(3)).all())
        .isEqualTo(Arrays.asList(0, 1, 2, 3, (short) 4));
    assertThat(
        JRoutineCore.with(consumerCommand(range((byte) 0, (short) 4))).close().in(seconds(3)).all())
        .isEqualTo(Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (short) 4));
    assertThat(JRoutineCore.with(consumerCommand(range((byte) 0, (byte) 5)))
                           .close()
                           .in(seconds(3))
                           .all()).isEqualTo(
        Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
  }

  @Test
  public void testRangeEquals() {
    final Consumer<? extends Channel<? extends Number, ?>> range1 =
        Sequences.range(BigDecimal.ONE, 10);
    assertThat(range1).isEqualTo(range1);
    assertThat(range1).isNotEqualTo(null);
    assertThat(range1).isNotEqualTo("test");
    assertThat(range1).isNotEqualTo(range(BigDecimal.ONE, 10, 3));
    assertThat(range1).isEqualTo(range(BigDecimal.ONE, 10));
    assertThat(range1.hashCode()).isEqualTo(range(BigDecimal.ONE, 10).hashCode());

    final Consumer<? extends Channel<? extends Number, ?>> range2 =
        Sequences.range(BigInteger.ONE, 10);
    assertThat(range2).isEqualTo(range2);
    assertThat(range2).isNotEqualTo(null);
    assertThat(range2).isNotEqualTo("test");
    assertThat(range2).isNotEqualTo(range(BigInteger.ONE, 10, 3));
    assertThat(range2).isEqualTo(range(BigInteger.ONE, 10));
    assertThat(range2.hashCode()).isEqualTo(range(BigInteger.ONE, 10).hashCode());

    final Consumer<? extends Channel<? extends Number, ?>> range3 = Sequences.range(1, 10);
    assertThat(range3).isEqualTo(range3);
    assertThat(range3).isNotEqualTo(null);
    assertThat(range3).isNotEqualTo("test");
    assertThat(range3).isNotEqualTo(range(1, 10, 3));
    assertThat(range3).isEqualTo(range(1, 10));
    assertThat(range3.hashCode()).isEqualTo(range(1, 10).hashCode());

    final Consumer<? extends Channel<? extends Number, ?>> range4 = Sequences.range(1, 10, -2);
    assertThat(range4).isEqualTo(range4);
    assertThat(range4).isNotEqualTo(null);
    assertThat(range4).isNotEqualTo("test");
    assertThat(range4).isNotEqualTo(range(1, 10, 1));
    assertThat(range4).isEqualTo(range(1, 10, -2));
    assertThat(range4.hashCode()).isEqualTo(range(1, 10, -2).hashCode());

    final Function<Character, Character> function = new Function<Character, Character>() {

      public Character apply(final Character character) {
        return (char) (character + 1);
      }
    };
    final Consumer<? extends Channel<? extends Character, ?>> range5 =
        Sequences.range('a', 'f', function);
    assertThat(range5).isEqualTo(range5);
    assertThat(range5).isNotEqualTo(null);
    assertThat(range5).isNotEqualTo("test");
    assertThat(range5).isNotEqualTo(range('b', 'f', function));
    assertThat(range5).isEqualTo(range('a', 'f', function));
    assertThat(range5.hashCode()).isEqualTo(range('a', 'f', function).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testRangeError() {
    try {
      Sequences.range(null, 'f', new Function<Character, Character>() {

        public Character apply(final Character character) {
          return (char) (character + 1);
        }
      });
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Sequences.range('a', null, new Function<Character, Character>() {

        public Character apply(final Character character) {
          return (char) (character + 1);
        }
      });
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Sequences.range('a', 'f', null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Sequences.range(null, 1, 1);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Sequences.range(1, null, 1);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Sequences.range(1, 1, (Number) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    final Number number = new Number() {

      @Override
      public int intValue() {
        return 0;
      }

      @Override
      public long longValue() {
        return 0;
      }

      @Override
      public float floatValue() {
        return 0;
      }

      @Override
      public double doubleValue() {
        return 0;
      }
    };

    try {
      Sequences.range(number, number, number);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      Sequences.range(number, number);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testSequence() {
    assertThat(JRoutineCore.with(
        consumerCommand(sequence('a', 5, new BiFunction<Character, Long, Character>() {

          public Character apply(final Character character, final Long n) {
            return (char) (character + 1);
          }
        }))).close().in(seconds(3)).all()).containsExactly('a', 'b', 'c', 'd', 'e');
    assertThat(JRoutineCore.with(
        consumerCommand(sequence('a', 5, new BiFunction<Character, Long, Character>() {

          public Character apply(final Character character, final Long n) {
            return (char) (character + 1);
          }
        })))
                           .applyInvocationConfiguration()
                           .withOutputOrder(OrderType.SORTED)
                           .configured()
                           .callParallel()
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly('a', 'b', 'c', 'd', 'e');
    assertThat(JRoutineCore.with(
        consumerCommand(sequence('a', 5, new BiFunction<Character, Long, Character>() {

          public Character apply(final Character character, final Long n) {
            return (char) (character + 1);
          }
        })))
                           .applyInvocationConfiguration()
                           .withRunner(Runners.syncRunner())
                           .configured()
                           .close()
                           .all()).containsExactly('a', 'b', 'c', 'd', 'e');
  }

  @Test
  public void testSequenceEquals() {
    final Consumer<Channel<Integer, ?>> sequence =
        sequence(1, 10, Functions.<Integer, Long>first());
    assertThat(sequence).isEqualTo(sequence);
    assertThat(sequence).isNotEqualTo(null);
    assertThat(sequence).isNotEqualTo("test");
    assertThat(sequence).isNotEqualTo(sequence(1, 9, Functions.<Integer, Long>first()));
    assertThat(sequence).isEqualTo(sequence(1, 10, Functions.<Integer, Long>first()));
    assertThat(sequence.hashCode()).isEqualTo(
        sequence(1, 10, Functions.<Integer, Long>first()).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testSequenceError() {
    try {
      sequence(null, 2, new BiFunction<Character, Long, Character>() {

        public Character apply(final Character character, final Long n) {
          return (char) (character + 1);
        }
      });
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      sequence('a', 2, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      sequence(1, -1, Functions.<Integer, Long>first());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }
}
