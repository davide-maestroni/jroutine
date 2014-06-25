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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Helper class providing utility methods to convert native arrays in object lists.
 * <p/>
 * Created by davide on 6/12/14.
 */
public class Drops<DATA> implements List<DATA> {

    private final List<DATA> mList;

    private final Class<DATA> mType;

    private Drops(final Class<DATA> type, final List<DATA> list) {

        mType = type;
        mList = list;
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Boolean> asList(final boolean... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Boolean>(boolean.class, new ArrayList<Boolean>(0));
        }

        final ArrayList<Boolean> list = new ArrayList<Boolean>(data.length);

        for (final boolean b : data) {

            list.add(b);
        }

        return new Drops<Boolean>(boolean.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Byte> asList(final byte... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Byte>(byte.class, new ArrayList<Byte>(0));
        }

        final ArrayList<Byte> list = new ArrayList<Byte>(data.length);

        for (final byte b : data) {

            list.add(b);
        }

        return new Drops<Byte>(byte.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Character> asList(final char... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Character>(char.class, new ArrayList<Character>(0));
        }

        final ArrayList<Character> list = new ArrayList<Character>(data.length);

        for (final char c : data) {

            list.add(c);
        }

        return new Drops<Character>(char.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Double> asList(final double... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Double>(double.class, new ArrayList<Double>(0));
        }

        final ArrayList<Double> list = new ArrayList<Double>(data.length);

        for (final double d : data) {

            list.add(d);
        }

        return new Drops<Double>(double.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Float> asList(final float... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Float>(float.class, new ArrayList<Float>(0));
        }

        final ArrayList<Float> list = new ArrayList<Float>(data.length);

        for (final float f : data) {

            list.add(f);
        }

        return new Drops<Float>(float.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Integer> asList(final int... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Integer>(int.class, new ArrayList<Integer>(0));
        }

        final ArrayList<Integer> list = new ArrayList<Integer>(data.length);

        for (final int i : data) {

            list.add(i);
        }

        return new Drops<Integer>(int.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Long> asList(final long... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Long>(long.class, new ArrayList<Long>(0));
        }

        final ArrayList<Long> list = new ArrayList<Long>(data.length);

        for (final long l : data) {

            list.add(l);
        }

        return new Drops<Long>(long.class, list);
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data The data to fill the list with.
     * @return The newly created drop list.
     */
    public static Drops<Short> asList(final short... data) {

        if ((data == null) || (data.length == 0)) {

            return new Drops<Short>(short.class, new ArrayList<Short>(0));
        }

        final ArrayList<Short> list = new ArrayList<Short>(data.length);

        for (final short s : data) {

            list.add(s);
        }

        return new Drops<Short>(short.class, list);
    }

    @Override
    public int size() {

        return mList.size();
    }

    @Override
    public boolean isEmpty() {

        return mList.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {

        return mList.contains(o);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Iterator<DATA> iterator() {

        return mList.iterator();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Object[] toArray() {

        return mList.toArray();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public <T> T[] toArray(final T[] a) {

        //noinspection SuspiciousToArrayCall
        return mList.toArray(a);
    }

    @Override
    public boolean add(final DATA e) {

        return mList.add(e);
    }

    @Override
    public boolean remove(final Object o) {

        return mList.remove(o);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean containsAll(final Collection<?> c) {

        return mList.containsAll(c);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean addAll(final Collection<? extends DATA> c) {

        return mList.addAll(c);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean addAll(final int index, final Collection<? extends DATA> c) {

        return mList.addAll(index, c);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean removeAll(final Collection<?> c) {

        return mList.removeAll(c);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean retainAll(final Collection<?> c) {

        return mList.retainAll(c);
    }

    @Override
    public void clear() {

        mList.clear();
    }

    @Override
    public DATA get(final int index) {

        return mList.get(index);
    }

    @Override
    public DATA set(final int index, final DATA element) {

        return mList.set(index, element);
    }

    @Override
    public void add(final int index, final DATA element) {

        mList.add(index, element);
    }

    @Override
    public DATA remove(final int index) {

        return mList.remove(index);
    }

    @Override
    public int indexOf(final Object o) {

        return mList.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {

        return mList.lastIndexOf(o);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ListIterator<DATA> listIterator() {

        return mList.listIterator();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ListIterator<DATA> listIterator(final int index) {

        return mList.listIterator(index);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Drops<DATA> subList(final int fromIndex, final int toIndex) {

        return new Drops<DATA>(mType, mList.subList(fromIndex, toIndex));
    }

    /**
     * Transforms this list in a list of booleans by making a copy of it.
     *
     * @return A list of booleans.
     */
    public Drops<Boolean> toBooleans() {

        if (boolean.class.equals(mType)) {

            //noinspection unchecked
            return new Drops<Boolean>(boolean.class, new ArrayList<Boolean>(
                    (Collection<? extends Boolean>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Boolean>(boolean.class, new ArrayList<Boolean>(0));
        }

        final ArrayList<Boolean> newList = new ArrayList<Boolean>(list.size());

        for (final DATA data : list) {

            final Number number = (Number) data;

            newList.add(number.longValue() != 0);
        }

        return new Drops<Boolean>(boolean.class, newList);
    }

    /**
     * Transforms this list in a list of bytes by making a copy of it.
     *
     * @return A list of bytes.
     */
    public Drops<Byte> toBytes() {

        final Class<DATA> type = mType;

        if (byte.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Byte>(byte.class,
                                   new ArrayList<Byte>((Collection<? extends Byte>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Byte>(byte.class, new ArrayList<Byte>(0));
        }

        final ArrayList<Byte> newList = new ArrayList<Byte>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((byte) (bool ? 1 : 0));
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add(number.byteValue());
            }
        }

        return new Drops<Byte>(byte.class, newList);
    }

    /**
     * Transforms this list in a list of chars by making a copy of it.
     *
     * @return A list of chars.
     */
    public Drops<Character> toCharacters() {

        final Class<DATA> type = mType;

        if (char.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Character>(char.class, new ArrayList<Character>(
                    (Collection<? extends Character>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Character>(char.class, new ArrayList<Character>(0));
        }

        final ArrayList<Character> newList = new ArrayList<Character>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((char) (bool ? 1 : 0));
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add((char) number.shortValue());
            }
        }

        return new Drops<Character>(char.class, newList);
    }

    /**
     * Transforms this list in a list of doubles by making a copy of it.
     *
     * @return A list of doubles.
     */
    public Drops<Double> toDoubles() {

        final Class<DATA> type = mType;

        if (double.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Double>(double.class,
                                     new ArrayList<Double>((Collection<? extends Double>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Double>(double.class, new ArrayList<Double>(0));
        }

        final ArrayList<Double> newList = new ArrayList<Double>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((double) (bool ? 1 : 0));
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add(number.doubleValue());
            }
        }

        return new Drops<Double>(double.class, newList);
    }

    /**
     * Transforms this list in a list of floats by making a copy of it.
     *
     * @return A list of floats.
     */
    public Drops<Float> toFloats() {

        final Class<DATA> type = mType;

        if (float.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Float>(float.class,
                                    new ArrayList<Float>((Collection<? extends Float>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Float>(float.class, new ArrayList<Float>(0));
        }

        final ArrayList<Float> newList = new ArrayList<Float>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((float) (bool ? 1 : 0));
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add(number.floatValue());
            }
        }

        return new Drops<Float>(float.class, newList);
    }

    /**
     * Transforms this list in a list of integers by making a copy of it.
     *
     * @return A list of ints.
     */
    public Drops<Integer> toIntegers() {

        final Class<DATA> type = mType;

        if (int.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Integer>(int.class, new ArrayList<Integer>(
                    (Collection<? extends Integer>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Integer>(int.class, new ArrayList<Integer>(0));
        }

        final ArrayList<Integer> newList = new ArrayList<Integer>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add(bool ? 1 : 0);
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add(number.intValue());
            }
        }

        return new Drops<Integer>(int.class, newList);
    }

    /**
     * Transforms this list in a list of longs by making a copy of it.
     *
     * @return A list of longs.
     */
    public Drops<Long> toLongs() {

        final Class<DATA> type = mType;

        if (long.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Long>(long.class,
                                   new ArrayList<Long>((Collection<? extends Long>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Long>(long.class, new ArrayList<Long>(0));
        }

        final ArrayList<Long> newList = new ArrayList<Long>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((long) (bool ? 1 : 0));
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add(number.longValue());
            }
        }

        return new Drops<Long>(long.class, newList);
    }

    /**
     * Transforms this list in a list of objects by making a copy of it.
     *
     * @return A list of objects.
     */
    public List<Object> toObjects() {

        return new ArrayList<Object>(mList);
    }

    /**
     * Transforms this list in a list of shorts by making a copy of it.
     *
     * @return A list of shorts.
     */
    public Drops<Short> toShorts() {

        final Class<DATA> type = mType;

        if (short.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Short>(short.class,
                                    new ArrayList<Short>((Collection<? extends Short>) mList));
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Short>(short.class, new ArrayList<Short>(0));
        }

        final ArrayList<Short> newList = new ArrayList<Short>(list.size());

        if (boolean.class.equals(type)) {

            for (final DATA data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((short) (bool ? 1 : 0));
            }

        } else {

            for (final DATA data : list) {

                final Number number = (Number) data;

                newList.add(number.shortValue());
            }
        }

        return new Drops<Short>(short.class, newList);
    }
}