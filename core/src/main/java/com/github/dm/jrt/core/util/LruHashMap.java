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

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * LRU map implementation.
 * <p>
 * Created by davide-maestroni on 06/16/2016.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 */
public class LruHashMap<K, V> extends LinkedHashMap<K, V> {

  private static final int DEFAULT_INITIAL_CAPACITY = 16;

  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  private static final long serialVersionUID = 3190208293198477083L;

  private final int mMaxCapacity;

  /**
   * Constructor.
   *
   * @param maxCapacity the maximum capacity.
   * @see HashMap#HashMap()
   */
  public LruHashMap(final int maxCapacity) {
    this(maxCapacity, DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor.
   *
   * @param maxCapacity     the maximum capacity.
   * @param initialCapacity the initial capacity.
   * @see HashMap#HashMap(int)
   */
  public LruHashMap(final int maxCapacity, final int initialCapacity) {
    this(maxCapacity, initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Constructor.
   *
   * @param maxCapacity     the maximum capacity.
   * @param initialCapacity the initial capacity.
   * @param loadFactor      the load factor.
   * @see HashMap#HashMap(int, float)
   */
  public LruHashMap(final int maxCapacity, final int initialCapacity, final float loadFactor) {
    super(initialCapacity, loadFactor, true);
    mMaxCapacity = ConstantConditions.positive("maximum capacity", maxCapacity);
  }

  /**
   * Constructor.
   *
   * @param maxCapacity the maximum capacity.
   * @param map         the initial content.
   * @see HashMap#HashMap(Map)
   */
  public LruHashMap(final int maxCapacity, @NotNull final Map<? extends K, ? extends V> map) {
    this(maxCapacity, map.size());
    putAll(map);
  }

  @Override
  protected boolean removeEldestEntry(final Entry<K, V> eldest) {
    return (size() > mMaxCapacity);
  }
}
