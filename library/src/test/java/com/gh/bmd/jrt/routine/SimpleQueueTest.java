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
package com.gh.bmd.jrt.routine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Simple queue unit tests.
 * <p/>
 * Created by davide on 10/1/14.
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
    public void testClear() {

        final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();

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
    public void testMove() {

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

        queue.moveTo(list);

        assertThat(queue.isEmpty()).isTrue();

        for (int i = 3; i < 7; i++) {

            assertThat(list.get(i - 3)).isEqualTo(i);
        }
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
    public void testRemoveEmptyError() {

        final SimpleQueue<Integer> queue = new SimpleQueue<Integer>();

        try {

            queue.removeFirst();

            fail();

        } catch (final NoSuchElementException ignored) {

        }
    }
}
