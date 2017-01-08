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

import com.github.dm.jrt.android.channel.AndroidChannels;
import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.android.core.invocation.AbstractContextInvocation;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.channel.builder.ChunkStreamConfiguration.CloseActionType;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkOutputStream;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.retrofit.ErrorResponseException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;

/**
 * Implementation of a Context invocation handling OkHttp requests.
 * <p>
 * Created by davide-maestroni on 05/17/2016.
 */
@SuppressWarnings("WeakerAccess")
public class ServiceCallInvocation
    extends AbstractContextInvocation<ParcelableFlow<Object>, ParcelableFlow<Object>> {

  /**
   * The ID of the flow dedicated to the transfer of request and response body bytes.
   */
  static final int BYTES_ID = 1;

  /**
   * The ID of the flow dedicated to the transfer of request and response body media type.
   */
  static final int MEDIA_TYPE_ID = 0;

  /**
   * The ID of the flow dedicated to the transfer of the request data.
   */
  static final int REQUEST_DATA_ID = -1;

  private boolean mHasMediaType;

  private boolean mHasRequest;

  private Channel<ParcelableByteChunk, ParcelableByteChunk> mInputChannel;

  private MediaType mMediaType;

  private RequestData mRequestData;

  @Override
  public void onComplete(@NotNull final Channel<ParcelableFlow<Object>, ?> result) throws
      Exception {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> inputChannel = mInputChannel;
    if (inputChannel != null) {
      inputChannel.close();
      if (!mHasRequest) {
        asyncRequest(result);
      }

    } else {
      syncRequest(result);
    }
  }

  @Override
  public void onInput(final ParcelableFlow<Object> input,
      @NotNull final Channel<ParcelableFlow<Object>, ?> result) throws Exception {
    switch (input.id) {
      case REQUEST_DATA_ID:
        mRequestData = input.data();
        break;

      case MEDIA_TYPE_ID:
        final String mediaType = input.data();
        mMediaType = (mediaType != null) ? MediaType.parse(mediaType) : null;
        mHasMediaType = true;
        break;

      case BYTES_ID:
        if (mInputChannel == null) {
          mInputChannel = JRoutineCore.<ParcelableByteChunk>ofInputs().buildChannel();
        }

        final ParcelableByteChunk chunk = input.data();
        mInputChannel.pass(chunk);
        break;

      default:
        throw new IllegalArgumentException("unknown flow ID: " + input.id);
    }

    if (!mHasRequest && mHasMediaType && (mRequestData != null) && (mInputChannel != null)) {
      mHasRequest = true;
      asyncRequest(result);
    }
  }

  @Override
  public void onRecycle(final boolean isReused) {
    mRequestData = null;
    mMediaType = null;
    mInputChannel = null;
    mHasMediaType = false;
    mHasRequest = false;
  }

  private void asyncRequest(@NotNull final Channel<ParcelableFlow<Object>, ?> result) throws
      Exception {
    final Request request =
        mRequestData.requestWithBody(new AsyncRequestBody(mMediaType, mInputChannel));
    final Channel<ParcelableFlow<Object>, ParcelableFlow<Object>> outputChannel =
        JRoutineCore.<ParcelableFlow<Object>>ofInputs().buildChannel();
    outputChannel.bind(result);
    getClient().newCall(request).enqueue(new Callback() {

      @Override
      public void onFailure(final Call call, final IOException e) {
        outputChannel.abort(e);
      }

      @Override
      public void onResponse(final Call call, final Response response) throws IOException {
        try {
          publishResult(response.body(), outputChannel);

        } catch (final Throwable t) {
          outputChannel.abort(t);
          InvocationInterruptedException.throwIfInterrupt(t);

        } finally {
          outputChannel.close();
        }
      }
    });
  }

  @NotNull
  private OkHttpClient getClient() throws Exception {
    return ConstantConditions.notNull("http client instance", (OkHttpClient) ContextInvocationTarget
        .instanceOf(OkHttpClient.class)
        .getInvocationTarget(getContext())
        .getTarget());
  }

  private void publishResult(@NotNull final ResponseBody responseBody,
      @NotNull final Channel<ParcelableFlow<Object>, ?> result) throws IOException {
    final MediaType mediaType = responseBody.contentType();
    result.pass(new ParcelableFlow<Object>(ConverterChannelConsumer.MEDIA_TYPE_ID,
        (mediaType != null) ? mediaType.toString() : null));
    result.pass(new ParcelableFlow<Object>(ConverterChannelConsumer.CONTENT_LENGTH_ID,
        responseBody.contentLength()));
    final Channel<Object, ?> channel =
        AndroidChannels.parcelableFlowInput(result, ConverterChannelConsumer.BYTES_ID)
                       .buildChannel();
    final ChunkOutputStream outputStream = ParcelableByteChannel.withOutput(channel)
                                                                .applyChunkStreamConfiguration()
                                                                .withOnClose(
                                                                    CloseActionType.CLOSE_CHANNEL)
                                                                .configured()
                                                                .buildOutputStream();
    try {
      outputStream.transferFrom(responseBody.byteStream());

    } finally {
      outputStream.close();
    }
  }

  private void syncRequest(@NotNull final Channel<ParcelableFlow<Object>, ?> result) throws
      Exception {
    final Request request = mRequestData.requestWithBody(null);
    final Response response = getClient().newCall(request).execute();
    final ResponseBody responseBody = response.body();
    if (response.isSuccessful()) {
      publishResult(responseBody, result);

    } else {
      result.abort(new ErrorResponseException(retrofit2.Response.error(responseBody, response)));
    }
  }

  /**
   * Asynchronous request body.
   */
  private static class AsyncRequestBody extends RequestBody {

    private final Channel<ParcelableByteChunk, ParcelableByteChunk> mInputChannel;

    private final MediaType mMediaType;

    /**
     * Constructor.
     *
     * @param mediaType    the media type.
     * @param inputChannel the input channel.
     */
    private AsyncRequestBody(@Nullable final MediaType mediaType,
        @NotNull final Channel<ParcelableByteChunk, ParcelableByteChunk> inputChannel) {
      mMediaType = mediaType;
      mInputChannel = inputChannel;
    }

    @Override
    public MediaType contentType() {
      return mMediaType;
    }

    @Override
    @SuppressWarnings("ThrowFromFinallyBlock")
    public void writeTo(final BufferedSink sink) throws IOException {
      final OutputStream outputStream = sink.outputStream();
      try {
        for (final ParcelableByteChunk chunk : mInputChannel.in(indefiniteTime())) {
          ParcelableByteChannel.getInputStream(chunk).transferTo(outputStream);
        }

      } finally {
        outputStream.close();
      }
    }
  }
}
