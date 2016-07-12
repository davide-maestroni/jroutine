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

import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Routine exception wrapping an error response.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 */
public class ErrorResponseException extends RoutineException {

    private final Response<?> mResponse;

    /**
     * Constructor.
     *
     * @param response the unsuccessful response.
     */
    public ErrorResponseException(@NotNull final Response<?> response) {
        if (response.isSuccessful()) {
            throw new IllegalArgumentException("the response is successful");
        }

        mResponse = response;
    }

    /**
     * Returns the response HTTP status code.
     *
     * @return the status code.
     */
    public int code() {
        return mResponse.code();
    }

    /**
     * Returns the raw response body.
     *
     * @return the response body.
     */
    public ResponseBody errorBody() {
        return mResponse.errorBody();
    }

    /**
     * Returns the response HTTP headers.
     *
     * @return the HTTP headers.
     */
    public Headers headers() {
        return mResponse.headers();
    }

    /**
     * Returns the response HTTP status message (or null if unknown).
     *
     * @return the status message.
     */
    public String message() {
        return mResponse.message();
    }

    /**
     * Returns the raw response from the HTTP client.
     *
     * @return the raw response.
     */
    public okhttp3.Response raw() {
        return mResponse.raw();
    }
}
