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
 * Basic implementation of a nested queue.
 * <p/>
 * No data ordering is guaranteed.
 * <p/>
 * Created by davide on 9/30/14.
 *
 * @param <E> the element type.
 */
class SimpleNestedQueue<E> implements NestedQueue<E> {

    private final SimpleQueue<E> mQueue;

    private boolean mClosed;

    /**
     * Constructor.
     */
    SimpleNestedQueue() {

        this(new SimpleQueue<E>());
    }

    /**
     * Constructor.
     *
     * @param queue the internal data queue.
     */
    private SimpleNestedQueue(@Nonnull final SimpleQueue<E> queue) {

        mQueue = queue;
    }

    public void add(@Nullable final E element) {

        checkOpen();
        mQueue.add(element);
    }

    public void addAll(@Nonnull final Iterable<? extends E> elements) {

        checkOpen();
        mQueue.addAll(elements);
    }

    @Nonnull
    public NestedQueue<E> addNested() {

        checkOpen();
        return new SimpleNestedQueue<E>(mQueue);
    }

    public void clear() {

        mQueue.clear();
    }

    public void close() {

        mClosed = true;
    }

    public boolean isEmpty() {

        return mQueue.isEmpty();
    }

    public void moveTo(@Nonnull final Collection<? super E> collection) {

        mQueue.moveTo(collection);
    }

    @Nullable
    public E removeFirst() {

        return mQueue.removeFirst();
    }

    private void checkOpen() {

        if (mClosed) {

            throw new IllegalStateException("the queue is closed");
        }
    }
}