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
 * Created by davide on 9/30/14.
 */
class SimpleNestedQueue<E> implements NestedQueue<E> {

    private final SimpleQueue<E> mQueue;

    private boolean mClosed;

    SimpleNestedQueue() {

        this(new SimpleQueue<E>());
    }

    private SimpleNestedQueue(final SimpleQueue<E> queue) {

        mQueue = queue;
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

        return new SimpleNestedQueue<E>(mQueue);
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

        return mQueue.isEmpty();
    }

    @Override
    public NestedQueue<E> moveTo(final Collection<? super E> collection) {

        mQueue.moveTo(collection);

        return this;
    }

    @Override
    public E removeFirst() {

        return mQueue.removeFirst();
    }

    private void checkOpen() {

        if (mClosed) {

            throw new IllegalStateException("the queue is closed");
        }
    }
}