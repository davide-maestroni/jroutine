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

package com.github.dm.jrt.channel;

/**
 * Data class storing information about the specific flow.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the data type.
 */
public class FlowData<DATA> {

  /**
   * The data object.
   */
  public final DATA data;

  /**
   * The flow ID.
   */
  public final int id;

  /**
   * Constructor.
   *
   * @param id   the flow ID.
   * @param data the data object.
   */
  public FlowData(final int id, final DATA data) {
    this.data = data;
    this.id = id;
  }

  /**
   * Returns the data object casted to the specific type.
   *
   * @param <TYPE> the data type.
   * @return the data object.
   */
  @SuppressWarnings("unchecked")
  public <TYPE extends DATA> TYPE data() {
    return (TYPE) data;
  }

  @Override
  public int hashCode() {
    int result = (data != null) ? data.hashCode() : 0;
    result = 31 * result + id;
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof FlowData)) {
      return false;
    }

    final FlowData<?> that = (FlowData<?>) o;
    return (id == that.id) && ((data != null) ? data.equals(that.data) : (that.data == null));
  }

  @Override
  public String toString() {
    return "FlowData{data=" + data + ", id=" + id + "}";
  }
}
