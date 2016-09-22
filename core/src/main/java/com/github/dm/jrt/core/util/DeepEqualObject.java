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

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.github.dm.jrt.core.util.Reflection.cloneArgs;

/**
 * Base abstract class providing a default implementation of {@code equals()}, {@code hashCode()}
 * and {@code toString()} based on the list of objects passed as the constructor arguments.
 * <br>
 * Note that the inheriting class and the argument instances are expected to be immutable.
 * <p>
 * Created by davide-maestroni on 03/31/2016.
 */
public abstract class DeepEqualObject {

  private final Object[] mArgs;

  /**
   * Constructor.
   *
   * @param args the constructor arguments.
   */
  protected DeepEqualObject(@Nullable final Object[] args) {
    mArgs = cloneArgs(args);
  }

  @Override
  public int hashCode() {
    return 31 * getClass().hashCode() + Arrays.deepHashCode(mArgs);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final DeepEqualObject that = (DeepEqualObject) o;
    return Arrays.deepEquals(mArgs, that.mArgs);
  }

  @Override
  public String toString() {
    return super.toString() + "{" + Arrays.deepToString(mArgs) + "}";
  }
}
