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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Utility class focused on the optimization of the transfer of byte chunks through routine
 * channels.<br/>
 * Note that the channel streams should be properly closed as the Java best practices suggest.
 * <p/>
 * Created by davide-maestroni on 26/08/15.
 */
public class RecyclerByteChannel {

    public static final int DEFAULT_BUFFER_SIZE = 1024 << 3;

    public static final int DEFAULT_POOL_SIZE = 16;

    private final int mCorePoolSize;

    private final int mDataBufferSize;

    private final LinkedList<RecyclerInputStream> mDataPool = new LinkedList<RecyclerInputStream>();

    private final WeakIdentityHashMap<InputChannel<? super RecyclerInputStream>,
            RecyclerOutputStream>
            mStreams =
            new WeakIdentityHashMap<InputChannel<? super RecyclerInputStream>,
                    RecyclerOutputStream>();

    /**
     * Constructor.
     *
     * @param dataBufferSize the data buffer size.
     * @param corePoolSize   the maximum number of retained data buffers.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    RecyclerByteChannel(final int dataBufferSize, final int corePoolSize) {

        if (dataBufferSize < 1) {

            throw new IllegalArgumentException("the data buffer size must be greater than 0");
        }

        mCorePoolSize = corePoolSize;
        mDataBufferSize = dataBufferSize;
    }

    /**
     * Returns an input stream returning the concatenation of the data returned by the specified
     * recycler streams.
     *
     * @param streams the input streams whose data have to be concatenated.
     * @return the concatenated input stream.
     */
    @Nonnull
    public static ConcatRecyclerInputStream concat(@Nonnull final RecyclerInputStream... streams) {

        return concat(Arrays.asList(streams));
    }

    /**
     * Returns an input stream returning the concatenation of the data returned by the specified
     * recycler streams.
     *
     * @param streams the input streams whose data have to be concatenated.
     * @return the concatenated input stream.
     */
    @Nonnull
    public static ConcatRecyclerInputStream concat(
            @Nonnull final List<RecyclerInputStream> streams) {

        return new ConcatRecyclerInputStream(streams);
    }

    /**
     * Returns the output stream used to write bytes into the specified channel.
     *
     * @param channel the input channel to which pass the data.
     * @return the output stream.
     */
    @Nonnull
    public RecyclerOutputStream passTo(
            @Nonnull final InputChannel<? super RecyclerInputStream> channel) {

        RecyclerOutputStream stream;

        synchronized (mStreams) {

            final WeakIdentityHashMap<InputChannel<? super RecyclerInputStream>,
                    RecyclerOutputStream>
                    streams = mStreams;
            stream = streams.get(channel);

            if (stream == null) {

                stream = new RecyclerOutputStream(channel);
                streams.put(channel, stream);
            }
        }

        return stream;
    }

    @Nonnull
    private RecyclerInputStream obtain() {

        RecyclerInputStream byteData = null;

        synchronized (mDataPool) {

            final LinkedList<RecyclerInputStream> dataPool = mDataPool;

            if (!dataPool.isEmpty()) {

                byteData = dataPool.removeFirst();
            }
        }

        if (byteData != null) {

            return byteData.resetState();
        }

        return new RecyclerInputStream(mDataBufferSize);
    }

    private void recycle(@Nonnull final RecyclerInputStream recyclable) {

        synchronized (mDataPool) {

            final LinkedList<RecyclerInputStream> dataPool = mDataPool;

            if (dataPool.size() < mCorePoolSize) {

                dataPool.add(recyclable);
            }
        }
    }

    /**
     * Recyclable internal state enumeration.
     */
    private enum RecyclableState {

        WRITE,      // can write data into the buffer
        READ,       // can read data from the buffer
        RECYCLED    // the buffer is not usable
    }

    /**
     * Input stream returning the concatenation of the data returned by a collection of recycler
     * input streams.
     */
    public static class ConcatRecyclerInputStream extends InputStream {

        private final Object mMutex = new Object();

        private final ArrayList<RecyclerInputStream> mStreams;

        private int mIndex;

        private int mMarkIndex;

