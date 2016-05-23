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
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Implementation of a context invocation handling OkHttp requests.
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

    private MediaType mMediaType;

    private ByteArrayOutputStream mOutputStream;

    private RequestData mRequestData;

    public void onInput(final ParcelableSelectable<Object> input,
            @NotNull final ResultChannel<ParcelableSelectable<Object>> result) throws IOException {

        switch (input.index) {
            case REQUEST_DATA_INDEX:
                mRequestData = input.data();
                break;

            case MEDIA_TYPE_INDEX:
                final String mediaType = input.data();
                mMediaType = MediaType.parse(mediaType);
                break;

            case BYTES_INDEX:
                if (mOutputStream == null) {
                    mOutputStream = new ByteArrayOutputStream();
                }

                final ParcelableByteBuffer buffer = input.data();
                ParcelableByteChannel.inputStream(buffer).transferTo(mOutputStream);
                break;

            default:
                throw new IllegalArgumentException("unknown selectable index: " + input.index);
        }
    }

    @Override
    public void onResult(@NotNull final ResultChannel<ParcelableSelectable<Object>> result) throws
            Exception {

        final ByteArrayOutputStream byteStream = mOutputStream;
        final Request request = mRequestData.requestWithBody(
                (byteStream != null) ? RequestBody.create(mMediaType, byteStream.toByteArray())
                        : null);
        final ResponseBody responseBody = getClient().newCall(request).execute().body();
        final MediaType mediaType = responseBody.contentType();
        if (mediaType != null) {
            result.pass(new ParcelableSelectable<Object>(mediaType.toString(), MEDIA_TYPE_INDEX));
        }

        final IOChannel<Object> channel =
                AndroidChannels.selectParcelable(result, BYTES_INDEX).buildChannels();
        final BufferOutputStream outputStream = ParcelableByteChannel.byteChannel().bind(channel);
        try {
            outputStream.transferFrom(responseBody.byteStream());

        } finally {
            outputStream.close();
        }
    }

    @Override
    public void onTerminate() {

        mRequestData = null;
        mOutputStream = null;
        mMediaType = null;
    }

    @NotNull
    private OkHttpClient getClient() throws Exception {

        return (OkHttpClient) ConstantConditions.notNull("http client instance",
                ContextInvocationTarget.instanceOf(OkHttpClient.class)
                                       .getInvocationTarget(getContext())
                                       .getTarget());
    }
}
