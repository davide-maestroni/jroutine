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

import com.github.dm.jrt.core.util.SimpleQueue.SimpleQueueIterator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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
  public void testDrain() {
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
  public void testDrain2() {
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
    final SimpleQueueIterator<Integer> iterator = queue.iterator();
    while (iterator.hasNext()) {
      final Integer integer = iterator.next();
      assertThat(integer).isEqualTo(count++);
      iterator.replace(-13);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.removeFirst()).isEqualTo(-13);
    }

    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void testIteratorError() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    final SimpleQueueIterator<Integer> iterator = queue.iterator();
    // TODO: 09/06/2017 fix it
//    try {
//      iterator.remove();
//      fail();
//
//    } catch (final UnsupportedOperationException ignored) {
//    }

    try {
      iterator.replace(-11);
      fail();

    } catch (final IllegalStateException ignored) {
    }

    assertThat(iterator.next()).isEqualTo(0);
    iterator.replace(-11);
    iterator.replace(31);
    queue.add(8);
    try {
      iterator.next();
      fail();

    } catch (final ConcurrentModificationException ignored) {
    }

    try {
      iterator.replace(-11);
      fail();

    } catch (final ConcurrentModificationException ignored) {
    }
  }

  @Test
  public void testIteratorError2() {
    final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    final SimpleQueueIterator<Integer> iterator1 = queue.iterator();
    final SimpleQueueIterator<Integer> iterator2 = queue.iterator();
    assertThat(iterator1.next()).isEqualTo(0);
    assertThat(iterator2.next()).isEqualTo(0);
    iterator1.replace(-11);
    assertThat(iterator1.next()).isEqualTo(1);
    assertThat(iterator2.next()).isEqualTo(1);
    iterator1.replace(31);
    try {
      iterator2.replace(77);
      fail();

    } catch (final ConcurrentModificationException ignored) {
    }

    try {
      for (final Integer ignored : queue) {
        queue.add(8);
      }

      fail();

    } catch (final ConcurrentModificationException ignored) {
    }

    final SimpleQueueIterator<Integer> iterator3 = queue.iterator();
    assertThat(iterator3.next()).isEqualTo(-11);
    assertThat(iterator3.next()).isEqualTo(31);
    assertThat(queue.removeFirst()).isEqualTo(-11);
    assertThat(queue.removeFirst()).isEqualTo(31);
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
}