        /**
         * Constructor.
         *
         * @param streams the list of input streams whose data have to be concatenated.
         */
        private ConcatRecyclerInputStream(@Nonnull List<RecyclerInputStream> streams) {

            mStreams = new ArrayList<RecyclerInputStream>(streams);
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

                final ArrayList<RecyclerInputStream> streams = mStreams;
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

                final ArrayList<RecyclerInputStream> streams = mStreams;
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

                final ArrayList<RecyclerInputStream> streams = mStreams;
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

                final ArrayList<RecyclerInputStream> streams = mStreams;
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

                final ArrayList<RecyclerInputStream> streams = mStreams;
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

                final ArrayList<RecyclerInputStream> streams = mStreams;
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

                for (final RecyclerInputStream stream : mStreams) {

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
                final ArrayList<RecyclerInputStream> streams = mStreams;
                streams.get(index).reset();
                final int size = streams.size();

                for (int i = index + 1; i < size; i++) {

                    streams.get(i).mOffset = 0;
                }
            }
        }

        @Override
        public boolean markSupported() {

            return true;
        }
    }

    /**
     * Input stream implementation used to read the data from the a recycler buffer instance.
     */
    public class RecyclerInputStream extends InputStream {

        private final byte[] mBuffer;

        private final Object mMutex = new Object();

        private int mMark;

        private int mOffset;

        private int mSize;

        private RecyclableState mState = RecyclableState.WRITE;

        /**
         * Constructor.
         *
         * @param bufferSize the internal buffer size.
         */
        private RecyclerInputStream(final int bufferSize) {

            mBuffer = new byte[bufferSize];
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

                checkRead();
                final int offset = mOffset;
                final int size = mSize;

                if (offset >= size) {

                    return -1;
                }

                final int count = size - offset;
                // TODO: 27/08/15 inside mutex
                out.write(mBuffer, offset, count);
                mOffset = size;
                return count;
            }
        }

        private void changeState(@Nonnull final RecyclableState expected,
                @Nonnull final RecyclableState updated, @Nonnull final String errorMessage) {

            if (mState != expected) {

                throw new IllegalStateException(errorMessage + ": " + mState);
            }

            mState = updated;
        }

        private void checkRead() {

            if (mState != RecyclableState.READ) {

                throw new IllegalStateException(
                        "attempting to read from buffer while in illegal recyclable state: "
                                + mState);
            }
        }

        @Nonnull
        private RecyclerInputStream resetState() {

            synchronized (mMutex) {

                changeState(RecyclableState.RECYCLED, RecyclableState.WRITE,
                            "attempting to reuse instance while in illegal recyclable state");
            }

            return this;
        }

        @Nonnull
        private RecyclerInputStream setSize(final int size) {

            synchronized (mMutex) {

                changeState(RecyclableState.WRITE, RecyclableState.READ,
                            "attempting to write to output while in illegal recyclable state");
                mOffset = 0;
                mSize = size;
            }

            return this;
        }

        @Override
        public int read() {

            synchronized (mMutex) {

                checkRead();

                if (mOffset >= mSize) {

                    return -1;
                }

                return mBuffer[mOffset++];
            }
        }

        @Override
        public int read(@Nonnull final byte[] b) {

            synchronized (mMutex) {

                checkRead();
                final int len = b.length;

                if (len == 0) {

                    return 0;
                }

                final int offset = mOffset;
                final int size = mSize;

                if (offset >= size) {

                    return -1;
                }

                final int count = Math.min(len, size - offset);
                System.arraycopy(mBuffer, offset, b, 0, count);
                mOffset += count;
                return count;
            }
        }


        @Override
        @SuppressWarnings("ConstantConditions")
        public int read(@Nonnull final byte[] b, final int off, final int len) {

            synchronized (mMutex) {

                checkRead();

                if (b == null) {

                    throw new NullPointerException();

                } else if ((off < 0) || (len < 0) || (len > b.length - off) || ((off + len) < 0)) {

                    throw new IndexOutOfBoundsException();

                } else if (len == 0) {

                    return 0;
                }

                final int offset = mOffset;
                final int size = mSize;

                if (offset >= size) {

                    return -1;
                }

                final int count = Math.min(len, size - offset);
                System.arraycopy(mBuffer, offset, b, off, count);
                mOffset += count;
                return count;
            }
        }

        @Override
        public long skip(final long n) {

            synchronized (mMutex) {

                checkRead();
                final long skipped = Math.min(mSize - mOffset, n);
                mOffset += skipped;
                return skipped;
            }
        }

        @Override
        public int available() {

            synchronized (mMutex) {

                checkRead();
                return (mSize - mOffset);
            }
        }

