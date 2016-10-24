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

import org.junit.Test;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Error response exception unit test.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 */
public class ErrorResponseExceptionTest {

  @Test
  public void testException() throws IOException {
    final Response<Object> response =
        Response.error(400, ResponseBody.create(MediaType.parse("plain/text"), "Error"));
    final ErrorResponseException exception = new ErrorResponseException(response);
    assertThat(exception.code()).isEqualTo(response.code());
    assertThat(exception.message()).isEqualTo(response.message());
    assertThat(exception.headers().toMultimap()).isEqualTo(response.headers().toMultimap());
    assertThat(exception.errorBody().contentType()).isEqualTo(response.errorBody().contentType());
    assertThat(exception.errorBody().contentLength()).isEqualTo(
        response.errorBody().contentLength());
    assertThat(exception.errorBody().string()).isEqualTo("Error");
  }
}
