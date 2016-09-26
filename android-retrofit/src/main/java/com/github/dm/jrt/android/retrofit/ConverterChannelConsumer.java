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

import com.github.dm.jrt.android.channel.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.ParcelableByteChannel.ParcelableByteBuffer;
import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import static com.github.dm.jrt.android.retrofit.ServiceCallInvocation.BYTES_INDEX;
import static com.github.dm.jrt.android.retrofit.ServiceCallInvocation.MEDIA_TYPE_INDEX;

/**
 * Channel consumer implementation converting the request response body into and instance of the
 * output type.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
class ConverterChannelConsumer implements ChannelConsumer<ParcelableSelectable<Object>> {

  private final Converter<ResponseBody, ?> mConverter;

  private final Channel<Object, ?> mOutputChannel;

  private final ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();

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
    final ResponseBody responseBody = ResponseBody.create(mMediaType, mOutputStream.toByteArray());
    final Channel<Object, ?> outputChannel = mOutputChannel;
    try {
      outputChannel.pass(mConverter.convert(responseBody)).close();

    } catch (final Throwable t) {
      outputChannel.abort(InvocationException.wrapIfNeeded(t));
      InvocationInterruptedException.throwIfInterrupt(t);
    }
  }

  @Override
  public void onError(@NotNull final RoutineException error) {
    mOutputChannel.abort(error);
  }

  @Override
  public void onOutput(final ParcelableSelectable<Object> output) throws IOException {
    switch (output.index) {
      case MEDIA_TYPE_INDEX:
        final String mediaType = output.data();
        mMediaType = MediaType.parse(mediaType);
        break;

      case BYTES_INDEX:
        final ParcelableByteBuffer buffer = output.data();
        ParcelableByteChannel.inputStream(buffer).transferTo(mOutputStream);
        break;

      default:
        throw new IllegalArgumentException("unknown selectable index: " + output.index);
    }
  }
}
