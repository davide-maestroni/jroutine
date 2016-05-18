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

import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Class used to make an OkHttp request parcelable.
 * <p>
 * Created by davide-maestroni on 05/17/2016.
 */
public class ParcelableRequest implements Parcelable {

    public static final Creator<ParcelableRequest> CREATOR = new Creator<ParcelableRequest>() {

        public ParcelableRequest createFromParcel(final Parcel in) {

            final Request.Builder builder = new Request.Builder();
            final String method = in.readString();
            builder.url(in.readString())
                   .headers(fromBundle(in.readBundle(ParcelableRequest.class.getClassLoader())));
            final String mediaType = in.readString();
            final byte[] bytes = in.createByteArray();
            if (bytes != null) {
                builder.method(method,
                        RequestBody.create((mediaType != null) ? MediaType.parse(mediaType) : null,
                                bytes));
            }

            return new ParcelableRequest(builder.build());
        }

        public ParcelableRequest[] newArray(final int size) {

            return new ParcelableRequest[size];
        }
    };

    private final Request mRequest;

    /**
     * Constructor.
     *
     * @param request the request.
     */
    private ParcelableRequest(@NotNull final Request request) {

        mRequest = request;
    }

    /**
     * Builds and return a wrapper making the specified request parcelable.
     *
     * @param request the request instance.
     * @return the parcelable request.
     */
    @NotNull
    public static ParcelableRequest of(@NotNull final Request request) {

        return new ParcelableRequest(ConstantConditions.notNull("request instance", request));
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

    public int describeContents() {

        return 0;
    }

    public void writeToParcel(final Parcel dest, final int flags) {

        final Request request = mRequest;
        dest.writeString(request.method());
        dest.writeString(request.url().toString());
        dest.writeBundle(toBundle(request.headers()));
        final RequestBody body = request.body();
        if (body != null) {
            final MediaType mediaType = body.contentType();
            dest.writeString((mediaType != null) ? mediaType.toString() : null);
            final Buffer buffer = new Buffer();
            try {
                body.writeTo(buffer);
                dest.writeByteArray(buffer.readByteArray());
            } catch (final IOException e) {
                throw InvocationException.wrapIfNeeded(e);
            }

        } else {
            dest.writeString(null);
            dest.writeByteArray(null);
        }
    }

    /**
     * Gets the wrapped request.
     *
     * @return the request instance.
     */
    @NotNull
    public Request rawRequest() {

        return mRequest;
    }
}
