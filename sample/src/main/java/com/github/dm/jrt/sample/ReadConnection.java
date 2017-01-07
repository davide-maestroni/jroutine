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

package com.github.dm.jrt.sample;

import com.github.dm.jrt.channel.io.ByteChannel;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkOutputStream;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.MappingInvocation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

/**
 * Invocation reading data from the URL connection.
 * <p>
 * Created by davide-maestroni on 10/17/2014.
 */
@SuppressWarnings("WeakerAccess")
public class ReadConnection extends MappingInvocation<URI, ByteChunk> {

  private static final int MAX_CHUNK_SIZE = 2048;

  /**
   * Constructor.
   */
  public ReadConnection() {
    super(null);
  }

  public void onInput(final URI uri, @NotNull final Channel<ByteChunk, ?> result) throws
      IOException {
    final URLConnection connection = uri.toURL().openConnection();
    connection.setConnectTimeout(3000);
    if (connection instanceof HttpURLConnection) {
      final int code = ((HttpURLConnection) connection).getResponseCode();
      if ((code < 200) || (code >= 300)) {
        throw new IOException();
      }
    }

    // We employ the utility class dedicated to the optimized transfer of bytes through a routine
    // channel
    final ChunkOutputStream outputStream = ByteChannel.withOutput(result)
                                                      .applyChunkStreamConfiguration()
                                                      .withChunkSize(MAX_CHUNK_SIZE)
                                                      .configured()
                                                      .buildOutputStream();
    try {
      outputStream.transferFrom(connection.getInputStream());

    } finally {
      outputStream.close();
    }
  }
}
