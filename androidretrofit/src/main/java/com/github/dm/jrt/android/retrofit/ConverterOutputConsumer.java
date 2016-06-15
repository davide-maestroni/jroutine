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
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
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
 * Output consumer implementation converting the request response body into and instance of the
 * output type.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
class ConverterOutputConsumer implements OutputConsumer<ParcelableSelectable<Object>> {

    private final Converter<ResponseBody, ?> mConverter;

    private final IOChannel<Object> mOutputChannel;

    private final ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();

    private MediaType mMediaType;

    /**
     * Constructor.
     *
     * @param converter the body converter.
     * @param channel   the output channel.
     */
    ConverterOutputConsumer(@NotNull final Converter<ResponseBody, ?> converter,
            @NotNull final IOChannel<Object> channel) {
        mConverter = ConstantConditions.notNull("converter instance", converter);
        mOutputChannel = ConstantConditions.notNull("output I/O channel", channel);
    }

    @Override
    public void onComplete() {
        final ResponseBody responseBody =
                ResponseBody.create(mMediaType, mOutputStream.toByteArray());
        try {
            mOutputChannel.pass(mConverter.convert(responseBody)).close();

        } catch (final IOException e) {
            mOutputChannel.abort(InvocationException.wrapIfNeeded(e));
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
