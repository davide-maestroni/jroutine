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
package com.github.dm.jrt.core;

import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.util.WeakIdentityHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Utility class focused on the optimization of the transfer of byte chunks through routine
 * channels.
 * <p/>
 * Each instance maintains a pool of byte buffers which are re-used to minimize memory consumption.
 * When the pool is empty, additional buffers are created in order to avoid blocking the caller
 * thread. Though, the pool will retain its maximum capacity and every buffer exceeding it will be
 * discarded.<br/>
 * Note that the streams used to write into and read from buffers should be properly closed as the
 * Java best practices suggest.
 * <p/>
 * Created by davide-maestroni on 08/26/2015.
 */
public class ByteChannel {

    /**
     * The default buffer size in number of bytes.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024 << 3;

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
    protected ByteChannel(final int dataBufferSize, final int corePoolSize) {

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
    @Nonnull
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
    @Nonnull
    public static ByteChannel byteChannel(final int dataBufferSize) {

        return new ByteChannel(dataBufferSize, DEFAULT_MEM_SIZE / Math.max(dataBufferSize, 1));
    }

    /**
     * Returns a new byte channel.
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @param corePoolSize   the maximum number of data retained in the pool. Additional data
     *                       created to fulfill the bytes requirement will be discarded.
     * @return the byte channel.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    @Nonnull
    public static ByteChannel byteChannel(final int dataBufferSize, final int corePoolSize) {

        return new ByteChannel(dataBufferSize, corePoolSize);
    }

    /**
     * Creates an input stream returning the data contained in the specified buffer.<br/>
     * Note that only one input stream can be created for each buffer.
     *
     * @param buffer the byte buffer.
     * @return the input stream.
     * @throws java.lang.IllegalStateException if an input stream has been already created for the
     *                                         specified buffer.
     */
    @Nonnull
    public static BufferInputStream inputStream(@Nonnull final ByteBuffer buffer) {

        return buffer.getStream();
    }

    /**
     * Creates an input stream returning the concatenation of the data contained in the specified
     * buffers.<br/>
     * Note that only one input stream can be created for each buffer.
     *
     * @param buffers the byte buffers whose data have to be concatenated.
     * @return the input stream.
     * @throws java.lang.IllegalStateException if an input stream has been already created for one
     *                                         of the specified buffers.
     */
    @Nonnull
    public static BufferInputStream inputStream(@Nonnull final ByteBuffer... buffers) {

        return new MultiBufferInputStream(buffers);
    }

    /**
     * Creates an input stream returning the concatenation of the data contained in the specified
     * buffers.<br/>
     * Note that only one input stream can be created for each buffer.
     *
     * @param buffers the byte buffers whose data have to be concatenated.
     * @return the input stream.
     * @throws java.lang.IllegalStateException if an input stream has been already created for one
     *                                         of the specified buffers.
     */
    @Nonnull
    public static BufferInputStream inputStream(@Nonnull final List<ByteBuffer> buffers) {

        return new MultiBufferInputStream(buffers);
    }

