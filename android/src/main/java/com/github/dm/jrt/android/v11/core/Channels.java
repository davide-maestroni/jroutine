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
package com.github.dm.jrt.android.v11.core;

import android.util.SparseArray;

import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 08/03/2015.
 */
public class Channels extends com.github.dm.jrt.android.core.Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Combines the specified channels into a selectable one.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the map of indexes and input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> IOChannel<Selectable<? extends IN>> combine(
            @NotNull final SparseArray<? extends InputChannel<? extends IN>> channels) {

        final int size = channels.size();

        if (size == 0) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final SparseArray<IOChannel<?>> channelMap = new SparseArray<IOChannel<?>>(size);

        for (int i = 0; i < size; ++i) {

            final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channels.valueAt(i)));
            channelMap.put(channels.keyAt(i), ioChannel);
        }

        final IOChannel<Selectable<? extends IN>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingMapOutputConsumer(channelMap));
        return ioChannel;
    }

    /**
     * Merges the specified channels into a selectable one.<br/>
     * Note that the passed channels will be bound as a result of the call.
     *
     * @param channelMap the map of indexes and output channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> merge(
            @NotNull final SparseArray<? extends OutputChannel<? extends OUT>> channelMap) {

        final int size = channelMap.size();

        if (size == 0) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final IOChannel<ParcelableSelectable<OUT>> ioChannel = JRoutine.io().buildChannel();

        for (int i = 0; i < size; ++i) {

            ioChannel.pass(toSelectable(channelMap.valueAt(i), channelMap.keyAt(i)));
        }

        return ioChannel.close();
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> SparseArray<IOChannel<IN>> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final int... indexes) {

        final int size = indexes.length;
        final SparseArray<IOChannel<IN>> channelMap = new SparseArray<IOChannel<IN>>(size);

        for (final int index : indexes) {

            channelMap.append(index, Channels.<DATA, IN>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> SparseArray<IOChannel<IN>> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final SparseArray<IOChannel<IN>> channelMap = new SparseArray<IOChannel<IN>>();

        for (final Integer index : indexes) {

            channelMap.append(index, Channels.<DATA, IN>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <DATA>     the channel data type.
     * @param <IN>       the input data type.
     * @return the map of indexes and I/O channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @NotNull
    public static <DATA, IN extends DATA> SparseArray<IOChannel<IN>> selectParcelable(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final SparseArray<IOChannel<IN>> channelMap = new SparseArray<IOChannel<IN>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            channelMap.append(index, Channels.<DATA, IN>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <br/>
     * Note that the passed channel will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <OUT>      the output data type.
     * @return the map of indexes and output channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @NotNull
    public static <OUT> SparseArray<OutputChannel<OUT>> selectParcelable(final int startIndex,
            final int rangeSize,
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final SparseArray<IOChannel<OUT>> inputMap = new SparseArray<IOChannel<OUT>>(rangeSize);
        final SparseArray<OutputChannel<OUT>> outputMap =
                new SparseArray<OutputChannel<OUT>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            final Integer integer = index;
            final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(integer, ioChannel);
            outputMap.put(integer, ioChannel);
        }

        channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the outputs filtered by the specified indexes.
     * <br/>
     * Note that the passed channel will be bound as a result of the call.
     *
     * @param channel the selectable output channel.
     * @param indexes the list of indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels.
     */
    @NotNull
    public static <OUT> SparseArray<OutputChannel<OUT>> selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final int... indexes) {

        final int size = indexes.length;
        final SparseArray<IOChannel<OUT>> inputMap = new SparseArray<IOChannel<OUT>>(size);
        final SparseArray<OutputChannel<OUT>> outputMap = new SparseArray<OutputChannel<OUT>>(size);

        for (final Integer index : indexes) {

            final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(index, ioChannel);
            outputMap.put(index, ioChannel);
        }

        channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <br/>
     * Note that the passed channel will be bound as a result of the call.
     *
     * @param channel the selectable output channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels.
     */
    @NotNull
    public static <OUT> SparseArray<OutputChannel<OUT>> selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final SparseArray<IOChannel<OUT>> inputMap = new SparseArray<IOChannel<OUT>>();
        final SparseArray<OutputChannel<OUT>> outputMap = new SparseArray<OutputChannel<OUT>>();

        for (final Integer index : indexes) {

            final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(index, ioChannel);
            outputMap.put(index, ioChannel);
        }

        channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Output consumer sorting the output data among a map of channels.
     *
     * @param <OUT> the output data type.
     */
    private static class SortingMapOutputConsumer<OUT>
            implements OutputConsumer<ParcelableSelectable<? extends OUT>> {

        private final SparseArray<IOChannel<OUT>> mChannels;

        /**
         * Constructor.
         *
         * @param channels the map of indexes and I/O channels.
         */
        private SortingMapOutputConsumer(@NotNull final SparseArray<IOChannel<OUT>> channels) {

            mChannels = channels;
        }

        public void onComplete() {

            final SparseArray<IOChannel<OUT>> channels = mChannels;
            final int size = channels.size();

            for (int i = 0; i < size; ++i) {

                channels.valueAt(i).close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            final SparseArray<IOChannel<OUT>> channels = mChannels;
            final int size = channels.size();

            for (int i = 0; i < size; ++i) {

                channels.valueAt(i).abort(error);
            }
        }

        public void onOutput(final ParcelableSelectable<? extends OUT> selectable) {

            final IOChannel<OUT> channel = mChannels.get(selectable.index);

            if (channel != null) {

                channel.pass(selectable.data);
            }
        }
    }
}
