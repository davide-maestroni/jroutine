/**
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
package com.bmd.jrt.sample;

import com.bmd.jrt.annotation.VeryLongExecution;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.execution.BasicExecution;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

import javax.annotation.Nonnull;

/**
 * Execution reading from URL connection.
 * <p/>
 * Created by davide on 10/17/14.
 */
@VeryLongExecution
public class ReadConnection extends BasicExecution<URI, Chunk> {

    private static final int MAX_CHUNK_SIZE = 2048;

    @Override
    public void onInput(final URI uri, @Nonnull final ResultChannel<Chunk> results) {

        try {

            final URLConnection connection = uri.toURL().openConnection();

            if (connection instanceof HttpURLConnection) {

                final int code = ((HttpURLConnection) connection).getResponseCode();

                if ((code < 200) || (code >= 300)) {

                    throw new IOException();
                }
            }

            final InputStream inputStream = connection.getInputStream();
            Chunk chunk = new Chunk(MAX_CHUNK_SIZE);

            while (chunk.readFrom(inputStream)) {

                results.pass(chunk);
                chunk = new Chunk(MAX_CHUNK_SIZE);
            }

        } catch (final IOException e) {

            throw new RoutineException(e);
        }
    }
}