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

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Minimal implementation of a light-weight queue, storing elements into a dynamically increasing
 * circular buffer.
 * <p>
 * Created by davide-maestroni on 09/27/2014.
 *
 * @param <E> the element type.
 */
public class SimpleQueue<E> implements Iterable<E> {

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
   * Adds the specified element to end of the queue.
   * <p>
   * Note that the element can be null.
   *
   * @param element the element to add.
   */
  public void add(@Nullable final E element) {
    final int last = mLast;
    mData[last] = element;
    if (mFirst == (mLast = (last + 1) & mMask)) {
      doubleCapacity();
    }

    ++mSize;
  }

  /**
   * Adds all the elements returned by the specified iterable to end of the queue.
   * <p>
   * Note that any of the returned element can be null.
   *
   * @param elements the element iterable.
   */
  public void addAll(@NotNull final Iterable<? extends E> elements) {
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
   * Clears the queue.
   */
  public void clear() {
    mFirst = 0;
    mLast = 0;
    mSize = 0;
    Arrays.fill(mData, null);
  }

  /**
   * Check if the queue does not contain any element.
   *
   * @return whether the queue is empty.
   */
  public boolean isEmpty() {
    return mSize == 0;
  }

  public SimpleQueueIterator<E> iterator() {
    return new SimpleQueueIterator<E>(this);
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
   * Returns the size of the queue (that is, the number of elements).
   *
   * @return the size.
   */
  public int size() {
    return mSize;
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

  /**
   * Removes all the elements from this queue and add them to the specified one.
   *
   * @param other the queue to fill.
   */
  @SuppressWarnings("unchecked")
  public void transferTo(@NotNull final SimpleQueue<? super E> other) {
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    int i = mFirst;
    while (i != last) {
      other.add((E) data[i]);
      data[i] = null;
      i = (i + 1) & mask;
    }

    mFirst = 0;
    mLast = 0;
    mSize = 0;
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

    private final int mOriginalFirst;

    private final int mOriginalLast;

    private final SimpleQueue<E> mQueue;

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

      mPointer = (pointer + 1) & queue.mMask;
      return (E) queue.mData[pointer];
    }

    public void remove() {
      throw new UnsupportedOperationException();
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
  }
}