        @Override
        public void close() {

            synchronized (mMutex) {

                changeState(RecyclableState.READ, RecyclableState.RECYCLED,
                            "attempting to read from buffer while in illegal recyclable state");
            }

            recycle(this);
        }

        @Override
        public void mark(final int readlimit) {

            synchronized (mMutex) {

                checkRead();
                mMark = mOffset;
            }
        }

        @Override
        public void reset() {

            synchronized (mMutex) {

                checkRead();
                mOffset = mMark;
            }
        }

        @Override
        public boolean markSupported() {

            return true;
        }
    }

    /**
     * Output stream implementation used to write data into the recycler buffers.
     */
    public class RecyclerOutputStream extends OutputStream {

        private final InputChannel<? super RecyclerInputStream> mChannel;

        private final Object mMutex = new Object();

        private RecyclerInputStream mByteData;

        private int mOffset;

        /**
         * Constructor
         *
         * @param channel the input channel to which pass the data.
         */
        @SuppressWarnings("ConstantConditions")
        private RecyclerOutputStream(
                @Nonnull final InputChannel<? super RecyclerInputStream> channel) {

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
            final RecyclerInputStream byteData;

            synchronized (mMutex) {

                byteData = getBuffer();
                final byte[] buffer = byteData.mBuffer;
                final int length = buffer.length;
                final int offset = mOffset;
                // TODO: 26/08/15 inside mutex
                read = in.read(buffer, offset, length - offset);
                mOffset += read;
                isPass = (mOffset >= length);

                if (isPass) {

                    byteData.setSize(mOffset);
                    mOffset = 0;
                    mByteData = null;
                }
            }

            if (isPass) {

                mChannel.pass(byteData);
            }

            return read;
        }

        @Override
        public void write(final int b) {

            final boolean isPass;
            final RecyclerInputStream byteData;

            synchronized (mMutex) {

                byteData = getBuffer();
                final byte[] buffer = byteData.mBuffer;
                buffer[mOffset++] = (byte) b;
                isPass = (mOffset >= buffer.length);

                if (isPass) {

                    byteData.setSize(mOffset);
                    mOffset = 0;
                    mByteData = null;
                }
            }

            if (isPass) {

                mChannel.pass(byteData);
            }
        }

        @Override
        public void write(@Nonnull final byte[] b) {

            final int len = b.length;

            if (len == 0) {

                return;
            }

            int written = 0;

            do {

                final boolean isPass;
                final RecyclerInputStream byteData;

                synchronized (mMutex) {

                    byteData = getBuffer();
                    final byte[] buffer = byteData.mBuffer;
                    final int length = buffer.length;
                    final int offset = mOffset;
                    final int count = Math.min(len - written, length - offset);
                    System.arraycopy(b, written, buffer, offset, count);
                    written += count;
                    mOffset += count;
                    isPass = (mOffset >= length);

                    if (isPass) {

                        byteData.setSize(mOffset);
                        mOffset = 0;
                        mByteData = null;
                    }
                }

                if (isPass) {

                    mChannel.pass(byteData);
                }

            } while (written < len);
        }

        @Override
        @SuppressWarnings("ConstantConditions")
        public void write(@Nonnull final byte[] b, final int off, final int len) {

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
                final RecyclerInputStream byteData;

                synchronized (mMutex) {

                    byteData = getBuffer();
                    final byte[] buffer = byteData.mBuffer;
                    final int length = buffer.length;
                    final int offset = mOffset;
                    final int count = Math.min(len - written, length - offset);
                    System.arraycopy(b, off + written, buffer, offset, count);
                    written += count;
                    mOffset += count;
                    isPass = (mOffset >= length);

                    if (isPass) {

                        byteData.setSize(mOffset);
                        mOffset = 0;
                        mByteData = null;
                    }
                }

                if (isPass) {

                    mChannel.pass(byteData);
                }

            } while (written < len);
        }

        @Override
        public void flush() {

            final RecyclerInputStream byteData;

            synchronized (mMutex) {

                final int offset = mOffset;

                if (offset == 0) {

                    return;
                }

                byteData = getBuffer().setSize(offset);
                mOffset = 0;
                mByteData = null;
            }

            mChannel.pass(byteData);
        }

        @Override
        public void close() {

            flush();
        }

        @Nonnull
        private RecyclerInputStream getBuffer() {

            final RecyclerInputStream byteData = mByteData;

            if (byteData != null) {

                return byteData;
            }

            return (mByteData = obtain());
        }
    }
}
