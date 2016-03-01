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

package com.github.dm.jrt.android.ext.channel;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.ext.channel.Selectable;

import org.jetbrains.annotations.NotNull;

/**
 * Data class storing information about the origin of the data.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the data type.
 */
public class ParcelableSelectable<DATA> extends Selectable<DATA> implements Parcelable {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ParcelableSelectable> CREATOR =
            new Creator<ParcelableSelectable>() {

                public ParcelableSelectable createFromParcel(final Parcel source) {

                    return new ParcelableSelectable(source);
                }

                public ParcelableSelectable[] newArray(final int size) {

                    return new ParcelableSelectable[size];
                }
            };

    /**
     * Constructor.
     *
     * @param data  the data object.
     * @param index the channel index.
     */
    public ParcelableSelectable(final DATA data, final int index) {

        super(data, index);
    }

    /**
     * Constructor.
     *
     * @param source the source parcel.
     */
    @SuppressWarnings("unchecked")
    protected ParcelableSelectable(@NotNull final Parcel source) {

        super((DATA) source.readValue(ParcelableSelectable.class.getClassLoader()),
              source.readInt());
    }

    public int describeContents() {

        return 0;
    }

    public void writeToParcel(final Parcel dest, final int flags) {

        dest.writeValue(data);
        dest.writeInt(index);
    }
}
