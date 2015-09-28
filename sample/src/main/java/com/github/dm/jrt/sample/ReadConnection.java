/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.sample;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.ByteChannel;
import com.github.dm.jrt.core.ByteChannel.BufferOutputStream;
import com.github.dm.jrt.core.ByteChannel.ByteBuffer;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

/**
 * Invocation reading data from the URL connection.
 * <p/>
 * Created by davide-maestroni on 10/17/2014.
 */
public class ReadConnection extends FilterInvocation<URI, ByteBuffer> {

    private static final int MAX_CHUNK_SIZE = 2048;

    @SuppressWarnings("StatementWithEmptyBody")
    public void onInput(final URI uri, @NotNull final ResultChannel<ByteBuffer> result) {

        try {

            final URLConnection connection = uri.toURL().openConnection();

            if (connection instanceof HttpURLConnection) {

                final int code = ((HttpURLConnection) connection).getResponseCode();

                if ((code < 200) || (code >= 300)) {

                    throw new IOException();
                }
            }

            final InputStream inputStream = connection.getInputStream();
            // We employ the utility class dedicated to the optimized transfer of bytes through a
            // routine channel
            final BufferOutputStream outputStream =
                    ByteChannel.byteChannel(MAX_CHUNK_SIZE).passTo(result);

            try {

                while (outputStream.write(inputStream) > 0) {

                    // Keep looping...
                }

            } finally {

                outputStream.close();
            }

        } catch (final IOException e) {

            throw new InvocationException(e);
        }
    }
}
