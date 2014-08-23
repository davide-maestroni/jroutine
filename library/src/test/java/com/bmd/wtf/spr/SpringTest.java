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

import com.bmd.wtf.fll.Waterfall;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import static com.bmd.wtf.fll.Waterfall.fall;
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

        assertThat(readData(Springs.sequence(Byte.MIN_VALUE, 0))).containsExactly(Byte.MIN_VALUE);
        assertThat(readData(Springs.sequence((byte) (Byte.MIN_VALUE + 1), -1))).containsExactly(
                (byte) (Byte.MIN_VALUE + 1), Byte.MIN_VALUE);
        assertThat(Springs.sequence((byte) 0, Byte.MIN_VALUE).nextDrop()).isEqualTo((byte) 0);
        assertThat(Springs.sequence(Byte.MAX_VALUE, -Byte.MAX_VALUE * 2).nextDrop()).isEqualTo(
                Byte.MAX_VALUE);
        assertThat(readData(Springs.sequence((byte) 3, -7))).containsExactly((byte) 3, (byte) 2,
                                                                             (byte) 1, (byte) 0,
                                                                             (byte) -1, (byte) -2,
                                                                             (byte) -3, (byte) -4);

        assertThat(readData(Springs.sequence(Integer.MIN_VALUE, 0))).containsExactly(
                Integer.MIN_VALUE);
        assertThat(readData(Springs.sequence(Integer.MIN_VALUE + 1, -1))).containsExactly(
                Integer.MIN_VALUE + 1, Integer.MIN_VALUE);
        assertThat(Springs.sequence(0, Integer.MIN_VALUE).nextDrop()).isEqualTo(0);
        assertThat(Springs.sequence(Integer.MAX_VALUE, (long) -Integer.MAX_VALUE * 2)
                          .nextDrop()).isEqualTo(Integer.MAX_VALUE);
        assertThat(readData(Springs.sequence(3, -7))).containsExactly(3, 2, 1, 0, -1, -2, -3, -4);

        assertThat(readData(Springs.sequence(Long.MIN_VALUE, 0))).containsExactly(Long.MIN_VALUE);
        assertThat(readData(Springs.sequence(Long.MIN_VALUE + 1, -1))).containsExactly(
                Long.MIN_VALUE + 1, Long.MIN_VALUE);
        assertThat(Springs.sequence((long) 0, Long.MIN_VALUE).nextDrop()).isEqualTo((long) 0);
        assertThat(Springs.sequence(Long.MAX_VALUE, Long.MIN_VALUE).nextDrop()).isEqualTo(
                Long.MAX_VALUE);
        assertThat(readData(Springs.sequence((long) 3, -7))).containsExactly((long) 3, (long) 2,
                                                                             (long) 1, (long) 0,
                                                                             (long) -1, (long) -2,
                                                                             (long) -3, (long) -4);

        assertThat(readData(Springs.sequence(Short.MIN_VALUE, 0))).containsExactly(Short.MIN_VALUE);
        assertThat(readData(Springs.sequence((short) (Short.MIN_VALUE + 1), -1))).containsExactly(
                (short) (Short.MIN_VALUE + 1), Short.MIN_VALUE);
        assertThat(Springs.sequence((short) 0, Short.MIN_VALUE).nextDrop()).isEqualTo((short) 0);
        assertThat(Springs.sequence(Short.MAX_VALUE, -Short.MAX_VALUE * 2).nextDrop()).isEqualTo(
                Short.MAX_VALUE);
        assertThat(readData(Springs.sequence((short) 3, -7))).containsExactly((short) 3, (short) 2,
                                                                              (short) 1, (short) 0,
                                                                              (short) -1,
                                                                              (short) -2,
                                                                              (short) -3,
                                                                              (short) -4);
    }

    public void testError() {

        try {

            Springs.randomBools(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomBools(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomBools(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Boolean> spring = Springs.randomBools(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Boolean> spring = Springs.randomBools(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Boolean> spring = Springs.randomBools(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Boolean> spring = Springs.randomBools(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomBytes(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomBytes(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomBytes(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.randomBytes(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.randomBytes(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.randomBytes(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.randomBytes(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomDoubles(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomDoubles(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomDoubles(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomDoubles(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomDoubles(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomDoubles(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomDoubles(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomFloats(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomFloats(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomFloats(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Float> spring = Springs.randomFloats(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Float> spring = Springs.randomFloats(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Float> spring = Springs.randomFloats(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Float> spring = Springs.randomFloats(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomGaussian(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomGaussian(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomGaussian(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomGaussian(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomGaussian(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomGaussian(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Double> spring = Springs.randomGaussian(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomInts(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomInts(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomInts(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.randomInts(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.randomInts(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.randomInts(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.randomInts(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomLongs(null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomLongs(new Random(), -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.randomLongs(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.randomLongs(new Random(), 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.randomLongs(new Random(), 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.randomLongs(0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.randomLongs(1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Byte.MIN_VALUE, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence((byte) (Byte.MIN_VALUE + 5), -6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Byte.MAX_VALUE, 1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence((byte) (Byte.MAX_VALUE - 5), 6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.sequence((byte) 0, 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.sequence((byte) 0, 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Byte> spring = Springs.sequence((byte) 0, -1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Character.MIN_VALUE, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence((char) (Character.MIN_VALUE + 5), -6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Character.MAX_VALUE, 1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence((char) (Character.MAX_VALUE - 5), 6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Character> spring = Springs.sequence((char) 0, 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Character> spring = Springs.sequence((char) 0, 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Character> spring = Springs.sequence((char) 0, -1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Integer.MIN_VALUE, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Integer.MIN_VALUE + 5, -6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Integer.MAX_VALUE, 1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Integer.MAX_VALUE - 5, 6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.sequence(0, 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.sequence(0, 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Integer> spring = Springs.sequence(0, -1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Long.MIN_VALUE, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Long.MIN_VALUE + 5, -6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Long.MAX_VALUE, 1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Long.MAX_VALUE - 5, 6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.sequence((long) 0, 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.sequence((long) 0, 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Long> spring = Springs.sequence((long) 0, -1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Short.MIN_VALUE, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence((short) (Short.MIN_VALUE + 5), -6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence(Short.MAX_VALUE, 1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.sequence((short) (Short.MAX_VALUE - 5), 6);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Short> spring = Springs.sequence((short) 0, 0);
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Short> spring = Springs.sequence((short) 0, 1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Spring<Short> spring = Springs.sequence((short) 0, -1);
            spring.nextDrop();
            spring.nextDrop();
            spring.nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((Waterfall<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from(fall().start()).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((InputStream) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from(new ByteArrayInputStream(new byte[0])).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((CharSequence) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from("").nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((Reader) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from(new StringReader("")).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((BufferedReader) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from(new BufferedReader(new StringReader(""))).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((Object[]) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from().nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((Iterable<Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from(Collections.emptyList()).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from((Iterator<Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Springs.from(Collections.emptyIterator()).nextDrop();

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testFrom() {

        //noinspection unchecked
        final Waterfall<Void, Void, Integer> fall =
                fall().spring(Collections.singleton(Springs.sequence(1, 3)));
        assertThat(readData(Springs.from(fall))).containsExactly(1, 2, 3, 4);

        final byte[] bytes = new byte[]{-1, 1, 33};
        final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        assertThat(readData(Springs.from(stream))).containsExactly((byte) -1, (byte) 1, (byte) 33);

        final String str = "test1\ntest2\ntest3";
        assertThat(readData(Springs.from(str))).containsExactly('t', 'e', 's', 't', '1', '\n', 't',
                                                                'e', 's', 't', '2', '\n', 't', 'e',
                                                                's', 't', '3');

        final StringReader reader = new StringReader(str);
        assertThat(readData(Springs.from(reader))).containsExactly('t', 'e', 's', 't', '1', '\n',
                                                                   't', 'e', 's', 't', '2', '\n',
                                                                   't', 'e', 's', 't', '3');

        final BufferedReader bReader = new BufferedReader(new StringReader(str));
        assertThat(readData(Springs.from(bReader))).containsExactly("test1", "test2", "test3");

        final Integer[] data = new Integer[]{-1, 1, 33};
        assertThat(readData(Springs.from(data))).containsExactly(-1, 1, 33);
        assertThat(readData(Springs.from(Arrays.asList(data)))).containsExactly(-1, 1, 33);
        assertThat(readData(Springs.from(Arrays.asList(data).iterator()))).containsExactly(-1, 1,
                                                                                           33);
    }

    public void testInc() {

        assertThat(readData(Springs.sequence(Byte.MAX_VALUE, 0))).containsExactly(Byte.MAX_VALUE);
        assertThat(readData(Springs.sequence((byte) (Byte.MAX_VALUE - 1), 1))).containsExactly(
                (byte) (Byte.MAX_VALUE - 1), Byte.MAX_VALUE);
        assertThat(Springs.sequence((byte) 0, Byte.MAX_VALUE).nextDrop()).isEqualTo((byte) 0);
        assertThat(Springs.sequence(Byte.MIN_VALUE, Byte.MAX_VALUE * 2).nextDrop()).isEqualTo(
                Byte.MIN_VALUE);
        assertThat(readData(Springs.sequence((byte) -3, 7))).containsExactly((byte) -3, (byte) -2,
                                                                             (byte) -1, (byte) 0,
                                                                             (byte) 1, (byte) 2,
                                                                             (byte) 3, (byte) 4);

        assertThat(readData(Springs.sequence(Character.MAX_VALUE, 0))).containsExactly(
                Character.MAX_VALUE);
        assertThat(readData(Springs.sequence((char) (Character.MAX_VALUE - 1), 1))).containsExactly(
                (char) (Character.MAX_VALUE - 1), Character.MAX_VALUE);
        assertThat(Springs.sequence((char) 0, Character.MAX_VALUE).nextDrop()).isEqualTo((char) 0);
        assertThat(Springs.sequence(Character.MIN_VALUE, Character.MAX_VALUE).nextDrop()).isEqualTo(
                Character.MIN_VALUE);
        assertThat(readData(Springs.sequence((char) 2, 7))).containsExactly((char) 2, (char) 3,
                                                                            (char) 4, (char) 5,
                                                                            (char) 6, (char) 7,
                                                                            (char) 8, (char) 9);

        assertThat(readData(Springs.sequence(Integer.MAX_VALUE, 0))).containsExactly(
                Integer.MAX_VALUE);
        assertThat(readData(Springs.sequence(Integer.MAX_VALUE - 1, 1))).containsExactly(
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertThat(Springs.sequence(0, Integer.MAX_VALUE).nextDrop()).isEqualTo(0);
        assertThat(Springs.sequence(Integer.MIN_VALUE, (long) Integer.MAX_VALUE * 2)
                          .nextDrop()).isEqualTo(Integer.MIN_VALUE);
        assertThat(readData(Springs.sequence(-3, 7))).containsExactly(-3, -2, -1, 0, 1, 2, 3, 4);

        assertThat(readData(Springs.sequence(Long.MAX_VALUE, 0))).containsExactly(Long.MAX_VALUE);
        assertThat(readData(Springs.sequence(Long.MAX_VALUE - 1, 1))).containsExactly(
                Long.MAX_VALUE - 1, Long.MAX_VALUE);
        assertThat(Springs.sequence((long) 0, Long.MAX_VALUE).nextDrop()).isEqualTo((long) 0);
        assertThat(Springs.sequence(Long.MIN_VALUE, Long.MAX_VALUE).nextDrop()).isEqualTo(
                Long.MIN_VALUE);
        assertThat(readData(Springs.sequence((long) -3, 7))).containsExactly((long) -3, (long) -2,
                                                                             (long) -1, (long) 0,
                                                                             (long) 1, (long) 2,
                                                                             (long) 3, (long) 4);

        assertThat(readData(Springs.sequence(Short.MAX_VALUE, 0))).containsExactly(Short.MAX_VALUE);
        assertThat(readData(Springs.sequence((short) (Short.MAX_VALUE - 1), 1))).containsExactly(
                (short) (Short.MAX_VALUE - 1), Short.MAX_VALUE);
        assertThat(Springs.sequence((short) 0, Short.MAX_VALUE).nextDrop()).isEqualTo((short) 0);
        assertThat(Springs.sequence(Short.MIN_VALUE, Short.MAX_VALUE * 2).nextDrop()).isEqualTo(
                Short.MIN_VALUE);
        assertThat(readData(Springs.sequence((short) -3, 7))).containsExactly((short) -3,
                                                                              (short) -2,
                                                                              (short) -1, (short) 0,
                                                                              (short) 1, (short) 2,
                                                                              (short) 3, (short) 4);
    }

    public void testRandom() {

        final Random rand1 = new Random(0);
        final Random rand2 = new Random(0);

        assertThat(readData(Springs.randomBools(rand1, 2))).containsExactly(rand2.nextBoolean(),
                                                                            rand2.nextBoolean());
        final byte[] byte1 = new byte[1];
        final byte[] byte2 = new byte[1];
        rand2.nextBytes(byte1);
        rand2.nextBytes(byte2);
        assertThat(readData(Springs.randomBytes(rand1, 2))).containsExactly(byte1[0], byte2[0]);
        assertThat(readData(Springs.randomDoubles(rand1, 2))).containsExactly(rand2.nextDouble(),
                                                                              rand2.nextDouble());
        assertThat(readData(Springs.randomFloats(rand1, 2))).containsExactly(rand2.nextFloat(),
                                                                             rand2.nextFloat());
        assertThat(readData(Springs.randomGaussian(rand1, 2))).containsExactly(rand2.nextGaussian(),
                                                                               rand2.nextGaussian());
        assertThat(readData(Springs.randomInts(rand1, 2))).containsExactly(rand2.nextInt(),
                                                                           rand2.nextInt());
        assertThat(readData(Springs.randomLongs(rand1, 2))).containsExactly(rand2.nextLong(),
                                                                            rand2.nextLong());
    }
}