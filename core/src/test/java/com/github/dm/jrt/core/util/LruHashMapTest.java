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

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * LRU hash map unit tests.
 * <p>
 * Created by davide-maestroni on 08/07/2016.
 */
public class LruHashMapTest {

  @Test
  public void testCapacity() {
    final LruHashMap<String, String> map = new LruHashMap<String, String>(1);
    map.put("test1", "test1");
    map.put("test2", "test2");
    assertThat(map).containsOnlyKeys("test2");
    map.put("test3", "test3");
    assertThat(map).containsOnlyKeys("test3");
  }

  @Test
  public void testCopy() {
    final LruHashMap<String, String> map1 = new LruHashMap<String, String>(3);
    map1.put("test1", "test1");
    map1.put("test2", "test2");
    map1.put("test3", "test3");
    final LruHashMap<String, String> map2 = new LruHashMap<String, String>(1, map1);
    assertThat(map2).containsOnlyKeys("test3");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testErrors() {
    try {
      new LruHashMap<String, String>(0);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      new LruHashMap<String, String>(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      new LruHashMap<String, String>(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testLRU() {
    final LruHashMap<String, String> map = new LruHashMap<String, String>(3);
    map.put("test1", "test1");
    map.put("test2", "test2");
    map.put("test3", "test3");
    assertThat(map).containsOnlyKeys("test1", "test2", "test3");
    map.get("test1");
    map.get("test3");
    map.get("test2");
    final Iterator<String> iterator = map.keySet().iterator();
    assertThat(iterator.next()).isEqualTo("test1");
    assertThat(iterator.next()).isEqualTo("test3");
    assertThat(iterator.next()).isEqualTo("test2");
    assertThat(iterator.hasNext()).isFalse();
  }
}
