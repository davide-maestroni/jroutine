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

import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.core.RecyclerByteChannel.ConcatRecyclerInputStream;
import com.gh.bmd.jrt.core.RecyclerByteChannel.RecyclerInputStream;
import com.gh.bmd.jrt.core.RecyclerByteChannel.RecyclerOutputStream;

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
 * Recycler channel unit tests.
 * <p/>
 * Created by davide-maestroni on 29/08/15.
 */
public class RecyclerByteChannelTest {

    @Test
    public void testAvailable() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        final byte[] b = new byte[16];
        stream.write(b);
        stream.close();
        final RecyclerInputStream inputStream = channel.next();
        assertThat(inputStream.available()).isEqualTo(16);
        assertThat(inputStream.read()).isNotEqualTo(-1);
        assertThat(inputStream.available()).isEqualTo(15);
        assertThat(inputStream.read(new byte[16], 4, 8)).isEqualTo(8);
        assertThat(inputStream.available()).isEqualTo(7);
        assertThat(inputStream.skip(4)).isEqualTo(4);
        assertThat(inputStream.available()).isEqualTo(3);
    }

    @Test
    public void testChannelError() {

        try {

            Channels.byteChannel(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testConcatAvailable() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(8).passTo(channel);
        final byte[] b = new byte[16];
        stream.write(b);
        stream.close();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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
    public void testConcatClose() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(2).passTo(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
        inputStream.close();

        try {

            inputStream.read();

            fail();

        } catch (final IllegalStateException ignored) {

        }
    }

    @Test
    public void testConcatMark() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(4).passTo(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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
    public void testConcatReadByte() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(2).passTo(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
        assertThat(inputStream.read()).isEqualTo(31);
        assertThat(inputStream.read()).isEqualTo(17);
        assertThat(inputStream.read()).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(13);
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testConcatReadByteArray() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(2).passTo(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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
    public void testConcatReadBytes() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(3).passTo(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(2).passTo(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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

        assertThat(inputStream.read(new byte[0])).isEqualTo(0);
        assertThat(inputStream.read(b, 8, 0)).isEqualTo(0);
    }

    @Test
    public void testConcatReadOutput() throws IOException {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(3).passTo(channel);
        stream.write(new byte[]{31, 17, (byte) 155, 13});
        stream.flush();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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
    public void testConcatSkip() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(4).passTo(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final ConcatRecyclerInputStream inputStream =
                RecyclerByteChannel.concat(channel.next(), channel.next());
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
    public void testDataPool() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(31);
        stream.flush();
        final RecyclerInputStream inputStream = channel.next();
        inputStream.close();
        stream.write(77);
        stream.flush();
        assertThat(channel.next()).isSameAs(inputStream);
    }

    @Test
    public void testDataPoolZero() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream =
                Channels.byteChannel(RecyclerByteChannel.DEFAULT_BUFFER_SIZE, 0).passTo(channel);
        stream.write(31);
        stream.flush();
        final RecyclerInputStream inputStream = channel.next();
        inputStream.close();
        stream.write(77);
        stream.flush();
        assertThat(channel.next()).isNotSameAs(inputStream);
    }

    @Test
    public void testMark() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final RecyclerInputStream inputStream = channel.next();
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
    public void testReadByte() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(77);
        stream.flush();
        assertThat(channel.next().read()).isEqualTo(77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        assertThat(inputStream.read()).isEqualTo(31);
        assertThat(inputStream.read()).isEqualTo(17);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = channel.next();
        assertThat(inputStream.read()).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(13);
        assertThat(inputStream.read()).isEqualTo(-1);
    }

    @Test
    public void testReadByteArray() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(77);
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 77);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = channel.next();
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = channel.next();
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 13);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 155);
        assertThat(b[1]).isEqualTo((byte) 13);
        stream.write(new byte[]{11, 111});
        stream.flush();
        inputStream = channel.next();
        b = new byte[1];
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 11);
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 111);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 111);
    }

    @Test
    public void testReadBytes() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(77);
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b, 0, 2)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 77);
        assertThat(inputStream.read(b, 0, 4)).isEqualTo(-1);
        assertThat(b[0]).isEqualTo((byte) 77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = channel.next();
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
        inputStream = channel.next();
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
        inputStream = channel.next();
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

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(77);
        stream.flush();
        final RecyclerInputStream inputStream = channel.next();
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

        assertThat(inputStream.read(new byte[0])).isEqualTo(0);
        assertThat(inputStream.read(b, 8, 0)).isEqualTo(0);
    }

    @Test
    public void testReadOutput() throws IOException {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(77);
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(1);
        assertThat(outputStream.size()).isEqualTo(1);
        assertThat(outputStream.toByteArray()[0]).isEqualTo((byte) 77);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(1);
        assertThat(outputStream.toByteArray()[0]).isEqualTo((byte) 77);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = channel.next();
        outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(2);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = channel.next();
        outputStream = new ByteArrayOutputStream();
        assertThat(inputStream.read(outputStream)).isEqualTo(2);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 155, (byte) 13);
        assertThat(inputStream.read(outputStream)).isEqualTo(-1);
        assertThat(outputStream.size()).isEqualTo(2);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 155, (byte) 13);
    }

    @Test
    public void testSkip() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        final byte[] b =
                new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7,
                           (byte) 8};
        stream.write(b);
        stream.close();
        final RecyclerInputStream inputStream = channel.next();
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

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(2).passTo(channel);
        stream.write(1);
        stream.write(2);
        stream.write(new byte[]{3, 4, 5});
        stream.write(new byte[]{4, 5, 6, 7, 8, 9}, 2, 3);
        stream.write(new ByteArrayInputStream(new byte[]{9, 10}));
        stream.close();
        final List<RecyclerInputStream> inputStreams = channel.close().all();
        assertThat(inputStreams).hasSize(5);
        final byte[] b = new byte[10];
        assertThat(RecyclerByteChannel.concat(inputStreams).read(b)).isEqualTo(10);
        assertThat(b).containsExactly((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6,
                                      (byte) 7, (byte) 8, (byte) 9, (byte) 10);
    }

    @Test
    public void testStreamCache() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerByteChannel byteChannel = Channels.byteChannel();
        final RecyclerOutputStream stream = byteChannel.passTo(channel);
        assertThat(byteChannel.passTo(channel)).isSameAs(stream);
    }

    @Test
    public void testWriteByte() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(77);
        stream.flush();
        assertThat(channel.next().read()).isEqualTo(77);
        stream.write(31);
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(155);
        stream.flush();
        inputStream = channel.next();
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(1);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }

    @Test
    public void testWriteByteArray() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(new byte[]{77, 33});
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(33);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new byte[]{31, 17});
        stream.flush();
        inputStream = channel.next();
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(new byte[]{(byte) 155, 13});
        stream.flush();
        inputStream = channel.next();
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }

    @Test
    public void testWriteBytes() {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
        stream.write(new byte[]{1, 77, 33}, 1, 1);
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new byte[]{31, 17, 1}, 0, 2);
        stream.flush();
        inputStream = channel.next();
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(new byte[]{1, (byte) 155, 13}, 1, 2);
        stream.flush();
        inputStream = channel.next();
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

            Channels.byteChannel().passTo(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel().passTo(channel);
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

        stream.write(new byte[0]);
        stream.flush();
        assertThat(channel.eventuallyExit().all()).isEmpty();
        stream.write(b, 8, 0);
        stream.flush();
        assertThat(channel.eventuallyExit().all()).isEmpty();
    }

    @Test
    public void testWriteInput() throws IOException {

        final TransportChannel<RecyclerInputStream> channel = JRoutine.transport().buildChannel();
        final RecyclerOutputStream stream = Channels.byteChannel(4).passTo(channel);
        stream.write(new ByteArrayInputStream(new byte[]{77, 33}));
        stream.flush();
        RecyclerInputStream inputStream = channel.next();
        assertThat(inputStream.read()).isEqualTo(77);
        assertThat(inputStream.read()).isEqualTo(33);
        assertThat(inputStream.read()).isEqualTo(-1);
        stream.write(new ByteArrayInputStream(new byte[]{31, 17}));
        stream.flush();
        inputStream = channel.next();
        final byte[] b = new byte[16];
        assertThat(inputStream.read(b)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 17);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
        stream.write(new ByteArrayInputStream(new byte[]{(byte) 155, 13}));
        stream.flush();
        inputStream = channel.next();
        assertThat(inputStream.read(b, 1, 3)).isEqualTo(2);
        assertThat(b[0]).isEqualTo((byte) 31);
        assertThat(b[1]).isEqualTo((byte) 155);
        assertThat(b[2]).isEqualTo((byte) 13);
        assertThat(inputStream.read()).isEqualTo(-1);
        assertThat(inputStream.read(b)).isEqualTo(-1);
        assertThat(inputStream.read(b, 3, 3)).isEqualTo(-1);
    }
}