    /**
     * Returns the output stream used to write bytes into the specified channel.<br/>
     * Note that, if the method is called more than one time, passing the same input channel, it
     * will return the same output stream.
     *
     * @param channel the input channel to which pass the data.
     * @return the output stream.
     */
    @Nonnull
    public BufferOutputStream passTo(@Nonnull final InputChannel<? super ByteBuffer> channel) {

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

    @Nonnull
    private ByteBuffer acquire() {

        ByteBuffer buffer = null;

        synchronized (mBufferPool) {

            final LinkedList<ByteBuffer> bufferPool = mBufferPool;

            if (!bufferPool.isEmpty()) {

                buffer = bufferPool.removeFirst();
            }
        }

        if (buffer != null) {

            return buffer.unlock();
        }

        return new ByteBuffer(mDataBufferSize);
    }

    private void release(@Nonnull final ByteBuffer buffer) {

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
         * Reads some bytes from the input stream and writes them into the specified output stream.
         *
         * @param out the output stream.
         * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
         * more data because the end of the stream has been reached.
         * @throws IOException if an I/O error occurs. In particular, an <code>IOException</code>
         *                     may be thrown if the output stream has been closed.
         */
        public abstract int read(@Nonnull OutputStream out) throws IOException;

        @Override
        public abstract int read();

        @Override
        public abstract int read(@Nonnull byte[] b);

        @Override
        public abstract int read(@Nonnull byte[] b, int off, int len);

        @Override
        public abstract long skip(long n);

        @Override
        public abstract int available();

        @Override
        public abstract void close();

        @Override
        public abstract void mark(int readlimit);

        @Override
        public abstract void reset();
    }

    /**
     * Output stream used to write data into the buffer channel.
     */
    public static abstract class BufferOutputStream extends OutputStream {

        /**
         * Writes some bytes into the output stream by reading them from the specified input stream.
         *
         * @param in the input stream.
         * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
         * more data because the end of the stream has been reached.
         * @throws IOException If the first byte cannot be read for any reason other than end of
         *                     file, or if the input stream has been closed, or if some other I/O
         *                     error occurs.
         */
        public abstract int write(@Nonnull InputStream in) throws IOException;

        @Override
        public abstract void flush();

        @Override
        public abstract void close();
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
        private MultiBufferInputStream(@Nonnull final ByteBuffer[] buffers) {

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
        private MultiBufferInputStream(@Nonnull final List<ByteBuffer> buffers) {

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
         * @throws IOException if an I/O error occurs. In particular, an <code>IOException</code>
         *                     may be thrown if the output stream has been closed.
         */
        public int read(@Nonnull final OutputStream out) throws IOException {

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
        public int read(@Nonnull final byte[] b) {

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
        @SuppressWarnings("ConstantConditions")
        public int read(@Nonnull final byte[] b, final int off, final int len) {

            if (b == null) {

                throw new NullPointerException();

            } else if ((off < 0) || (len < 0) || (len > b.length - off) || ((off + len) < 0)) {

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

                for (int i = mIndex; i < size; i++) {

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
        public void mark(final int readlimit) {

            synchronized (mMutex) {

                final int index = (mMarkIndex = mIndex);
                mStreams.get(index).mark(readlimit);
            }
        }

        @Override
        public void reset() {

            synchronized (mMutex) {

                final int index = (mIndex = mMarkIndex);
                final ArrayList<BufferInputStream> streams = mStreams;
                streams.get(index).reset();
                final int size = streams.size();

                for (int i = index + 1; i < size; i++) {

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
     * <p/>
     * Buffer instances are managed by the owning byte channel and recycled when released, in order
     * to minimize memory consumption. Byte buffers are automatically acquired by
     * <code>BufferOutputStream</code>s and passed to the underlying channel.<br/>
     * The data contained in a buffer can be read through the dedicated
     * {@code BufferInputStream} returned by one of the {@code ByteChannel.inputStream()}
     * methods. Note that only one input stream can be created for each buffer, any further attempt
     * will generate an exception.<br/>
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

            mBuffer = new byte[bufferSize];
            mStream = new DefaultBufferInputStream(this);
        }

        @Override
        public int hashCode() {

            final int size = getSize();
            final byte[] buffer = mBuffer;
            int result = size;

            for (int i = 0; i < size; i++) {

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

            for (int i = 0; i < size; i++) {

                if (thisBuffer[i] != thatBuffer[i]) {

                    return false;
                }
            }

            return true;
        }

        private void changeState(@Nonnull final BufferState expected,
                @Nonnull final BufferState updated, @Nonnull final String errorMessage) {

            if (mState != expected) {

                throw new IllegalStateException(errorMessage + ": " + mState);
            }

            mState = updated;
        }

        private int getSize() {

            synchronized (mMutex) {

                return mSize;
            }
        }

        @Nonnull
        private BufferInputStream getStream() {

            synchronized (mMutex) {

                changeState(BufferState.TRANSFER, BufferState.READ,
                            "attempting to get buffer stream while in illegal state");
                return mStream;
            }
        }

        @Nonnull
        private ByteBuffer lock(final int size) {

            synchronized (mMutex) {

                changeState(BufferState.WRITE, BufferState.TRANSFER,
                            "attempting to write to output while in illegal state");
                mSize = size;
            }

            return this;
        }

        @Nonnull
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

            release(this);
        }

        @Nonnull
        private ByteBuffer unlock() {

            synchronized (mMutex) {

                changeState(BufferState.RECYCLED, BufferState.WRITE,
                            "attempting to reuse instance while in illegal state");
            }

            return this;
        }

        @Nonnull
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
        private DefaultBufferInputStream(@Nonnull final ByteBuffer buffer) {

            mBuffer = buffer;
        }

        @Override
        public int read(@Nonnull final OutputStream out) throws IOException {

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
        public int read(@Nonnull final byte[] b) {

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
        @SuppressWarnings("ConstantConditions")
        public int read(@Nonnull final byte[] b, final int off, final int len) {

            if (b == null) {

                throw new NullPointerException();

            } else if ((off < 0) || (len < 0) || (len > b.length - off) || ((off + len) < 0)) {

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
        public void mark(final int readlimit) {

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
        @SuppressWarnings("ConstantConditions")
        private DefaultBufferOutputStream(@Nonnull final InputChannel<? super ByteBuffer> channel) {

            if (channel == null) {

                throw new NullPointerException("the input channel must not be null");
            }

            mChannel = channel;
        }

        /**
         * Writes some bytes into the output stream by reading them from the specified input stream.
         *
         * @param in the input stream.
         * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
         * more data because the end of the stream has been reached.
         * @throws IOException If the first byte cannot be read for any reason other than end of
         *                     file, or if the input stream has been closed, or if some other I/O
         *                     error occurs.
         */
        public int write(@Nonnull final InputStream in) throws IOException {

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
                mOffset += read;
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
        public void write(@Nonnull final byte[] b) throws IOException {

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
        @SuppressWarnings("ConstantConditions")
        public void write(@Nonnull final byte[] b, final int off, final int len) throws
                IOException {

            if (b == null) {

                throw new NullPointerException();

            } else if ((off < 0) || (len < 0) || (len > b.length - off) || ((off + len) < 0)) {

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

        @Nonnull
        private ByteBuffer getBuffer() {

            final ByteBuffer byteBuffer = mBuffer;

            if (byteBuffer != null) {

                return byteBuffer;
            }

            return (mBuffer = acquire());
        }
    }
}