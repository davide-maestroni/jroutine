/*
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
package com.github.dm.jrt.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Minimal implementation of a light-weight queue storing elements into dynamically increasing
 * circular buffer.
 * <p/>
 * Created by davide-maestroni on 09/27/2014.
 *
 * @param <E> the element type.
 */
class SimpleQueue<E> {

    private int mFirst;

    private int mLast;

    private Object[] mQueue = new Object[8];

    private static void resizeArray(@NotNull final Object[] src, @NotNull final Object[] dst,
            final int first) {

        final int remainder = src.length - first;
        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    /**
     * Adds the specified element to the queue.
     * <p/>
     * Note that the element can be null.
     *
     * @param element the element to add.
     */
    public void add(@Nullable final E element) {

        final int i = mLast;
        final int newLast = (i + 1) & (mQueue.length - 1);
        mQueue[i] = element;
        if (mFirst == newLast) {
            doubleCapacity();
        }

        mLast = newLast;
    }

    /**
     * Adds all the elements returned by the specified iterable.
     * <p/>
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
     * Clears the queue.
     */
    public void clear() {

        mFirst = 0;
        mLast = 0;
        Arrays.fill(mQueue, null);
    }

    /**
     * Removes all the elements from this queue and add them to the specified collection.
     *
     * @param collection the collection to fill.
     */
    public void drainTo(@NotNull final Collection<? super E> collection) {

        while (!isEmpty()) {
            collection.add(removeFirst());
        }
    }

    /**
     * Check if the queue does not contain any element.
     *
     * @return whether the queue is empty.
     */
    public boolean isEmpty() {

        return mFirst == mLast;
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
        final int i = mFirst;
        mFirst = (i + 1) & (queue.length - 1);
        final Object output = queue[i];
        queue[i] = null;
        return (E) output;
    }

    /**
     * Returns the size of the queue (that is, the number of element).
     *
     * @return the size.
     */
    public int size() {

        final int first = mFirst;
        final int last = mLast;
        return (last >= first) ? last - first : mQueue.length + last - first;
    }

    private void doubleCapacity() {

        final int size = mQueue.length;
        final int newSize = size << 1;
        if (newSize < size) {
            throw new OutOfMemoryError();
        }

        final int first = mFirst;
        final int last = mLast;
        final Object[] newQueue = new Object[newSize];
        resizeArray(mQueue, newQueue, first);
        mQueue = newQueue;
        final int shift = newSize - size;
        mFirst = first + shift;
        mLast = (last < first) ? last : last + shift;
    }
}
