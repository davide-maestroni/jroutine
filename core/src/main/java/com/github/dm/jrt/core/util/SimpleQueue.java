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

package com.github.dm.jrt.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Minimal implementation of a light-weight queue, storing elements into a dynamically increasing
 * circular buffer.
 * <p>
 * Created by davide-maestroni on 09/27/2014.
 *
 * @param <E> the element type.
 */
public class SimpleQueue<E> extends AbstractCollection<E> implements Queue<E> {

  private static final int DEFAULT_SIZE = 1 << 3; // 8

  private Object[] mData;

  private int mFirst;

  private int mLast;

  private int mMask;

  private volatile long mReplaceCount = Long.MIN_VALUE;

  private int mSize;

  /**
   * Constructor.
   */
  public SimpleQueue() {
    mData = new Object[DEFAULT_SIZE];
    mMask = DEFAULT_SIZE - 1;
  }

  /**
   * Constructor.
   *
   * @param minCapacity the minimum capacity.
   * @throws java.lang.IllegalArgumentException if the specified capacity is less than 1.
   */
  public SimpleQueue(final int minCapacity) {
    final int msb =
        Integer.highestOneBit(ConstantConditions.positive("minimum capacity", minCapacity));
    final int initialCapacity = (minCapacity == msb) ? msb : msb << 1;
    mData = new Object[initialCapacity];
    mMask = initialCapacity - 1;
  }

  /**
   * Adds all the elements returned by the specified iterable to end of the queue.
   * <p>
   * Note that any of the returned element can be null.
   *
   * @param elements the element iterable.
   */
  public void addAll(@NotNull final Iterable<? extends E> elements) {
    // TODO: 09/06/2017 remove?
    for (final E element : elements) {
      add(element);
    }
  }

  /**
   * Adds the specified element as the first element of the queue.
   * <p>
   * Note that the element can be null.
   *
   * @param element the element to add.
   */
  public void addFirst(@Nullable final E element) {
    int mask = mMask;
    int newFirst = (mFirst = (mFirst + mask) & mask);
    mData[newFirst] = element;
    if (newFirst == mLast) {
      doubleCapacity();
    }

    ++mSize;
  }

  /**
   * Adds the specified element to end of the queue.
   * <p>
   * Note that the element can be null.
   *
   * @param element the element to add.
   */
  public void addLast(@Nullable final E element) {
    final int last = mLast;
    mData[last] = element;
    if (mFirst == (mLast = (last + 1) & mMask)) {
      doubleCapacity();
    }

    ++mSize;
  }

  @NotNull
  @Override
  public SimpleQueueIterator<E> iterator() {
    return new SimpleQueueIterator<E>(this);
  }

  @Override
  public int size() {
    return mSize;
  }

  @Override
  public boolean isEmpty() {
    return mSize == 0;
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return copyElements(new Object[size()]);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(@NotNull T[] array) {
    int size = size();
    if (array.length < size) {
      array = (T[]) Array.newInstance(array.getClass().getComponentType(), size);
      copyElements(array);

    } else {
      copyElements(array);
      if (array.length > size) {
        array[size] = null;
      }
    }

    return array;
  }

  @Override
  public boolean add(@Nullable final E element) {
    addLast(element);
    return true;
  }

  @Override
  public void clear() {
    final int mask = mMask;
    final int last = mLast;
    final Object[] data = mData;
    int index = mFirst;
    while (index != last) {
      data[index] = null;
      index = (index + 1) & mask;
    }

    mFirst = 0;
    mLast = 0;
    mSize = 0;
  }

  public boolean offer(final E e) {
    addLast(e);
    return true;
  }

  public E remove() {
    return removeFirst();
  }

  public E poll() {
    if (isEmpty()) {
      return null;
    }

    return removeFirst();
  }

  public E element() {
    return peekFirst();
  }

  public E peek() {
    if (isEmpty()) {
      return null;
    }

    return peekFirst();
  }

  /**
   * Peeks the first element of the queue.
   *
   * @return the element.
   * @throws java.util.NoSuchElementException if the queue is empty.
   */
  @SuppressWarnings("unchecked")
  public E peekFirst() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    return (E) mData[mFirst];
  }

