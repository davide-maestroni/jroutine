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
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
 *         public void onInput(final IN in,
 *                 final ResultChannel&lt;ParcelableByteBuffer&gt; result) {
 *
 *             ...
 *             final BufferOutputStream outputStream =
 *                     ParcelableByteChannel.byteChannel().bind(result);
 *             ...
 *         }
 *     </code>
 * </pre>
 * While an invocation reading them:
 * <pre>
 *     <code>
 *
 *         public void onInput(final ParcelableByteBuffer buffer,
 *                 final ResultChannel&lt;OUT&gt; result) {
 *
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
public class ParcelableByteChannel {

    private static final EmptyBufferInputStream EMPTY_INPUT_STREAM = new EmptyBufferInputStream();

    private final ByteChannel mByteChannel;

    private final WeakIdentityHashMap<InputChannel<? super ParcelableByteBuffer>,
            IOChannel<ByteBuffer>>
            mChannels =
            new WeakIdentityHashMap<InputChannel<? super ParcelableByteBuffer>,
                    IOChannel<ByteBuffer>>();

    private final WeakIdentityHashMap<IOChannel<? super ParcelableByteBuffer>,
            IOChannel<ByteBuffer>>
            mIOChannels =
            new WeakIdentityHashMap<IOChannel<? super ParcelableByteBuffer>,
                    IOChannel<ByteBuffer>>();

    /**
     * Constructor.
     */
    private ParcelableByteChannel() {

        mByteChannel = ByteChannel.byteChannel();
    }

    /**
     * Constructor.
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    private ParcelableByteChannel(final int dataBufferSize) {

        mByteChannel = ByteChannel.byteChannel(dataBufferSize);
    }

    /**
     * Constructor.
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @param corePoolSize   the maximum number of data retained in the pool. Additional data
     *                       created to fulfill the bytes requirement will be discarded.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    private ParcelableByteChannel(final int dataBufferSize, final int corePoolSize) {

        mByteChannel = ByteChannel.byteChannel(dataBufferSize, corePoolSize);
    }

    /**
     * Returns a new byte channel.
     *
     * @return the byte channel.
     */
    @NotNull
    public static ParcelableByteChannel byteChannel() {

        return new ParcelableByteChannel();
    }

    /**
     * Returns a new byte channel.
     * <br>
     * Since the byte buffers generated by the channel are likely to be part of a remote procedure
     * call, be aware of the limits imposed by the Android OS architecture when choosing a specific
     * buffer size (see {@link android.os.TransactionTooLargeException}).
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @return the byte channel.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    @NotNull
    public static ParcelableByteChannel byteChannel(final int dataBufferSize) {

        return new ParcelableByteChannel(dataBufferSize);
    }

    /**
     * Returns a new byte channel.
     * <br>
     * Since the byte buffers generated by the channel are likely to be part of a remote procedure
     * call, be aware of the limits imposed by the Android OS architecture when choosing a specific
     * buffer size (see {@link android.os.TransactionTooLargeException}).
     *
     * @param dataBufferSize the size of the data buffer used to transfer the bytes through the
     *                       routine channels.
     * @param corePoolSize   the maximum number of data retained in the pool. Additional data
     *                       created to fulfill the bytes requirement will be discarded.
     * @return the byte channel.
     * @throws java.lang.IllegalArgumentException if the specified size is 0 or negative.
     */
    @NotNull
    public static ParcelableByteChannel byteChannel(final int dataBufferSize,
            final int corePoolSize) {

        return new ParcelableByteChannel(dataBufferSize, corePoolSize);
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

        final ByteBuffer byteBuffer = buffer.getBuffer();
        if (byteBuffer != null) {
            return ByteChannel.inputStream(byteBuffer);
        }

        return EMPTY_INPUT_STREAM;
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
            final ByteBuffer byteBuffer = buffer.getBuffer();
            if (byteBuffer != null) {
                byteBuffers.add(byteBuffer);
            }
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
    public static BufferInputStream inputStream(@NotNull final List<ParcelableByteBuffer> buffers) {

        final ArrayList<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>(buffers.size());
        for (final ParcelableByteBuffer buffer : buffers) {
            final ByteBuffer byteBuffer = buffer.getBuffer();
            if (byteBuffer != null) {
                byteBuffers.add(byteBuffer);
            }
        }

        return ByteChannel.inputStream(byteBuffers);
    }

    /**
     * Returns the output stream used to write bytes into the specified channel.
     *
     * @param channel the input channel to which to pass the data.
     * @return the output stream.
     */
    @NotNull
    public BufferOutputStream bind(
            @NotNull final InputChannel<? super ParcelableByteBuffer> channel) {

        IOChannel<ByteBuffer> ioChannel;
        synchronized (mChannels) {
            final WeakIdentityHashMap<InputChannel<? super ParcelableByteBuffer>,
                    IOChannel<ByteBuffer>>
                    channels = mChannels;
            ioChannel = channels.get(channel);
            if (ioChannel == null) {
                ioChannel = JRoutineCore.io().buildChannel();
                ioChannel.bind(new BufferOutputConsumer(channel));
                channels.put(channel, ioChannel);
            }
        }

        return mByteChannel.bind(ioChannel.asInput());
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
    public BufferOutputStream bind(@NotNull final IOChannel<? super ParcelableByteBuffer> channel) {

        IOChannel<ByteBuffer> ioChannel;
        synchronized (mIOChannels) {
            final WeakIdentityHashMap<IOChannel<? super ParcelableByteBuffer>,
                    IOChannel<ByteBuffer>>
                    channels = mIOChannels;
            ioChannel = channels.get(channel);
            if (ioChannel == null) {
                ioChannel = JRoutineCore.io().buildChannel();
                ioChannel.bind(new IOBufferOutputConsumer(channel));
                channels.put(channel, ioChannel);
            }
        }

        return mByteChannel.bind(ioChannel);
    }

    /**
     * Parcelable buffer of bytes.
     * <p>
     * Buffer instances are managed by the owning byte channel and recycled when released, in order
     * to minimize memory consumption. Byte buffers are automatically acquired by
     * <code>BufferOutputStream</code>s and passed to the underlying channel.
     * <br>
     * The data contained in a buffer can be read through the dedicated
     * {@code BufferInputStream} returned by one of the {@code ParcelableByteChannel.inputStream()}
     * methods. Note that only one input stream can be created for each buffer, any further attempt
     * will generate an exception.
     * <br>
     * Used buffers will be released as soon as the corresponding input stream is closed.
     *
     * @see ParcelableByteChannel#inputStream(ParcelableByteBuffer)
     * @see ParcelableByteChannel#inputStream(ParcelableByteBuffer...)
     * @see ParcelableByteChannel#inputStream(List)
     */
    public static class ParcelableByteBuffer extends DeepEqualObject implements Parcelable {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ParcelableByteBuffer> CREATOR =
                new Creator<ParcelableByteBuffer>() {

                    public ParcelableByteBuffer createFromParcel(final Parcel in) {

                        final byte[] data = in.createByteArray();
                        if (data.length > 0) {
                            final IOChannel<ByteBuffer> ioChannel =
                                    JRoutineCore.io().buildChannel();
                            final BufferOutputStream outputStream =
                                    ByteChannel.byteChannel(data.length).bind(ioChannel);
                            try {
                                outputStream.write(data);
                                outputStream.close();
                                return new ParcelableByteBuffer(ioChannel.next());

                            } catch (final IOException ignored) {
                                // It should never happen...
                            }
                        }

                        return new ParcelableByteBuffer(null);
                    }

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
        private ParcelableByteBuffer(@Nullable final ByteBuffer buffer) {

            super(asArgs(buffer));
            mBuffer = buffer;
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            final ByteBuffer buffer = mBuffer;
            if (buffer != null) {
                final BufferInputStream inputStream = ByteChannel.inputStream(buffer);
                final ParcelOutputStream outputStream =
                        new ParcelOutputStream(inputStream.available());
                try {
                    inputStream.readAll(outputStream);
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
        public int getSize() {

            final ByteBuffer buffer = mBuffer;
            return (buffer != null) ? buffer.getSize() : 0;
        }

        @Nullable
        private ByteBuffer getBuffer() {

            return mBuffer;
        }
    }

    /**
     * Output consumer transforming byte buffers into parcelable buffers.
     */
    private static class BufferOutputConsumer extends TemplateOutputConsumer<ByteBuffer> {

        private final InputChannel<? super ParcelableByteBuffer> mChannel;

        /**
         * Constructor.
         *
         * @param channel the input channel to which to pass the data.
         */
        private BufferOutputConsumer(
                @NotNull final InputChannel<? super ParcelableByteBuffer> channel) {

            mChannel = ConstantConditions.notNull("input channel", channel);
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
     * Input stream implementation representing an empty buffer.
     */
    private static class EmptyBufferInputStream extends BufferInputStream {

        @Override
        public int read(@NotNull final OutputStream out) throws IOException {

            return -1;
        }

        @Override
        public int read() {

            return -1;
        }

        @Override
        public int read(@NotNull final byte[] b) {

            return -1;
        }

        @Override
        public int read(@NotNull final byte[] b, final int off, final int len) {

            return -1;
        }

        @Override
        public long skip(final long n) {

            return 0;
        }

        @Override
        public int available() {

            return 0;
        }

        @Override
        public void close() {

        }

        @Override
        public void mark(final int readLimit) {

        }

        @Override
        public void reset() {

        }

        @Override
        public long readAll(@NotNull final OutputStream out) throws IOException {

            return 0;
        }
    }

    /**
     * Output consumer transforming byte buffers into parcelable buffers.
     */
    private static class IOBufferOutputConsumer implements OutputConsumer<ByteBuffer> {

        private final IOChannel<? super ParcelableByteBuffer> mChannel;

        /**
         * Constructor.
         *
         * @param channel the input channel to which to pass the data.
         */
        private IOBufferOutputConsumer(
                @NotNull final IOChannel<? super ParcelableByteBuffer> channel) {

            mChannel = ConstantConditions.notNull("I/O channel", channel);
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@NotNull final RoutineException error) {

            mChannel.abort(error);
        }

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
