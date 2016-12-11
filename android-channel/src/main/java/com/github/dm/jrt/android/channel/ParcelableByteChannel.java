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

package com.github.dm.jrt.android.channel;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.channel.ByteChannel;
import com.github.dm.jrt.channel.ByteChannel.BufferInputStream;
import com.github.dm.jrt.channel.ByteChannel.BufferOutputStream;
import com.github.dm.jrt.channel.ByteChannel.ByteBuffer;
import com.github.dm.jrt.channel.Channels;
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
 * <br>
 * <p>
 * For example, an invocation writing bytes can be implemented as:
 * <pre>
 *     <code>
 *
 *         public void onInput(final IN in, final Channel&lt;ParcelableByteBuffer, ?&gt; result) {
 *             ...
 *             final BufferOutputStream outputStream =
 *                     AndroidChannels.parcelableByteChannel().bind(result);
 *             ...
 *         }
 *     </code>
 * </pre>
 * While an invocation reading them:
 * <pre>
 *     <code>
 *
 *         public void onInput(final ParcelableByteBuffer buffer,
 *                 final Channel&lt;OUT, ?&gt; result) {
 *             ...
 *             final BufferInputStream inputStream =
 *                     ParcelableByteChannel.inputStream(buffer);
 *             ...
 *         }
 *     </code>
 * </pre>
 * The generated buffers implement the parcelable interface.
 * <br>
 * Note that the streams used to write into and read from buffers should be properly closed as the
 * Java best practices suggest.
 * <p>
 * Created by davide-maestroni on 09/03/2015.
 */
@SuppressWarnings("WeakerAccess")
public class ParcelableByteChannel {

  private final ByteChannel mByteChannel;

  /**
   * Constructor.
   *
   * @param byteChannel the backing byte channel.
   */
  ParcelableByteChannel(@NotNull final ByteChannel byteChannel) {
    mByteChannel = ConstantConditions.notNull("byte channel", byteChannel);
  }

  /**
   * Creates an input stream returning the data contained in the specified buffer.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffer the byte buffer.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for the
   *                                         specified buffer.
   */
  @NotNull
  public static BufferInputStream inputStream(@NotNull final ParcelableByteBuffer buffer) {
    return ByteChannel.inputStream(buffer.getBuffer());
  }

  /**
   * Creates an input stream returning the concatenation of the data contained in the specified
   * buffers.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffers the byte buffers whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified buffers.
   */
  @NotNull
  public static BufferInputStream inputStream(@NotNull final ParcelableByteBuffer... buffers) {
    final ArrayList<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>(buffers.length);
    for (final ParcelableByteBuffer buffer : buffers) {
      byteBuffers.add(buffer.getBuffer());
    }

    return ByteChannel.inputStream(byteBuffers);
  }

  /**
   * Creates an input stream returning the concatenation of the data contained in the specified
   * buffers.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffers the byte buffers whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified buffers.
   */
  @NotNull
  public static BufferInputStream inputStream(
      @NotNull final Iterable<? extends ParcelableByteBuffer> buffers) {
    final ArrayList<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>();
    for (final ParcelableByteBuffer buffer : buffers) {
      byteBuffers.add(buffer.getBuffer());
    }

    return ByteChannel.inputStream(byteBuffers);
  }

  /**
   * Returns the output stream used to write bytes into the specified channel.
   *
   * @param channel the channel to which to pass the data.
   * @return the output stream.
   */
  @NotNull
  public BufferOutputStream bind(@NotNull final Channel<? super ParcelableByteBuffer, ?> channel) {
    final Channel<ByteBuffer, ByteBuffer> outputChannel =
        JRoutineCore.<ByteBuffer>ofInputs().buildChannel();
    outputChannel.bind(new BufferChannelConsumer(channel));
    return mByteChannel.bind(outputChannel);
  }

  /**
   * Returns the output stream used to write bytes into the specified channel.
   * <br>
   * The channel will be automatically closed as soon as the output stream is.
   *
   * @param channel the channel to which to pass the data.
   * @return the output stream.
   */
  @NotNull
  public BufferOutputStream bindDeep(
      @NotNull final Channel<? super ParcelableByteBuffer, ?> channel) {
    final Channel<ByteBuffer, ByteBuffer> outputChannel =
        JRoutineCore.<ByteBuffer>ofInputs().buildChannel();
    outputChannel.bind(new BufferChannelConsumer(channel));
    return mByteChannel.bindDeep(outputChannel);
  }

  /**
   * Parcelable buffer of bytes.
   * <p>
   * Buffer instances are managed by the owning byte channel and recycled when released, in order
   * to minimize memory consumption. Byte buffers are automatically acquired by
   * {@code BufferOutputStream}s and passed to the underlying channel.
   * <br>
   * The data contained in a buffer can be read through the dedicated {@code BufferInputStream}
   * returned by one of the {@code ParcelableByteChannel.inputStream()} methods. Note that only one
   * input stream can be created for each buffer, any further attempt will generate an exception.
   * <br>
   * Used buffers will be released as soon as the corresponding input stream is closed.
   *
   * @see ParcelableByteChannel#inputStream(ParcelableByteBuffer)
   * @see ParcelableByteChannel#inputStream(ParcelableByteBuffer...)
   * @see ParcelableByteChannel#inputStream(Iterable)
   */
  public static class ParcelableByteBuffer extends DeepEqualObject implements Parcelable {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ParcelableByteBuffer> CREATOR =
        new Creator<ParcelableByteBuffer>() {

          @Override
          public ParcelableByteBuffer createFromParcel(final Parcel in) {
            final byte[] data = in.createByteArray();
            final Channel<ByteBuffer, ByteBuffer> channel =
                JRoutineCore.<ByteBuffer>ofInputs().buildChannel();
            final BufferOutputStream outputStream =
                Channels.byteChannel(Math.max(data.length, 1)).bind(channel);
            try {
              outputStream.write(data);
              outputStream.close();
              return new ParcelableByteBuffer(channel.next());

            } catch (final IOException e) {
              // It should never happen...
              throw new IllegalStateException(e);
            }
          }

          @Override
          public ParcelableByteBuffer[] newArray(final int size) {
            return new ParcelableByteBuffer[size];
          }
        };

    private static final byte[] EMPTY_ARRAY = new byte[0];

    private final ByteBuffer mBuffer;

    /**
     * Constructor.
     *
     * @param buffer the backing byte buffer or null.
     */
    private ParcelableByteBuffer(@NotNull final ByteBuffer buffer) {
      super(asArgs(ConstantConditions.notNull("byte buffer", buffer)));
      mBuffer = buffer;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      final ByteBuffer buffer = mBuffer;
      if (buffer != null) {
        final BufferInputStream inputStream = ByteChannel.inputStream(buffer);
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
     * Returns the size in number of bytes of this buffer.
     *
     * @return the buffer size.
     */
    public int size() {
      final ByteBuffer buffer = mBuffer;
      return (buffer != null) ? buffer.size() : 0;
    }

    @NotNull
    private ByteBuffer getBuffer() {
      return mBuffer;
    }
  }

  /**
   * Channel consumer transforming byte buffers into parcelable buffers.
   */
  private static class BufferChannelConsumer implements ChannelConsumer<ByteBuffer> {

    private final Channel<? super ParcelableByteBuffer, ?> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel to which to pass the data.
     */
    private BufferChannelConsumer(@NotNull final Channel<? super ParcelableByteBuffer, ?> channel) {
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
    public void onOutput(final ByteBuffer output) {
      mChannel.pass(new ParcelableByteBuffer(output));
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
