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
package com.gh.bmd.jrt.routine;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Minimal implementation of a light-weight queue storing elements into dynamically increasing
 * circular buffer.
 * <p/>
 * Created by davide on 9/27/14.
 *
 * @param <E> the element type.
 */
class SimpleQueue<E> {

    private int mFirst;

    private int mLast;

    private Object[] mQueue = new Object[8];

    private static <T> void resizeArray(@Nonnull final T[] src, @Nonnull final T[] dst,
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
     * Note that the any of the returned element can be null.
     *
     * @param elements the element iterable.
     */
    public void addAll(@Nonnull final Iterable<? extends E> elements) {

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
     * Check if the queue does not contain any element.
     *
     * @return whether the queue is empty.
     */
    public boolean isEmpty() {

        return mFirst == mLast;
    }

    /**
     * Moves all the elements to the specified collection.
     *
     * @param collection the collection to fill.
     */
    public void moveTo(@Nonnull final Collection<? super E> collection) {

        while (!isEmpty()) {

            collection.add(removeFirst());
        }
    }

    /**
     * Peeks the first element added into the queue.
     *
     * @return the element.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    @Nullable
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
    @Nullable
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
