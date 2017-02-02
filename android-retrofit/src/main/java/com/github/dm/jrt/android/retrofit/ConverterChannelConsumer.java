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

import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkInputStream;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Source;
import okio.Timeout;
import retrofit2.Converter;

import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;

/**
 * Channel consumer implementation converting the request response body into and instance of the
 * output type.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
class ConverterChannelConsumer implements ChannelConsumer<ParcelableFlow<Object>> {

  /**
   * The ID of the flow dedicated to the transfer of request and response body bytes.
   */
  static final int BYTES_ID = 2;

  /**
   * The ID of the flow dedicated to the transfer of the content length.
   */
  static final int CONTENT_LENGTH_ID = 1;

  /**
   * The ID of the flow dedicated to the transfer of request and response body media type.
   */
  static final int MEDIA_TYPE_ID = 0;

  private static final ExecutorService sService = Executors.newCachedThreadPool();

  private final Converter<ResponseBody, ?> mConverter;

  private final Channel<Object, ?> mOutputChannel;

  private Channel<ParcelableByteChunk, ParcelableByteChunk> mChunkChannel;

  private long mContentLength;

  private boolean mHasContentLength;

  private boolean mHasMediaType;

  private boolean mHasResponse;

  private MediaType mMediaType;

  /**
   * Constructor.
   *
   * @param converter the body converter.
   * @param channel   the output channel.
   */
  ConverterChannelConsumer(@NotNull final Converter<ResponseBody, ?> converter,
      @NotNull final Channel<Object, ?> channel) {
    mConverter = ConstantConditions.notNull("converter instance", converter);
    mOutputChannel = ConstantConditions.notNull("channel instance", channel);
  }

  @Override
  public void onComplete() {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel = mChunkChannel;
    if (channel != null) {
      channel.close();

    } else if (mHasMediaType && mHasContentLength) {
      mHasResponse = true;
      final ResponseBody responseBody =
          ResponseBody.create(mMediaType, mContentLength, new Buffer());
      publishResult(responseBody);
    }

    if (!mHasResponse) {
      mOutputChannel.close();
    }
  }

  @Override
  public void onError(@NotNull final RoutineException error) {
    mOutputChannel.abort(error);
    mChunkChannel.abort();
  }

  @Override
  public void onOutput(final ParcelableFlow<Object> output) {
    switch (output.id) {
      case MEDIA_TYPE_ID:
        final String mediaType = output.data();
        mMediaType = (mediaType != null) ? MediaType.parse(mediaType) : null;
        mHasMediaType = true;
        break;

      case CONTENT_LENGTH_ID:
        final Long contentLength = output.data();
        mContentLength = (contentLength != null) ? contentLength : -1;
        mHasContentLength = true;
        break;

      case BYTES_ID:
        final ParcelableByteChunk chunk = output.data();
        if (mChunkChannel == null) {
          mChunkChannel = JRoutineCore.<ParcelableByteChunk>ofInputs().buildChannel();
        }

        mChunkChannel.pass(chunk);
        break;

      default:
        throw new IllegalArgumentException("unknown flow ID: " + output.id);
    }

    if (!mHasResponse && mHasMediaType && mHasContentLength && (mChunkChannel != null)) {
      mHasResponse = true;
      sService.execute(new Runnable() {

        @Override
        public void run() {
          final ResponseBody responseBody = ResponseBody.create(mMediaType, mContentLength,
              Okio.buffer(new BlockingSource(mChunkChannel)));
          publishResult(responseBody);
        }
      });
    }
  }

  private void publishResult(@NotNull final ResponseBody responseBody) {
    final Channel<Object, ?> outputChannel = mOutputChannel;
    try {
      outputChannel.pass(mConverter.convert(responseBody)).close();

    } catch (final Throwable t) {
      outputChannel.abort(InvocationException.wrapIfNeeded(t));
    }
  }

  /**
   * Blocking implementation of an Okio source.
   */
  private static class BlockingSource implements Source {

    private final Channel<ParcelableByteChunk, ParcelableByteChunk> mChannel;

    private ChunkInputStream mInputStream;

    /**
     * Constructor.
     *
     * @param channel the chunk channel.
     */
    private BlockingSource(
        @NotNull final Channel<ParcelableByteChunk, ParcelableByteChunk> channel) {
      mChannel = channel;
    }

    @Override
    public long read(final Buffer sink, final long byteCount) throws IOException {
      if (ConstantConditions.notNegative("byte count", byteCount) == 0) {
        return 0;
      }

      long count = 0;
      while (count < 1) {
        if (mInputStream == null) {
          final Channel<ParcelableByteChunk, ParcelableByteChunk> channel = mChannel;
          if (channel.in(indefiniteTime()).hasNext()) {
            mInputStream = ParcelableByteChannel.getInputStream(channel.next());

          } else {
            return -1;
          }
        }

        final ChunkInputStream inputStream = mInputStream;
        count = Math.min(byteCount, inputStream.available());
        sink.readFrom(inputStream, count);
        if (inputStream.available() == 0) {
          inputStream.close();
          mInputStream = null;
        }
      }

      return count;
    }

    @Override
    public Timeout timeout() {
      return new Timeout();
    }

    @Override
    public void close() {
      mChannel.close();
    }
  }
}
