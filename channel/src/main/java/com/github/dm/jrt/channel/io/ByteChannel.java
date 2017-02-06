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

package com.github.dm.jrt.channel.io;

import com.github.dm.jrt.channel.config.ChunkStreamConfiguration;
import com.github.dm.jrt.channel.config.ChunkStreamConfiguration.Builder;
import com.github.dm.jrt.channel.config.ChunkStreamConfiguration.CloseActionType;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Utility class focused on the optimization of the transfer of byte chunks through routine
 * channels.
 * <p>
 * For example, an invocation writing bytes can be implemented as:
 * <pre><code>
 * public void onInput(final IN in, final Channel&lt;ByteChunk, ?&gt; result) {
 *   ...
 *   final ChunkOutputStream outputStream = ByteChannel.withOutput(result).buildOutputStream();
 *   ...
 * }
 * </code></pre>
 * <p>
 * While an invocation reading them:
 * <pre><code>
 * public void onInput(final ByteChunk chunk, final Channel&lt;OUT, ?&gt; result) {
 *   ...
 *   final ChunkInputStream inputStream = ByteChannel.getInputStream(chunk);
 *   ...
 * }
 * </code></pre>
 * <p>
 * Each instance maintains a pool of byte chunks which are re-used to minimize memory consumption.
 * When the pool is empty, additional chunks are created in order to avoid blocking the caller
 * thread. Though, the pool will retain its maximum capacity and every chunk exceeding it will be
 * discarded.
 * <br>
 * Note that the streams used to write into and read from chunks should be properly closed as the
 * Java best practices suggest.
 * <p>
 * Created by davide-maestroni on 08/26/2015.
 */
@SuppressWarnings("WeakerAccess")
public class ByteChannel {

  private static final int DEFAULT_CHUNK_SIZE = 16 << 10;

  private static final int DEFAULT_POOL_SIZE = 16;

  private static final int DEFAULT_MEM_SIZE = DEFAULT_POOL_SIZE * DEFAULT_CHUNK_SIZE;

  private final SimpleQueue<ByteChunk> mChunkPool;

  private final ChunkStreamConfiguration mConfiguration;

  private final int mCorePoolSize;

  private final int mDataChunkSize;

