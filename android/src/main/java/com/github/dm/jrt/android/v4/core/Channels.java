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
package com.github.dm.jrt.android.v4.core;

import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.TransportChannel;

import java.util.Collection;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 03/08/15.
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
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels must be closed in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the collection of indexes.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the map of indexes and output channels.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> SparseArrayCompat<TransportChannel<INPUT>>
    mapParcelable(
            @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @Nonnull final Collection<Integer> indexes) {

        final int size = indexes.size();
        final SparseArrayCompat<TransportChannel<INPUT>> channelMap =
                new SparseArrayCompat<TransportChannel<INPUT>>(size);

        for (final Integer index : indexes) {

            channelMap.append(index, Channels.<DATA, INPUT>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels must be closed in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the map of indexes and output channels.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> SparseArrayCompat<TransportChannel<INPUT>>
    mapParcelable(
            @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @Nonnull final int... indexes) {

        final int size = indexes.length;
        final SparseArrayCompat<TransportChannel<INPUT>> channelMap =
                new SparseArrayCompat<TransportChannel<INPUT>>(size);

        for (final int index : indexes) {

            channelMap.append(index, Channels.<DATA, INPUT>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels must be closed in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <DATA>     the channel data type.
     * @param <INPUT>    the input data type.
     * @return the map of indexes and output channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> SparseArrayCompat<TransportChannel<INPUT>>
    mapParcelable(
            final int startIndex, final int rangeSize,
            @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final SparseArrayCompat<TransportChannel<INPUT>> channelMap =
                new SparseArrayCompat<TransportChannel<INPUT>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            channelMap.append(index, Channels.<DATA, INPUT>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channelMap the map of indexes and output channels.
     * @param <OUTPUT>   the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends ParcelableSelectable<OUTPUT>> mergeParcelable(
            @Nonnull final SparseArrayCompat<? extends OutputChannel<? extends OUTPUT>>
                    channelMap) {

        final int size = channelMap.size();

        if (size == 0) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final TransportChannel<ParcelableSelectable<OUTPUT>> transportChannel =
                JRoutine.transport().buildChannel();

        for (int i = 0; i < size; i++) {

            transportChannel.pass(toSelectable(channelMap.valueAt(i), channelMap.keyAt(i)));
        }

        return transportChannel.close();
    }
}
