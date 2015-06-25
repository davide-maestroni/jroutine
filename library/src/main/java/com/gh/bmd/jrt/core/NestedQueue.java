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
package com.gh.bmd.jrt.core;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of a nested queue ensuring that data are returned in the same order as they are
 * added, even if added later through a nested queue.
 * <p/>
 * Created by davide-maestroni on 9/30/14.
 *
 * @param <E> the element type.
 */
class NestedQueue<E> {

    private final SimpleQueue<Object> mQueue = new SimpleQueue<Object>();

    private boolean mClosed;

    /**
     * Constructor.
     */
    NestedQueue() {

    }

    private static void prune(@Nonnull final NestedQueue<?> queue) {

        final SimpleQueue<Object> simpleQueue = queue.mQueue;

        if (simpleQueue.isEmpty()) {

            return;
        }

        Object element = simpleQueue.peekFirst();

        while (element instanceof NestedQueue) {

            final NestedQueue<?> nested = ((NestedQueue<?>) element);

            if (!nested.mClosed) {

                return;
            }

            final SimpleQueue<Object> nestedSimpleQueue = nested.mQueue;

            if (!nestedSimpleQueue.isEmpty()) {

                prune(nested);

                if (!nestedSimpleQueue.isEmpty()) {

                    return;
                }
            }

            simpleQueue.removeFirst();

            if (simpleQueue.isEmpty()) {

                return;
            }

            element = simpleQueue.peekFirst();
        }
    }

    /**
     * Adds the specified element to the queue.
     * <p/>
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
     * <p/>
     * Note that the any of the returned element can be null.
     *
     * @param elements the element iterable.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    public void addAll(@Nonnull final Iterable<? extends E> elements) {

        checkOpen();
        mQueue.addAll(elements);
    }

    /**
     * Adds a nested queue to this one.
     *
     * @return the newly added nested queue.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    @Nonnull
    public NestedQueue<E> addNested() {

        checkOpen();
        final NestedQueue<E> queue = new NestedQueue<E>();
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
     * Closes this queue.<br/>
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

        prune(this);
        final SimpleQueue<Object> queue = mQueue;

        if (queue.isEmpty()) {

            return true;
        }

        final Object element = queue.peekFirst();
        return (element instanceof NestedQueue) && ((NestedQueue<?>) element).isEmpty();
    }

    /**
     * Moves all the elements to the specified collection.
     *
     * @param collection the collection to fill.
     */
    @SuppressWarnings("unchecked")
    public void moveTo(@Nonnull final Collection<? super E> collection) {

        prune(this);
        final SimpleQueue<Object> queue = mQueue;

        while (!queue.isEmpty()) {

            final Object element = queue.peekFirst();

            if (element instanceof NestedQueue) {

                final NestedQueue<E> nested = (NestedQueue<E>) element;
                nested.moveTo(collection);

                if (!nested.mClosed || !nested.mQueue.isEmpty()) {

                    return;
                }

                queue.removeFirst();

            } else {

                collection.add((E) queue.removeFirst());
            }
        }
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

        prune(this);
        final Object element = mQueue.peekFirst();

        if (element instanceof NestedQueue) {

            return ((NestedQueue<E>) element).removeFirst();
        }

        return (E) mQueue.removeFirst();
    }

    private void checkOpen() {

        if (mClosed) {

            throw new IllegalStateException("the queue is closed");
        }
    }
}
