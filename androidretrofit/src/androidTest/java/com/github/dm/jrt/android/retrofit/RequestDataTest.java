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

import android.os.Parcel;
import android.test.AndroidTestCase;

import java.io.IOException;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parcelable request data unit tests.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
public class RequestDataTest extends AndroidTestCase {

    public void testParcelable() {

        final Parcel parcel = Parcel.obtain();
        final Request request = new Request.Builder().url("http://www.google.com").get().build();
        parcel.writeParcelable(RequestData.of(request), 0);
        parcel.setDataPosition(0);
        final RequestData parcelable = parcel.readParcelable(RequestData.class.getClassLoader());
        assertThat(ComparableCall.of(new CallWrapper(parcelable.requestWithBody(null)))).isEqualTo(
                ComparableCall.of(new CallWrapper(request)));
        parcel.recycle();
    }

    private static class CallWrapper implements Call<Object> {

        private final Request mRequest;

        private CallWrapper(final Request request) {

            mRequest = request;
        }

        @SuppressWarnings("CloneDoesntCallSuperClone")
        public Call<Object> clone() {

            return new CallWrapper(mRequest);
        }

        public Response<Object> execute() throws IOException {

            throw new IOException();
        }

        public void enqueue(final Callback<Object> callback) {

        }

        public boolean isExecuted() {

            return false;
        }

        public void cancel() {

        }

        public boolean isCanceled() {

            return false;
        }

        public Request request() {

            return mRequest;
        }
    }
}
