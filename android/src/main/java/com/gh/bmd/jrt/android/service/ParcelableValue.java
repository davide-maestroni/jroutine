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
package com.gh.bmd.jrt.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.Nonnull;

/**
 * Parcelable implementation wrapping a generic value.<br/>
 * Note that specified object must be among the ones supported by the
 * {@link android.os.Parcel#writeValue(Object)} method.
 * <p/>
 * Created by davide-maestroni on 1/10/15.
 */
class ParcelableValue implements Parcelable {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ParcelableValue> CREATOR = new Creator<ParcelableValue>() {

        public ParcelableValue createFromParcel(@Nonnull final Parcel source) {

            return new ParcelableValue(source.readValue(ParcelableValue.class.getClassLoader()));
        }

        @Nonnull
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
    ParcelableValue(final Object value) {

        mValue = value;
    }

    public int describeContents() {

        return 0;
    }

    public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

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
