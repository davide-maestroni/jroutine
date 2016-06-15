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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Implementation of a nested queue ensuring that data are returned in the same order as they are
 * added, even if added later through a nested queue.
 * <p>
 * Created by davide-maestroni on 09/30/2014.
 *
 * @param <E> the element type.
 */
class NestedQueue<E> {

    private final static Object EMPTY_ELEMENT = new Object();

    private final SimpleQueue<Object> mQueue = new SimpleQueue<Object>();

    private boolean mClosed;

    /**
     * Constructor.
     */
    NestedQueue() {
    }

    /**
     * Returns the first element in the specified queue by first pruning any nested queues (a nested
     * queue can be safely pruned when closed and empty). In case the queue is empty, the method
     * will return the special {@link #EMPTY_ELEMENT} object (null is a valid element).
     *
     * @param queue the queue to prune.
     * @return the first element or {@link #EMPTY_ELEMENT}.
     */
    @Nullable
    private static Object prune(@NotNull final NestedQueue<?> queue) {
        final SimpleQueue<Object> simpleQueue = queue.mQueue;
        if (simpleQueue.isEmpty()) {
            return EMPTY_ELEMENT;
        }

        Object element = simpleQueue.peekFirst();
        while (element instanceof InnerNestedQueue) {
            final NestedQueue<?> nested = ((NestedQueue<?>) element);
            if (!nested.mClosed || (prune(nested) != EMPTY_ELEMENT)) {
                return nested;
            }

            simpleQueue.removeFirst();
            if (simpleQueue.isEmpty()) {
                return EMPTY_ELEMENT;
            }

            element = simpleQueue.peekFirst();
        }

        return element;
    }

    /**
     * Adds the specified element to the queue.
     * <p>
     * Note that the element can be null.
     *
     * @param element the element to add.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    public void add(@Nullable final E element) {
        checkOpen();
        mQueue.add(element);
    }

    /**
     * Adds all the elements returned by the specified iterable.
     * <p>
     * Note that the any of the returned element can be null.
     *
     * @param elements the element iterable.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    public void addAll(@NotNull final Iterable<? extends E> elements) {
        checkOpen();
        mQueue.addAll(elements);
    }

    /**
     * Adds a nested queue to this one.
     *
     * @return the newly added nested queue.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    @NotNull
    public NestedQueue<E> addNested() {
        checkOpen();
        final InnerNestedQueue<E> queue = new InnerNestedQueue<E>();
        mQueue.add(queue);
        return queue;
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        mQueue.clear();
    }

    /**
     * Closes this queue.
     * <br>
     * After the method returns no further additions can be made to this queue. Though, elements can
     * be safely removed.
     */
    public void close() {
        mClosed = true;
    }

    /**
     * Check if the queue does not contain any element.
     *
     * @return whether the queue is empty.
     */
    public boolean isEmpty() {
        final Object element = prune(this);
        return (element == EMPTY_ELEMENT) || ((element instanceof InnerNestedQueue)
                && ((InnerNestedQueue<?>) element).isEmpty());
    }

    /**
     * Removes the first element added into the queue.
     *
     * @return the element.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    @SuppressWarnings("unchecked")
    public E removeFirst() {
        final Object element = prune(this);
        if (element instanceof InnerNestedQueue) {
            return ((InnerNestedQueue<E>) element).removeFirst();
        }

        return (E) mQueue.removeFirst();
    }

    /**
     * Removes all the elements from this queue and add them to the specified collection.
     *
     * @param collection the collection to fill.
     */
    @SuppressWarnings("unchecked")
    public void transferTo(@NotNull final Collection<? super E> collection) {
        if (prune(this) == EMPTY_ELEMENT) {
            return;
        }

        final SimpleQueue<Object> queue = mQueue;
        while (!queue.isEmpty()) {
            final Object element = queue.peekFirst();
            if (element instanceof InnerNestedQueue) {
                final NestedQueue<E> nested = (NestedQueue<E>) element;
                nested.transferTo(collection);
                if (!nested.mClosed || !nested.mQueue.isEmpty()) {
                    return;
                }

                queue.removeFirst();

            } else {
                collection.add((E) queue.removeFirst());
            }
        }
    }

    private void checkOpen() {
        if (mClosed) {
            throw new IllegalStateException("the queue is closed");
        }
    }

    /**
     * Internal class used to discriminate between an element and a nested queue.
     *
     * @param <E>
     */
    private static class InnerNestedQueue<E> extends NestedQueue<E> {

    }
}
