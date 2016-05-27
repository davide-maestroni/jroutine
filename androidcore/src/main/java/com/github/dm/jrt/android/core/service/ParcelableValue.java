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

package com.github.dm.jrt.android.core.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Nullable;

/**
 * Parcelable implementation wrapping a generic value.
 * <p>
 * Note that specified object must be among the ones supported by the
 * {@link android.os.Parcel#writeValue(Object)} method.
 * <p>
 * Created by davide-maestroni on 01/10/2015.
 */
class ParcelableValue implements Parcelable {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ParcelableValue> CREATOR = new Creator<ParcelableValue>() {

        @Override
        public ParcelableValue createFromParcel(final Parcel source) {

            return new ParcelableValue(source.readValue(ParcelableValue.class.getClassLoader()));
        }

        @Override
        public ParcelableValue[] newArray(final int size) {

            return new ParcelableValue[size];
        }
    };

    private final Object mValue;

    /**
     * Constructor.
     *
     * @param value the wrapped value.
     */
    ParcelableValue(@Nullable final Object value) {

        mValue = value;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {

        dest.writeValue(mValue);
    }

    /**
     * Returns the wrapped value.
     *
     * @return the value.
     */
    public Object getValue() {

        return mValue;
    }
}
