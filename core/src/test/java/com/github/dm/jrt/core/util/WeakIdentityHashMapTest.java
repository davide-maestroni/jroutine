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

import org.assertj.core.data.MapEntry;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Weak identity hash map unit tests.
 * <p>
 * Created by davide-maestroni on 11/18/2014.
 */
public class WeakIdentityHashMapTest {

  @Test
  public void testClear() {

    final WeakIdentityHashMap<Object, String> map =
        new WeakIdentityHashMap<Object, String>(4, 0.75f);

    final HashMap<Object, String> entries = new HashMap<Object, String>();

    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();

    entries.put(key0, "test0");
    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");

    map.putAll(entries);

    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));

    map.clear();

    assertThat(map).isEmpty();
  }

  @Test
  public void testEntryIteratorRemove() {

    final WeakIdentityHashMap<Object, String> map =
        new WeakIdentityHashMap<Object, String>(4, 0.75f);

    final Object key0 = new Object();

    map.put(key0, "test0");

    final Iterator<Entry<Object, String>> entryIterator = map.entrySet().iterator();
    final Entry<Object, String> nextEntry = entryIterator.next();

    assertThat(map.get(nextEntry.getKey())).isEqualTo(nextEntry.getValue());

    entryIterator.remove();

    try {

      entryIterator.remove();

      fail();

    } catch (final Exception ignored) {

    }

    assertThat(map).doesNotContainKey(nextEntry.getKey());
    assertThat(map).doesNotContainValue(nextEntry.getValue());
    assertThat(map).isEmpty();

    while (entryIterator.hasNext()) {

      entryIterator.next();
    }

    try {

      entryIterator.next();

      fail();

    } catch (final NoSuchElementException ignored) {

    }
  }

  @Test
  public void testEquals() {

    final HashMap<Object, String> entries = new HashMap<Object, String>();

    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();

    entries.put(key0, "test0");
    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");

    final WeakIdentityHashMap<Object, String> map =
        new WeakIdentityHashMap<Object, String>(entries);

    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));

    assertThat(map).isEqualTo(entries);
    assertThat(map.keySet()).isEqualTo(entries.keySet());
    assertThat(map.values()).containsOnly(entries.values().toArray(new String[entries.size()]));
    assertThat(map.entrySet()).isEqualTo(entries.entrySet());
  }

  @Test
  public void testIdentity() {

    final MyInteger key0 = new MyInteger(3);
    final MyInteger key1 = new MyInteger(3);

    assertThat(key0).isEqualTo(key1);

    final WeakIdentityHashMap<MyInteger, String> map = new WeakIdentityHashMap<MyInteger, String>();

    map.put(key0, "test0");
    map.put(key1, "test1");

    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"));
    map.remove(key0);
    assertThat(map).contains(MapEntry.entry(key1, "test1"));
  }

  @Test
  public void testKeyIteratorRemove() {

    final WeakIdentityHashMap<Object, String> map =
        new WeakIdentityHashMap<Object, String>(4, 0.75f);

    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key3 = new Object();

    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key3, "test3");

    final Iterator<Object> keyIterator = map.keySet().iterator();
    final Object nextKey = keyIterator.next();

    assertThat(map).containsKey(nextKey);

    keyIterator.remove();

    try {

      keyIterator.remove();

      fail();

    } catch (final Exception ignored) {

    }

    assertThat(map).doesNotContainKey(nextKey);
    assertThat(map).hasSize(2);

    while (keyIterator.hasNext()) {

      keyIterator.next();
    }

    try {

      keyIterator.next();

      fail();

    } catch (final NoSuchElementException ignored) {

    }
  }

  @Test
  public void testPut() {

    final WeakIdentityHashMap<Object, String> map = new WeakIdentityHashMap<Object, String>(13);

    assertThat(map).isEmpty();

    final Object key0 = new Object();
    map.put(key0, "test0");

    assertThat(map).hasSize(1);
    assertThat(map).contains(MapEntry.entry(key0, "test0"));
    assertThat(map).containsKey(key0);
    assertThat(map).containsValue("test0");
    assertThat(map).doesNotContainKey("test0");
    assertThat(map).doesNotContainValue("test1");
  }

  @Test
  public void testPutAll() {

    final WeakIdentityHashMap<Object, String> map = new WeakIdentityHashMap<Object, String>(13);

    assertThat(map).isEmpty();

    final Object key0 = new Object();
    map.put(key0, "test0");

    final HashMap<Object, String> entries = new HashMap<Object, String>();

    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();

    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");

    map.putAll(entries);

    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));
  }

  @Test
  public void testRemove() {

    final WeakIdentityHashMap<Object, String> map =
        new WeakIdentityHashMap<Object, String>(4, 0.75f);

    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();

    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key2, "test2");
    map.put(key3, "test3");

    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));

    assertThat(map.get(key2)).isEqualTo("test2");

    map.remove(key2);

    assertThat(map).hasSize(3);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key3, "test3"));
  }

  @Test
  public void testSetEntry() {

    final WeakIdentityHashMap<Object, String> map = new WeakIdentityHashMap<Object, String>(13);

    final Object key0 = new Object();
    map.put(key0, "test0");

    final HashMap<Object, String> entries = new HashMap<Object, String>();

    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();

    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");

    map.putAll(entries);

    final Entry<Object, String> entry = map.entrySet().iterator().next();

    entry.setValue("test");

    assertThat(entry.getValue()).isEqualTo("test");
    assertThat(map.get(entry.getKey())).isEqualTo("test");
  }

  @Test
  public void testValueIteratorRemove() {

    final WeakIdentityHashMap<Object, String> map =
        new WeakIdentityHashMap<Object, String>(4, 0.75f);

    final Object key0 = new Object();
    final Object key1 = new Object();

    map.put(key0, "test0");
    map.put(key1, "test1");

    final Iterator<String> valueIterator = map.values().iterator();
    final String nextValue = valueIterator.next();

    assertThat(map).containsValue(nextValue);

    valueIterator.remove();

    try {

      valueIterator.remove();

      fail();

    } catch (final Exception ignored) {

    }

    assertThat(map).doesNotContainValue(nextValue);
    assertThat(map).hasSize(1);

    while (valueIterator.hasNext()) {

      valueIterator.next();
    }

    try {

      valueIterator.next();

      fail();

    } catch (final NoSuchElementException ignored) {

    }
  }

  @Test
  @SuppressWarnings("UnusedAssignment")
  public void testWeakReference() throws InterruptedException {

    final WeakIdentityHashMap<Object, String> map = new WeakIdentityHashMap<Object, String>(4);

    final Object key0 = new Object();
    final Object key1 = new Object();
    Object key2 = new Object();
    final Object key3 = new Object();

    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key2, "test2");
    map.put(key3, "test3");

    key2 = null;

    // This is not guaranteed to work, so let's try a few times...
    for (int i = 0; i < 5; i++) {

      System.gc();
      Thread.sleep(100);

      if (!map.prune().containsValue("test2")) {

        return;
      }
    }

    fail();
  }

  private static class MyInteger {

    private final int mInt;

    private MyInteger(final int i) {

      mInt = i;
    }

    @Override
    public int hashCode() {

      return mInt;
    }

    @Override
    public boolean equals(final Object o) {

      if (this == o) {

        return true;
      }

      if (!(o instanceof MyInteger)) {

        return false;
      }

      final MyInteger myInteger = (MyInteger) o;
      return mInt == myInteger.mInt;
    }
  }
}
