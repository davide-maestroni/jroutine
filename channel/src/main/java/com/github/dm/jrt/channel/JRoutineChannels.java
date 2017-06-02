/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.channel.builder.ChannelHandler;
import com.github.dm.jrt.channel.io.ByteChannel;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkOutputStreamBuilder;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * This utility class acts as a factory of handlers providing several methods to split and merge
 * channels together, making also possible to transfer data in multiple flows.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 */
public class JRoutineChannels {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineChannels() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a channel handler employing the default executor.
   *
   * @return the handler instance.
   */
  @NotNull
  public static ChannelHandler channelHandler() {
    return channelHandlerOn(defaultExecutor());
  }

  /**
   * Returns a channel handler employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the handler instance.
   */
  @NotNull
  public static ChannelHandler channelHandlerOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultChannelHandler(executor);
  }

  /**
   * Gets an input stream returning the data contained in the specified chunk.
   * <p>
   * Note that only one input stream can be created for each chunk.
   *
   * @param chunk the byte chunk.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for the
   *                                         specified chunk.
   * @see com.github.dm.jrt.channel.io.ByteChannel ByteChannel
   */
  @NotNull
  public static ByteChunkInputStream inputStreamOf(@NotNull final ByteChunk chunk) {
    return ByteChannel.inputStreamOf(chunk);
  }

  /**
   * Gets an input stream returning the concatenation of the data contained in the specified chunks.
   * <p>
   * Note that only one input stream can be created for each chunk.
   *
   * @param chunks the byte chunks whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified chunks.
   * @see com.github.dm.jrt.channel.io.ByteChannel ByteChannel
   */
  @NotNull
  public static ByteChunkInputStream inputStreamOf(@NotNull final ByteChunk... chunks) {
    return ByteChannel.inputStreamOf(chunks);
  }

  /**
   * Gets an input stream returning the concatenation of the data contained in the specified chunks.
   * <p>
   * Note that only one input stream can be created for each chunk.
   *
   * @param chunks the byte chunks whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified chunks.
   * @see com.github.dm.jrt.channel.io.ByteChannel ByteChannel
   */
  @NotNull
  public static ByteChunkInputStream inputStreamOf(
      @NotNull final Iterable<? extends ByteChunk> chunks) {
    return ByteChannel.inputStreamOf(chunks);
  }

  /**
   * Returns a builder of chunk output streams.
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @return the output stream builder.
   * @see com.github.dm.jrt.channel.io.ByteChannel ByteChannel
   */
  @NotNull
  public static ByteChunkOutputStreamBuilder outputStream() {
    return ByteChannel.outputStream();
  }
}
