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
 * Created by davide on 6/12/14.
 */
public class Drops<DATA> implements List<DATA> {

    private final List<DATA> mList;

    private final Class<DATA> mType;

    private Drops(final Class<DATA> type, final List<DATA> list) {

        mType = type;
        mList = list;
    }

    public static Drops<Boolean> asList(final boolean... data) {

        if (data == null) {

            return new Drops<Boolean>(boolean.class, new ArrayList<Boolean>(0));
        }

        final ArrayList<Boolean> list = new ArrayList<Boolean>(data.length);

        for (final boolean b : data) {

            list.add(b);
        }

        return new Drops<Boolean>(boolean.class, list);
    }

    public static Drops<Byte> asList(final byte... data) {

        if (data == null) {

            return new Drops<Byte>(byte.class, new ArrayList<Byte>(0));
        }

        final ArrayList<Byte> list = new ArrayList<Byte>(data.length);

        for (final byte b : data) {

            list.add(b);
        }

        return new Drops<Byte>(byte.class, list);
    }

    public static Drops<Character> asList(final char... data) {

        if (data == null) {

            return new Drops<Character>(char.class, new ArrayList<Character>(0));
        }

        final ArrayList<Character> list = new ArrayList<Character>(data.length);

        for (final char c : data) {

            list.add(c);
        }

        return new Drops<Character>(char.class, list);
    }

    public static Drops<Double> asList(final double... data) {

        if (data == null) {

            return new Drops<Double>(double.class, new ArrayList<Double>(0));
        }

        final ArrayList<Double> list = new ArrayList<Double>(data.length);

        for (final double d : data) {

            list.add(d);
        }

        return new Drops<Double>(double.class, list);
    }

    public static Drops<Float> asList(final float... data) {

        if (data == null) {

            return new Drops<Float>(float.class, new ArrayList<Float>(0));
        }

        final ArrayList<Float> list = new ArrayList<Float>(data.length);

        for (final float f : data) {

            list.add(f);
        }

        return new Drops<Float>(float.class, list);
    }

    public static Drops<Integer> asList(final int... data) {

        if (data == null) {

            return new Drops<Integer>(int.class, new ArrayList<Integer>(0));
        }

        final ArrayList<Integer> list = new ArrayList<Integer>(data.length);

        for (final int i : data) {

            list.add(i);
        }

        return new Drops<Integer>(int.class, list);
    }

    public static Drops<Long> asList(final long... data) {

        if (data == null) {

            return new Drops<Long>(long.class, new ArrayList<Long>(0));
        }

        final ArrayList<Long> list = new ArrayList<Long>(data.length);

        for (final long l : data) {

            list.add(l);
        }

        return new Drops<Long>(long.class, list);
    }

    public static Drops<Short> asList(final short... data) {

        if (data == null) {

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
    public Iterator<DATA> iterator() {

        return mList.iterator();
    }

    @Override
    public Object[] toArray() {

        return mList.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {

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
    public boolean containsAll(final Collection<?> c) {

        return mList.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends DATA> c) {

        return mList.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends DATA> c) {

        return mList.addAll(index, c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {

        return mList.removeAll(c);
    }

    @Override
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
    public ListIterator<DATA> listIterator() {

        return mList.listIterator();
    }

    @Override
    public ListIterator<DATA> listIterator(final int index) {

        return mList.listIterator(index);
    }

    @Override
    public List<DATA> subList(final int fromIndex, final int toIndex) {

        return new Drops<DATA>(mType, mList.subList(fromIndex, toIndex));
    }

    public Drops<Boolean> toBooleans() {

        if (boolean.class.equals(mType)) {

            //noinspection unchecked
            return (Drops<Boolean>) this;
        }

        final List<DATA> list = mList;

        if (list.isEmpty()) {

            return new Drops<Boolean>(boolean.class, new ArrayList<Boolean>(0));
        }

        final ArrayList<Boolean> newList = new ArrayList<Boolean>(list.size());

        for (final DATA data : list) {

            final Number number = (Number) data;

            // TODO: double and float?
            newList.add(number.longValue() != 0);
        }

        return new Drops<Boolean>(boolean.class, newList);
    }

    public Drops<Byte> toBytes() {

        final Class<DATA> type = mType;

        if (byte.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Byte>) this;
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

    public Drops<Character> toCharacters() {

        final Class<DATA> type = mType;

        if (char.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Character>) this;
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

    public Drops<Double> toDoubles() {

        final Class<DATA> type = mType;

        if (double.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Double>) this;
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

    public Drops<Float> toFloats() {

        final Class<DATA> type = mType;

        if (float.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Float>) this;
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

    public Drops<Integer> toIntegers() {

        final Class<DATA> type = mType;

        if (int.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Integer>) this;
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

    public Drops<Long> toLongs() {

        final Class<DATA> type = mType;

        if (long.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Long>) this;
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

    public Drops<Short> toShorts() {

        final Class<DATA> type = mType;

        if (short.class.equals(type)) {

            //noinspection unchecked
            return (Drops<Short>) this;
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