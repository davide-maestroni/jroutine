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
interface NestedQueue<E> {

    public NestedQueue<E> add(E element);

    public NestedQueue<E> addAll(Iterable<? extends E> elements);

    public NestedQueue<E> addNested();

    public NestedQueue<E> clear();

    public NestedQueue<E> close();

    public boolean isEmpty();

    public NestedQueue<E> moveTo(final Collection<? super E> collection);

    public E removeFirst();
}