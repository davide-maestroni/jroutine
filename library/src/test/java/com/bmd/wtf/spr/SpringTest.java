/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.spr;

import junit.framework.TestCase;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for spring classes.
 * <p/>
 * Created by davide on 8/21/14.
 */
public class SpringTest extends TestCase {

    private static <DATA> ArrayList<DATA> readData(final Spring<DATA> spring) {

        final ArrayList<DATA> list = new ArrayList<DATA>();

        while (spring.hasDrops()) {

            list.add(spring.nextDrop());
        }

        return list;
    }

    public void testDec() {

        assertThat(readData(Springs.decFrom(Byte.MIN_VALUE, (byte) 0))).isEmpty();
        assertThat(readData(Springs.decFrom(Byte.MIN_VALUE, (byte) 1))).containsExactly(
                Byte.MIN_VALUE);
        assertThat(Springs.decFrom((byte) -1, Byte.MAX_VALUE).nextDrop()).isEqualTo((byte) -1);
        assertThat(Springs.decFrom(Byte.MAX_VALUE, Byte.MAX_VALUE).nextDrop()).isEqualTo(
                Byte.MAX_VALUE);
        assertThat(readData(Springs.decFrom((byte) 3, (byte) 7))).containsExactly((byte) 3,
                                                                                  (byte) 2,
                                                                                  (byte) 1,
                                                                                  (byte) 0,
                                                                                  (byte) -1,
                                                                                  (byte) -2,
                                                                                  (byte) -3);

        assertThat(readData(Springs.decFrom(Character.MIN_VALUE, (char) 0))).isEmpty();
        assertThat(readData(Springs.decFrom(Character.MIN_VALUE, (char) 1))).containsExactly(
                Character.MIN_VALUE);
        assertThat(Springs.decFrom((char) -1, Character.MAX_VALUE).nextDrop()).isEqualTo((char) -1);
        assertThat(Springs.decFrom(Character.MAX_VALUE, Character.MAX_VALUE).nextDrop()).isEqualTo(
                Character.MAX_VALUE);
        assertThat(readData(Springs.decFrom((char) 9, (char) 7))).containsExactly((char) 9,
                                                                                  (char) 8,
                                                                                  (char) 7,
                                                                                  (char) 6,
                                                                                  (char) 5,
                                                                                  (char) 4,
                                                                                  (char) 3);

        assertThat(readData(Springs.decFrom(Integer.MIN_VALUE, 0))).isEmpty();
        assertThat(readData(Springs.decFrom(Integer.MIN_VALUE, 1))).containsExactly(
                Integer.MIN_VALUE);
        assertThat(Springs.decFrom(-1, Integer.MAX_VALUE).nextDrop()).isEqualTo(-1);
        assertThat(Springs.decFrom(Integer.MAX_VALUE, Integer.MAX_VALUE).nextDrop()).isEqualTo(
                Integer.MAX_VALUE);
        assertThat(readData(Springs.decFrom(3, 7))).containsExactly(3, 2, 1, 0, -1, -2, -3);

        assertThat(readData(Springs.decFrom(Long.MIN_VALUE, (long) 0))).isEmpty();
        assertThat(readData(Springs.decFrom(Long.MIN_VALUE, (long) 1))).containsExactly(
                Long.MIN_VALUE);
        assertThat(Springs.decFrom((long) -1, Long.MAX_VALUE).nextDrop()).isEqualTo((long) -1);
        assertThat(Springs.decFrom(Long.MAX_VALUE, Long.MAX_VALUE).nextDrop()).isEqualTo(
                Long.MAX_VALUE);
        assertThat(readData(Springs.decFrom((long) 3, (long) 7))).containsExactly((long) 3,
                                                                                  (long) 2,
                                                                                  (long) 1,
                                                                                  (long) 0,
                                                                                  (long) -1,
                                                                                  (long) -2,
                                                                                  (long) -3);

        assertThat(readData(Springs.decFrom(Short.MIN_VALUE, (short) 0))).isEmpty();
        assertThat(readData(Springs.decFrom(Short.MIN_VALUE, (short) 1))).containsExactly(
                Short.MIN_VALUE);
        assertThat(Springs.decFrom((short) -1, Short.MAX_VALUE).nextDrop()).isEqualTo((short) -1);
        assertThat(Springs.decFrom(Short.MAX_VALUE, Short.MAX_VALUE).nextDrop()).isEqualTo(
                Short.MAX_VALUE);
        assertThat(readData(Springs.decFrom((short) 3, (short) 7))).containsExactly((short) 3,
                                                                                    (short) 2,
                                                                                    (short) 1,
                                                                                    (short) 0,
                                                                                    (short) -1,
                                                                                    (short) -2,
                                                                                    (short) -3);
    }

