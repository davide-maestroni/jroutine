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
 * Minimal implementation of a light-weight queue storing elements into dynamically increasing
 * circular buffer.
 * <p>
 * Created by davide-maestroni on 09/27/2014.
 *
 * @param <E> the element type.
 */
public class SimpleQueue<E> implements Iterable<E> {

    private static final int INITIAL_SIZE = 1 << 3; // 8

    private int mFirst;

    private int mLast;

    private int mMask = INITIAL_SIZE - 1;

    private Object[] mQueue = new Object[INITIAL_SIZE];

    private int mSize;

    private static void resizeArray(@NotNull final Object[] src, @NotNull final Object[] dst,
            final int first) {
        final int remainder = src.length - first;
        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    /**
     * Adds the specified element to the queue.
     * <p>
     * Note that the element can be null.
     *
     * @param element the element to add.
     */
    public void add(@Nullable final E element) {
        final int last = mLast;
        final int newLast = (last + 1) & mMask;
        mQueue[last] = element;
        if (mFirst == newLast) {
            doubleCapacity();
        }

        mLast = newLast;
        ++mSize;
    }

    /**
     * Adds all the elements returned by the specified iterable.
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
        int newFirst = (mFirst + mask) & mask;
        if (newFirst == mLast) {
            doubleCapacity();
            mask = mMask;
            newFirst = (mFirst + mask) & mask;
        }

        mQueue[newFirst] = element;
        mFirst = newFirst;
        ++mSize;
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        mFirst = 0;
        mLast = 0;
        mSize = 0;
        Arrays.fill(mQueue, null);
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
     * Peeks the first element added into the queue.
     *
     * @return the element.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    @SuppressWarnings("unchecked")
    public E peekFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return (E) mQueue[mFirst];
    }

    /**
     * Removes the first element added into the queue.
     *
     * @return the element.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    @SuppressWarnings("unchecked")
    public E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        final Object[] queue = mQueue;
        final int first = mFirst;
        mFirst = (first + 1) & mMask;
        final Object output = queue[first];
        queue[first] = null;
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
        final Object[] queue = mQueue;
        final int mask = mMask;
        final int last = mLast;
        int i = mFirst;
        while (i != last) {
            collection.add((E) queue[i]);
            queue[i] = null;
            i = (i + 1) & mask;
        }

        mFirst = 0;
        mLast = 0;
        mSize = 0;
    }

    private void doubleCapacity() {
        final Object[] queue = mQueue;
        final int size = queue.length;
        final int newSize = size << 1;
        if (newSize < size) {
            throw new OutOfMemoryError();
        }

        final int first = mFirst;
        final Object[] newQueue = new Object[newSize];
        resizeArray(queue, newQueue, first);
        mQueue = newQueue;
        final int shift = newSize - size;
        mFirst = first + shift;
        if (mLast >= first) {
            mLast += shift;
        }

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

        /**
         * Constructor.
         */
        private SimpleQueueIterator(@NotNull final SimpleQueue<E> queue) {
            mQueue = queue;
            mPointer = (mOriginalFirst = queue.mFirst);
            mOriginalLast = queue.mLast;
        }

        public boolean hasNext() {
            return (mPointer != mOriginalLast);
        }

        @SuppressWarnings("unchecked")
        public E next() {
            final SimpleQueue<E> queue = mQueue;
            if (queue.mFirst != mOriginalFirst) {
                throw new ConcurrentModificationException();
            }

            final int pointer = mPointer;
            if (pointer == mOriginalLast) {
                throw new NoSuchElementException();
            }

            mPointer = (pointer + 1) & queue.mMask;
            return (E) queue.mQueue[pointer];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Replace the last element returned by {@code next()} with the specified one.
         *
         * @param element the replacement element.
         * @throws java.lang.IllegalStateException if the {@code next()} method has not yet been
         *                                         called.
         */
        public void replace(final E element) {
            final int pointer = mPointer;
            if (pointer == mOriginalFirst) {
                throw new IllegalStateException();
            }

            final SimpleQueue<E> queue = mQueue;
            final int mask = queue.mMask;
            queue.mQueue[(pointer + mask) & mask] = element;
        }
    }
}
