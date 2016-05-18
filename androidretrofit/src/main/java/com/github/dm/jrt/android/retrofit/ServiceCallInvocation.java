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

import android.util.SparseArray;

import com.github.dm.jrt.android.channel.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.channel.ByteChannel.BufferOutputStream;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

/**
 * Implementation of a context invocation handling OkHttp requests.
 * <p>
 * Created by davide-maestroni on 05/17/2016.
 */
public class ServiceCallInvocation
        extends TemplateContextInvocation<ParcelableRequest, ParcelableSelectable<Object>> {

    public static final int BYTES_INDEX = 1;

    public static final int MEDIA_TYPE_INDEX = 0;

    private final OkHttpClient mClient;

    /**
     * Constructor.
     *
     * @throws java.lang.UnsupportedOperationException if called.
     */
    @SuppressWarnings("unused")
    public ServiceCallInvocation() {

        mClient = ConstantConditions.unsupported();
    }

    /**
     * Constructor.
     *
     * @param client the OkHttp client.
     */
    public ServiceCallInvocation(@NotNull final OkHttpClient client) {

        mClient = ConstantConditions.notNull("http client instance", client);
    }

    public void onInput(final ParcelableRequest input,
            @NotNull final ResultChannel<ParcelableSelectable<Object>> result) throws IOException {

        final ResponseBody responseBody = mClient.newCall(input.rawRequest()).execute().body();
        final SparseArray<IOChannel<Object>> channels =
                SparseChannels.selectParcelable(result, MEDIA_TYPE_INDEX, BYTES_INDEX)
                              .buildChannels();
        final MediaType mediaType = responseBody.contentType();
        final IOChannel<Object> mediaTypeChannel = channels.get(MEDIA_TYPE_INDEX);
        if (mediaType != null) {
            mediaTypeChannel.pass(mediaType.toString());
        }

        mediaTypeChannel.close();
        final BufferOutputStream outputStream =
                ParcelableByteChannel.byteChannel().bind(channels.get(BYTES_INDEX));
        final InputStream inputStream = responseBody.byteStream();
        try {
            outputStream.writeAll(inputStream);

        } finally {
            inputStream.close();
            outputStream.close();
        }
    }
}
