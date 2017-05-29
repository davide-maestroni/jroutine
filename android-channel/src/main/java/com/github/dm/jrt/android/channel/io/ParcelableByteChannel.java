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

package com.github.dm.jrt.android.channel.io;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.channel.config.ByteChunkStreamConfigurable;
import com.github.dm.jrt.channel.config.ByteChunkStreamConfiguration;
import com.github.dm.jrt.channel.config.ByteChunkStreamConfiguration.Builder;
import com.github.dm.jrt.channel.io.ByteChannel;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkOutputStream;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Utility class focused on the optimization of the transfer of byte chunks through routine
 * channels.
 * <p>
 * For example, an invocation writing bytes can be implemented as:
 * <pre><code>
 * public void onInput(final IN in, final Channel&lt;ParcelableByteChunk, ?&gt; result) {
 *   ...
 *   final ByteChunkOutputStream parcelableOutputStream =
 *           ParcelableByteChannel.parcelableOutputStream().of(result);
 *   ...
 * }
 * </code></pre>
 * <p>
 * While an invocation reading them:
 * <pre><code>
 * public void onInput(final ParcelableByteChunk chunk, final Channel&lt;OUT, ?&gt; result) {
 *   ...
 *   final ByteChunkInputStream parcelableInputStream =
 *           ParcelableByteChannel.parcelableInputStream(chunk);
 *   ...
 * }
 * </code></pre>
 * The generated chunks implement the parcelable interface.
 * <br>
 * Note that the streams used to write into and read from chunks should be properly closed as the
 * Java best practices suggest.
 * <p>
 * Created by davide-maestroni on 09/03/2015.
 */
public class ParcelableByteChannel {

  /**
   * Constructor.
   */
  private ParcelableByteChannel() {
  }

  /**
   * Creates an input stream returning the concatenation of the data contained in the specified
   * chunks.
   * <p>
   * Note that only one input stream can be created for each chunk.
   *
   * @param chunks the byte chunks whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified chunks.
   */
  @NotNull
  public static ByteChunkInputStream parcelableInputStream(
      @NotNull final Iterable<? extends ParcelableByteChunk> chunks) {
    final ArrayList<ByteChunk> byteChunks = new ArrayList<ByteChunk>();
    for (final ParcelableByteChunk chunk : chunks) {
      byteChunks.add(chunk.getChunk());
    }

    return ByteChannel.inputStream(byteChunks);
  }

  /**
   * Creates an input stream returning the data contained in the specified chunk.
   * <p>
   * Note that only one input stream can be created for each chunk.
   *
   * @param chunk the byte chunk.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for the
   *                                         specified chunk.
   */
  @NotNull
  public static ByteChunkInputStream parcelableInputStream(
      @NotNull final ParcelableByteChunk chunk) {
    return ByteChannel.inputStream(chunk.getChunk());
  }

  /**
   * Creates an input stream returning the concatenation of the data contained in the specified
   * chunks.
   * <p>
   * Note that only one input stream can be created for each chunk.
   *
   * @param chunks the byte chunks whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified chunks.
   */
  @NotNull
  public static ByteChunkInputStream parcelableInputStream(
      @NotNull final ParcelableByteChunk... chunks) {
    final ArrayList<ByteChunk> byteChunks = new ArrayList<ByteChunk>(chunks.length);
    for (final ParcelableByteChunk chunk : chunks) {
      byteChunks.add(chunk.getChunk());
    }

    return ByteChannel.inputStream(byteChunks);
  }

  /**
   * Returns a builder of chunk output streams.
   * <br>
   * Since the byte chunks generated by the channel are likely to be part of a remote procedure
   * call, be aware of the limits imposed by the Android OS architecture when choosing a specific
   * chunk size (see {@link android.os.TransactionTooLargeException}).
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @return the output stream builder.
   */
  @NotNull
  public static ParcelableByteChunkOutputStreamBuilder parcelableOutputStream() {
    return new DefaultChunkOutputStreamBuilder();
  }

  /**
   * Interface defining a builder of chunk output streams.
   */
  public interface ParcelableByteChunkOutputStreamBuilder
      extends ByteChunkStreamConfigurable<ParcelableByteChunkOutputStreamBuilder> {

    /**
     * Builds a new output stream instance.
     *
     * @return the output stream instance.
     */
    @NotNull
    ByteChunkOutputStream of(@NotNull Channel<? super ParcelableByteChunk, ?> channel);
  }

  /**
   * Parcelable chunk of bytes.
   * <p>
   * Chunk instances are managed by the owning byte channel and recycled when released, in order
   * to minimize memory consumption. Byte chunks are automatically acquired by
   * {@code ByteChunkOutputStream}s and passed to the underlying channel.
   * <br>
   * The data contained in a chunk can be read through the dedicated {@code ByteChunkInputStream}
   * returned by one of the {@code ParcelableByteChannel.parcelableInputStream()} methods. Note that
   * only one
   * input stream can be created for each chunk, any further attempt will generate an exception.
   * <br>
   * Used chunks will be released as soon as the corresponding input stream is closed.
   *
   * @see ParcelableByteChannel#parcelableInputStream(ParcelableByteChunk)
   * @see ParcelableByteChannel#parcelableInputStream(ParcelableByteChunk...)
   * @see ParcelableByteChannel#parcelableInputStream(Iterable)
   */
  public static class ParcelableByteChunk extends DeepEqualObject implements Parcelable {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ParcelableByteChunk> CREATOR = new Creator<ParcelableByteChunk>() {

