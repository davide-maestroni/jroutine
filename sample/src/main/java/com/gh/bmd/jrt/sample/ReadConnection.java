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
package com.gh.bmd.jrt.sample;

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.invocation.FilterInvocation;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

import javax.annotation.Nonnull;

/**
 * Invocation reading data from the URL connection.
 * <p/>
 * Created by davide-maestroni on 10/17/14.
 */
public class ReadConnection extends FilterInvocation<URI, Chunk> {

    private static final int MAX_CHUNK_SIZE = 2048;

    public void onInput(final URI uri, @Nonnull final ResultChannel<Chunk> result) {

        try {

            final URLConnection connection = uri.toURL().openConnection();

            if (connection instanceof HttpURLConnection) {

                final int code = ((HttpURLConnection) connection).getResponseCode();

                if ((code < 200) || (code >= 300)) {

                    throw new IOException();
                }
            }

            final InputStream inputStream = connection.getInputStream();
            Chunk chunk = new Chunk(MAX_CHUNK_SIZE, inputStream);

            while (chunk.getLength() > 0) {

                result.pass(chunk);
                chunk = new Chunk(MAX_CHUNK_SIZE, inputStream);
            }

        } catch (final IOException e) {

            throw new InvocationException(e);
        }
    }
}