    public void testError() {

        try {

            Springs.decFrom((byte) 0, (byte) -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Byte.MIN_VALUE, (byte) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((byte) (Byte.MIN_VALUE + 5), (byte) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((byte) 0, (byte) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Character.MIN_VALUE, (char) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((char) (Character.MIN_VALUE + 5), (char) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((char) 0, (char) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(0, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Integer.MIN_VALUE, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Integer.MIN_VALUE + 5, 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(0, 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((long) 0, (long) -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Long.MIN_VALUE, (long) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Long.MIN_VALUE + 5, (long) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((long) 0, (long) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((short) 0, (short) -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom(Short.MIN_VALUE, (short) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((short) (Short.MIN_VALUE + 5), (short) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.decFrom((short) 0, (short) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((byte) 0, (byte) -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Byte.MAX_VALUE, (byte) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((byte) (Byte.MAX_VALUE - 5), (byte) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((byte) 0, (byte) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Character.MAX_VALUE, (char) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((char) (Character.MAX_VALUE - 5), (char) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((char) 0, (char) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(0, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Integer.MAX_VALUE, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Integer.MAX_VALUE - 5, 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(0, 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((long) 0, (long) -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Long.MAX_VALUE, (long) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Long.MAX_VALUE - 5, (long) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((long) 0, (long) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((short) 0, (short) -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom(Short.MAX_VALUE, (short) 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((short) (Short.MAX_VALUE - 5), (short) 7);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.incFrom((short) 0, (short) 0).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testInc() {

        assertThat(readData(Springs.incFrom(Byte.MAX_VALUE, (byte) 0))).isEmpty();
        assertThat(readData(Springs.incFrom(Byte.MAX_VALUE, (byte) 1))).containsExactly(
                Byte.MAX_VALUE);
        assertThat(Springs.incFrom((byte) 0, Byte.MAX_VALUE).nextDrop()).isEqualTo((byte) 0);
        assertThat(Springs.incFrom(Byte.MIN_VALUE, Byte.MAX_VALUE).nextDrop()).isEqualTo(
                Byte.MIN_VALUE);
        assertThat(readData(Springs.incFrom((byte) -3, (byte) 7))).containsExactly((byte) -3,
                                                                                   (byte) -2,
                                                                                   (byte) -1,
                                                                                   (byte) 0,
                                                                                   (byte) 1,
                                                                                   (byte) 2,
                                                                                   (byte) 3);

        assertThat(readData(Springs.incFrom(Character.MAX_VALUE, (char) 0))).isEmpty();
        assertThat(readData(Springs.incFrom(Character.MAX_VALUE, (char) 1))).containsExactly(
                Character.MAX_VALUE);
        assertThat(Springs.incFrom((char) 0, Character.MAX_VALUE).nextDrop()).isEqualTo((char) 0);
        assertThat(Springs.incFrom(Character.MIN_VALUE, Character.MAX_VALUE).nextDrop()).isEqualTo(
                Character.MIN_VALUE);
        assertThat(readData(Springs.incFrom((char) 3, (char) 7))).containsExactly((char) 3,
                                                                                  (char) 4,
                                                                                  (char) 5,
                                                                                  (char) 6,
                                                                                  (char) 7,
                                                                                  (char) 8,
                                                                                  (char) 9);

        assertThat(readData(Springs.incFrom(Integer.MAX_VALUE, 0))).isEmpty();
        assertThat(readData(Springs.incFrom(Integer.MAX_VALUE, 1))).containsExactly(
                Integer.MAX_VALUE);
        assertThat(Springs.incFrom(0, Integer.MAX_VALUE).nextDrop()).isEqualTo(0);
        assertThat(Springs.incFrom(Integer.MIN_VALUE, Integer.MAX_VALUE).nextDrop()).isEqualTo(
                Integer.MIN_VALUE);
        assertThat(readData(Springs.incFrom(-3, 7))).containsExactly(-3, -2, -1, 0, 1, 2, 3);

        assertThat(readData(Springs.incFrom(Long.MAX_VALUE, (long) 0))).isEmpty();
        assertThat(readData(Springs.incFrom(Long.MAX_VALUE, (long) 1))).containsExactly(
                Long.MAX_VALUE);
        assertThat(Springs.incFrom((long) 0, Long.MAX_VALUE).nextDrop()).isEqualTo((long) 0);
        assertThat(Springs.incFrom(Long.MIN_VALUE, Long.MAX_VALUE).nextDrop()).isEqualTo(
                Long.MIN_VALUE);
        assertThat(readData(Springs.incFrom((long) -3, (long) 7))).containsExactly((long) -3,
                                                                                   (long) -2,
                                                                                   (long) -1,
                                                                                   (long) 0,
                                                                                   (long) 1,
                                                                                   (long) 2,
                                                                                   (long) 3);

        assertThat(readData(Springs.incFrom(Short.MAX_VALUE, (short) 0))).isEmpty();
        assertThat(readData(Springs.incFrom(Short.MAX_VALUE, (short) 1))).containsExactly(
                Short.MAX_VALUE);
        assertThat(Springs.incFrom((short) 0, Short.MAX_VALUE).nextDrop()).isEqualTo((short) 0);
        assertThat(Springs.incFrom(Short.MIN_VALUE, Short.MAX_VALUE).nextDrop()).isEqualTo(
                Short.MIN_VALUE);
        assertThat(readData(Springs.incFrom((short) -3, (short) 7))).containsExactly((short) -3,
                                                                                     (short) -2,
                                                                                     (short) -1,
                                                                                     (short) 0,
                                                                                     (short) 1,
                                                                                     (short) 2,
                                                                                     (short) 3);
    }
}