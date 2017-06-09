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
 * @param <E> the element data type.
 */
class NestedQueue<E> {

  private boolean mClosed;

  private SimpleQueue<Object> mQueue = new SimpleQueue<Object>();

  private QueueManager<E> mQueueManager;

  /**
   * Constructor.
   */
  NestedQueue() {
    mQueueManager = new SimpleQueueManager();
  }

  /**
   * Check if the specified internal queue can be pruned.
   *
   * @param queue the queue.
   * @return whether the queue can be pruned.
   */
  private static boolean canPrune(@NotNull final NestedQueue<?> queue) {
    return queue.mClosed && queue.mQueue.isEmpty();
  }

  /**
   * Adds the specified element to the queue.
   * <p>
   * Note that the element can be null.
   *
   * @param element the element to add.
   * @throws java.lang.IllegalStateException if the queue has been already closed.
   */
  void add(@Nullable final E element) {
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
  void addAll(@NotNull final Iterable<? extends E> elements) {
    mQueue.addAll(elements);
  }

  /**
   * Adds a nested queue to this one.
   *
   * @return the newly added nested queue.
   * @throws java.lang.IllegalStateException if the queue has been already closed.
   */
  @NotNull
  NestedQueue<E> addNested() {
    final InnerNestedQueue<E> queue = new InnerNestedQueue<E>();
    mQueue.add(queue);
    mQueueManager = new NestedQueueManager();
    return queue;
  }

  /**
   * Clears the queue.
   */
  void clear() {
    mQueue.clear();
  }

  /**
   * Closes this queue.
   * <br>
   * After the method returns no further additions can be made to this queue. Though, elements can
   * be safely removed.
   */
  void close() {
    mClosed = true;
    mQueue = new ReadOnlyQueue<Object>(mQueue);
  }

  /**
   * Check if the queue does not contain any element.
   *
   * @return whether the queue is empty.
   */
  boolean isEmpty() {
    return mQueueManager.isEmpty();
  }

  /**
   * Removes the first element added into the queue.
   *
   * @return the element.
   * @throws java.util.NoSuchElementException if the queue is empty.
   */
  E removeFirst() {
    return mQueueManager.removeFirst();
  }

  /**
   * Removes all the elements from this queue and add them to the specified collection.
   *
   * @param collection the collection to fill.
   */
  void transferTo(@NotNull final Collection<? super E> collection) {
    mQueueManager.transferTo(collection);
  }

  /**
   * Interface describing a manager of the internal queue.
   *
   * @param <E> the element data type.
   */
  private interface QueueManager<E> {

    /**
     * Check if the queue does not contain any element.
     *
     * @return whether the queue is empty.
     */
    boolean isEmpty();

    /**
     * Removes the first element added into the queue.
     *
     * @return the element.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    E removeFirst();

    /**
     * Removes all the elements from this queue and add them to the specified collection.
     *
     * @param collection the collection to fill.
     */
    void transferTo(@NotNull Collection<? super E> collection);
  }

  /**
   * Internal class used to discriminate between an element and a nested queue.
   *
   * @param <E> the element data type.
   */
  private static class InnerNestedQueue<E> extends NestedQueue<E> {}

  /**
   * Read only queue implementation.
   *
   * @param <E> the element type.
   */
  private static class ReadOnlyQueue<E> extends SimpleQueue<E> {

    // TODO: 09/06/2017 fix

    private final SimpleQueue<E> mQueue;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped queue.
     */
    private ReadOnlyQueue(@NotNull final SimpleQueue<E> wrapped) {
      mQueue = wrapped;
    }

    @Override
    public void addAll(@NotNull final Iterable<? extends E> elements) {
      throw exception();
    }

    @Override
    public void addFirst(@Nullable final E element) {
      throw exception();
    }

    @Override
    public void addLast(@Nullable final E element) {
      throw exception();
    }

    @NotNull
    @Override
    public SimpleQueueIterator<E> iterator() {
      return mQueue.iterator();
    }

    @Override
    public int size() {
      return mQueue.size();
    }

    @Override
    public boolean isEmpty() {
      return mQueue.isEmpty();
    }

    @NotNull
    @Override
    public Object[] toArray() {
      return mQueue.toArray();
    }

    @NotNull
    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(@NotNull final T[] array) {
      return mQueue.toArray(array);
    }

    @Override
    public boolean add(@Nullable final E element) {
      throw exception();
    }

    @Override
    public void clear() {
      mQueue.clear();
    }

    @Override
    public boolean offer(final E e) {
      throw exception();
    }

    @Override
    public E remove() {
      return mQueue.remove();
    }

    @Override
    public E poll() {
      return mQueue.poll();
    }

    @Override
    public E element() {
      return mQueue.element();
    }

    @Override
    public E peek() {
      return mQueue.peek();
    }

    @Override
    public E peekFirst() {
      return mQueue.peekFirst();
    }

    @Override
    public E peekLast() {
      return mQueue.peekLast();
    }

    @Override
    public E removeFirst() {
      return mQueue.removeFirst();
    }

    @Override
    public E removeLast() {
      return mQueue.removeLast();
    }

    @Override
    public <T> int transferTo(@NotNull final T[] dst, final int destPos) {
      return mQueue.transferTo(dst, destPos);
    }

    @Override
    public void transferTo(@NotNull final Collection<? super E> collection) {
      mQueue.transferTo(collection);
    }

    @Override
    public boolean addAll(final Collection<? extends E> collection) {
      throw exception();
    }

    @NotNull
    private IllegalStateException exception() {
      return new IllegalStateException("the queue is closed");
    }
  }

  /**
   * Nested queue manager implementation.
   */
  private class NestedQueueManager implements QueueManager<E> {

    @SuppressWarnings("unchecked")
    public boolean isEmpty() {
      final SimpleQueue<Object> queue = mQueue;
      while (!queue.isEmpty()) {
        final Object element = queue.peekFirst();
        if (element instanceof InnerNestedQueue) {
          final NestedQueue<E> nested = (NestedQueue<E>) element;
          final boolean isEmpty = nested.isEmpty();
          if (canPrune(nested)) {
            queue.removeFirst();
            continue;
          }

          return isEmpty;
        }

        return false;
      }

      return true;
    }

    @SuppressWarnings("unchecked")
    public E removeFirst() {
      final SimpleQueue<Object> queue = mQueue;
      while (true) {
        final Object element = queue.peekFirst();
        if (element instanceof InnerNestedQueue) {
          final NestedQueue<E> nested = (NestedQueue<E>) element;
          if (canPrune(nested)) {
            queue.removeFirst();
            continue;
          }

          final E e = nested.removeFirst();
          if (canPrune(nested)) {
            queue.removeFirst();
          }

          return e;
        }

        return (E) queue.removeFirst();
      }
    }

    @SuppressWarnings("unchecked")
    public void transferTo(@NotNull final Collection<? super E> collection) {
      final SimpleQueue<Object> queue = mQueue;
      while (!queue.isEmpty()) {
        final Object element = queue.peekFirst();
        if (element instanceof InnerNestedQueue) {
          final NestedQueue<E> nested = (NestedQueue<E>) element;
          nested.transferTo(collection);
          if (canPrune(nested)) {
            queue.removeFirst();
            continue;
          }

          return;

        } else {
          collection.add((E) queue.removeFirst());
        }
      }
    }
  }

  /**
   * Simple queue manager implementation.
   */
  private class SimpleQueueManager implements QueueManager<E> {

    public boolean isEmpty() {
      return mQueue.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public E removeFirst() {
      return (E) mQueue.removeFirst();
    }

    @SuppressWarnings("unchecked")
    public void transferTo(@NotNull final Collection<? super E> collection) {
      ((SimpleQueue<E>) mQueue).transferTo(collection);
    }
  }
}
