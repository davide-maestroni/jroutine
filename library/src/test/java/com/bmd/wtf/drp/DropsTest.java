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
package com.bmd.wtf.drp;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for drops utility classes.
 * <p/>
 * Created by davide on 6/25/14.
 */
public class DropsTest extends TestCase {

    public void testBooleans() {

        Object[] data = new Object[]{true, true, false, true, false};

        final Drops<Boolean> booleans = Drops.asList(true, true, false, true, false);

        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{true, false, true, false};

        assertThat(booleans.remove(0)).isEqualTo(true);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{false, true, false};

        assertThat(booleans.remove(true)).isEqualTo(true);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{false, true, false, true};

        assertThat(booleans.add(true)).isEqualTo(true);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{false, false, true, false, true};

        booleans.add(0, false);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{false, false, true, false, true, true, false};

        assertThat(booleans.addAll(Arrays.asList(true, false))).isEqualTo(true);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{true, true, false, false, true, false, true, true, false};

        assertThat(booleans.addAll(0, Arrays.asList(true, true))).isEqualTo(true);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{true, true, true, false, true, false, true, true, false};

        assertThat(booleans.set(2, true)).isEqualTo(false);
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        final Drops<Boolean> booleans1 = booleans.toBooleans();

        data = new Object[]{false, false, false};

        assertThat(booleans1.removeAll(Arrays.asList(true))).isEqualTo(true);
        testBooleans(booleans1, toBooleans(data));
        testAllConversions(booleans1, data);

        final Drops<Boolean> booleans2 = booleans.toBooleans();

        data = new Object[]{true, true, true, true, true, true};

        assertThat(booleans2.retainAll(Arrays.asList(true))).isEqualTo(true);
        testBooleans(booleans2, toBooleans(data));
        testAllConversions(booleans2, data);

        data = new Object[]{true, false, true, false};

        testBooleans(booleans.subList(2, 6), toBooleans(data));
        testAllConversions(booleans.subList(2, 6), data);

        data = new Object[]{true, true, true, true, false};

        booleans.subList(2, 6).clear();
        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        booleans.clear();
        testAllEmpty(booleans);

        testAllEmpty(Drops.asList(new boolean[0]));
    }

    public void testBytes() {

        Object[] data = new Object[]{(byte) 4, (byte) -77, (byte) 100, (byte) 0, (byte) -32};

        final Drops<Byte> bytes =
                Drops.asList((byte) 4, (byte) -77, (byte) 100, (byte) 0, (byte) -32);

        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -77, (byte) 100, (byte) 0, (byte) -32};