      @Override
      public ParcelableByteChunk createFromParcel(final Parcel in) {
        final byte[] data = in.createByteArray();
        final Channel<ByteChunk, ByteChunk> channel = JRoutineCore.channel().ofType();
        final ByteChunkOutputStream outputStream = ByteChannel.outputStream()
                                                              .withStream()
                                                              .withChunkSize(
                                                                  Math.max(data.length, 1))
                                                              .configuration()
                                                              .of(channel);
        try {
          outputStream.write(data);
          outputStream.close();
          return new ParcelableByteChunk(channel.next());

        } catch (final IOException e) {
          // It should never happen...
          throw new IllegalStateException(e);
        }
      }

      @Override
      public ParcelableByteChunk[] newArray(final int size) {
        return new ParcelableByteChunk[size];
      }
    };

    private static final byte[] EMPTY_ARRAY = new byte[0];

    private final ByteChunk mChunk;

    /**
     * Constructor.
     *
     * @param chunk the backing byte chunk or null.
     */
    private ParcelableByteChunk(@NotNull final ByteChunk chunk) {
      super(asArgs(ConstantConditions.notNull("byte chunk", chunk)));
      mChunk = chunk;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      final ByteChunk chunk = mChunk;
      if (chunk != null) {
        final ByteChunkInputStream inputStream = ByteChannel.inputStream(chunk);
        final ParcelOutputStream outputStream = new ParcelOutputStream(inputStream.available());
        try {
          inputStream.transferTo(outputStream);
          outputStream.writeToParcel(dest);

        } catch (final IOException ignored) {
          // It should never happen...
          dest.writeByteArray(EMPTY_ARRAY);
        }

      } else {
        dest.writeByteArray(EMPTY_ARRAY);
      }
    }

    /**
     * Returns the size in number of bytes of this chunk.
     *
     * @return the chunk size.
     */
    public int size() {
      final ByteChunk chunk = mChunk;
      return (chunk != null) ? chunk.size() : 0;
    }

    @NotNull
    private ByteChunk getChunk() {
      return mChunk;
    }
  }

  /**
   * Channel consumer transforming byte chunks into parcelable chunks.
   */
  private static class ChunkChannelConsumer implements ChannelConsumer<ByteChunk> {

    private final Channel<? super ParcelableByteChunk, ?> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel to which to pass the data.
     */
    private ChunkChannelConsumer(@NotNull final Channel<? super ParcelableByteChunk, ?> channel) {
      mChannel = ConstantConditions.notNull("channel instance", channel);
    }

    @Override
    public void onComplete() {
      mChannel.close();
    }

    @Override
    public void onError(@NotNull final RoutineException error) {
      mChannel.abort(error);
    }

    @Override
    public void onOutput(final ByteChunk output) {
      mChannel.pass(new ParcelableByteChunk(output));
    }
  }

  /**
   * Default implementation of an output stream builder.
   */
  private static class DefaultChunkOutputStreamBuilder
      implements ParcelableByteChunkOutputStreamBuilder {

    private ByteChunkStreamConfiguration mConfiguration =
        ByteChunkStreamConfiguration.defaultConfiguration();

    /**
     * Constructor.
     */
    private DefaultChunkOutputStreamBuilder() {
    }

    @NotNull
    @Override
    public ByteChunkOutputStream of(
        @NotNull final Channel<? super ParcelableByteChunk, ?> channel) {
      final Channel<ByteChunk, ByteChunk> outputChannel = JRoutineCore.channel().ofType();
      outputChannel.consume(new ChunkChannelConsumer(channel));
      return ByteChannel.outputStream().withConfiguration(mConfiguration).of(outputChannel);
    }

    @NotNull
    public ParcelableByteChunkOutputStreamBuilder withConfiguration(
        @NotNull final ByteChunkStreamConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("output stream configuration", configuration);
      return this;
    }

    @NotNull
    public Builder<? extends ParcelableByteChunkOutputStreamBuilder> withStream() {
      return new Builder<ParcelableByteChunkOutputStreamBuilder>(this, mConfiguration);
    }
  }

  /**
   * Utility output stream used to efficiently write byte data into a parcel.
   */
  private static class ParcelOutputStream extends ByteArrayOutputStream {

    /**
     * Constructor.
     *
     * @param size the initial capacity.
     */
    private ParcelOutputStream(final int size) {
      super(size);
    }

    private void writeToParcel(@NotNull final Parcel dest) {
      dest.writeByteArray(buf, 0, count);
    }
  }
}
