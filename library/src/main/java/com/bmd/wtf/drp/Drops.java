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
 *
 * @param <DATA> the list element type.
 */
public class Drops<DATA> implements List<DATA> {

    private final List<Object> mList;

    private final Class<DATA> mType;

    /**
     * Avoid direct instantiation.
     *
     * @param type    the list element type.
     * @param wrapped the wrapped list.
     */
    private Drops(final Class<DATA> type, final List<Object> wrapped) {

        mType = type;
        mList = wrapped;
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Boolean> asList(final boolean... data) {

        return new Drops<Boolean>(boolean.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Byte> asList(final byte... data) {

        return new Drops<Byte>(byte.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Character> asList(final char... data) {

        return new Drops<Character>(char.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Double> asList(final double... data) {

        return new Drops<Double>(double.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Float> asList(final float... data) {

        return new Drops<Float>(float.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Integer> asList(final int... data) {

        return new Drops<Integer>(int.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Long> asList(final long... data) {

        return new Drops<Long>(long.class, asObjects(data));
    }

    /**
     * Returns a drop list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created drop list.
     */
    public static Drops<Short> asList(final short... data) {

        return new Drops<Short>(short.class, asObjects(data));
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final long... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final long l : data) {

            list.add(l);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final short... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final short s : data) {

            list.add(s);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final byte... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final byte b : data) {

            list.add(b);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final char... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final char c : data) {

            list.add(c);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final double... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final double d : data) {

            list.add(d);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final float... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final float f : data) {

            list.add(f);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final int... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final int i : data) {

            list.add(i);
        }

        return list;
    }

    /**
     * Returns a list containing the specified native data.
     *
     * @param data the data to fill the list with.
     * @return the newly created list.
     */
    public static List<Object> asObjects(final boolean... data) {

        if ((data == null) || (data.length == 0)) {

            return new ArrayList<Object>(0);
        }

        final ArrayList<Object> list = new ArrayList<Object>(data.length);

        for (final boolean b : data) {

            list.add(b);
        }

        return list;
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

        //noinspection unchecked
        return (Iterator<DATA>) mList.iterator();
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

        //noinspection unchecked
        return (DATA) mList.get(index);
    }

    @Override
    public DATA set(final int index, final DATA element) {

        //noinspection unchecked
        return (DATA) mList.set(index, element);
    }

    @Override
    public void add(final int index, final DATA element) {

        mList.add(index, element);
    }

    @Override
    public DATA remove(final int index) {

        //noinspection unchecked
        return (DATA) mList.remove(index);
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

        //noinspection unchecked
        return (ListIterator<DATA>) mList.listIterator();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ListIterator<DATA> listIterator(final int index) {

        //noinspection unchecked
        return (ListIterator<DATA>) mList.listIterator(index);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Drops<DATA> subList(final int fromIndex, final int toIndex) {

        return new Drops<DATA>(mType, mList.subList(fromIndex, toIndex));
    }

    /**
     * Transforms this list in a list of booleans.
     *
     * @return a newly created list of booleans.
     */
    public Drops<Boolean> toBooleans() {

        if (boolean.class.equals(mType)) {

            //noinspection unchecked
            return new Drops<Boolean>(boolean.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Boolean>(boolean.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add(((Character) data) != 0);
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.longValue() != 0);
            }
        }

        return new Drops<Boolean>(boolean.class, newList);
    }

    /**
     * Transforms this list in a list of bytes.
     *
     * @return a newly created list of bytes.
     */
    public Drops<Byte> toBytes() {

        final Class<DATA> type = mType;

        if (byte.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Byte>(byte.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Byte>(byte.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((byte) (bool ? 1 : 0));
            }

        } else if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add((byte) ((Character) data).charValue());
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.byteValue());
            }
        }

        return new Drops<Byte>(byte.class, newList);
    }

    /**
     * Transforms this list in a list of chars.
     *
     * @return a newly created list of chars.
     */
    public Drops<Character> toCharacters() {

        final Class<DATA> type = mType;

        if (char.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Character>(char.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Character>(char.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((char) (bool ? 1 : 0));
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add((char) number.shortValue());
            }
        }

        return new Drops<Character>(char.class, newList);
    }

    /**
     * Transforms this list in a list of doubles.
     *
     * @return a newly created list of doubles.
     */
    public Drops<Double> toDoubles() {

        final Class<DATA> type = mType;

        if (double.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Double>(double.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Double>(double.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((double) (bool ? 1 : 0));
            }

        } else if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add((double) ((Character) data));
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.doubleValue());
            }
        }

        return new Drops<Double>(double.class, newList);
    }

    /**
     * Transforms this list in a list of floats.
     *
     * @return a newly created list of floats.
     */
    public Drops<Float> toFloats() {

        final Class<DATA> type = mType;

        if (float.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Float>(float.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Float>(float.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((float) (bool ? 1 : 0));
            }

        } else if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add((float) ((Character) data));
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.floatValue());
            }
        }

        return new Drops<Float>(float.class, newList);
    }

    /**
     * Transforms this list in a list of integers.
     *
     * @return a newly created list of integers.
     */
    public Drops<Integer> toIntegers() {

        final Class<DATA> type = mType;

        if (int.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Integer>(int.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Integer>(int.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add(bool ? 1 : 0);
            }

        } else if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add((int) ((Character) data));
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.intValue());
            }
        }

        return new Drops<Integer>(int.class, newList);
    }

    /**
     * Transforms this list in a list of longs.
     *
     * @return a newly created list of longs.
     */
    public Drops<Long> toLongs() {

        final Class<DATA> type = mType;

        if (long.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Long>(long.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Long>(long.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((long) (bool ? 1 : 0));
            }

        } else if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add((long) ((Character) data));
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.longValue());
            }
        }

        return new Drops<Long>(long.class, newList);
    }

    /**
     * Transforms this list in a list of objects.
     *
     * @return a newly created list of objects.
     */
    public List<Object> toObjects() {

        return new ArrayList<Object>(mList);
    }

    /**
     * Transforms this list in a list of shorts.
     *
     * @return a newly created list of shorts.
     */
    public Drops<Short> toShorts() {

        final Class<DATA> type = mType;

        if (short.class.equals(type)) {

            //noinspection unchecked
            return new Drops<Short>(short.class, new ArrayList<Object>(mList));
        }

        final List<Object> list = mList;

        if (list.isEmpty()) {

            return new Drops<Short>(short.class, new ArrayList<Object>(0));
        }

        final ArrayList<Object> newList = new ArrayList<Object>(list.size());

        if (boolean.class.equals(type)) {

            for (final Object data : list) {

                final Boolean bool = (Boolean) data;

                newList.add((short) (bool ? 1 : 0));
            }

        } else if (char.class.equals(mType)) {

            for (final Object data : list) {

                newList.add((short) ((Character) data).charValue());
            }

        } else {

            for (final Object data : list) {

                final Number number = (Number) data;

                newList.add(number.shortValue());
            }
        }

        return new Drops<Short>(short.class, newList);
    }
}