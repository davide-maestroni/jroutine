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

import com.github.dm.jrt.channel.ByteChannel.BufferInputStream;
import com.github.dm.jrt.channel.ByteChannel.BufferOutputStream;
import com.github.dm.jrt.channel.ByteChannel.ByteBuffer;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Byte channel unit tests.
 * <p>
 * Created by davide-maestroni on 08/29/2015.
 */
public class ByteChannelTest {

    @Test
    public void testAvailable() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        final byte[] b = new byte[16];
        stream.write(b);
        stream.close();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.available()).isEqualTo(16);
        assertThat(inputStream.read()).isNotEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(15);
        assertThat(inputStream.read(new byte[16], 4, 8)).isEqualTo(8);
        assertThat(inputStream.available()).isEqualTo(7);
        assertThat(inputStream.skip(4)).isEqualTo(4);
        assertThat(inputStream.available()).isEqualTo(3);
    }

    @Test
    public void testBufferEquals() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ByteBuffer buffer1 = channel.next();
        assertThat(buffer1).isEqualTo(buffer1);
        assertThat(buffer1).isNotEqualTo("test");
        stream.write(31);
        stream.write(17);
        stream.write(155);
        stream.write(13);
        stream.flush();
        final ByteBuffer buffer2 = channel.next();
        assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode());
        assertThat(buffer1).isEqualTo(buffer2);
        assertThat(buffer2).isEqualTo(buffer1);
        ByteChannel.inputStream(buffer2).close();
        stream.write(new byte[]{31, 17, (byte) 155});
        stream.flush();
        final ByteBuffer buffer3 = channel.next();
        assertThat(buffer1.hashCode()).isNotEqualTo(buffer3.hashCode());
        assertThat(buffer1).isNotEqualTo(buffer3);
        assertThat(buffer3).isNotEqualTo(buffer1);
    }

    @Test
    public void testChannelError() {

        try {

            ByteChannel.byteChannel(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            ByteChannel.byteChannel(-1, 0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testConcatAvailable() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(8).bind(channel);
        final byte[] b = new byte[16];
        stream.write(b);
        stream.close();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        assertThat(inputStream.available()).isEqualTo(16);
        assertThat(inputStream.read()).isNotEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(15);
        assertThat(inputStream.read(new byte[16], 4, 8)).isEqualTo(8);
        assertThat(inputStream.available()).isEqualTo(7);
        assertThat(inputStream.skip(4)).isEqualTo(4);
        assertThat(inputStream.available()).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testConcatClose() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(2).bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        inputStream.close();
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testConcatMark() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(4).bind(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        assertThat(inputStream.read()).isEqualTo(1);
        assertThat(inputStream.markSupported()).isTrue();
        inputStream.mark(3);
        assertThat(inputStream.read(new byte[4])).isEqualTo(4);
        assertThat(inputStream.read()).isEqualTo(6);
        inputStream.reset();
        assertThat(inputStream.read()).isEqualTo(2);
        assertThat(inputStream.read()).isEqualTo(3);
        assertThat(inputStream.read()).isEqualTo(4);
        final byte[] r = new byte[5];
        assertThat(inputStream.read(r)).isEqualTo(4);
        assertThat(r).containsExactly((byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 0);
    }

    @Test
    public void testConcatReadByte() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(2).bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        assertThat(inputStream.read()).isEqualTo(31);
        assertThat(inputStream.read()).isEqualTo(17);
        assertThat(inputStream.read()).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(13);
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testConcatReadByteArray() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(2).bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read()).isEqualTo(31);
        assertThat(inputStream.read(b)).isEqualTo(3);
        assertThat(b[0]).isEqualTo((byte) 17);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 17);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
    }

    @Test
    public void testConcatReadBytes() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(3).bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b, 2, 3)).isEqualTo(3);
        assertThat(b[2]).isEqualTo((byte) 31);
        assertThat(b[3]).isEqualTo((byte) 17);
        assertThat(b[4]).isEqualTo((byte) 155);
        assertThat(inputStream.read(b, 0, 4)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 13);
        assertThat(inputStream.read(b, 4, 8)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 13);
        assertThat(b[2]).isEqualTo((byte) 31);
        assertThat(b[3]).isEqualTo((byte) 17);
        assertThat(b[4]).isEqualTo((byte) 155);
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public void testConcatReadError() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(2).bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        final byte[] b = new byte[16];

        try {

            inputStream.read(null, 0, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            inputStream.read(b, -1, 1);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            inputStream.read(b, 0, -1);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            inputStream.read(b, 8, 16);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            inputStream.read((byte[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            inputStream.read((OutputStream) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            inputStream.readAll(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        assertThat(inputStream.read(new byte[0])).isEqualTo(0);
        assertThat(inputStream.read(b, 8, 0)).isEqualTo(0);
    }

    @Test
    public void testConcatReadOutput() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(3).bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(3);
        assertThat(outputStream.size()).isEqualTo(3);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155);
        assertThat(inputStream.read(outputStream)).isEqualTo(1);
        assertThat(outputStream.size()).isEqualTo(4);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
                (byte) 13);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(4);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
                (byte) 13);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    }

    @Test
    public void testConcatSkip() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(4).bind(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final BufferInputStream inputStream =
                ByteChannel.inputStream(channel.next(), channel.next());
        assertThat(inputStream.available()).isEqualTo(8);
        assertThat(inputStream.skip(2)).isEqualTo(2);
        assertThat(inputStream.read()).isEqualTo(3);
        assertThat(inputStream.available()).isEqualTo(5);
        assertThat(inputStream.skip(2)).isEqualTo(2);
        assertThat(inputStream.read(new byte[16], 4, 2)).isEqualTo(2);
        assertThat(inputStream.available()).isEqualTo(1);
        assertThat(inputStream.skip(4)).isEqualTo(1);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(0);
        assertThat(inputStream.skip(4)).isEqualTo(0);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(0);
    }

    @Test
    public void testDataPoolZero() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream =
                ByteChannel.byteChannel(ByteChannel.DEFAULT_BUFFER_SIZE, 0).bind(channel);
        stream.write(31);
        stream.flush();
        final ByteBuffer buffer = channel.next();
        final BufferInputStream inputStream = ByteChannel.inputStream(buffer);
        inputStream.close();
        stream.write(77);
        stream.flush();
        assertThat(channel.next()).isNotSameAs(buffer);
    }

    @Test
    public void testInputClose() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(31);
        stream.flush();
        final ByteBuffer buffer = channel.next();
        final BufferInputStream inputStream = ByteChannel.inputStream(buffer);
        inputStream.close();
        final byte[] b = new byte[16];
        assertThat(inputStream.available()).isZero();
        assertThat(inputStream.skip(100)).isLessThanOrEqualTo(0);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 2, 4)).isEqualTo(-1);
        assertThat(inputStream.read(new ByteArrayOutputStream())).isEqualTo(-1);
        inputStream.mark(10);
        assertThat(inputStream.available()).isZero();
        assertThat(inputStream.skip(100)).isLessThanOrEqualTo(0);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 2, 4)).isEqualTo(-1);
        assertThat(inputStream.read(new ByteArrayOutputStream())).isEqualTo(-1);
        inputStream.reset();
        assertThat(inputStream.available()).isZero();
        assertThat(inputStream.skip(100)).isLessThanOrEqualTo(0);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 2, 4)).isEqualTo(-1);
        assertThat(inputStream.read(new ByteArrayOutputStream())).isEqualTo(-1);
        inputStream.close();
        assertThat(inputStream.available()).isZero();
        assertThat(inputStream.skip(100)).isLessThanOrEqualTo(0);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 2, 4)).isEqualTo(-1);
        assertThat(inputStream.read(new ByteArrayOutputStream())).isEqualTo(-1);
    }

    @Test
    public void testMark() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(1);
        assertThat(inputStream.markSupported()).isTrue();
        inputStream.mark(3);
        assertThat(inputStream.read(new byte[4])).isEqualTo(4);
        assertThat(inputStream.read()).isEqualTo(6);
        inputStream.reset();
        assertThat(inputStream.read()).isEqualTo(2);
        assertThat(inputStream.read()).isEqualTo(3);
        assertThat(inputStream.read()).isEqualTo(4);
        final byte[] r = new byte[5];
        assertThat(inputStream.read(r)).isEqualTo(4);
        assertThat(r).containsExactly((byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 0);
    }

    @Test
    public void testOutputClose() {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.close();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
        final byte[] b = new byte[16];

        try {

            stream.write(77);

            fail();

        } catch (final IOException ignored) {

        }

        try {

            stream.write(b);

            fail();

        } catch (final IOException ignored) {

        }

        try {

            stream.write(b, 3, 8);

            fail();

        } catch (final IOException ignored) {

        }

        try {

            stream.write(new ByteArrayInputStream(b));

            fail();

        } catch (final IOException ignored) {

        }

        stream.flush();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
        stream.close();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
    }

    @Test
    public void testReadAll() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.readAll(outputStream)).isEqualTo(4);
        assertThat(outputStream.size()).isEqualTo(4);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
                (byte) 13);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(4);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
                (byte) 13);
    }

    @Test
    public void testReadByte() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(77);
        stream.flush();
        assertThat(ByteChannel.inputStream(channel.next()).read()).isEqualTo(77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(31);
        assertThat(inputStream.read()).isEqualTo(17);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(13);
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testReadByteArray() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(77);
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 77);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 13);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 13);
        stream.write(new byte[]{11, 111});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        b = new byte[1];
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 11);
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 111);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 111);
    }

    @Test
    public void testReadBytes() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(77);
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b, 0, 2)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 77);
        assertThat(inputStream.read(b, 0, 4)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 1, 8)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 77);
        assertThat(b[1]).isEqualTo((byte) 31);
        assertThat(b[2]).isEqualTo((byte) 17);
        assertThat(inputStream.read(b, 2, 8)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 77);
        assertThat(b[1]).isEqualTo((byte) 31);
        assertThat(b[2]).isEqualTo((byte) 17);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 0, 4)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 13);
        assertThat(b[2]).isEqualTo((byte) 17);
        assertThat(inputStream.read(b, 1, 4)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 13);
        assertThat(b[2]).isEqualTo((byte) 17);
        stream.write(new byte[]{11, 111});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 1, 1)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 11);
        assertThat(b[2]).isEqualTo((byte) 17);
        assertThat(inputStream.read(b, 1, 8)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 111);
        assertThat(b[2]).isEqualTo((byte) 17);
        assertThat(inputStream.read(b, 0, 1)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 111);
        assertThat(b[2]).isEqualTo((byte) 17);
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public void testReadError() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(77);
        stream.flush();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        final byte[] b = new byte[16];

        try {

            inputStream.read(null, 0, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            inputStream.read(b, -1, 1);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            inputStream.read(b, 0, -1);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            inputStream.read(b, 8, 16);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            inputStream.read((byte[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            inputStream.read((OutputStream) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            inputStream.readAll(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        assertThat(inputStream.read(new byte[0])).isEqualTo(0);
        assertThat(inputStream.read(b, 8, 0)).isEqualTo(0);
    }

    @Test
    public void testReadOutput() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(77);
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(1);
        assertThat(outputStream.size()).isEqualTo(1);
        assertThat(outputStream.toByteArray()[0]).isEqualTo((byte) 77);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(1);
        assertThat(outputStream.toByteArray()[0]).isEqualTo((byte) 77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(2);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(2);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 155, (byte) 13);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 155, (byte) 13);
    }

    @Test
    public void testSkip() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.available()).isEqualTo(8);
        assertThat(inputStream.skip(2)).isEqualTo(2);
        assertThat(inputStream.read()).isEqualTo(3);
        assertThat(inputStream.available()).isEqualTo(5);
        assertThat(inputStream.skip(2)).isEqualTo(2);
        assertThat(inputStream.read(new byte[16], 4, 2)).isEqualTo(2);
        assertThat(inputStream.available()).isEqualTo(1);
        assertThat(inputStream.skip(4)).isEqualTo(1);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(0);
        assertThat(inputStream.skip(4)).isEqualTo(0);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(0);
    }

    @Test
    public void testStream() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(2).bind(channel);
        stream.write(1);
        stream.write(2);
        stream.write(new byte[]{3, 4, 5});
        stream.write(new byte[]{4, 5, 6, 7, 8, 9}, 2, 3);
        stream.write(new ByteArrayInputStream(new byte[]{9, 10}));
        stream.close();
        final List<ByteBuffer> inputStreams = channel.close().all();
        assertThat(inputStreams).hasSize(5);
        final byte[] b = new byte[10];
        assertThat(ByteChannel.inputStream(inputStreams).read(b)).isEqualTo(10);
        assertThat(b).containsExactly((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6,
                (byte) 7, (byte) 8, (byte) 9, (byte) 10);
    }

    @Test
    public void testStreamCache() {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final ByteChannel byteChannel = ByteChannel.byteChannel();
        final BufferOutputStream stream = byteChannel.bind(channel.asInput());
        assertThat(byteChannel.bind(channel.asInput())).isSameAs(stream);
    }

    @Test
    public void testTransferFrom() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(4).bind(channel);
        stream.transferFrom(new ByteArrayInputStream(new byte[]{77, 33, (byte) 155, 13}));
        stream.flush();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(33);
        assertThat(inputStream.read()).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(13);
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testTransferTo() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.transferTo(outputStream)).isEqualTo(4);
        assertThat(outputStream.size()).isEqualTo(4);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
                (byte) 13);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(4);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
                (byte) 13);
    }

    @Test
    public void testWriteAll() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(4).bind(channel);
        stream.writeAll(new ByteArrayInputStream(new byte[]{77, 33, (byte) 155, 13}));
        stream.flush();
        final BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(33);
        assertThat(inputStream.read()).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(13);
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testWriteByte() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(77);
        stream.flush();
        assertThat(ByteChannel.inputStream(channel.next()).read()).isEqualTo(77);
        stream.write(31);
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(155);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }

    @Test
    public void testWriteByteArray() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(new byte[]{77, 33});
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(33);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(new byte[]{(byte) 155, 13});
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }

    @Test
    public void testWriteBytes() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        stream.write(new byte[]{1, 77, 33}, 1, 1);
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new byte[]{31, 17, 1}, 0, 2);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public void testWriteError() throws IOException {

        try {

            ByteChannel.byteChannel().bind(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel().bind(channel);
        final byte[] b = new byte[16];

        try {

            stream.write(null, 0, 2);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            stream.write(b, -1, 1);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            stream.write(b, 0, -1);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            stream.write(b, 8, 16);

            fail();

        } catch (final IndexOutOfBoundsException ignored) {

        }

        try {

            stream.write((byte[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            stream.write((InputStream) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            stream.writeAll(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        stream.write(new byte[0]);
        stream.flush();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
        stream.write(b, 8, 0);
        stream.flush();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
        stream.write(new ByteArrayInputStream(new byte[0]));
        stream.flush();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
        stream.writeAll(new ByteArrayInputStream(new byte[0]));
        stream.flush();
        assertThat(channel.eventuallyBreak().all()).isEmpty();
    }

    @Test
    public void testWriteInput() throws IOException {

        final IOChannel<ByteBuffer> channel = JRoutineCore.io().buildChannel();
        final BufferOutputStream stream = ByteChannel.byteChannel(4).bind(channel);
        stream.write(new ByteArrayInputStream(new byte[]{77, 33}));
        stream.flush();
        BufferInputStream inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(33);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new ByteArrayInputStream(new byte[]{31, 17}));
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(new ByteArrayInputStream(new byte[]{(byte) 155, 13}));
        stream.flush();
        inputStream = ByteChannel.inputStream(channel.next());
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }
}
