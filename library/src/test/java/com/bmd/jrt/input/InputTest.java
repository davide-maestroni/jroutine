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
package com.bmd.jrt.input;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for inputs utility classes.
 * <p/>
 * Created by davide on 6/25/14.
 */
public class InputTest extends TestCase {

    public void testBooleans() {

        Object[] data = new Object[]{true, true, false, true, false};

        final Inputs<Boolean> booleans = Inputs.asList(true, true, false, true, false);

        testBooleans(booleans, toBooleans(data));
        testAllConversions(booleans, data);

        data = new Object[]{false, true};

        testBooleans(booleans.subList(2, 4), toBooleans(data));
        testAllConversions(booleans.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new boolean[0]));
    }

    public void testBytes() {

        Object[] data = new Object[]{(byte) 4, (byte) -77, (byte) 100, (byte) 0, (byte) -32};

        final Inputs<Byte> bytes =
                Inputs.asList((byte) 4, (byte) -77, (byte) 100, (byte) 0, (byte) -32);

        testBytes(bytes, toBytes(data));
        testAllConversions(bytes, data);

        data = new Object[]{(byte) 100, (byte) 0};

        testBytes(bytes.subList(2, 4), toBytes(data));
        testAllConversions(bytes.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new byte[0]));
    }

    public void testChars() {

        Object[] data = new Object[]{(char) 4, (char) -77, (char) 100, (char) 0, (char) -32};

        final Inputs<Character> chars =
                Inputs.asList((char) 4, (char) -77, (char) 100, (char) 0, (char) -32);

        testCharacters(chars, toChars(data));
        testAllConversions(chars, data);

        data = new Object[]{(char) 100, (char) 0};

        testCharacters(chars.subList(2, 4), toChars(data));
        testAllConversions(chars.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new char[0]));
    }

    public void testDoubles() {

        Object[] data =
                new Object[]{(double) 4, (double) -77, (double) 100, (double) 0, (double) -32};

        final Inputs<Double> doubles =
                Inputs.asList((double) 4, (double) -77, (double) 100, (double) 0, (double) -32);

        testDoubles(doubles, toDoubles(data));
        testAllConversions(doubles, data);

        data = new Object[]{(double) 100, (double) 0};

        testDoubles(doubles.subList(2, 4), toDoubles(data));
        testAllConversions(doubles.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new double[0]));
    }

    public void testFloats() {

        Object[] data = new Object[]{(float) 4, (float) -77, (float) 100, (float) 0, (float) -32};

        final Inputs<Float> floats =
                Inputs.asList((float) 4, (float) -77, (float) 100, (float) 0, (float) -32);

        testFloats(floats, toFloats(data));
        testAllConversions(floats, data);

        data = new Object[]{(float) 100, (float) 0};

        testFloats(floats.subList(2, 4), toFloats(data));
        testAllConversions(floats.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new float[0]));
    }

    public void testIntegers() {

        Object[] data = new Object[]{4, -77, 100, 0, -32};

        final Inputs<Integer> integers = Inputs.asList(4, -77, 100, 0, -32);

        testIntegers(integers, toIntegers(data));
        testAllConversions(integers, data);

        data = new Object[]{100, 0};

        testIntegers(integers.subList(2, 4), toIntegers(data));
        testAllConversions(integers.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new int[0]));
    }

    public void testLongs() {

        Object[] data = new Object[]{(long) 4, (long) -77, (long) 100, (long) 0, (long) -32};

        final Inputs<Long> longs =
                Inputs.asList((long) 4, (long) -77, (long) 100, (long) 0, (long) -32);

        testLongs(longs, toLongs(data));
        testAllConversions(longs, data);

        data = new Object[]{(long) 100, (long) 0};

        testLongs(longs.subList(2, 4), toLongs(data));
        testAllConversions(longs.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new long[0]));
    }

    public void testShorts() {

        Object[] data = new Object[]{(short) 4, (short) -77, (short) 100, (short) 0, (short) -32};

        final Inputs<Short> shorts =
                Inputs.asList((short) 4, (short) -77, (short) 100, (short) 0, (short) -32);

        testShorts(shorts, toShorts(data));
        testAllConversions(shorts, data);

        data = new Object[]{(short) 100, (short) 0};

        testShorts(shorts.subList(2, 4), toShorts(data));
        testAllConversions(shorts.subList(2, 4), data);

        testAllEmpty(Inputs.asList(new short[0]));
    }

    private void testAllConversions(final Inputs<?> drops, final Object... data) {

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

    private void testAllEmpty(final Inputs<?> drops) {

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

    private void testBooleans(final Inputs<Boolean> drops, final Boolean... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testBytes(final Inputs<Byte> drops, final Byte... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testCharacters(final Inputs<Character> drops, final Character... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testDoubles(final Inputs<Double> drops, final Double... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

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

    private void testFloats(final Inputs<Float> drops, final Float... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testIntegers(final Inputs<Integer> drops, final Integer... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testLongs(final Inputs<Long> drops, final Long... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private void testShorts(final Inputs<Short> drops, final Short... data) {

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

        for (int i = 0; i < data.length; ++i) {

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
        for (int i = 1; i < data.length; ++i) {

            assertThat(indexIterator.next()).isEqualTo(data[i]);
        }
        assertThat(indexIterator.hasNext()).isFalse();
    }

    private Boolean[] toBooleans(final Object... data) {

        final int length = data.length;

        final Boolean[] booleans = new Boolean[length];

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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

        for (int i = 0; i < length; ++i) {

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