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
import com.github.dm.jrt.android.channel.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.ParcelableByteChannel.ParcelableByteBuffer;
import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.channel.ByteChannel.BufferOutputStream;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
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

import static com.github.dm.jrt.core.util.UnitDuration.infinity;

/**
 * Implementation of a Context invocation handling OkHttp requests.
 * <p>
 * Created by davide-maestroni on 05/17/2016.
 */
public class ServiceCallInvocation extends
        TemplateContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

    /**
     * The index of the selectable channel dedicated to the transfer of request and response body
     * bytes.
     */
    public static final int BYTES_INDEX = 1;

    /**
     * The index of the selectable channel dedicated to the transfer of request and response body
     * media type.
     */
    public static final int MEDIA_TYPE_INDEX = 0;

    /**
     * The index of the selectable channel dedicated to the transfer of the request data.
     */
    public static final int REQUEST_DATA_INDEX = -1;

    private boolean mHasMediaType;

    private Channel<ParcelableByteBuffer, ParcelableByteBuffer> mInputChannel;

    private boolean mIsRequest;

    private MediaType mMediaType;

    private RequestData mRequestData;

    @Override
    public void onComplete(@NotNull final Channel<ParcelableSelectable<Object>, ?> result) throws
            Exception {
        final Channel<ParcelableByteBuffer, ParcelableByteBuffer> inputChannel = mInputChannel;
        if (inputChannel != null) {
            inputChannel.close();
            asyncRequest(result);

        } else {
            syncRequest(result);
        }
    }

    @Override
    public void onInput(final ParcelableSelectable<Object> input,
            @NotNull final Channel<ParcelableSelectable<Object>, ?> result) throws Exception {
        switch (input.index) {
            case REQUEST_DATA_INDEX:
                mRequestData = input.data();
                break;

            case MEDIA_TYPE_INDEX:
                mHasMediaType = true;
                final String mediaType = input.data();
                mMediaType = (mediaType != null) ? MediaType.parse(mediaType) : null;
                break;

            case BYTES_INDEX:
                if (mInputChannel == null) {
                    mInputChannel = JRoutineCore.io().buildChannel();
                }

                final ParcelableByteBuffer buffer = input.data();
                mInputChannel.pass(buffer);
                break;

            default:
                throw new IllegalArgumentException("unknown selectable index: " + input.index);
        }

        if (mHasMediaType && (mRequestData != null) && (mInputChannel != null)) {
            asyncRequest(result);
        }
    }

    @Override
    public void onRecycle(final boolean isReused) {
        mRequestData = null;
        mMediaType = null;
        mInputChannel = null;
        mHasMediaType = false;
    }

    private void asyncRequest(@NotNull final Channel<ParcelableSelectable<Object>, ?> result) throws
            Exception {
        if (mIsRequest) {
            return;
        }

        mIsRequest = true;
        final Request request =
                mRequestData.requestWithBody(new AsyncRequestBody(mMediaType, mInputChannel));
        final Channel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> outputChannel =
                JRoutineCore.io().buildChannel();
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

                } catch (final IOException e) {
                    outputChannel.abort(e);

                } finally {
                    outputChannel.close();
                }
            }
        });
    }

    @NotNull
    private OkHttpClient getClient() throws Exception {
        return (OkHttpClient) ConstantConditions.notNull("http client instance",
                ContextInvocationTarget.instanceOf(OkHttpClient.class)
                                       .getInvocationTarget(getContext())
                                       .getTarget());
    }

    private void publishResult(@NotNull final ResponseBody responseBody,
            @NotNull final Channel<ParcelableSelectable<Object>, ?> result) throws IOException {
        final MediaType mediaType = responseBody.contentType();
        if (mediaType != null) {
            result.pass(new ParcelableSelectable<Object>(mediaType.toString(), MEDIA_TYPE_INDEX));
        }

        final Channel<Object, ?> channel =
                AndroidChannels.selectInputParcelable(result, BYTES_INDEX).buildChannels();
        final BufferOutputStream outputStream =
                AndroidChannels.parcelableByteChannel().bindDeep(channel);
        try {
            outputStream.transferFrom(responseBody.byteStream());

        } finally {
            outputStream.close();
        }
    }

    private void syncRequest(@NotNull final Channel<ParcelableSelectable<Object>, ?> result) throws
            Exception {
        final Request request = mRequestData.requestWithBody(null);
        final Response response = getClient().newCall(request).execute();
        final ResponseBody responseBody = response.body();
        if (response.isSuccessful()) {
            publishResult(responseBody, result);

        } else {
            result.abort(
                    new ErrorResponseException(retrofit2.Response.error(responseBody, response)));
        }
    }

    /**
     * Asynchronous request body.
     */
    private static class AsyncRequestBody extends RequestBody {

        private final Channel<ParcelableByteBuffer, ParcelableByteBuffer> mInputChannel;

        private final MediaType mMediaType;

        /**
         * Constructor.
         *
         * @param mediaType    the media type.
         * @param inputChannel the input channel.
         */
        private AsyncRequestBody(@Nullable final MediaType mediaType,
                @NotNull final Channel<ParcelableByteBuffer, ParcelableByteBuffer> inputChannel) {
            mMediaType = mediaType;
            mInputChannel = inputChannel;
        }

        @Override
        public MediaType contentType() {
            return mMediaType;
        }

        @Override
        public void writeTo(final BufferedSink sink) throws IOException {
            final OutputStream outputStream = sink.outputStream();
            try {
                for (final ParcelableByteBuffer buffer : mInputChannel.after(infinity())) {
                    ParcelableByteChannel.inputStream(buffer).transferTo(outputStream);
                }

            } finally {
                outputStream.close();
            }
        }
    }
}