  /**
   * Constructor.
   *
   * @param configuration the output stream configuration.
   */
  private ByteChannel(@NotNull final ChunkStreamConfiguration configuration) {
    mConfiguration = configuration;
    final int chunkSize = (mDataChunkSize = configuration.getChunkSizeOrElse(DEFAULT_CHUNK_SIZE));
    final int poolSize =
        (mCorePoolSize = configuration.getCorePoolSizeOrElse(DEFAULT_MEM_SIZE / chunkSize));
    mChunkPool = new SimpleQueue<ByteChunk>(Math.max(poolSize, 1));
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
   */
  @NotNull
  public static ChunkInputStream getInputStream(@NotNull final ByteChunk... chunks) {
    return new MultiChunkInputStream(chunks);
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
   */
  @NotNull
  public static ChunkInputStream getInputStream(
      @NotNull final Iterable<? extends ByteChunk> chunks) {
    return new MultiChunkInputStream(chunks);
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
   */
  @NotNull
  public static ChunkInputStream getInputStream(@NotNull final ByteChunk chunk) {
    return chunk.getStream();
  }

  /**
   * Returns a builder of chunk output streams.
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @param channel the output channel to feed with data.
   * @return the output stream builder.
   */
  @NotNull
  public static ChunkOutputStreamBuilder withOutput(
      @NotNull final Channel<? super ByteChunk, ?> channel) {
    return new DefaultChunkOutputStreamBuilder(channel);
  }

  private static boolean outOfBound(final int off, final int len, final int bytes) {
    return (off < 0) || (len < 0) || (len > bytes - off) || ((off + len) < 0);
  }

  @NotNull
  private ByteChunk acquire() {
    ByteChunk chunk = null;
    synchronized (mChunkPool) {
      final SimpleQueue<ByteChunk> chunkPool = mChunkPool;
      if (!chunkPool.isEmpty()) {
        chunk = chunkPool.removeFirst();
      }
    }

    if (chunk != null) {
      return chunk;
    }

    return new ByteChunk(mDataChunkSize);
  }

  @NotNull
  private ChunkOutputStream getOutputStream(@NotNull final Channel<? super ByteChunk, ?> channel) {
    return new DefaultChunkOutputStream(mConfiguration, channel);
  }

  private void release(@NotNull final ByteChunk chunk) {
    synchronized (mChunkPool) {
      final SimpleQueue<ByteChunk> chunkPool = mChunkPool;
      if (chunkPool.size() < mCorePoolSize) {
        chunkPool.add(chunk);
      }
    }
  }

  /**
   * Internal chunk state enumeration.
   */
  private enum ChunkState {

    WRITE,      // can write data into the chunk
    TRANSFER,   // the chunk is being transferred through the channel
    READ,       // can read data from the chunk
    RECYCLED    // the chunk is not usable
  }

  /**
   * Input stream used to read the data contained in a chunk instance.
   */
  public static abstract class ChunkInputStream extends InputStream {

    /**
     * Reads some bytes from the input stream and writes them into the specified output stream.
     *
     * @param out the output stream.
     * @return the total number of bytes read into the chunk, or {@code -1} if there is no more
     * data because the end of the stream has been reached.
     * @throws java.io.IOException if an I/O error occurs. In particular, an {@code IOException} may
     *                             be thrown if the output stream has been closed.
     */
    public abstract int read(@NotNull OutputStream out) throws IOException;

    /**
     * Reads up to {@code limit} bytes from the input stream and writes them into the specified
     * output stream.
     *
     * @param out   the output stream.
     * @param limit the maximum number of bytes to read.
     * @return the total number of bytes read into the chunk, or {@code -1} if there is no more
     * data because the end of the stream has been reached.
     * @throws java.lang.IllegalArgumentException if the limit is negative.
     * @throws java.io.IOException                if an I/O error occurs. In particular, an
     *                                            {@code IOException} may be thrown if the output
     *                                            stream has been closed.
     */
    public abstract int read(@NotNull OutputStream out, int limit) throws IOException;

    @Override
    public abstract int read();

    @Override
    public abstract int read(@NotNull byte[] b);

    @Override
    public abstract int read(@NotNull byte[] b, int off, int len);

    @Override
    public abstract long skip(long n);

    @Override
    public abstract int available();

    @Override
    public void close() {
    }

    @Override
    public abstract void mark(int readLimit);

    @Override
    public void reset() {
    }

    /**
     * Reads all the bytes returned by the input stream and writes them into the specified
     * output stream.
     * <p>
     * Calling this method has the same effect as calling:
     * <pre><code>
     * while (inputStream.read(outputStream) &gt; 0) {
     *   // Keep looping
     * }
     * </code></pre>
     *
     * @param out the output stream.
     * @return the total number of bytes read.
     * @throws java.io.IOException if an I/O error occurs. In particular, an {@code IOException} may
     *                             be thrown if the output stream has been closed.
     */
    public long readAll(@NotNull final OutputStream out) throws IOException {
      long count = 0;
      for (int b; (b = read(out)) > 0; ) {
        count += b;
      }

      return count;
    }

    /**
     * Transfers all the bytes to the specified output stream and close this one.
     * <p>
     * Calling this method has the same effect as calling:
     * <pre><code>
     * try {
     *   readAll(out);
     *
     * } finally {
     *   close();
     * }
     * </code></pre>
     *
     * @param out the output stream.
     * @return the total number of bytes read.
     * @throws java.io.IOException if an I/O error occurs. In particular, an {@code IOException} may
     *                             be thrown if the output stream has been closed.
     */
    public long transferTo(@NotNull final OutputStream out) throws IOException {
      try {
        return readAll(out);

      } finally {
        close();
      }
    }
  }

  /**
   * Output stream used to write data into the chunk channel.
   */
  public static abstract class ChunkOutputStream extends OutputStream {

    /**
     * Transfers all the bytes from the specified input stream and close it.
     * <p>
     * Calling this method has the same effect as calling:
     * <pre><code>
     * try {
     *   writeAll(in);
     *
     * } finally {
     *   in.close();
     * }
     * </code></pre>
     *
     * @param in the input stream.
     * @return the total number of bytes written.
     * @throws java.io.IOException If the first byte cannot be read for any reason other than
     *                             end of file, or if the input stream has been closed, or if
     *                             some other I/O error occurs.
     */
    @SuppressWarnings("ThrowFromFinallyBlock")
    public long transferFrom(@NotNull final InputStream in) throws IOException {
      try {
        return writeAll(in);

      } finally {
        in.close();
      }
    }

    /**
     * Writes some bytes into the output stream by reading them from the specified input stream.
     *
     * @param in the input stream.
     * @return the total number of bytes written into the chunk, or {@code -1} if there is no more
     * data because the end of the stream has been reached.
     * @throws java.io.IOException If the first byte cannot be read for any reason other than end of
     *                             file, or if the input stream has been closed, or if some other
     *                             I/O error occurs.
     */
    public abstract int write(@NotNull InputStream in) throws IOException;

    /**
     * Writes up to {@code limit} bytes into the output stream by reading them from the specified
     * \input stream.
     *
     * @param in    the input stream.
     * @param limit the maximum number of bytes to write.
     * @return the total number of bytes written into the chunk, or {@code -1} if there is no more
     * data because the end of the stream has been reached.
     * @throws java.lang.IllegalArgumentException if the limit is negative.
     * @throws java.io.IOException                If the first byte cannot be read for any reason
     *                                            other than end of file, or if the input stream has
     *                                            been closed, or if some other I/O error occurs.
     */
    public abstract int write(@NotNull InputStream in, int limit) throws IOException;

    /**
     * Writes all the returned bytes into the output stream by reading them from the specified
     * input stream.
     * <p>
     * Calling this method has the same effect as calling:
     * <pre><code>
     * while (outputStream.write(inputStream) &gt; 0) {
     *   // Keep looping
     * }
     * </code></pre>
     *
     * @param in the input stream.
     * @return the total number of bytes written.
     * @throws java.io.IOException If the first byte cannot be read for any reason other than
     *                             end of file, or if the input stream has been closed, or if
     *                             some other I/O error occurs.
     */
    public long writeAll(@NotNull final InputStream in) throws IOException {
      long count = 0;
      for (int b; (b = write(in)) > 0; ) {
        count += b;
      }

      return count;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
  }

  /**
   * Default implementation of an output stream builder.
   */
  private static class DefaultChunkOutputStreamBuilder implements ChunkOutputStreamBuilder {

    private final Channel<? super ByteChunk, ?> mChannel;

    private ChunkStreamConfiguration mConfiguration =
        ChunkStreamConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param channel the output channel to feed with data.
     */
    private DefaultChunkOutputStreamBuilder(@NotNull final Channel<? super ByteChunk, ?> channel) {
      mChannel = ConstantConditions.notNull("channel instance", channel);
    }

    @NotNull
    public ChunkOutputStreamBuilder apply(@NotNull final ChunkStreamConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("output stream configuration", configuration);
      return this;
    }

    @NotNull
    public ChunkOutputStream buildOutputStream() {
      return new ByteChannel(mConfiguration).getOutputStream(mChannel);
    }

    @NotNull
    public Builder<? extends ChunkOutputStreamBuilder> chunkStreamConfiguration() {
      return new Builder<ChunkOutputStreamBuilder>(this, mConfiguration);
    }
  }

  /**
   * Input stream returning the concatenation of a collection of byte chunk data.
   */
  private static class MultiChunkInputStream extends ChunkInputStream {

    private final Object mMutex = new Object();

    private final ArrayList<ChunkInputStream> mStreams;

    private int mIndex;

    private int mMarkIndex;

    /**
     * Constructor.
     *
     * @param chunks the array of byte chunks whose data have to be concatenated.
     */
    private MultiChunkInputStream(@NotNull final ByteChunk[] chunks) {
      final ArrayList<ChunkInputStream> streams =
          (mStreams = new ArrayList<ChunkInputStream>(chunks.length));
      for (final ByteChunk chunk : chunks) {
        streams.add(chunk.getStream());
      }
    }

    /**
     * Constructor.
     *
     * @param chunks the list of byte chunks whose data have to be concatenated.
     */
    private MultiChunkInputStream(@NotNull final Iterable<? extends ByteChunk> chunks) {
      final ArrayList<ChunkInputStream> streams = (mStreams = new ArrayList<ChunkInputStream>());
      for (final ByteChunk chunk : chunks) {
        streams.add(chunk.getStream());
      }
    }

    @Override
    public int read(@NotNull final OutputStream out) throws IOException {
      synchronized (mMutex) {
        final ArrayList<ChunkInputStream> streams = mStreams;
        final int size = streams.size();
        if (mIndex >= size) {
          return -1;
        }

        int read = streams.get(mIndex).read(out);
        while (read < 0) {
          if (++mIndex >= size) {
            return -1;
          }

          read = streams.get(mIndex).read(out);
        }

        return read;
      }
    }

    @Override
    public int read(@NotNull final OutputStream out, final int limit) throws IOException {
      if (ConstantConditions.notNegative("byte limit", limit) == 0) {
        return 0;
      }

      synchronized (mMutex) {
        int count = 0;
        while (count < limit) {
          final ArrayList<ChunkInputStream> streams = mStreams;
          final int size = streams.size();
          if (mIndex >= size) {
            return (count > 0) ? count : -1;
          }

          int read = streams.get(mIndex).read(out, limit - count);
          while (read < 0) {
            if (++mIndex >= size) {
              return (count > 0) ? count : -1;
            }

            read = streams.get(mIndex).read(out, limit - count);
          }

          count += read;
        }

        return count;
      }
    }

    @Override
    public int read() {
      synchronized (mMutex) {
        final ArrayList<ChunkInputStream> streams = mStreams;
        final int size = streams.size();
        if (mIndex >= size) {
          return -1;
        }

        int read = streams.get(mIndex).read();
        while (read == -1) {
          if (++mIndex >= size) {
            return -1;
          }

          read = streams.get(mIndex).read();
        }

        return read;
      }
    }

    @Override
    public int read(@NotNull final byte[] b) {
      final int len = b.length;
      if (len == 0) {
        return 0;
      }

      synchronized (mMutex) {
        final ArrayList<ChunkInputStream> streams = mStreams;
        final int size = streams.size();
        if (mIndex >= size) {
          return -1;
        }

        int count = 0;
        int read = streams.get(mIndex).read(b);
        if (read > 0) {
          count += read;
        }

        while (count < len) {
          if (++mIndex >= size) {
            return (count > 0) ? count : -1;
          }

          read = streams.get(mIndex).read(b, count, len - count);
          if (read > 0) {
            count += read;
          }
        }

        return count;
      }
    }

    @Override
    public int read(@NotNull final byte[] b, final int off, final int len) {
      if (outOfBound(off, len, b.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return 0;
      }

      synchronized (mMutex) {
        final ArrayList<ChunkInputStream> streams = mStreams;
        final int size = streams.size();
        if (mIndex >= size) {
          return -1;
        }

        int count = 0;
        int read = streams.get(mIndex).read(b, off, len);
        if (read > 0) {
          count += read;
        }

        while (count < len) {
          if (++mIndex >= size) {
            return (count > 0) ? count : -1;
          }

          read = streams.get(mIndex).read(b, off + count, len - count);
          if (read > 0) {
            count += read;
          }
        }

        return count;
      }
    }

    @Override
    public long skip(final long n) {
      synchronized (mMutex) {
        final ArrayList<ChunkInputStream> streams = mStreams;
        final int size = streams.size();
        if (mIndex >= size) {
          return 0;
        }

        long count = 0;
        long skipped = streams.get(mIndex).skip(n);
        if (skipped > 0) {
          count += skipped;
        }

        while (count < n) {
          if (++mIndex >= size) {
            return count;
          }

          skipped = streams.get(mIndex).skip(n - count);
          if (skipped > 0) {
            count += skipped;
          }
        }

        return count;
      }
    }

    @Override
    public int available() {
      int available = 0;
      synchronized (mMutex) {
        final ArrayList<ChunkInputStream> streams = mStreams;
        final int size = streams.size();
        for (int i = mIndex; i < size; ++i) {
          available += streams.get(i).available();
        }
      }

      return available;
    }

    @Override
    public void close() {
      synchronized (mMutex) {
        for (final ChunkInputStream stream : mStreams) {
          stream.close();
        }
      }
    }

    @Override
    public void mark(final int readLimit) {
      synchronized (mMutex) {
        final int index = (mMarkIndex = mIndex);
        mStreams.get(index).mark(readLimit);
      }
    }

    @Override
    public void reset() {
      synchronized (mMutex) {
        final int index = (mIndex = mMarkIndex);
        final ArrayList<ChunkInputStream> streams = mStreams;
        streams.get(index).reset();
        final int size = streams.size();
        for (int i = index + 1; i < size; ++i) {
          streams.get(i).reset();
        }
      }
    }

    @Override
    public boolean markSupported() {
      return true;
    }
  }

  /**
   * Object acting as a chunk of bytes.
   * <p>
   * Chunk instances are managed by the owning byte channel and recycled when released, in order
   * to minimize memory consumption. Byte chunks are automatically acquired by
   * {@code ChunkOutputStream}s and passed to the underlying channel.
   * <br>
   * The data contained in a chunk can be read through the dedicated {@code ChunkInputStream}
   * returned by one of the {@code ByteChannel.getInputStream()} methods. Note that only one input
   * stream can be created for each chunk, any further attempt will generate an exception.
   * <br>
   * Used chunks will be released as soon as the corresponding input stream is closed.
   *
   * @see ByteChannel#getInputStream(ByteChunk)
   * @see ByteChannel#getInputStream(ByteChunk...)
   * @see ByteChannel#getInputStream(Iterable)
   */
  public class ByteChunk {

    private final byte[] mBuffer;

    private final Object mMutex = new Object();

    private final DefaultChunkInputStream mStream;

    private int mSize;

    private ChunkState mState = ChunkState.WRITE;

    /**
     * Constructor.
     *
     * @param bufferSize the internal buffer size.
     */
    private ByteChunk(final int bufferSize) {
      this(new byte[bufferSize]);
    }

    /**
     * Constructor.
     *
     * @param buffer the internal buffer.
     */
    private ByteChunk(final byte[] buffer) {
      mBuffer = buffer;
      mStream = new DefaultChunkInputStream(this);
    }

    @Override
    public int hashCode() {
      final int size = size();
      final byte[] buffer = mBuffer;
      int result = size;
      for (int i = 0; i < size; ++i) {
        result = 31 * result + buffer[i];
      }

      return result;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof ByteChunk)) {
        return false;
      }

      final ByteChunk that = (ByteChunk) o;
      final int size = size();
      if (size != that.size()) {
        return false;
      }

      final byte[] thisBuffer = mBuffer;
      final byte[] thatBuffer = that.mBuffer;
      for (int i = 0; i < size; ++i) {
        if (thisBuffer[i] != thatBuffer[i]) {
          return false;
        }
      }

      return true;
    }

    /**
     * Returns the size in number of bytes of this chunk.
     *
     * @return the chunk size.
     */
    public int size() {
      synchronized (mMutex) {
        return mSize;
      }
    }

    private void changeState(@NotNull final ChunkState expected, @NotNull final ChunkState updated,
        @NotNull final String errorMessage) {
      if (mState != expected) {
        throw new IllegalStateException(errorMessage + ": " + mState);
      }

      mState = updated;
    }

    private void copyFrom(@NotNull final byte[] src, final int srcPos, final int dstPos,
        final int len) {
      System.arraycopy(src, srcPos, mBuffer, dstPos, len);
    }

    private void copyTo(final int srcPos, @NotNull final byte[] dest, final int dstPos,
        final int len) {
      System.arraycopy(mBuffer, srcPos, dest, dstPos, len);
    }

    private byte getByte(final int pos) {
      return mBuffer[pos];
    }

    @NotNull
    private ChunkInputStream getStream() {
      synchronized (mMutex) {
        changeState(ChunkState.TRANSFER, ChunkState.READ,
            "attempting to get chunk stream while in illegal state");
        return mStream;
      }
    }

    private int length() {
      return mBuffer.length;
    }

    @NotNull
    private ByteChunk lock(final int size) {
      synchronized (mMutex) {
        changeState(ChunkState.WRITE, ChunkState.TRANSFER,
            "attempting to write to output while in illegal state");
        mSize = size;
      }

      return this;
    }

    private int readFrom(@NotNull final InputStream in, final int off, final int len) throws
        IOException {
      return in.read(mBuffer, off, len);
    }

    private void recycle() {
      synchronized (mMutex) {
        changeState(ChunkState.READ, ChunkState.RECYCLED,
            "attempting to read from chunk while in illegal state");
        mSize = 0;
      }

      release(new ByteChunk(mBuffer));
    }

    private void setByte(final int pos, final byte b) {
      mBuffer[pos] = b;
    }

    private void writeTo(@NotNull final OutputStream out, final int off, final int len) throws
        IOException {
      out.write(mBuffer, off, len);
    }
  }

  /**
   * Default chunk input stream implementation.
   */
  private class DefaultChunkInputStream extends ChunkInputStream {

    private final ByteChunk mChunk;

    private final Object mMutex = new Object();

    private boolean mIsClosed;

    private int mMark;

    private int mOffset;

    /**
     * Constructor.
     *
     * @param chunk the internal chunk.
     */
    private DefaultChunkInputStream(@NotNull final ByteChunk chunk) {
      mChunk = chunk;
    }

    @Override
    public int read(@NotNull final OutputStream out) throws IOException {
      return read(out, Integer.MAX_VALUE);
    }

    @Override
    public int read(@NotNull final OutputStream out, final int limit) throws IOException {
      if (ConstantConditions.notNegative("byte limit", limit) == 0) {
        return 0;
      }

      synchronized (mMutex) {
        final ByteChunk chunk = mChunk;
        final int size = chunk.size();
        final int offset = mOffset;
        if (offset >= size) {
          return -1;
        }

        final int count = Math.min(size - offset, limit);
        chunk.writeTo(out, offset, count);
        mOffset = size;
        return count;
      }
    }

    @Override
    public int read(@NotNull final byte[] b) {
      final int len = b.length;
      if (len == 0) {
        return 0;
      }

      synchronized (mMutex) {
        final ByteChunk chunk = mChunk;
        final int size = chunk.size();
        final int offset = mOffset;
        if (offset >= size) {
          return -1;
        }

        final int count = Math.min(len, size - offset);
        chunk.copyTo(offset, b, 0, count);
        mOffset += count;
        return count;
      }
    }

    @Override
    public int read(@NotNull final byte[] b, final int off, final int len) {
      if (outOfBound(off, len, b.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return 0;
      }

      synchronized (mMutex) {
        final ByteChunk chunk = mChunk;
        final int size = chunk.size();
        final int offset = mOffset;
        if (offset >= size) {
          return -1;
        }

        final int count = Math.min(len, size - offset);
        chunk.copyTo(offset, b, off, count);
        mOffset += count;
        return count;
      }
    }

    @Override
    public long skip(final long n) {
      synchronized (mMutex) {
        final long skipped = Math.min(mChunk.size() - mOffset, n);
        if (skipped > 0) {
          mOffset += skipped;
          return skipped;
        }

        return 0;
      }
    }

    @Override
    public int available() {
      synchronized (mMutex) {
        return Math.max(0, mChunk.size() - mOffset);
      }
    }

    @Override
    public void close() {
      synchronized (mMutex) {
        if (mIsClosed) {
          return;
        }

        mIsClosed = true;
        mMark = 0;
        mChunk.recycle();
      }
    }

    @Override
    public void reset() {
      synchronized (mMutex) {
        mOffset = mMark;
      }
    }

    @Override
    public int read() {
      synchronized (mMutex) {
        final ByteChunk chunk = mChunk;
        final int size = chunk.size();
        if (mOffset >= size) {
          return -1;
        }

        return chunk.getByte(mOffset++);
      }
    }

    @Override
    public void mark(final int readLimit) {
      synchronized (mMutex) {
        mMark = mOffset;
      }
    }

    @Override
    public boolean markSupported() {
      return true;
    }
  }

  /**
   * Default chunk output stream implementation.
   */
  private class DefaultChunkOutputStream extends ChunkOutputStream {

    private final Channel<? super ByteChunk, ?> mChannel;

    private final CloseActionType mCloseAction;

    private final Object mMutex = new Object();

    private ByteChunk mChunk;

    private boolean mIsClosed;

    private int mOffset;

    /**
     * Constructor
     *
     * @param configuration the output stream configuration.
     * @param channel       the channel to which pass the data.
     */
    private DefaultChunkOutputStream(@NotNull final ChunkStreamConfiguration configuration,
        @NotNull final Channel<? super ByteChunk, ?> channel) {
      mChannel = channel;
      mCloseAction = configuration.getCloseActionTypeOrElse(CloseActionType.CLOSE_STREAM);
    }

    @Override
    public int write(@NotNull final InputStream in) throws IOException {
      return write(in, Integer.MAX_VALUE);
    }

    @Override
    public int write(@NotNull final InputStream in, final int limit) throws IOException {
      if (ConstantConditions.notNegative("byte limit", limit) == 0) {
        return 0;
      }

      final int read;
      final boolean isPass;
      final ByteChunk byteChunk;
      final int size;
      synchronized (mMutex) {
        if (mIsClosed) {
          throw new IOException("cannot write into a closed output stream");
        }

        byteChunk = getChunk();
        final int length = byteChunk.length();
        final int offset = mOffset;
        read = byteChunk.readFrom(in, offset, Math.min(length - offset, limit));
        if (read > 0) {
          mOffset += read;
          size = mOffset;
          isPass = (size >= length);
          if (isPass) {
            mOffset = 0;
            mChunk = null;
          }

        } else {
          size = mOffset;
          isPass = false;
        }
      }

      if (isPass) {
        mChannel.pass(byteChunk.lock(size));
      }

      return read;
    }

    @Override
    public void flush() {
      final ByteChunk byteChunk;
      final int size;
      synchronized (mMutex) {
        size = mOffset;
        if (size == 0) {
          return;
        }

        byteChunk = getChunk();
        mOffset = 0;
        mChunk = null;
      }

      mChannel.pass(byteChunk.lock(size));
    }

    @Override
    public void close() {
      final CloseActionType closeAction = mCloseAction;
      if (closeAction == CloseActionType.IGNORE) {
        return;
      }

      if ((closeAction == CloseActionType.CLOSE_STREAM) || (closeAction
          == CloseActionType.CLOSE_CHANNEL)) {
        synchronized (mMutex) {
          if (mIsClosed) {
            return;
          }

          mIsClosed = true;
        }
      }

      flush();
      if (closeAction == CloseActionType.CLOSE_CHANNEL) {
        mChannel.close();
      }
    }

    @Override
    public void write(final int b) throws IOException {
      final boolean isPass;
      final ByteChunk byteChunk;
      final int size;
      synchronized (mMutex) {
        if (mIsClosed) {
          throw new IOException("cannot write into a closed output stream");
        }

        byteChunk = getChunk();
        byteChunk.setByte(mOffset++, (byte) b);
        size = mOffset;
        isPass = (size >= byteChunk.length());
        if (isPass) {
          mOffset = 0;
          mChunk = null;
        }
      }

      if (isPass) {
        mChannel.pass(byteChunk.lock(size));
      }
    }

    @Override
    public void write(@NotNull final byte[] b) throws IOException {
      final int len = b.length;
      if (len == 0) {
        return;
      }

      int written = 0;
      do {
        final boolean isPass;
        final ByteChunk byteChunk;
        final int size;
        synchronized (mMutex) {
          if (mIsClosed) {
            throw new IOException("cannot write into a closed output stream");
          }

          byteChunk = getChunk();
          final int length = byteChunk.length();
          final int offset = mOffset;
          final int count = Math.min(len - written, length - offset);
          byteChunk.copyFrom(b, written, offset, count);
          written += count;
          mOffset += count;
          size = mOffset;
          isPass = (size >= length);
          if (isPass) {
            mOffset = 0;
            mChunk = null;
          }
        }

        if (isPass) {
          mChannel.pass(byteChunk.lock(size));
        }

      } while (written < len);
    }

    @Override
    public void write(@NotNull final byte[] b, final int off, final int len) throws IOException {
      if (outOfBound(off, len, b.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return;
      }

      int written = 0;
      do {
        final boolean isPass;
        final ByteChunk byteChunk;
        final int size;
        synchronized (mMutex) {
          if (mIsClosed) {
            throw new IOException("cannot write into a closed output stream");
          }

          byteChunk = getChunk();
          final int length = byteChunk.length();
          final int offset = mOffset;
          final int count = Math.min(len - written, length - offset);
          byteChunk.copyFrom(b, off + written, offset, count);
          written += count;
          mOffset += count;
          size = mOffset;
          isPass = (size >= length);
          if (isPass) {
            mOffset = 0;
            mChunk = null;
          }
        }

        if (isPass) {
          mChannel.pass(byteChunk.lock(size));
        }

      } while (written < len);
    }

    @NotNull
    private ByteChunk getChunk() {
      final ByteChunk byteChunk = mChunk;
      if (byteChunk != null) {
        return byteChunk;
      }

      return (mChunk = acquire());
    }
  }
}
