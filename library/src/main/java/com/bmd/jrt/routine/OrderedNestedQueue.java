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
package com.bmd.jrt.routine;

import java.util.Collection;

/**
 * Implementation of a nested queue ensuring that data are returned in the same order as they are
 * added, even if added later through a nested queue.
 * <p/>
 * Created by davide on 9/30/14.
 *
 * @param <E> the element type.
 */
class OrderedNestedQueue<E> implements NestedQueue<E> {

    private final SimpleQueue<Object> mQueue;

    private boolean mClosed;

    /**
     * Default constructor.
     */
    OrderedNestedQueue() {

        this(new SimpleQueue<Object>());
    }

    /**
     * Constructor.
     *
     * @param queue the internal queue.
     */
    private OrderedNestedQueue(final SimpleQueue<Object> queue) {

        mQueue = queue;
    }

    private static void purge(final OrderedNestedQueue<?> queue) {

        final SimpleQueue<Object> simpleQueue = queue.mQueue;

        if (simpleQueue.isEmpty()) {

            return;
        }

        Object element = simpleQueue.peekFirst();

        while (element instanceof OrderedNestedQueue) {

            final OrderedNestedQueue<?> nested = ((OrderedNestedQueue<?>) element);

            if (!nested.mClosed) {

                return;
            }

            final SimpleQueue<Object> nestedSimpleQueue = nested.mQueue;

            if (!nestedSimpleQueue.isEmpty()) {

                purge(nested);

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

    @Override
    public NestedQueue<E> add(final E element) {

        checkOpen();

        mQueue.add(element);

        return this;
    }

    @Override
    public NestedQueue<E> addAll(final Iterable<? extends E> elements) {

        checkOpen();

        mQueue.addAll(elements);

        return this;
    }

    @Override
    public NestedQueue<E> addNested() {

        checkOpen();

        final OrderedNestedQueue<E> queue = new OrderedNestedQueue<E>();
        mQueue.add(queue);

        return queue;
    }

    @Override
    public NestedQueue<E> clear() {

        mQueue.clear();

        return this;
    }

    @Override
    public NestedQueue<E> close() {

        mClosed = true;

        return this;
    }

    @Override
    public boolean isEmpty() {

        purge(this);

        final SimpleQueue<Object> queue = mQueue;

        if (queue.isEmpty()) {

            return true;
        }

        final Object element = queue.peekFirst();

        return (element instanceof OrderedNestedQueue)
                && ((OrderedNestedQueue<?>) element).isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NestedQueue<E> moveTo(final Collection<? super E> collection) {

        purge(this);

        final SimpleQueue<Object> queue = mQueue;

        while (!queue.isEmpty()) {

            final Object element = queue.peekFirst();

            if (element instanceof OrderedNestedQueue) {

                final OrderedNestedQueue<E> nested = (OrderedNestedQueue<E>) element;

                nested.moveTo(collection);

                if (!nested.mClosed || !nested.mQueue.isEmpty()) {

                    return this;
                }

                queue.removeFirst();

            } else {

                collection.add((E) queue.removeFirst());
            }
        }

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E removeFirst() {

        purge(this);

        final Object element = mQueue.peekFirst();

        if (element instanceof OrderedNestedQueue) {

            return ((OrderedNestedQueue<E>) element).removeFirst();
        }

        return (E) mQueue.removeFirst();
    }

    private void checkOpen() {

        if (mClosed) {

            throw new IllegalStateException("the queue is closed");
        }
    }
}