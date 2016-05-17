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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Decorator implementation making a call instance comparable (that is, {@code equals(Object)} and
 * {@code hashCode()} methods are properly implemented.
 * <p>
 * Created by davide-maestroni on 03/25/2016.
 *
 * @param <T> the response type.
 */
public class ComparableCall<T> implements Call<T> {

    private final Call<T> mCall;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("unchecked")
    private ComparableCall(@NotNull final Call<?> wrapped) {

        mCall = (Call<T>) ConstantConditions.notNull("call instance", wrapped);
    }

    /**
     * Returns a call instance wrapping the specified one.
     *
     * @param wrapped the wrapped instance.
     * @param <T>     the response type.
     * @return the comparable call.
     */
    @NotNull
    public static <T> ComparableCall<T> of(@NotNull final Call<?> wrapped) {

        return new ComparableCall<>(wrapped);
    }

    private static boolean equals(@Nullable final Object o1, @Nullable final Object o2) {

        return (o1 == o2) || !((o1 == null) || (o2 == null)) && o1.equals(o2);
    }

    private static boolean equals(@Nullable final Headers h1, @Nullable final Headers h2) {

        if (h1 == h2) {
            return true;
        }

        if ((h1 == null) || (h2 == null)) {
            return false;
        }

        final int size = h1.size();
        if (size != h2.size()) {
            return false;
        }

        final Locale locale = Locale.getDefault();
        final HashMap<String, List<String>> map1 = new HashMap<>();
        final HashMap<String, List<String>> map2 = new HashMap<>();
        for (int i = 0; i < size; ++i) {
            final String name1 = h1.name(i);
            map1.put(name1.toLowerCase(locale), h1.values(name1));
            final String name2 = h2.name(i);
            map2.put(name2.toLowerCase(locale), h2.values(name2));
        }

        return map1.equals(map2);
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Call<T> clone() {

        return new ComparableCall<>(mCall.clone());
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final ComparableCall<?> that = (ComparableCall<?>) o;
        final Request thisRequest = request();
        final Request thatRequest = that.request();
        return equals(thisRequest.url(), thatRequest.url()) && equals(thisRequest.method(),
                thatRequest.method()) && equals(thisRequest.body(), thatRequest.body()) && equals(
                thisRequest.tag(), thisRequest.tag()) && equals(thisRequest.headers(),
                thatRequest.headers());
    }

    @Override
    public int hashCode() {

        final Request request = mCall.request();
        int result = 0;
        final HttpUrl url = request.url();
        result += result * 31 + ((url != null) ? url.hashCode() : 0);
        final String method = request.method();
        result += result * 31 + ((method != null) ? method.hashCode() : 0);
        final RequestBody body = request.body();
        result += result * 31 + ((body != null) ? body.hashCode() : 0);
        final Object tag = request.tag();
        if (tag != request) {
            result += result * 31 + ((tag != null) ? tag.hashCode() : 0);
        }

        final Headers headers = request.headers();
        if (headers.size() > 0) {
            final Locale locale = Locale.getDefault();
            final TreeSet<String> names = new TreeSet<>();
            for (final String name : headers.names()) {
                names.add((name != null) ? name.toLowerCase(locale) : null);
            }

            for (final String name : names) {
                result += result * 31 + ((name != null) ? name.hashCode() : 0);
                final List<String> values = headers.values(name);
                result += result * 31 + ((values != null) ? values.hashCode() : 0);
            }
        }

        return result;
    }

    @Override
    public Response<T> execute() throws IOException {

        return mCall.execute();
    }

    @Override
    public void enqueue(final Callback<T> callback) {

        mCall.enqueue(callback);
    }

    @Override
    public boolean isExecuted() {

        return mCall.isExecuted();
    }

    @Override
    public void cancel() {

        mCall.cancel();
    }

    @Override
    public boolean isCanceled() {

        return mCall.isCanceled();
    }

    @Override
    public Request request() {

        return mCall.request();
    }
}
