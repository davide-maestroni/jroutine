/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.common;

import junit.framework.TestCase;

import org.assertj.core.data.MapEntry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cache hash map unit tests.
 * <p/>
 * Created by davide on 11/18/14.
 */
public class WeakIdentityHashMapTest extends TestCase {

    public void testAdd() {

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

        final Entry<Object, String> entry = map.entrySet().iterator().next();

        entry.setValue("test");

        assertThat(entry.getValue()).isEqualTo("test");
        assertThat(map.get(entry.getKey())).isEqualTo("test");
    }

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

    @SuppressWarnings({"UnnecessaryBoxing", "Annotator"})
    public void testIdentity() {

        final Integer key0 = new Integer(3);
        final Integer key1 = new Integer(3);

        assertThat(key0).isEqualTo(key1);

        final WeakIdentityHashMap<Integer, String> map = new WeakIdentityHashMap<Integer, String>();

        map.put(key0, "test0");
        map.put(key1, "test1");

        assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"));

        map.remove(key0);

        assertThat(map).contains(MapEntry.entry(key1, "test1"));
    }

    public void testRemove() {

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

        assertThat(map.get(key2)).isEqualTo("test2");

        map.remove(key2);

        assertThat(map).hasSize(3);
        assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
                                 MapEntry.entry(key3, "test3"));

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

        map.putAll(entries);

        assertThat(map).hasSize(4);
        assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
                                 MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));

        map.clear();

        assertThat(map).isEmpty();
    }

    @SuppressWarnings("UnusedAssignment")
    public void testWeakReference() {

        final HashMap<Object, String> entries = new HashMap<Object, String>();

        final Object key0 = new Object();
        final Object key1 = new Object();
        Object key2 = new Object();
        final Object key3 = new Object();

        entries.put(key0, "test0");
        entries.put(key1, "test1");
        entries.put(key2, "test2");
        entries.put(key3, "test3");

        final WeakIdentityHashMap<Object, String> map =
                new WeakIdentityHashMap<Object, String>(entries);

        key2 = null;

        // this is not guaranteed to work, so let's try a few times...
        for (int i = 0; i < 3; i++) {

            System.gc();

            if (map.containsValue("test2")) {

                return;
            }
        }

        fail();
    }
}
