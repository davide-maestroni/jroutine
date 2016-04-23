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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class focused on the optimization of the transfer of byte chunks through routine
 * channels.
 * <p>
 * For example, an invocation writing bytes can be implemented as:
 * <pre>
 *     <code>
 *
 *         public void onInput(final IN in, final ResultChannel&lt;ByteBuffer&gt; result) {
 *
 *             ...
 *             final BufferOutputStream outputStream = ByteChannel.byteChannel().bind(result);
 *             ...
 *         }
 *     </code>
 * </pre>
 * While an invocation reading them:
 * <pre>
 *     <code>
 *
 *         public void onInput(final ByteBuffer buffer, final ResultChannel&lt;OUT&gt; result) {
 *
 *             ...
 *             final BufferInputStream inputStream = ByteChannel.inputStream(buffer);
 *             ...
 *         }
 *     </code>
 * </pre>
 * <p>
 * Each instance maintains a pool of byte buffers which are re-used to minimize memory consumption.
 * When the pool is empty, additional buffers are created in order to avoid blocking the caller
 * thread. Though, the pool will retain its maximum capacity and every buffer exceeding it will be
 * discarded.
 * <br>
 * Note that the streams used to write into and read from buffers should be properly closed as the
 * Java best practices suggest.
 * <p>
 * Created by davide-maestroni on 08/26/2015.
 */
public class ByteChannel {

    /**
     * The default buffer size in number of bytes.
     */
    public static final int DEFAULT_BUFFER_SIZE = 16 << 10;

    /**
     * The default core pool size.
     */
    public static final int DEFAULT_POOL_SIZE = 16;

    private static final int DEFAULT_MEM_SIZE = DEFAULT_POOL_SIZE * DEFAULT_BUFFER_SIZE;

    private final LinkedList<ByteBuffer> mBufferPool = new LinkedList<ByteBuffer>();

    private final int mCorePoolSize;

    private final int mDataBufferSize;

    private final WeakIdentityHashMap<InputChannel<? super ByteBuffer>, BufferOutputStream>
            mStreams =
            new WeakIdentityHashMap<InputChannel<? super ByteBuffer>, BufferOutputStream>();

    /**
     * Constructor.
     *
     * @param dataBufferSize the data buffer size.
     * @param corePoolSize   the maximum number of retained data buffers.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    private ByteChannel(final int dataBufferSize, final int corePoolSize) {

        if (dataBufferSize < 1) {
            throw new IllegalArgumentException("the data buffer size must be greater than 0");
        }

        mCorePoolSize = corePoolSize;
        mDataBufferSize = dataBufferSize;
    }

    /**
     * Returns a new byte channel.
     *
     * @return the byte channel.
     */
    @NotNull
    public static ByteChannel byteChannel() {

        return new ByteChannel(DEFAULT_BUFFER_SIZE, DEFAULT_POOL_SIZE);
    }

