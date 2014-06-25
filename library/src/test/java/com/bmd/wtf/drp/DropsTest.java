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

        final Drops<Boolean> booleans = Drops.asList(true, true, false, true, false);

        testBooleans(booleans, true, true, false, true, false);
        testBooleans(booleans.toBooleans(), true, true, false, true, false);
        testBytes(booleans.toBytes(), (byte) 1, (byte) 1, (byte) 0, (byte) 1, (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 1, (char) 1, (char) 0, (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 1, (double) 1, (double) 0, (double) 1,
                    (double) 0);
        testFloats(booleans.toFloats(), (float) 1, (float) 1, (float) 0, (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 1, 1, 0, 1, 0);
        testLongs(booleans.toLongs(), (long) 1, (long) 1, (long) 0, (long) 1, (long) 0);
        testShorts(booleans.toShorts(), (short) 1, (short) 1, (short) 0, (short) 1, (short) 0);
        testObjects(booleans.toObjects(), true, true, false, true, false);

        assertThat(booleans.remove(0)).isEqualTo(true);
        testBooleans(booleans, true, false, true, false);
        testBooleans(booleans.toBooleans(), true, false, true, false);
        testBytes(booleans.toBytes(), (byte) 1, (byte) 0, (byte) 1, (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 1, (char) 0, (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 1, (double) 0, (double) 1, (double) 0);
        testFloats(booleans.toFloats(), (float) 1, (float) 0, (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 1, 0, 1, 0);
        testLongs(booleans.toLongs(), (long) 1, (long) 0, (long) 1, (long) 0);
        testShorts(booleans.toShorts(), (short) 1, (short) 0, (short) 1, (short) 0);
        testObjects(booleans.toObjects(), true, false, true, false);

        assertThat(booleans.remove(true)).isEqualTo(true);
        testBooleans(booleans, false, true, false);
        testBooleans(booleans.toBooleans(), false, true, false);
        testBytes(booleans.toBytes(), (byte) 0, (byte) 1, (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 0, (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 0, (double) 1, (double) 0);
        testFloats(booleans.toFloats(), (float) 0, (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 0, 1, 0);
        testLongs(booleans.toLongs(), (long) 0, (long) 1, (long) 0);
        testShorts(booleans.toShorts(), (short) 0, (short) 1, (short) 0);
        testObjects(booleans.toObjects(), false, true, false);

        assertThat(booleans.add(true)).isEqualTo(true);
        testBooleans(booleans, false, true, false, true);
        testBooleans(booleans.toBooleans(), false, true, false, true);
        testBytes(booleans.toBytes(), (byte) 0, (byte) 1, (byte) 0, (byte) 1);
        testCharacters(booleans.toCharacters(), (char) 0, (char) 1, (char) 0, (char) 1);
        testDoubles(booleans.toDoubles(), (double) 0, (double) 1, (double) 0, (double) 1);
        testFloats(booleans.toFloats(), (float) 0, (float) 1, (float) 0, (float) 1);
        testIntegers(booleans.toIntegers(), 0, 1, 0, 1);
        testLongs(booleans.toLongs(), (long) 0, (long) 1, (long) 0, (long) 1);
        testShorts(booleans.toShorts(), (short) 0, (short) 1, (short) 0, (short) 1);
        testObjects(booleans.toObjects(), false, true, false, true);

        booleans.add(0, false);
        testBooleans(booleans, false, false, true, false, true);
        testBooleans(booleans.toBooleans(), false, false, true, false, true);
        testBytes(booleans.toBytes(), (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 1);
        testCharacters(booleans.toCharacters(), (char) 0, (char) 0, (char) 1, (char) 0, (char) 1);
        testDoubles(booleans.toDoubles(), (double) 0, (double) 0, (double) 1, (double) 0,
                    (double) 1);
        testFloats(booleans.toFloats(), (float) 0, (float) 0, (float) 1, (float) 0, (float) 1);
        testIntegers(booleans.toIntegers(), 0, 0, 1, 0, 1);
        testLongs(booleans.toLongs(), (long) 0, (long) 0, (long) 1, (long) 0, (long) 1);
        testShorts(booleans.toShorts(), (short) 0, (short) 0, (short) 1, (short) 0, (short) 1);
        testObjects(booleans.toObjects(), false, false, true, false, true);

        assertThat(booleans.addAll(Arrays.asList(true, false))).isEqualTo(true);
        testBooleans(booleans, false, false, true, false, true, true, false);
        testBooleans(booleans.toBooleans(), false, false, true, false, true, true, false);
        testBytes(booleans.toBytes(), (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 1, (byte) 1,
                  (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 0, (char) 0, (char) 1, (char) 0, (char) 1,
                       (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 0, (double) 0, (double) 1, (double) 0,
                    (double) 1, (double) 1, (double) 0);
        testFloats(booleans.toFloats(), (float) 0, (float) 0, (float) 1, (float) 0, (float) 1,
                   (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 0, 0, 1, 0, 1, 1, 0);
        testLongs(booleans.toLongs(), (long) 0, (long) 0, (long) 1, (long) 0, (long) 1, (long) 1,
                  (long) 0);
        testShorts(booleans.toShorts(), (short) 0, (short) 0, (short) 1, (short) 0, (short) 1,
                   (short) 1, (short) 0);
        testObjects(booleans.toObjects(), false, false, true, false, true, true, false);

        assertThat(booleans.addAll(0, Arrays.asList(true, true))).isEqualTo(true);
        testBooleans(booleans, true, true, false, false, true, false, true, true, false);
        testBooleans(booleans.toBooleans(), true, true, false, false, true, false, true, true,
                     false);
        testBytes(booleans.toBytes(), (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 1, (byte) 0,
                  (byte) 1, (byte) 1, (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 1, (char) 1, (char) 0, (char) 0, (char) 1,
                       (char) 0, (char) 1, (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 1, (double) 1, (double) 0, (double) 0,
                    (double) 1, (double) 0, (double) 1, (double) 1, (double) 0);
        testFloats(booleans.toFloats(), (float) 1, (float) 1, (float) 0, (float) 0, (float) 1,
                   (float) 0, (float) 1, (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 1, 1, 0, 0, 1, 0, 1, 1, 0);
        testLongs(booleans.toLongs(), (long) 1, (long) 1, (long) 0, (long) 0, (long) 1, (long) 0,
                  (long) 1, (long) 1, (long) 0);
        testShorts(booleans.toShorts(), (short) 1, (short) 1, (short) 0, (short) 0, (short) 1,
                   (short) 0, (short) 1, (short) 1, (short) 0);
        testObjects(booleans.toObjects(), true, true, false, false, true, false, true, true, false);

        assertThat(booleans.set(2, true)).isEqualTo(false);
        testBooleans(booleans, true, true, true, false, true, false, true, true, false);
        testBooleans(booleans.toBooleans(), true, true, true, false, true, false, true, true,
                     false);
        testBytes(booleans.toBytes(), (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 1, (byte) 0,
                  (byte) 1, (byte) 1, (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 1, (char) 1, (char) 1, (char) 0, (char) 1,
                       (char) 0, (char) 1, (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 1, (double) 1, (double) 1, (double) 0,
                    (double) 1, (double) 0, (double) 1, (double) 1, (double) 0);
        testFloats(booleans.toFloats(), (float) 1, (float) 1, (float) 1, (float) 0, (float) 1,
                   (float) 0, (float) 1, (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 1, 1, 1, 0, 1, 0, 1, 1, 0);
        testLongs(booleans.toLongs(), (long) 1, (long) 1, (long) 1, (long) 0, (long) 1, (long) 0,
                  (long) 1, (long) 1, (long) 0);
        testShorts(booleans.toShorts(), (short) 1, (short) 1, (short) 1, (short) 0, (short) 1,
                   (short) 0, (short) 1, (short) 1, (short) 0);
        testObjects(booleans.toObjects(), true, true, true, false, true, false, true, true, false);

        final Drops<Boolean> booleans1 = booleans.toBooleans();

        assertThat(booleans1.removeAll(Arrays.asList(true))).isEqualTo(true);
        testBooleans(booleans1, false, false, false);
        testBooleans(booleans1.toBooleans(), false, false, false);
        testBytes(booleans1.toBytes(), (byte) 0, (byte) 0, (byte) 0);
        testCharacters(booleans1.toCharacters(), (char) 0, (char) 0, (char) 0);
        testDoubles(booleans1.toDoubles(), (double) 0, (double) 0, (double) 0);
        testFloats(booleans1.toFloats(), (float) 0, (float) 0, (float) 0);
        testIntegers(booleans1.toIntegers(), 0, 0, 0);
        testLongs(booleans1.toLongs(), (long) 0, (long) 0, (long) 0);
        testShorts(booleans1.toShorts(), (short) 0, (short) 0, (short) 0);
        testObjects(booleans1.toObjects(), false, false, false);

        final Drops<Boolean> booleans2 = booleans.toBooleans();

        assertThat(booleans2.retainAll(Arrays.asList(true))).isEqualTo(true);
        testBooleans(booleans2, true, true, true, true, true, true);
        testBooleans(booleans2.toBooleans(), true, true, true, true, true, true);
        testBytes(booleans2.toBytes(), (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1);
        testCharacters(booleans2.toCharacters(), (char) 1, (char) 1, (char) 1, (char) 1, (char) 1,
                       (char) 1);
        testDoubles(booleans2.toDoubles(), (double) 1, (double) 1, (double) 1, (double) 1,
                    (double) 1, (double) 1);
        testFloats(booleans2.toFloats(), (float) 1, (float) 1, (float) 1, (float) 1, (float) 1,
                   (float) 1);
        testIntegers(booleans2.toIntegers(), 1, 1, 1, 1, 1, 1);
        testLongs(booleans2.toLongs(), (long) 1, (long) 1, (long) 1, (long) 1, (long) 1, (long) 1);
        testShorts(booleans2.toShorts(), (short) 1, (short) 1, (short) 1, (short) 1, (short) 1,
                   (short) 1);
        testObjects(booleans2.toObjects(), true, true, true, true, true, true);

        testBooleans(booleans.subList(2, 6), true, false, true, false);
        testBooleans(booleans.subList(2, 6).toBooleans(), true, false, true, false);
        testBytes(booleans.subList(2, 6).toBytes(), (byte) 1, (byte) 0, (byte) 1, (byte) 0);
        testCharacters(booleans.subList(2, 6).toCharacters(), (char) 1, (char) 0, (char) 1,
                       (char) 0);
        testDoubles(booleans.subList(2, 6).toDoubles(), (double) 1, (double) 0, (double) 1,
                    (double) 0);
        testFloats(booleans.subList(2, 6).toFloats(), (float) 1, (float) 0, (float) 1, (float) 0);
        testIntegers(booleans.subList(2, 6).toIntegers(), 1, 0, 1, 0);
        testLongs(booleans.subList(2, 6).toLongs(), (long) 1, (long) 0, (long) 1, (long) 0);
        testShorts(booleans.subList(2, 6).toShorts(), (short) 1, (short) 0, (short) 1, (short) 0);
        testObjects(booleans.subList(2, 6).toObjects(), true, false, true, false);

        booleans.subList(2, 6).clear();
        testBooleans(booleans, true, true, true, true, false);
        testBooleans(booleans.toBooleans(), true, true, true, true, false);
        testBytes(booleans.toBytes(), (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 0);
        testCharacters(booleans.toCharacters(), (char) 1, (char) 1, (char) 1, (char) 1, (char) 0);
        testDoubles(booleans.toDoubles(), (double) 1, (double) 1, (double) 1, (double) 1,
                    (double) 0);
        testFloats(booleans.toFloats(), (float) 1, (float) 1, (float) 1, (float) 1, (float) 0);
        testIntegers(booleans.toIntegers(), 1, 1, 1, 1, 0);
        testLongs(booleans.toLongs(), (long) 1, (long) 1, (long) 1, (long) 1, (long) 0);
        testShorts(booleans.toShorts(), (short) 1, (short) 1, (short) 1, (short) 1, (short) 0);
        testObjects(booleans.toObjects(), true, true, true, true, false);

        booleans.clear();
        testEmpty(booleans);
        testEmpty(booleans.toBooleans());
        testEmpty(booleans.toBytes());
        testEmpty(booleans.toCharacters());
        testEmpty(booleans.toDoubles());
        testEmpty(booleans.toFloats());
        testEmpty(booleans.toIntegers());
        testEmpty(booleans.toLongs());
        testEmpty(booleans.toShorts());
        testEmpty(booleans.toObjects());
    }

    public void testBytes() {

        final Drops<Byte> bytes =
                Drops.asList((byte) 4, (byte) -77, (byte) 100, (byte) 101, (byte) -32);
    }

    private void testBooleans(final Drops<Boolean> drops, final Boolean... data) {

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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

        assertThat(drops).containsExactly(data);
        assertThat(drops.size()).isEqualTo(data.length);
        assertThat(drops.isEmpty()).isEqualTo(data.length == 0);
        assertThat(drops.contains(data[0])).isTrue();
        assertThat(drops.toArray()).containsExactly(data);
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
}