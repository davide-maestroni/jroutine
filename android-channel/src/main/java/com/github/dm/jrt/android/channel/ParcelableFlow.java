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

package com.github.dm.jrt.android.channel;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.channel.Flow;

import org.jetbrains.annotations.NotNull;

/**
 * Data class storing information about the specific flow of data.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the data type.
 */
public class ParcelableFlow<DATA> extends Flow<DATA> implements Parcelable {

  /**
   * Creator instance needed by the parcelable protocol.
   */
  public static final Creator<ParcelableFlow> CREATOR = new Creator<ParcelableFlow>() {

    @Override
    public ParcelableFlow createFromParcel(final Parcel source) {
      return new ParcelableFlow(source);
    }

    @Override
    public ParcelableFlow[] newArray(final int size) {
      return new ParcelableFlow[size];
    }
  };

  /**
   * Constructor.
   *
   * @param id   the flow ID.
   * @param data the data object.
   */
  public ParcelableFlow(final int id, final DATA data) {
    super(id, data);
  }

  /**
   * Constructor.
   *
   * @param source the source parcel.
   */
  @SuppressWarnings("unchecked")
  protected ParcelableFlow(@NotNull final Parcel source) {
    super(source.readInt(), (DATA) source.readValue(ParcelableFlow.class.getClassLoader()));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeInt(id);
    dest.writeValue(data);
  }
}
