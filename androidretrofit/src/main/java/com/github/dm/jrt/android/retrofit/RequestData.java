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

package com.github.dm.jrt.android.retrofit;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;

/**
 * Class used to make an OkHttp request data parcelable.
 * <p>
 * Note that the request body, if present, will not be part of the parcel.
 * <p>
 * Created by davide-maestroni on 05/17/2016.
 */
public class RequestData implements Parcelable {

    public static final Creator<RequestData> CREATOR = new Creator<RequestData>() {

        @Override
        public RequestData createFromParcel(final Parcel in) {

            return new RequestData(in);
        }

        @Override
        public RequestData[] newArray(final int size) {

            return new RequestData[size];
        }
    };

    private final Bundle mHeaders;

    private final String mMethod;

    private final String mUrl;

    /**
     * Constructor.
     *
     * @param request the request.
     */
    private RequestData(@NotNull final Request request) {

        mUrl = request.url().toString();
        mMethod = request.method();
        mHeaders = toBundle(request.headers());
    }

    /**
     * Constructor.
     *
     * @param in the input parcel.
     */
    private RequestData(@NotNull final Parcel in) {

        mUrl = in.readString();
        mMethod = in.readString();
        mHeaders = in.readBundle(RequestData.class.getClassLoader());
    }

    /**
     * Builds and return a parcelable object storing the data of the specified request.
     *
     * @param request the request instance.
     * @return the request data.
     */
    @NotNull
    public static RequestData of(@NotNull final Request request) {

        return new RequestData(request);
    }

    @NotNull
    private static Headers fromBundle(@NotNull final Bundle bundle) {

        final Headers.Builder builder = new Headers.Builder();
        for (final String key : bundle.keySet()) {
            final ArrayList<String> values = bundle.getStringArrayList(key);
            if (values != null) {
                for (final String value : values) {
                    builder.add(key, value);
                }
            }
        }

        return builder.build();
    }

    @NotNull
    private static Bundle toBundle(@NotNull final Headers headers) {

        final Bundle bundle = new Bundle();
        for (final String name : headers.names()) {
            bundle.putStringArrayList(name, new ArrayList<String>(headers.values(name)));
        }

        return bundle;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {

        dest.writeString(mUrl);
        dest.writeString(mMethod);
        dest.writeBundle(mHeaders);
    }

    /**
     * Returns a new request built from the internal data and the specified body.
     *
     * @param body the request body.
     * @return the request instance.
     */
    @NotNull
    public Request requestWithBody(@Nullable final RequestBody body) {

        return new Builder().url(mUrl).headers(fromBundle(mHeaders)).method(mMethod, body).build();
    }
}
