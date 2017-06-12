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

package com.github.dm.jrt.core.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Simple queue unit tests.
 * <p>
 * Created by davide-maestroni on 10/01/2014.
 */
public class SimpleQueueTest {

  @Test
  public void testAdd() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 77; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    for (int i = 7; i < 13; i++) {
      queue.add(i);
    }

    for (int i = 3; i < 13; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void testAddFirst() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.addFirst(i);
    }

    for (int i = 76; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.addFirst(i);
    }

    for (int i = 6; i >= 4; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    for (int i = 4; i < 13; i++) {
      queue.addFirst(i);
    }

    for (int i = 12; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void testClear() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>(10);
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    queue.clear();
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void testCollection() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    assertThat(queue.poll()).isNull();
    assertThat(queue.peek()).isNull();

    try {
      queue.remove();
      fail();

    } catch (final NoSuchElementException ignored) {
    }

    try {
      queue.element();
      fail();

    } catch (final NoSuchElementException ignored) {
    }

    queue.offer(31);
    assertThat(queue.peekLast()).isEqualTo(31);
    queue.offer(13);
    assertThat(queue.peekLast()).isEqualTo(13);
    assertThat(queue.poll()).isEqualTo(31);
    assertThat(queue.peek()).isEqualTo(13);
    assertThat(queue.poll()).isEqualTo(13);

    try {
      queue.peekLast();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testIterator() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = -1; i > -7; i--) {
      queue.addFirst(i);
    }

    int count = -6;
    for (final Integer integer : queue) {
      assertThat(integer).isEqualTo(count++);
    }

    queue.clear();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    count = 0;
    final Iterator<Integer> iterator = queue.iterator();
    while (iterator.hasNext()) {
      final Integer integer = iterator.next();
      assertThat(integer).isEqualTo(count++);
      iterator.remove();
    }

    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void testIteratorError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    final Iterator<Integer> iterator = queue.iterator();
    try {
      iterator.remove();
      fail();

    } catch (final IllegalStateException ignored) {
    }

    assertThat(iterator.next()).isEqualTo(0);
    queue.add(8);
    try {
      iterator.next();
      fail();

    } catch (final ConcurrentModificationException ignored) {
    }

    try {
      iterator.remove();
      fail();

    } catch (final ConcurrentModificationException ignored) {
    }
  }

  @Test
  public void testIteratorError1() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    queue.add(0);

    final Iterator<Integer> iterator = queue.iterator();
    iterator.next();
    iterator.remove();
    try {
      iterator.remove();
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      iterator.next();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testIteratorError2() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    try {
      for (final Integer ignored : queue) {
        queue.add(8);
      }

      fail();

    } catch (final ConcurrentModificationException ignored) {
    }

    final Iterator<Integer> iterator1 = queue.iterator();
    final Iterator<Integer> iterator2 = queue.iterator();
    assertThat(iterator1.next()).isEqualTo(0);
    assertThat(iterator2.next()).isEqualTo(0);
    iterator1.remove();
    assertThat(iterator1.next()).isEqualTo(1);
    try {
      iterator2.next();
      fail();

    } catch (final ConcurrentModificationException ignored) {
    }
  }

  @Test
  public void testIteratorRemove1() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    Iterator<Integer> iterator = queue.iterator();
    assertThat(iterator.next()).isEqualTo(0);
    assertThat(iterator.next()).isEqualTo(1);
    iterator.remove();
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.next()).isEqualTo(4);
    assertThat(iterator.next()).isEqualTo(5);
    iterator.remove();
    assertThat(iterator.next()).isEqualTo(6);
    iterator.remove();
    assertThat(iterator.hasNext()).isFalse();
    iterator = queue.iterator();
    assertThat(iterator.next()).isEqualTo(0);
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.next()).isEqualTo(4);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testIteratorRemove2() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 2; i++) {
      queue.add(0);
      queue.removeFirst();
    }
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    Iterator<Integer> iterator = queue.iterator();
    assertThat(iterator.next()).isEqualTo(0);
    assertThat(iterator.next()).isEqualTo(1);
    iterator.remove();
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.next()).isEqualTo(4);
    assertThat(iterator.next()).isEqualTo(5);
    iterator.remove();
    assertThat(iterator.next()).isEqualTo(6);
    iterator.remove();
    assertThat(iterator.hasNext()).isFalse();
    iterator = queue.iterator();
    assertThat(iterator.next()).isEqualTo(0);
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.next()).isEqualTo(4);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testIteratorRemove3() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(0);
      queue.removeFirst();
    }
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    Iterator<Integer> iterator = queue.iterator();
    assertThat(iterator.next()).isEqualTo(0);
    assertThat(iterator.next()).isEqualTo(1);
    iterator.remove();
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.next()).isEqualTo(4);
    assertThat(iterator.next()).isEqualTo(5);
    iterator.remove();
    assertThat(iterator.next()).isEqualTo(6);
    iterator.remove();
    assertThat(iterator.hasNext()).isFalse();
    iterator = queue.iterator();
    assertThat(iterator.next()).isEqualTo(0);
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.next()).isEqualTo(4);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testPeekAllError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    try {
      queue.peekFirst();
      fail();
    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testPeekClearError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    queue.clear();
    try {
      queue.peekFirst();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testPeekEmptyError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    try {
      queue.peekFirst();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveAllError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    try {
      queue.removeFirst();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveAllError1() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 6; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(0);
      assertThat(queue.removeLast()).isEqualTo(i);
    }

    try {
      queue.removeLast();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveClearError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    queue.clear();
    try {
      queue.removeFirst();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveClearError1() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    queue.clear();
    try {
      queue.removeLast();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveEmptyError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    try {
      queue.removeFirst();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveEmptyError1() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    try {
      queue.removeLast();
      fail();

    } catch (final NoSuchElementException ignored) {
    }
  }

  @Test
  public void testRemoveLast() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    for (int i = 76; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(0);
      assertThat(queue.removeLast()).isEqualTo(i);
    }

    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void testSize() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    assertThat(queue.size()).isEqualTo(77);
    queue.remove(11);
    assertThat(queue.size()).isEqualTo(76);
    final Iterator<Integer> iterator = queue.iterator();
    iterator.next();
    iterator.remove();
    assertThat(queue.size()).isEqualTo(75);
    queue.removeFirst();
    queue.removeLast();
    assertThat(queue.size()).isEqualTo(73);
    queue.clear();
    assertThat(queue.size()).isZero();
  }

  @Test
  public void testToArray() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    assertThat(queue.toArray()).containsExactly(0, 1, 2, 3, 4, 5, 6);
    Integer[] array = new Integer[0];
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6);
    array = new Integer[3];
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6);
    array = new Integer[7];
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6);
    array = new Integer[9];
    array[7] = 11;
    array[8] = 77;
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6, null, 77);
  }

  @Test
  public void testToArray2() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 4; i++) {
      queue.add(0);
      queue.removeFirst();
    }
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    assertThat(queue.toArray()).containsExactly(0, 1, 2, 3, 4, 5, 6);
    Integer[] array = new Integer[0];
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6);
    array = new Integer[3];
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6);
    array = new Integer[7];
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6);
    array = new Integer[9];
    array[7] = 11;
    array[8] = 77;
    assertThat(queue.toArray(array)).containsExactly(0, 1, 2, 3, 4, 5, 6, null, 77);
  }

  @Test
  public void testTransferToArray() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    assertThat(queue.transferTo(new Integer[0], 0)).isEqualTo(-7);
    Integer[] array = new Integer[3];
    assertThat(queue.transferTo(array, 0)).isEqualTo(-4);
    assertThat(array).containsExactly(0, 1, 2);
    assertThat(queue.transferTo(array, 2)).isEqualTo(-3);
    assertThat(array).containsExactly(0, 1, 3);
    array = new Integer[4];
    array[3] = -31;
    assertThat(queue.transferTo(array, 0)).isEqualTo(3);
    assertThat(array).containsExactly(4, 5, 6, -31);
    assertThat(queue.transferTo(array, 0)).isZero();
  }

  @Test
  public void testTransferToCollection() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    final ArrayList<Integer> list = new ArrayList<Integer>();
    queue.transferTo(list);
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 3; i < 7; i++) {
      assertThat(list.get(i - 3)).isEqualTo(i);
    }
  }

  @Test
  public void testTransferToCollection1() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    final SimpleQueue<Integer> other = new SimpleQueue<Integer>();
    queue.transferTo(other);
    assertThat(queue.isEmpty()).isTrue();
    int i = 3;
    for (final Integer integer : other) {
      assertThat(integer).isEqualTo(i++);
    }
  }
}