        assertThat(bytes.remove(0)).isEqualTo((byte) 4);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -77, (byte) 100, (byte) 0};

        assertThat(bytes.remove(Byte.valueOf((byte) -32))).isEqualTo(true);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -77, (byte) 100, (byte) 0, (byte) 111};

        assertThat(bytes.add((byte) 111)).isEqualTo(true);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -3, (byte) -77, (byte) 100, (byte) 0, (byte) 111};

        bytes.add(0, (byte) -3);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -3, (byte) -77, (byte) 100, (byte) 0, (byte) 111, (byte) 15,
                            (byte) 0};

        assertThat(bytes.addAll(Arrays.asList((byte) 15, (byte) 0))).isEqualTo(true);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -13, (byte) 0, (byte) -3, (byte) -77, (byte) 100, (byte) 0,
                            (byte) 111, (byte) 15, (byte) 0};

        assertThat(bytes.addAll(0, Arrays.asList((byte) -13, (byte) 0))).isEqualTo(true);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) -13, (byte) 0, (byte) 0, (byte) -77, (byte) 100, (byte) 0,
                            (byte) 111, (byte) 15, (byte) 0};

        assertThat(bytes.set(2, (byte) 0)).isEqualTo((byte) -3);
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        final Drops<Byte> bytes1 = bytes.toBytes();

        data = new Object[]{(byte) -77, (byte) 100, (byte) 111, (byte) 15};

        assertThat(bytes1.removeAll(Arrays.asList((byte) 0, (byte) -13))).isEqualTo(true);
        testBytes(bytes1, toBytes(data));
        testAllConversions(bytes1, data);

        final Drops<Byte> bytes2 = bytes.toBytes();

        data = new Object[]{(byte) -13, (byte) 0, (byte) 0, (byte) 0, (byte) 0};

        assertThat(bytes2.retainAll(Arrays.asList((byte) 0, (byte) -13))).isEqualTo(true);
        testBytes(bytes2, toBytes(data));
        testAllConversions(bytes2, data);

        data = new Object[]{(byte) 0, (byte) -77, (byte) 100, (byte) 0};

        testBytes(bytes.subList(2, 6), toBytes(data));
        testAllConversions(bytes.subList(2, 6), data);

        data = new Object[]{(byte) -13, (byte) 0, (byte) 111, (byte) 15, (byte) 0};

        bytes.subList(2, 6).clear();
        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        bytes.clear();
        testAllEmpty(bytes);

        testAllEmpty(Drops.asList(new byte[0]));
    }

    public void testChars() {

        Object[] data = new Object[]{(char) 4, (char) -77, (char) 100, (char) 0, (char) -32};

        final Drops<Character> chars =
                Drops.asList((char) 4, (char) -77, (char) 100, (char) 0, (char) -32);

        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -77, (char) 100, (char) 0, (char) -32};

        assertThat(chars.remove(0)).isEqualTo((char) 4);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -77, (char) 100, (char) 0};

        assertThat(chars.remove(Character.valueOf((char) -32))).isEqualTo(true);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -77, (char) 100, (char) 0, (char) 111};

        assertThat(chars.add((char) 111)).isEqualTo(true);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -3, (char) -77, (char) 100, (char) 0, (char) 111};

        chars.add(0, (char) -3);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -3, (char) -77, (char) 100, (char) 0, (char) 111, (char) 15,
                            (char) 0};

        assertThat(chars.addAll(Arrays.asList((char) 15, (char) 0))).isEqualTo(true);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -13, (char) 0, (char) -3, (char) -77, (char) 100, (char) 0,
                            (char) 111, (char) 15, (char) 0};

        assertThat(chars.addAll(0, Arrays.asList((char) -13, (char) 0))).isEqualTo(true);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) -13, (char) 0, (char) 0, (char) -77, (char) 100, (char) 0,
                            (char) 111, (char) 15, (char) 0};

        assertThat(chars.set(2, (char) 0)).isEqualTo((char) -3);
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        final Drops<Character> chars1 = chars.toCharacters();

        data = new Object[]{(char) -77, (char) 100, (char) 111, (char) 15};

        assertThat(chars1.removeAll(Arrays.asList((char) 0, (char) -13))).isEqualTo(true);
        testCharacters(chars1, toChars(data));
        testAllConversions(chars1, data);

        final Drops<Character> chars2 = chars.toCharacters();

        data = new Object[]{(char) -13, (char) 0, (char) 0, (char) 0, (char) 0};

        assertThat(chars2.retainAll(Arrays.asList((char) 0, (char) -13))).isEqualTo(true);
        testCharacters(chars2, toChars(data));
        testAllConversions(chars2, data);

        data = new Object[]{(char) 0, (char) -77, (char) 100, (char) 0};

        testCharacters(chars.subList(2, 6), toChars(data));
        testAllConversions(chars.subList(2, 6), data);

        data = new Object[]{(char) -13, (char) 0, (char) 111, (char) 15, (char) 0};

        chars.subList(2, 6).clear();
        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        chars.clear();
        testAllEmpty(chars);

        testAllEmpty(Drops.asList(new char[0]));
    }

    public void testDoubles() {

        Object[] data =
                new Object[]{(double) 4, (double) -77, (double) 100, (double) 0, (double) -32};

        final Drops<Double> doubles =
                Drops.asList((double) 4, (double) -77, (double) 100, (double) 0, (double) -32);

        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -77, (double) 100, (double) 0, (double) -32};

        assertThat(doubles.remove(0)).isEqualTo((double) 4);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -77, (double) 100, (double) 0};

        assertThat(doubles.remove(Double.valueOf((double) -32))).isEqualTo(true);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -77, (double) 100, (double) 0, (double) 111};

        assertThat(doubles.add((double) 111)).isEqualTo(true);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -3, (double) -77, (double) 100, (double) 0, (double) 111};

        doubles.add(0, (double) -3);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -3, (double) -77, (double) 100, (double) 0, (double) 111,
                            (double) 15, (double) 0};

        assertThat(doubles.addAll(Arrays.asList((double) 15, (double) 0))).isEqualTo(true);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -13, (double) 0, (double) -3, (double) -77, (double) 100,
                            (double) 0, (double) 111, (double) 15, (double) 0};

        assertThat(doubles.addAll(0, Arrays.asList((double) -13, (double) 0))).isEqualTo(true);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) -13, (double) 0, (double) 0, (double) -77, (double) 100,
                            (double) 0, (double) 111, (double) 15, (double) 0};

        assertThat(doubles.set(2, (double) 0)).isEqualTo((double) -3);
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        final Drops<Double> doubles1 = doubles.toDoubles();

        data = new Object[]{(double) -77, (double) 100, (double) 111, (double) 15};

        assertThat(doubles1.removeAll(Arrays.asList((double) 0, (double) -13))).isEqualTo(true);
        testDoubles(doubles1, toDoubles(data));
        testAllConversions(doubles1, data);

        final Drops<Double> doubles2 = doubles.toDoubles();

        data = new Object[]{(double) -13, (double) 0, (double) 0, (double) 0, (double) 0};

        assertThat(doubles2.retainAll(Arrays.asList((double) 0, (double) -13))).isEqualTo(true);
        testDoubles(doubles2, toDoubles(data));
        testAllConversions(doubles2, data);

        data = new Object[]{(double) 0, (double) -77, (double) 100, (double) 0};

        testDoubles(doubles.subList(2, 6), toDoubles(data));
        testAllConversions(doubles.subList(2, 6), data);

        data = new Object[]{(double) -13, (double) 0, (double) 111, (double) 15, (double) 0};

        doubles.subList(2, 6).clear();
        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        doubles.clear();
        testAllEmpty(doubles);

        testAllEmpty(Drops.asList(new double[0]));
    }

    public void testFloats() {

        Object[] data = new Object[]{(float) 4, (float) -77, (float) 100, (float) 0, (float) -32};

        final Drops<Float> floats =
                Drops.asList((float) 4, (float) -77, (float) 100, (float) 0, (float) -32);

        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -77, (float) 100, (float) 0, (float) -32};

        assertThat(floats.remove(0)).isEqualTo((float) 4);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -77, (float) 100, (float) 0};

        assertThat(floats.remove(Float.valueOf((float) -32))).isEqualTo(true);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -77, (float) 100, (float) 0, (float) 111};

        assertThat(floats.add((float) 111)).isEqualTo(true);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -3, (float) -77, (float) 100, (float) 0, (float) 111};

        floats.add(0, (float) -3);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -3, (float) -77, (float) 100, (float) 0, (float) 111,
                            (float) 15, (float) 0};

        assertThat(floats.addAll(Arrays.asList((float) 15, (float) 0))).isEqualTo(true);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -13, (float) 0, (float) -3, (float) -77, (float) 100, (float) 0,
                            (float) 111, (float) 15, (float) 0};

        assertThat(floats.addAll(0, Arrays.asList((float) -13, (float) 0))).isEqualTo(true);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) -13, (float) 0, (float) 0, (float) -77, (float) 100, (float) 0,
                            (float) 111, (float) 15, (float) 0};

        assertThat(floats.set(2, (float) 0)).isEqualTo((float) -3);
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        final Drops<Float> floats1 = floats.toFloats();

        data = new Object[]{(float) -77, (float) 100, (float) 111, (float) 15};

        assertThat(floats1.removeAll(Arrays.asList((float) 0, (float) -13))).isEqualTo(true);
        testFloats(floats1, toFloats(data));
        testAllConversions(floats1, data);

        final Drops<Float> floats2 = floats.toFloats();

        data = new Object[]{(float) -13, (float) 0, (float) 0, (float) 0, (float) 0};

        assertThat(floats2.retainAll(Arrays.asList((float) 0, (float) -13))).isEqualTo(true);
        testFloats(floats2, toFloats(data));
        testAllConversions(floats2, data);

        data = new Object[]{(float) 0, (float) -77, (float) 100, (float) 0};

        testFloats(floats.subList(2, 6), toFloats(data));
        testAllConversions(floats.subList(2, 6), data);

        data = new Object[]{(float) -13, (float) 0, (float) 111, (float) 15, (float) 0};

        floats.subList(2, 6).clear();
        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        floats.clear();
        testAllEmpty(floats);

        testAllEmpty(Drops.asList(new float[0]));
    }

    public void testIntegers() {

        Object[] data = new Object[]{4, -77, 100, 0, -32};

        final Drops<Integer> integers = Drops.asList(4, -77, 100, 0, -32);

        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-77, 100, 0, -32};

        assertThat(integers.remove(0)).isEqualTo(4);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-77, 100, 0};

        assertThat(integers.remove(Integer.valueOf(-32))).isEqualTo(true);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-77, 100, 0, 111};

        assertThat(integers.add(111)).isEqualTo(true);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-3, -77, 100, 0, 111};

        integers.add(0, -3);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-3, -77, 100, 0, 111, 15, 0};

        assertThat(integers.addAll(Arrays.asList(15, 0))).isEqualTo(true);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-13, 0, -3, -77, 100, 0, 111, 15, 0};

        assertThat(integers.addAll(0, Arrays.asList(-13, 0))).isEqualTo(true);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{-13, 0, 0, -77, 100, 0, 111, 15, 0};

        assertThat(integers.set(2, 0)).isEqualTo(-3);
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        final Drops<Integer> integers1 = integers.toIntegers();

        data = new Object[]{-77, 100, 111, 15};

        assertThat(integers1.removeAll(Arrays.asList(0, -13))).isEqualTo(true);
        testIntegers(integers1, toIntegers(data));
        testAllConversions(integers1, data);

        final Drops<Integer> integers2 = integers.toIntegers();

        data = new Object[]{-13, 0, 0, 0, 0};

        assertThat(integers2.retainAll(Arrays.asList(0, -13))).isEqualTo(true);
        testIntegers(integers2, toIntegers(data));
        testAllConversions(integers2, data);

        data = new Object[]{0, -77, 100, 0};

        testIntegers(integers.subList(2, 6), toIntegers(data));
        testAllConversions(integers.subList(2, 6), data);

        data = new Object[]{-13, 0, 111, 15, 0};

        integers.subList(2, 6).clear();
        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        integers.clear();
        testAllEmpty(integers);

        testAllEmpty(Drops.asList(new int[0]));
    }

    public void testLongs() {

        Object[] data = new Object[]{(long) 4, (long) -77, (long) 100, (long) 0, (long) -32};

        final Drops<Long> longs =
                Drops.asList((long) 4, (long) -77, (long) 100, (long) 0, (long) -32);

        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -77, (long) 100, (long) 0, (long) -32};

        assertThat(longs.remove(0)).isEqualTo((long) 4);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -77, (long) 100, (long) 0};

        assertThat(longs.remove(Long.valueOf((long) -32))).isEqualTo(true);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -77, (long) 100, (long) 0, (long) 111};

        assertThat(longs.add((long) 111)).isEqualTo(true);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -3, (long) -77, (long) 100, (long) 0, (long) 111};

        longs.add(0, (long) -3);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -3, (long) -77, (long) 100, (long) 0, (long) 111, (long) 15,
                            (long) 0};

        assertThat(longs.addAll(Arrays.asList((long) 15, (long) 0))).isEqualTo(true);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -13, (long) 0, (long) -3, (long) -77, (long) 100, (long) 0,
                            (long) 111, (long) 15, (long) 0};

        assertThat(longs.addAll(0, Arrays.asList((long) -13, (long) 0))).isEqualTo(true);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) -13, (long) 0, (long) 0, (long) -77, (long) 100, (long) 0,
                            (long) 111, (long) 15, (long) 0};

        assertThat(longs.set(2, (long) 0)).isEqualTo((long) -3);
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        final Drops<Long> longs1 = longs.toLongs();

        data = new Object[]{(long) -77, (long) 100, (long) 111, (long) 15};

        assertThat(longs1.removeAll(Arrays.asList((long) 0, (long) -13))).isEqualTo(true);
        testLongs(longs1, toLongs(data));
        testAllConversions(longs1, data);

        final Drops<Long> longs2 = longs.toLongs();

        data = new Object[]{(long) -13, (long) 0, (long) 0, (long) 0, (long) 0};

        assertThat(longs2.retainAll(Arrays.asList((long) 0, (long) -13))).isEqualTo(true);
        testLongs(longs2, toLongs(data));
        testAllConversions(longs2, data);

        data = new Object[]{(long) 0, (long) -77, (long) 100, (long) 0};

        testLongs(longs.subList(2, 6), toLongs(data));
        testAllConversions(longs.subList(2, 6), data);

        data = new Object[]{(long) -13, (long) 0, (long) 111, (long) 15, (long) 0};

        longs.subList(2, 6).clear();
        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        longs.clear();
        testAllEmpty(longs);

        testAllEmpty(Drops.asList(new long[0]));
    }

    public void testShorts() {

        Object[] data = new Object[]{(short) 4, (short) -77, (short) 100, (short) 0, (short) -32};

        final Drops<Short> shorts =
                Drops.asList((short) 4, (short) -77, (short) 100, (short) 0, (short) -32);

        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -77, (short) 100, (short) 0, (short) -32};

        assertThat(shorts.remove(0)).isEqualTo((short) 4);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -77, (short) 100, (short) 0};

        assertThat(shorts.remove(Short.valueOf((short) -32))).isEqualTo(true);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -77, (short) 100, (short) 0, (short) 111};

        assertThat(shorts.add((short) 111)).isEqualTo(true);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -3, (short) -77, (short) 100, (short) 0, (short) 111};

        shorts.add(0, (short) -3);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -3, (short) -77, (short) 100, (short) 0, (short) 111,
                            (short) 15, (short) 0};

        assertThat(shorts.addAll(Arrays.asList((short) 15, (short) 0))).isEqualTo(true);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -13, (short) 0, (short) -3, (short) -77, (short) 100, (short) 0,
                            (short) 111, (short) 15, (short) 0};

        assertThat(shorts.addAll(0, Arrays.asList((short) -13, (short) 0))).isEqualTo(true);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) -13, (short) 0, (short) 0, (short) -77, (short) 100, (short) 0,
                            (short) 111, (short) 15, (short) 0};

        assertThat(shorts.set(2, (short) 0)).isEqualTo((short) -3);
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        final Drops<Short> shorts1 = shorts.toShorts();

        data = new Object[]{(short) -77, (short) 100, (short) 111, (short) 15};

        assertThat(shorts1.removeAll(Arrays.asList((short) 0, (short) -13))).isEqualTo(true);
        testShorts(shorts1, toShorts(data));
        testAllConversions(shorts1, data);

        final Drops<Short> shorts2 = shorts.toShorts();

        data = new Object[]{(short) -13, (short) 0, (short) 0, (short) 0, (short) 0};

        assertThat(shorts2.retainAll(Arrays.asList((short) 0, (short) -13))).isEqualTo(true);
        testShorts(shorts2, toShorts(data));
        testAllConversions(shorts2, data);

        data = new Object[]{(short) 0, (short) -77, (short) 100, (short) 0};

        testShorts(shorts.subList(2, 6), toShorts(data));
        testAllConversions(shorts.subList(2, 6), data);

        data = new Object[]{(short) -13, (short) 0, (short) 111, (short) 15, (short) 0};

        shorts.subList(2, 6).clear();
        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        shorts.clear();
        testAllEmpty(shorts);

        testAllEmpty(Drops.asList(new short[0]));
    }

    private void testAllConversions(final Drops<?> drops, final Object... data) {

        testBooleans(drops.toBooleans(), toBooleans(data));
        testBytes(drops.toBytes(), toBytes(data));
        testCharacters(drops.toCharacters(), toChars(data));
        testDoubles(drops.toDoubles(), toDoubles(data));
        testFloats(drops.toFloats(), toFloats(data));
        testIntegers(drops.toIntegers(), toIntegers(data));
        testLongs(drops.toLongs(), toLongs(data));
        testShorts(drops.toShorts(), toShorts(data));
        testObjects(drops.toObjects(), data);
    }

    private void testAllEmpty(final Drops<?> drops) {

        testEmpty(drops);
        testEmpty(drops.toBooleans());
        testEmpty(drops.toBytes());
        testEmpty(drops.toCharacters());
        testEmpty(drops.toDoubles());
        testEmpty(drops.toFloats());
        testEmpty(drops.toIntegers());
        testEmpty(drops.toLongs());
        testEmpty(drops.toShorts());
        testEmpty(drops.toObjects());
    }

    private void testBooleans(final Drops<Boolean> drops, final Boolean... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Boolean[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Boolean> iterator = drops.iterator();
        for (final Boolean datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Boolean> listIterator = drops.listIterator();
        for (final Boolean datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Boolean> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testBytes(final Drops<Byte> drops, final Byte... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Byte[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Byte> iterator = drops.iterator();
        for (final Byte datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Byte> listIterator = drops.listIterator();
        for (final Byte datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Byte> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testCharacters(final Drops<Character> drops, final Character... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Character[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Character> iterator = drops.iterator();
        for (final Character datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Character> listIterator = drops.listIterator();
        for (final Character datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Character> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testDoubles(final Drops<Double> drops, final Double... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Double[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Double> iterator = drops.iterator();
        for (final Double datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Double> listIterator = drops.listIterator();
        for (final Double datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Double> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testEmpty(final List<?> drops) {

        assertThat(drops.size()).isEqualTo(0);
        assertThat(drops.isEmpty()).isTrue();
        assertThat(drops.toArray()).isEmpty();
        assertThat(drops.iterator().hasNext()).isFalse();
        assertThat(drops.listIterator().hasNext()).isFalse();
    }

    private void testFloats(final Drops<Float> drops, final Float... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Float[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Float> iterator = drops.iterator();
        for (final Float datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Float> listIterator = drops.listIterator();
        for (final Float datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Float> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testIntegers(final Drops<Integer> drops, final Integer... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Integer[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Integer> iterator = drops.iterator();
        for (final Integer datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Integer> listIterator = drops.listIterator();
        for (final Integer datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Integer> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testLongs(final Drops<Long> drops, final Long... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Long[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Long> iterator = drops.iterator();
        for (final Long datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Long> listIterator = drops.listIterator();
        for (final Long datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Long> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testObjects(final List<Object> drops, final Object... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Object[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Object> iterator = drops.iterator();
        for (final Object datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Object> listIterator = drops.listIterator();
        for (final Object datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Object> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testShorts(final Drops<Short> drops, final Short... data) {

        final Object[] objects = new Object[data.length];
        System.arraycopy(data, 0, objects, 0, data.length);

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(objects);
        assertThat(drops.toArray(new Short[data.length])).containsExactly(data);
        assertThat(drops.containsAll(Arrays.asList(data))).isTrue();
        assertThat(drops.indexOf(data[0])).isEqualTo(0);
        assertThat(drops.lastIndexOf(data[0])).isEqualTo(Arrays.asList(data).lastIndexOf(data[0]));
        assertThat(drops.retainAll(Arrays.asList(data))).isFalse();
        assertThat(drops).containsExactly(data);

        for (int i = 0; i < data.length; i++) {

            assertThat(drops.get(i)).isEqualTo(data[i]);
        }

        final Iterator<Short> iterator = drops.iterator();
        for (final Short datum : data) {

            assertThat(iterator.next()).isEqualTo(datum);
        }
        assertThat(iterator.hasNext()).isFalse();

        final ListIterator<Short> listIterator = drops.listIterator();
        for (final Short datum : data) {

            assertThat(listIterator.next()).isEqualTo(datum);
        }
        assertThat(listIterator.hasNext()).isFalse();

        final ListIterator<Short> indexIterator = drops.listIterator(1);
        for (int i = 1; i < data.length; i++) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private Boolean[] toBooleans(final Object... data) {

        final int length = data.length;

        final Boolean[] booleans = new Boolean[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                booleans[i] = ((Number) data[i]).longValue() != 0;

            } else if (data[i] instanceof Character) {

                booleans[i] = ((Character) data[i]) != 0;

            } else {

                booleans[i] = (Boolean) data[i];
            }
        }

        return booleans;
    }

    private Byte[] toBytes(final Object... data) {

        final int length = data.length;

        final Byte[] bytes = new Byte[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                bytes[i] = ((Number) data[i]).byteValue();

            } else if (data[i] instanceof Character) {

                bytes[i] = (byte) ((Character) data[i]).charValue();

            } else {

                bytes[i] = ((Boolean) data[i]) ? (byte) 1 : (byte) 0;
            }
        }

        return bytes;
    }

    private Character[] toChars(final Object... data) {

        final int length = data.length;

        final Character[] chars = new Character[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                chars[i] = (char) ((Number) data[i]).shortValue();

            } else if (data[i] instanceof Character) {

                chars[i] = (Character) data[i];

            } else {

                chars[i] = ((Boolean) data[i]) ? (char) 1 : (char) 0;
            }
        }

        return chars;
    }

    private Double[] toDoubles(final Object... data) {

        final int length = data.length;

        final Double[] doubles = new Double[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                doubles[i] = ((Number) data[i]).doubleValue();

            } else if (data[i] instanceof Character) {

                doubles[i] = (double) ((Character) data[i]);

            } else {

                doubles[i] = ((Boolean) data[i]) ? (double) 1 : (double) 0;
            }
        }

        return doubles;
    }

    private Float[] toFloats(final Object... data) {

        final int length = data.length;

        final Float[] floats = new Float[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                floats[i] = ((Number) data[i]).floatValue();

            } else if (data[i] instanceof Character) {

                floats[i] = (float) ((Character) data[i]);

            } else {

                floats[i] = ((Boolean) data[i]) ? (float) 1 : (float) 0;
            }
        }

        return floats;
    }

    private Integer[] toIntegers(final Object... data) {

        final int length = data.length;

        final Integer[] ints = new Integer[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                ints[i] = ((Number) data[i]).intValue();

            } else if (data[i] instanceof Character) {

                ints[i] = (int) ((Character) data[i]);

            } else {

                ints[i] = ((Boolean) data[i]) ? 1 : 0;
            }
        }

        return ints;
    }

    private Long[] toLongs(final Object... data) {

        final int length = data.length;

        final Long[] longs = new Long[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                longs[i] = ((Number) data[i]).longValue();

            } else if (data[i] instanceof Character) {

                longs[i] = (long) ((Character) data[i]);

            } else {

                longs[i] = ((Boolean) data[i]) ? (long) 1 : (long) 0;
            }
        }

        return longs;
    }

    private Short[] toShorts(final Object... data) {

        final int length = data.length;

        final Short[] shorts = new Short[length];

        for (int i = 0; i < length; i++) {

            if (data[i] instanceof Number) {

                shorts[i] = ((Number) data[i]).shortValue();

            } else if (data[i] instanceof Character) {

                shorts[i] = (short) ((Character) data[i]).charValue();

            } else {

                shorts[i] = ((Boolean) data[i]) ? (short) 1 : (short) 0;
            }
        }

        return shorts;
    }
}