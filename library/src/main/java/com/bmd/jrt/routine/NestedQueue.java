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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a queue with the possibility to add nested queues with additional elements.
 * <p/>
 * This interface is used to abstract the handling of placeholders for asynchronously available
 * data in order to support forced input and output ordering.
 * <p/>
 * Created by davide on 9/30/14.
 *
 * @param <E> the element type.
 */
interface NestedQueue<E> {

    /**
     * Adds the specified element to the queue.
     * <p/>
     * Note that the element can be null.
     *
     * @param element the element to add.
     * @return this queue.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    @Nonnull
    public NestedQueue<E> add(@Nullable E element);

    /**
     * Adds all the elements returned by the specified iterable.
     * <p/>
     * Note that the any of the returned element can be null.
     *
     * @param elements the element iterable.
     * @return this queue.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    @Nonnull
    public NestedQueue<E> addAll(@Nonnull Iterable<? extends E> elements);

    /**
     * Adds a nested queue to this one.
     *
     * @return the newly added nested queue.
     * @throws java.lang.IllegalStateException if the queue has been already closed.
     */
    @Nonnull
    public NestedQueue<E> addNested();

    /**
     * Clears the queue.
     *
     * @return this queue.
     */
    @Nonnull
    public NestedQueue<E> clear();

    /**
     * Closes this queue.<br/>
     * After the method returns no more addition can be made to this queue. Though, elements can
     * be safely removed.
     *
     * @return this queue.
     */
    @Nonnull
    public NestedQueue<E> close();

    /**
     * Check if the queue does not contain any element.
     *
     * @return whether the queue is empty.
     */
    public boolean isEmpty();

    /**
     * Moves all the elements to the specified collection.
     *
     * @param collection the collection to fill.
     * @return this queue.
     */
    @Nonnull
    public NestedQueue<E> moveTo(@Nonnull final Collection<? super E> collection);

    /**
     * Removes the first element added into the queue.
     *
     * @return the element.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    @Nullable
    public E removeFirst();
}
