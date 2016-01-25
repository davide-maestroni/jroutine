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

package com.github.dm.jrt.android.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.JRoutine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 06/18/2015.
 */
public class Channels extends com.github.dm.jrt.core.Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will start from
     * the specified one.<br/>
     * Note that the passed channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> merge(
            final int startIndex,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final IOChannel<ParcelableSelectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        int i = startIndex;
        for (final OutputChannel<? extends OUT> channel : channels) {
            ioChannel.pass(toSelectable(channel, i++));
        }

        return ioChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will start from
     * the specified one.<br/>
     * Note that the passed channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> merge(
            final int startIndex, @NotNull final OutputChannel<?>... channels) {

        if (channels.length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<ParcelableSelectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        int i = startIndex;
        for (final OutputChannel<?> channel : channels) {
            ioChannel.pass(toSelectable((OutputChannel<? extends OUT>) channel, i++));
        }

        return ioChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will be the same
     * as the list ones.<br/>
     * Note that the passed channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> merge(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return merge(0, channels);
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will be the same
     * as the array ones.<br/>
     * Note that the passed channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Returns a new channel transforming the input data into selectable ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the I/O channel.
     */
    @NotNull
    public static <DATA, IN extends DATA> IOChannel<IN> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            final int index) {

        final IOChannel<IN> inputChannel = JRoutine.io().buildChannel();
        final IOChannel<ParcelableSelectable<DATA>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(channel);
        return inputChannel.passTo(new SelectableOutputConsumer<DATA, IN>(ioChannel, index));
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that the passed channel will be bound as a result of the call.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable output channel.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        final IOChannel<ParcelableSelectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        channel.passTo(new SelectableOutputConsumer<OUT, OUT>(ioChannel, index));
        return ioChannel;
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
        protected ParcelableSelectable(@NotNull final Parcel source) {

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
     * Output consumer transforming data into selectable ones.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUT, IN extends OUT>
            implements OutputConsumer<IN> {

        private final IOChannel<? super
                ParcelableSelectable<OUT>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the selectable channel.
         * @param index   the selectable index.
         */
        private SelectableOutputConsumer(@NotNull final IOChannel<? super
                ParcelableSelectable<OUT>> channel, final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@NotNull final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final IN input) {

            mChannel.pass(new ParcelableSelectable<OUT>(input, mIndex));
        }
    }
}