    /**
     * Returns a new byte channel.
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @return the byte channel.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    @NotNull
    public static ByteChannel byteChannel(final int dataBufferSize) {

        return new ByteChannel(dataBufferSize, DEFAULT_MEM_SIZE / Math.max(dataBufferSize, 1));
    }

    /**
     * Returns a new byte channel.
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @param corePoolSize   the maximum number of buffers retained in the pool. Additional buffers
     *                       created to fulfill the bytes requirement will be discarded.
     * @return the byte channel.
     * @throws java.lang.IllegalArgumentException if the specified size is not positive.
     */
    @NotNull
    public static ByteChannel byteChannel(final int dataBufferSize, final int corePoolSize) {

        return new ByteChannel(dataBufferSize, corePoolSize);
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
    public static BufferInputStream inputStream(@NotNull final ByteBuffer buffer) {

        return buffer.getStream();
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
    public static BufferInputStream inputStream(@NotNull final ByteBuffer... buffers) {

        return new MultiBufferInputStream(buffers);
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
    public static BufferInputStream inputStream(@NotNull final List<ByteBuffer> buffers) {

        return new MultiBufferInputStream(buffers);
    }

    private static boolean outOfBound(final int off, final int len, final int bytes) {

        return (off < 0) || (len < 0) || (len > bytes - off) || ((off + len) < 0);
    }

    /**
     * Returns the output stream used to write bytes into the specified channel.
     *
     * @param channel the input channel to which pass the data.
     * @return the output stream.
     */
    @NotNull
    public BufferOutputStream bind(@NotNull final InputChannel<? super ByteBuffer> channel) {

        BufferOutputStream stream;
        synchronized (mStreams) {
            final WeakIdentityHashMap<InputChannel<? super ByteBuffer>, BufferOutputStream>
                    streams = mStreams;
            stream = streams.get(channel);
            if (stream == null) {
                stream = new DefaultBufferOutputStream(channel);
                streams.put(channel, stream);
            }
        }

        return stream;
    }

    /**
     * Returns the output stream used to write bytes into the specified channel.
     * <p>
     * Note that the channel will be automatically closed as soon as the returned output stream is
     * closed.
     *
     * @param channel the I/O channel to which pass the data.
     * @return the output stream.
     */
    @NotNull
    public BufferOutputStream bind(@NotNull final IOChannel<? super ByteBuffer> channel) {

        return new IOBufferOutputStream(bind(channel.asInput()), channel);
    }

    @NotNull
    private ByteBuffer acquire() {

        ByteBuffer buffer = null;
        synchronized (mBufferPool) {
            final LinkedList<ByteBuffer> bufferPool = mBufferPool;
            if (!bufferPool.isEmpty()) {
                buffer = bufferPool.removeFirst();
            }
        }

        if (buffer != null) {
            return buffer;
        }

        return new ByteBuffer(mDataBufferSize);
    }

    private void release(@NotNull final ByteBuffer buffer) {

        synchronized (mBufferPool) {
            final LinkedList<ByteBuffer> bufferPool = mBufferPool;
            if (bufferPool.size() < mCorePoolSize) {
                bufferPool.add(buffer);
            }
        }
    }

    /**
     * Internal buffer state enumeration.
     */
    private enum BufferState {

        WRITE,      // can write data into the buffer
        TRANSFER,   // the buffer is being transferred through the channel
        READ,       // can read data from the buffer
        RECYCLED    // the buffer is not usable
    }

    /**
     * Input stream used to read the data contained in a buffer instance.
     */
    public static abstract class BufferInputStream extends InputStream {

        /**
         * Constructor.
         */
        protected BufferInputStream() {

        }

        /**
         * Reads some bytes from the input stream and writes them into the specified output stream.
         *
         * @param out the output stream.
         * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
         * more data because the end of the stream has been reached.
         * @throws java.io.IOException if an I/O error occurs. In particular, an
         *                             <code>IOException</code> may be thrown if the output stream
         *                             has been closed.
         */
        public abstract int read(@NotNull OutputStream out) throws IOException;

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
        public abstract void close();

        @Override
        public abstract void mark(int readLimit);

        @Override
        public abstract void reset();

        /**
         * Reads all the bytes returned by the input stream and writes them into the specified
         * output stream.
         * <br>
         * Calling this method has the same effect as calling:
         * <pre>
         *     <code>
         *
         *         while (inputStream.read(outputStream) &gt; 0) {
         *
         *             // Keep looping
         *         }
         *     </code>
         * </pre>
         *
         * @param out the output stream.
         * @return the total number of bytes read.
         * @throws java.io.IOException if an I/O error occurs. In particular, an
         *                             <code>IOException</code> may be thrown if the output stream
         *                             has been closed.
         */
        public long readAll(@NotNull final OutputStream out) throws IOException {

            long count = 0;
            for (int b; (b = read(out)) > 0; ) {
                count += b;
            }

            return count;
        }
    }

    /**
     * Output stream used to write data into the buffer channel.
     */
    public static abstract class BufferOutputStream extends OutputStream {

        /**
         * Constructor.
         */
        protected BufferOutputStream() {

        }

        /**
         * Writes some bytes into the output stream by reading them from the specified input stream.
         *
         * @param in the input stream.
         * @return the total number of bytes written into the buffer, or <code>-1</code> if there is
         * no more data because the end of the stream has been reached.
         * @throws java.io.IOException If the first byte cannot be read for any reason other than
         *                             end of file, or if the input stream has been closed, or if
         *                             some other I/O error occurs.
         */
        public abstract int write(@NotNull InputStream in) throws IOException;

        /**
         * Writes all the returned bytes into the output stream by reading them from the specified
         * input stream.
         * <br>
         * Calling this method has the same effect as calling:
         * <pre>
         *     <code>
         *
         *         while (outputStream.write(inputStream) &gt; 0) {
         *
         *             // Keep looping
         *         }
         *     </code>
         * </pre>
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
        public abstract void flush();

        @Override
        public abstract void close();
    }

    /**
     * Implementation of a buffer output stream automatically closing the output I/O channel.
     */
    private static class IOBufferOutputStream extends BufferOutputStream {

        private final IOChannel<? super ByteBuffer> mIOChannel;

        private final BufferOutputStream mOutputStream;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped stream.
         * @param channel the I/O channel.
         */
        private IOBufferOutputStream(@NotNull final BufferOutputStream wrapped,
                @NotNull final IOChannel<? super ByteBuffer> channel) {

            mOutputStream = wrapped;
            mIOChannel = channel;
        }

        @Override
        public int write(@NotNull final InputStream in) throws IOException {

            return mOutputStream.write(in);
        }

        @Override
        public void write(final int b) throws IOException {

            mOutputStream.write(b);
        }

        @Override
        public void write(@NotNull final byte[] b) throws IOException {

            mOutputStream.write(b);
        }

        @Override
        public void write(@NotNull final byte[] b, final int off, final int len) throws
                IOException {

            mOutputStream.write(b, off, len);
        }

        @Override
        public void flush() {

            mOutputStream.flush();
        }

        @Override
        public void close() {

            try {
                mOutputStream.close();
            } finally {
                mIOChannel.close();
            }
        }
    }

    /**
     * Input stream returning the concatenation of a collection of byte buffer data.
     */
    private static class MultiBufferInputStream extends BufferInputStream {

        private final Object mMutex = new Object();

        private final ArrayList<BufferInputStream> mStreams;

        private int mIndex;

        private int mMarkIndex;

        /**
         * Constructor.
         *
         * @param buffers the array of input streams whose data have to be concatenated.
         */
        private MultiBufferInputStream(@NotNull final ByteBuffer[] buffers) {

            final ArrayList<BufferInputStream> streams =
                    (mStreams = new ArrayList<BufferInputStream>(buffers.length));
            for (final ByteBuffer buffer : buffers) {
                streams.add(buffer.getStream());
            }
        }

        /**
         * Constructor.
         *
         * @param buffers the list of input streams whose data have to be concatenated.
         */
        private MultiBufferInputStream(@NotNull final List<ByteBuffer> buffers) {

            final ArrayList<BufferInputStream> streams =
                    (mStreams = new ArrayList<BufferInputStream>(buffers.size()));
            for (final ByteBuffer buffer : buffers) {
                streams.add(buffer.getStream());
            }
        }

        /**
         * Reads some bytes from the input stream and writes them into the specified output stream.
         *
         * @param out the output stream.
         * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
         * more data because the end of the stream has been reached.
         * @throws java.io.IOException if an I/O error occurs. In particular, an
         *                             <code>IOException</code> may be thrown if the output stream
         *                             has been closed.
         */
        public int read(@NotNull final OutputStream out) throws IOException {

            synchronized (mMutex) {
                final ArrayList<BufferInputStream> streams = mStreams;
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
        public int read() {

            synchronized (mMutex) {
                final ArrayList<BufferInputStream> streams = mStreams;
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
                final ArrayList<BufferInputStream> streams = mStreams;
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

            ConstantConditions.notNull("byte array", b);
            if (outOfBound(off, len, b.length)) {
                throw new IndexOutOfBoundsException();

            } else if (len == 0) {
                return 0;
            }

            synchronized (mMutex) {
                final ArrayList<BufferInputStream> streams = mStreams;
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
                final ArrayList<BufferInputStream> streams = mStreams;
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
                final ArrayList<BufferInputStream> streams = mStreams;
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
                for (final BufferInputStream stream : mStreams) {
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
                final ArrayList<BufferInputStream> streams = mStreams;
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
     * Object acting as a buffer of bytes.
     * <p>
     * Buffer instances are managed by the owning byte channel and recycled when released, in order
     * to minimize memory consumption. Byte buffers are automatically acquired by
     * <code>BufferOutputStream</code>s and passed to the underlying channel.
     * <br>
     * The data contained in a buffer can be read through the dedicated
     * {@code BufferInputStream} returned by one of the {@code ByteChannel.inputStream()}
     * methods. Note that only one input stream can be created for each buffer, any further attempt
     * will generate an exception.
     * <br>
     * Used buffers will be released as soon as the corresponding input stream is closed.
     *
     * @see ByteChannel#inputStream(ByteBuffer)
     * @see ByteChannel#inputStream(ByteBuffer...)
     * @see ByteChannel#inputStream(List)
     */
    public class ByteBuffer {

        private final byte[] mBuffer;

        private final Object mMutex = new Object();

        private final DefaultBufferInputStream mStream;

        private int mSize;

        private BufferState mState = BufferState.WRITE;

        /**
         * Constructor.
         *
         * @param bufferSize the internal buffer size.
         */
        private ByteBuffer(final int bufferSize) {

            this(new byte[bufferSize]);
        }

        /**
         * Constructor.
         *
         * @param buffer the internal buffer.
         */
        private ByteBuffer(final byte[] buffer) {

            mBuffer = buffer;
            mStream = new DefaultBufferInputStream(this);
        }

        /**
         * Returns the size in number of bytes of this buffer.
         *
         * @return the buffer size.
         */
        public int getSize() {

            synchronized (mMutex) {
                return mSize;
            }
        }

        @Override
        public int hashCode() {

            final int size = getSize();
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

            if (!(o instanceof ByteBuffer)) {
                return false;
            }

            final ByteBuffer that = (ByteBuffer) o;
            final int size = getSize();
            if (size != that.getSize()) {
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

        private void changeState(@NotNull final BufferState expected,
                @NotNull final BufferState updated, @NotNull final String errorMessage) {

            if (mState != expected) {
                throw new IllegalStateException(errorMessage + ": " + mState);
            }

            mState = updated;
        }

        @NotNull
        private BufferInputStream getStream() {

            synchronized (mMutex) {
                changeState(BufferState.TRANSFER, BufferState.READ,
                        "attempting to get buffer stream while in illegal state");
                return mStream;
            }
        }

        @NotNull
        private ByteBuffer lock(final int size) {

            synchronized (mMutex) {
                changeState(BufferState.WRITE, BufferState.TRANSFER,
                        "attempting to write to output while in illegal state");
                mSize = size;
            }

            return this;
        }

        @NotNull
        private byte[] readBuffer() {

            synchronized (mMutex) {
                final BufferState state = mState;
                if (state != BufferState.READ) {
                    throw new IllegalStateException(
                            "attempting to read buffer data while in illegal state: " + state);
                }
            }

            return mBuffer;
        }

        private void recycle() {

            synchronized (mMutex) {
                changeState(BufferState.READ, BufferState.RECYCLED,
                        "attempting to read from buffer while in illegal state");
                mSize = 0;
            }

            release(new ByteBuffer(mBuffer));
        }

        @NotNull
        private byte[] writeBuffer() {

            synchronized (mMutex) {
                final BufferState state = mState;
                if (state != BufferState.WRITE) {
                    throw new IllegalStateException(
                            "attempting to write buffer data while in illegal state: " + state);
                }
            }

            return mBuffer;
        }
    }

    /**
     * Default buffer input stream implementation.
     */
    private class DefaultBufferInputStream extends BufferInputStream {

        private final ByteBuffer mBuffer;

        private final Object mMutex = new Object();

        private boolean mIsClosed;

        private int mMark;

        private int mOffset;

        /**
         * Constructor.
         *
         * @param buffer the internal buffer.
         */
        private DefaultBufferInputStream(@NotNull final ByteBuffer buffer) {

            mBuffer = buffer;
        }

        @Override
        public int read(@NotNull final OutputStream out) throws IOException {

            synchronized (mMutex) {
                final ByteBuffer buffer = mBuffer;
                final int size = buffer.getSize();
                final int offset = mOffset;
                if (offset >= size) {
                    return -1;
                }

                final int count = size - offset;
                out.write(buffer.readBuffer(), offset, count);
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
                final ByteBuffer buffer = mBuffer;
                final int size = buffer.getSize();
                final int offset = mOffset;
                if (offset >= size) {
                    return -1;
                }

                final int count = Math.min(len, size - offset);
                System.arraycopy(buffer.readBuffer(), offset, b, 0, count);
                mOffset += count;
                return count;
            }
        }

        @Override
        public int read(@NotNull final byte[] b, final int off, final int len) {

            ConstantConditions.notNull("byte array", b);
            if (outOfBound(off, len, b.length)) {
                throw new IndexOutOfBoundsException();

            } else if (len == 0) {
                return 0;
            }

            synchronized (mMutex) {
                final ByteBuffer buffer = mBuffer;
                final int size = buffer.getSize();
                final int offset = mOffset;
                if (offset >= size) {
                    return -1;
                }

                final int count = Math.min(len, size - offset);
                System.arraycopy(buffer.readBuffer(), offset, b, off, count);
                mOffset += count;
                return count;
            }
        }

        @Override
        public long skip(final long n) {

            synchronized (mMutex) {
                final long skipped = Math.min(mBuffer.getSize() - mOffset, n);
                if (skipped > 0) {
                    mOffset += skipped;
                }

                return skipped;
            }
        }

        @Override
        public int available() {

            synchronized (mMutex) {
                return Math.max(0, mBuffer.getSize() - mOffset);
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
                mBuffer.recycle();
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
                final ByteBuffer buffer = mBuffer;
                final int size = buffer.getSize();
                if (mOffset >= size) {
                    return -1;
                }

                return buffer.readBuffer()[mOffset++];
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
     * Default buffer output stream implementation.
     */
    private class DefaultBufferOutputStream extends BufferOutputStream {

        private final InputChannel<? super ByteBuffer> mChannel;

        private final Object mMutex = new Object();

        private ByteBuffer mBuffer;

        private boolean mIsClosed;

        private int mOffset;

        /**
         * Constructor
         *
         * @param channel the input channel to which pass the data.
         */
        private DefaultBufferOutputStream(@NotNull final InputChannel<? super ByteBuffer> channel) {

            mChannel = ConstantConditions.notNull("input channel", channel);
        }

        @NotNull
        private ByteBuffer getBuffer() {

            final ByteBuffer byteBuffer = mBuffer;
            if (byteBuffer != null) {
                return byteBuffer;
            }

            return (mBuffer = acquire());
        }

        @Override
        public int write(@NotNull final InputStream in) throws IOException {

            final int read;
            final boolean isPass;
            final ByteBuffer byteBuffer;
            final int size;
            synchronized (mMutex) {
                if (mIsClosed) {
                    throw new IOException("cannot write into a closed output stream");
                }

                byteBuffer = getBuffer();
                final byte[] buffer = byteBuffer.writeBuffer();
                final int length = buffer.length;
                final int offset = mOffset;
                read = in.read(buffer, offset, length - offset);
                if (read > 0) {
                    mOffset += Math.max(read, 0);
                    size = mOffset;
                    isPass = (size >= length);
                    if (isPass) {
                        mOffset = 0;
                        mBuffer = null;
                    }

                } else {
                    size = mOffset;
                    isPass = false;
                }
            }

            if (isPass) {
                mChannel.pass(byteBuffer.lock(size));
            }

            return read;
        }

        @Override
        public void flush() {

            final ByteBuffer byteBuffer;
            final int size;
            synchronized (mMutex) {
                size = mOffset;
                if (size == 0) {
                    return;
                }

                byteBuffer = getBuffer();
                mOffset = 0;
                mBuffer = null;
            }

            mChannel.pass(byteBuffer.lock(size));
        }

        @Override
        public void close() {

            synchronized (mMutex) {
                if (mIsClosed) {
                    return;
                }

                mIsClosed = true;
            }

            flush();
        }

        @Override
        public void write(final int b) throws IOException {

            final boolean isPass;
            final ByteBuffer byteBuffer;
            final int size;
            synchronized (mMutex) {
                if (mIsClosed) {
                    throw new IOException("cannot write into a closed output stream");
                }

                byteBuffer = getBuffer();
                final byte[] buffer = byteBuffer.writeBuffer();
                buffer[mOffset++] = (byte) b;
                size = mOffset;
                isPass = (size >= buffer.length);
                if (isPass) {
                    mOffset = 0;
                    mBuffer = null;
                }
            }

            if (isPass) {
                mChannel.pass(byteBuffer.lock(size));
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
                final ByteBuffer byteBuffer;
                final int size;
                synchronized (mMutex) {
                    if (mIsClosed) {
                        throw new IOException("cannot write into a closed output stream");
                    }

                    byteBuffer = getBuffer();
                    final byte[] buffer = byteBuffer.writeBuffer();
                    final int length = buffer.length;
                    final int offset = mOffset;
                    final int count = Math.min(len - written, length - offset);
                    System.arraycopy(b, written, buffer, offset, count);
                    written += count;
                    mOffset += count;
                    size = mOffset;
                    isPass = (size >= length);
                    if (isPass) {
                        mOffset = 0;
                        mBuffer = null;
                    }
                }

                if (isPass) {
                    mChannel.pass(byteBuffer.lock(size));
                }

            } while (written < len);
        }

        @Override
        public void write(@NotNull final byte[] b, final int off, final int len) throws
                IOException {

            ConstantConditions.notNull("byte array", b);
            if (outOfBound(off, len, b.length)) {
                throw new IndexOutOfBoundsException();

            } else if (len == 0) {
                return;
            }

            int written = 0;
            do {
                final boolean isPass;
                final ByteBuffer byteBuffer;
                final int size;
                synchronized (mMutex) {
                    if (mIsClosed) {
                        throw new IOException("cannot write into a closed output stream");
                    }

                    byteBuffer = getBuffer();
                    final byte[] buffer = byteBuffer.writeBuffer();
                    final int length = buffer.length;
                    final int offset = mOffset;
                    final int count = Math.min(len - written, length - offset);
                    System.arraycopy(b, off + written, buffer, offset, count);
                    written += count;
                    mOffset += count;
                    size = mOffset;
                    isPass = (size >= length);
                    if (isPass) {
                        mOffset = 0;
                        mBuffer = null;
                    }
                }

                if (isPass) {
                    mChannel.pass(byteBuffer.lock(size));
                }

            } while (written < len);
        }
    }
}
