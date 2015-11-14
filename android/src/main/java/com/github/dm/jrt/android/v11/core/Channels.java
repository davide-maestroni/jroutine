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

import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;

import org.jetbrains.annotations.NotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 08/03/2015.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class Channels extends com.github.dm.jrt.android.core.Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
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

        final IOChannel<ParcelableSelectable<OUT>, ParcelableSelectable<OUT>> ioChannel =
                JRoutine.io().buildChannel();

        for (int i = 0; i < size; ++i) {

            ioChannel.pass(toSelectable(channelMap.valueAt(i), channelMap.keyAt(i)));
        }

        return ioChannel.close();
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> SparseArray<IOChannel<IN, IN>> spread(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final int... indexes) {

        final int size = indexes.length;
        final SparseArray<IOChannel<IN, IN>> channelMap = new SparseArray<IOChannel<IN, IN>>(size);

        for (final int index : indexes) {

            channelMap.append(index, Channels.<DATA, IN>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> SparseArray<IOChannel<IN, IN>> spread(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final SparseArray<IOChannel<IN, IN>> channelMap = new SparseArray<IOChannel<IN, IN>>();

        for (final Integer index : indexes) {

            channelMap.append(index, Channels.<DATA, IN>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
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
    public static <DATA, IN extends DATA> SparseArray<IOChannel<IN, IN>> spread(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final SparseArray<IOChannel<IN, IN>> channelMap =
                new SparseArray<IOChannel<IN, IN>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            channelMap.append(index, Channels.<DATA, IN>selectParcelable(channel, index));
        }

        return channelMap;
    }
}
