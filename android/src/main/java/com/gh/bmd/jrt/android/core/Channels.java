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
package com.gh.bmd.jrt.android.core;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.SparseArrayCompat;

import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportInput;
import com.gh.bmd.jrt.core.JRoutine;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 6/18/15.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class Channels extends com.gh.bmd.jrt.core.Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     *
     * @param channel the selectable channel.
     * @param indexes the collection of indexes.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the map of indexes and output channels.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> SparseArrayCompat<InputChannel<INPUT>> mapParcelable(
            @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @Nonnull final Collection<Integer> indexes) {

        final int size = indexes.size();
        final SparseArrayCompat<InputChannel<INPUT>> channelMap =
                new SparseArrayCompat<InputChannel<INPUT>>(size);

        for (final Integer index : indexes) {

            channelMap.append(index, Channels.<DATA, INPUT>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the map of indexes and output channels.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> SparseArrayCompat<InputChannel<INPUT>> mapParcelable(
            @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @Nonnull final int... indexes) {

        final int size = indexes.length;
        final SparseArrayCompat<InputChannel<INPUT>> channelMap =
                new SparseArrayCompat<InputChannel<INPUT>>(size);

        for (final int index : indexes) {

            channelMap.append(index, Channels.<DATA, INPUT>selectParcelable(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
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
    public static <DATA, INPUT extends DATA> SparseArrayCompat<InputChannel<INPUT>> mapParcelable(
            final int startIndex, final int rangeSize,
            @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final SparseArrayCompat<InputChannel<INPUT>> channelMap =
                new SparseArrayCompat<InputChannel<INPUT>>(rangeSize);

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
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUTPUT>   the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends ParcelableSelectable<OUTPUT>> mergeParcelable(
            final int startIndex,
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final TransportChannel<ParcelableSelectable<OUTPUT>> transport =
                JRoutine.transport().buildChannel();
        final TransportInput<ParcelableSelectable<OUTPUT>> input = transport.input();
        int i = startIndex;

        for (final OutputChannel<? extends OUTPUT> channel : channels) {

            input.pass(toSelectable(channel, i++));
        }

        input.close();
        return transport.output();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static OutputChannel<? extends ParcelableSelectable<Object>> mergeParcelable(
            final int startIndex, @Nonnull final OutputChannel<?>... channels) {

        if (channels.length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final TransportChannel<ParcelableSelectable<Object>> transport =
                JRoutine.transport().buildChannel();
        final TransportInput<ParcelableSelectable<Object>> input = transport.input();
        int i = startIndex;

        for (final OutputChannel<?> channel : channels) {

            input.pass(toSelectable(channel, i++));
        }

        input.close();
        return transport.output();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends ParcelableSelectable<OUTPUT>> mergeParcelable(
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        return mergeParcelable(0, channels);
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
            @Nonnull final Map<Integer, ? extends OutputChannel<? extends OUTPUT>> channelMap) {

        if (channelMap.isEmpty()) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final TransportChannel<ParcelableSelectable<OUTPUT>> transport =
                JRoutine.transport().buildChannel();
        final TransportInput<ParcelableSelectable<OUTPUT>> input = transport.input();

        for (final Entry<Integer, ? extends OutputChannel<? extends OUTPUT>> entry : channelMap
                .entrySet()) {

            input.pass(toSelectable(entry.getValue(), entry.getKey()));
        }

        input.close();
        return transport.output();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static OutputChannel<? extends ParcelableSelectable<Object>> mergeParcelable(
            @Nonnull final OutputChannel<?>... channels) {

        return mergeParcelable(0, channels);
    }

    /**
     * Returns a new channel transforming the input data into selectable ones.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the input channel.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> InputChannel<INPUT> selectParcelable(
            @Nullable final InputChannel<? super ParcelableSelectable<DATA>> channel,
            final int index) {

        final TransportChannel<INPUT> transport = JRoutine.transport().buildChannel();

        if (channel != null) {

            transport.output().passTo(new SelectableInputConsumer<DATA, INPUT>(channel, index));
        }

        return transport.input();
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the channel to make selectable.
     * @param index    the channel index.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends ParcelableSelectable<OUTPUT>> toSelectable(
            @Nullable final OutputChannel<? extends OUTPUT> channel, final int index) {

        final TransportChannel<ParcelableSelectable<OUTPUT>> transport =
                JRoutine.transport().buildChannel();

        if (channel != null) {

            channel.passTo(new SelectableOutputConsumer<OUTPUT>(transport.input(), index));
        }

        return transport.output();
    }

    /**
     * Data class storing information about the origin of the data.
     *
     * @param <DATA> the data type.
     */
    public static class ParcelableSelectable<DATA> extends Selectable<DATA> implements Parcelable {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ParcelableSelectable> CREATOR =
                new Creator<ParcelableSelectable>() {

                    public ParcelableSelectable createFromParcel(final Parcel source) {

                        return new ParcelableSelectable(source);
                    }

                    public ParcelableSelectable[] newArray(final int size) {

                        return new ParcelableSelectable[size];
                    }
                };

        /**
         * Constructor.
         *
         * @param data  the data object.
         * @param index the channel index.
         */
        public ParcelableSelectable(final DATA data, final int index) {

            super(data, index);
        }

        /**
         * Constructor.
         *
         * @param source the source parcel.
         */
        @SuppressWarnings("unchecked")
        protected ParcelableSelectable(final Parcel source) {

            super((DATA) source.readValue(ParcelableSelectable.class.getClassLoader()),
                  source.readInt());
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeValue(data);
            dest.writeInt(index);
        }
    }

    /**
     * Output consumer transforming input data into selectable ones.
     *
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     */
    private static class SelectableInputConsumer<DATA, INPUT extends DATA>
            implements OutputConsumer<INPUT> {

        private final int mIndex;

        private final InputChannel<? super ParcelableSelectable<DATA>> mInputChannel;

        /**
         * Constructor.
         *
         * @param inputChannel the selectable channel.
         * @param index        the selectable index.
         */
        private SelectableInputConsumer(
                @Nonnull final InputChannel<? super ParcelableSelectable<DATA>> inputChannel,
                final int index) {

            mInputChannel = inputChannel;
            mIndex = index;
        }

        public void onComplete() {

        }

        public void onError(@Nullable final Throwable error) {

            mInputChannel.abort(error);
        }

        public void onOutput(final INPUT input) {

            mInputChannel.pass(new ParcelableSelectable<DATA>(input, mIndex));
        }
    }

    /**
     * Output consumer transforming output data into selectable ones.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUTPUT> implements OutputConsumer<OUTPUT> {

        private final int mIndex;

        private final TransportInput<ParcelableSelectable<OUTPUT>> mInputChannel;

        /**
         * Constructor.
         *
         * @param inputChannel the transport input channel.
         * @param index        the selectable index.
         */
        private SelectableOutputConsumer(
                @Nonnull final TransportInput<ParcelableSelectable<OUTPUT>> inputChannel,
                final int index) {

            mInputChannel = inputChannel;
            mIndex = index;
        }

        public void onComplete() {

            mInputChannel.close();
        }

        public void onError(@Nullable final Throwable error) {

            mInputChannel.abort(error);
        }

        public void onOutput(final OUTPUT output) {

            mInputChannel.pass(new ParcelableSelectable<OUTPUT>(output, mIndex));
        }
    }
}
