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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import okhttp3.Headers;
import okhttp3.Headers.Builder;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Routine exception wrapping an error response.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 */
@SuppressWarnings("WeakerAccess")
public class ErrorResponseException extends RoutineException {

  private final int mCode;

  private final byte[] mErrorBody;

  private final Map<String, List<String>> mHeaders;

  private final String mMediaType;

  private final String mMessage;

  /**
   * Constructor.
   *
   * @param response the unsuccessful response.
   * @throws java.io.IOException if an I/O error occurred.
   */
  public ErrorResponseException(@NotNull final Response<?> response) throws IOException {
    if (response.isSuccessful()) {
      throw new IllegalArgumentException("the response is successful");
    }

    mCode = response.code();
    mMessage = response.message();
    mHeaders = response.headers().toMultimap();
    final ResponseBody errorBody = response.errorBody();
    final MediaType contentType = errorBody.contentType();
    mMediaType = (contentType != null) ? contentType.toString() : null;
    mErrorBody = errorBody.bytes();
  }

  /**
   * Returns the response HTTP status code.
   *
   * @return the status code.
   */
  public int code() {
    return mCode;
  }

  /**
   * Returns the raw response body.
   *
   * @return the response body.
   */
  public ResponseBody errorBody() {
    final String mediaType = mMediaType;
    return ResponseBody.create((mediaType != null) ? MediaType.parse(mediaType) : null, mErrorBody);
  }

  /**
   * Returns the response HTTP headers.
   *
   * @return the HTTP headers.
   */
  public Headers headers() {
    final Builder builder = new Builder();
    for (final Entry<String, List<String>> entry : mHeaders.entrySet()) {
      final String name = entry.getKey();
      for (final String value : entry.getValue()) {
        builder.add(name, value);
      }
    }

    return builder.build();
  }

  /**
   * Returns the response HTTP status message (or null if unknown).
   *
   * @return the status message.
   */
  public String message() {
    return mMessage;
  }
}
