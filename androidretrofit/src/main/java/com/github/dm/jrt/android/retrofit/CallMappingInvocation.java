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
import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.channel.ByteChannel.BufferOutputStream;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.MappingInvocation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Call;

import static com.github.dm.jrt.android.retrofit.ServiceCallInvocation.BYTES_INDEX;
import static com.github.dm.jrt.android.retrofit.ServiceCallInvocation.MEDIA_TYPE_INDEX;
import static com.github.dm.jrt.android.retrofit.ServiceCallInvocation.REQUEST_DATA_INDEX;

/**
 * Mapping invocation used to split the Retrofit call into request data and body, so to be more
 * easily parceled.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
class CallMappingInvocation extends MappingInvocation<Call<?>, ParcelableSelectable<Object>> {

    /**
     * Constructor.
     */
    CallMappingInvocation() {
        super(null);
    }

    @Override
    public void onInput(final Call<?> input,
            @NotNull final Channel<ParcelableSelectable<Object>, ?> result) throws IOException {
        final Request request = input.request();
        result.pass(new ParcelableSelectable<Object>(RequestData.of(request), REQUEST_DATA_INDEX));
        final RequestBody body = request.body();
        if (body != null) {
            final MediaType mediaType = body.contentType();
            result.pass(new ParcelableSelectable<Object>(
                    (mediaType != null) ? mediaType.toString() : null, MEDIA_TYPE_INDEX));
            final Buffer buffer = new Buffer();
            body.writeTo(buffer);
            if (buffer.size() > 0) {
                final Channel<Object, ?> channel =
                        AndroidChannels.selectInputParcelable(result, BYTES_INDEX).buildChannels();
                final BufferOutputStream outputStream =
                        ParcelableByteChannel.byteChannel().bindDeep(channel);
                try {
                    outputStream.transferFrom(buffer.inputStream());

                } finally {
                    outputStream.close();
                }
            }
        }
    }
}