  /**
   * Peeks the last element of the queue.
   *
   * @return the element.
   * @throws java.util.NoSuchElementException if the queue is empty.
   */
  @SuppressWarnings("unchecked")
  public E peekLast() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    final int mask = mMask;
    return (E) mData[(mLast + mask) & mask];
  }

  /**
   * Removes the first element of the queue.
   *
   * @return the element.
   * @throws java.util.NoSuchElementException if the queue is empty.
   */
  @SuppressWarnings("unchecked")
  public E removeFirst() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    final Object[] data = mData;
    final int first = mFirst;
    mFirst = (first + 1) & mMask;
    final Object output = data[first];
    data[first] = null;
    --mSize;
    return (E) output;
  }

  /**
   * Removes the last element of the queue.
   *
   * @return the element.
   * @throws java.util.NoSuchElementException if the queue is empty.
   */
  @SuppressWarnings("unchecked")
  public E removeLast() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    final Object[] data = mData;
    final int mask = mMask;
    final int newLast = (mLast + mask) & mask;
    mLast = newLast;
    final Object output = data[newLast];
    data[newLast] = null;
    --mSize;
    return (E) output;
  }

  /**
   * TODO
   *
   * @param dst
   * @param destPos
   * @param <T>
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T> int transferTo(@NotNull final T[] dst, final int destPos) {
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    final int length = dst.length - destPos;
    int i = mFirst;
    int n = destPos;
    while (i != last) {
      if (n == length) {
        mFirst = i;
        return -(mSize -= (n - destPos));
      }

      dst[n++] = (T) data[i];
      data[i] = null;
      i = (i + 1) & mask;
    }

    mFirst = 0;
    mLast = 0;
    mSize = 0;
    return (n - destPos);
  }

  /**
   * Removes all the elements from this queue and add them to the specified collection.
   *
   * @param collection the collection to fill.
   */
  @SuppressWarnings("unchecked")
  public void transferTo(@NotNull final Collection<? super E> collection) {
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    int i = mFirst;
    while (i != last) {
      collection.add((E) data[i]);
      data[i] = null;
      i = (i + 1) & mask;
    }

    mFirst = 0;
    mLast = 0;
    mSize = 0;
  }

  @NotNull
  @SuppressWarnings("SuspiciousSystemArraycopy")
  private <T> T[] copyElements(@NotNull final T[] dst) {
    final Object[] data = mData;
    final int first = mFirst;
    final int last = mLast;
    if (first <= last) {
      System.arraycopy(data, first, dst, 0, mSize);

    } else {
      final int length = data.length - first;
      System.arraycopy(data, first, dst, 0, length);
      System.arraycopy(data, 0, dst, length, last);
    }

    return dst;
  }

  private void doubleCapacity() {
    final Object[] data = mData;
    final int size = data.length;
    final int newSize = size << 1;
    if (newSize < size) {
      throw new OutOfMemoryError();
    }

    final int first = mFirst;
    final int remainder = size - first;
    final Object[] newData = new Object[newSize];
    System.arraycopy(data, first, newData, 0, remainder);
    System.arraycopy(data, 0, newData, remainder, first);
    mData = newData;
    mFirst = 0;
    mLast = size;
    mMask = newSize - 1;
  }

  /**
   * Queue iterator implementation.
   */
  public static class SimpleQueueIterator<E> implements Iterator<E> {

    private final SimpleQueue<E> mQueue;

    private boolean mIsRemoved;

    private int mOriginalFirst;

    private int mOriginalLast;

    private int mPointer;

    private long mReplaceCount;

    /**
     * Constructor.
     */
    private SimpleQueueIterator(@NotNull final SimpleQueue<E> queue) {
      mQueue = queue;
      mPointer = (mOriginalFirst = queue.mFirst);
      mOriginalLast = queue.mLast;
      mReplaceCount = queue.mReplaceCount;
    }

    public boolean hasNext() {
      return (mPointer != mOriginalLast);
    }

    @SuppressWarnings("unchecked")
    public E next() {
      final SimpleQueue<E> queue = mQueue;
      final int originalLast = mOriginalLast;
      if (queue.mLast != originalLast) {
        throw new ConcurrentModificationException();
      }

      final int pointer = mPointer;
      if (pointer == originalLast) {
        throw new NoSuchElementException();
      }

      mIsRemoved = false;
      mPointer = (pointer + 1) & queue.mMask;
      return (E) queue.mData[pointer];
    }

    public void remove() {
      if (mIsRemoved) {
        throw new IllegalStateException("element already removed");
      }

      final SimpleQueue<E> queue = mQueue;
      final int last = queue.mLast;
      final int originalLast = mOriginalLast;
      if (last != originalLast) {
        throw new ConcurrentModificationException();
      }

      final int pointer = mPointer;
      if (pointer == originalLast) {
        throw new NoSuchElementException();
      }

      final Object[] data = queue.mData;
      final int mask = queue.mMask;
      final int first = queue.mFirst;
      final int front = (pointer - first) & mask;
      final int back = (last - pointer) & mask;
      if (front <= back) {
        if (first <= pointer) {
          System.arraycopy(data, first, data, first + 1, front);
          shiftForward();

        } else if (back <= (mask - first + pointer)) {
          System.arraycopy(data, pointer + 1, data, pointer, back);
          shiftBackward();

        } else {
          System.arraycopy(data, 0, data, 1, pointer);
          data[0] = data[mask];
          System.arraycopy(data, first, data, first + 1, mask - first);
          shiftForward();
        }

      } else {
        if (pointer < last) {
          System.arraycopy(data, pointer + 1, data, pointer, back);
          shiftBackward();

        } else if (front <= (mask - pointer + last)) {
          System.arraycopy(data, first, data, first + 1, front);
          shiftForward();

        } else {
          System.arraycopy(data, pointer + 1, data, pointer, mask - pointer);
          data[mask] = data[0];
          System.arraycopy(data, 1, data, 0, last);
          shiftBackward();
        }
      }

      mIsRemoved = true;
    }

    /**
     * Replaces the last element returned by {@code next()} with the specified one.
     * <br>
     * This method can be called several times to replace the same element.
     *
     * @param element the replacement element.
     * @throws java.lang.IllegalStateException if the {@code next()} method has not yet been called.
     */
    public void replace(final E element) {
      final int pointer = mPointer;
      if (pointer == mOriginalFirst) {
        throw new IllegalStateException();
      }

      final SimpleQueue<E> queue = mQueue;
      if ((queue.mLast != mOriginalLast) || (++mReplaceCount != ++queue.mReplaceCount)) {
        throw new ConcurrentModificationException();
      }

      final int mask = queue.mMask;
      queue.mData[(pointer + mask) & mask] = element;
    }

    private void shiftBackward() {
      final SimpleQueue<E> queue = mQueue;
      final int mask = queue.mMask;
      final int last = queue.mLast;
      queue.mLast = mOriginalLast = (last + mask) & mask;
      mPointer = (mPointer + mask) & mask;
    }

    private void shiftForward() {
      final SimpleQueue<E> queue = mQueue;
      final int first = queue.mFirst;
      queue.mData[first] = null;
      queue.mFirst = mOriginalFirst = (first + 1) & queue.mMask;
    }
  }
}
