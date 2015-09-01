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
package com.github.dm.jrt.android.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.TransportChannel;
import com.github.dm.jrt.core.JRoutine;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 06/18/2015.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class Channels extends com.github.dm.jrt.core.Channels {

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
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> mergeParcelable(
            final int startIndex,
            @Nonnull final List<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final TransportChannel<ParcelableSelectable<OUT>> transportChannel =
                JRoutine.transport().buildChannel();
        int i = startIndex;

        for (final OutputChannel<? extends OUT> channel : channels) {

            transportChannel.pass(toSelectable(channel, i++));
        }

        return transportChannel.close();
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
    public static OutputChannel<? extends ParcelableSelectable<?>> mergeParcelable(
            final int startIndex, @Nonnull final OutputChannel<?>... channels) {

        if (channels.length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final TransportChannel<ParcelableSelectable<Object>> transportChannel =
                JRoutine.transport().buildChannel();
        int i = startIndex;

        for (final OutputChannel<?> channel : channels) {

            transportChannel.pass(toSelectable(channel, i++));
        }

        return transportChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> mergeParcelable(
            @Nonnull final List<? extends OutputChannel<? extends OUT>> channels) {

        return mergeParcelable(0, channels);
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
    public static OutputChannel<? extends ParcelableSelectable<?>> mergeParcelable(
            @Nonnull final OutputChannel<?>... channels) {

        return mergeParcelable(0, channels);
    }

    /**
     * Returns a new channel transforming the input data into selectable ones.<br/>
     * Note that the returned channel must be closed in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the input channel.
     */
    @Nonnull
    public static <DATA, IN extends DATA> TransportChannel<IN> selectParcelable(
            @Nullable final InputChannel<? super ParcelableSelectable<DATA>> channel,
            final int index) {

        final TransportChannel<IN> inputChannel = JRoutine.transport().buildChannel();

        if (channel != null) {

            final TransportChannel<ParcelableSelectable<DATA>> transportChannel =
                    JRoutine.transport().buildChannel();
            transportChannel.passTo(channel);
            inputChannel.passTo(new SelectableInputConsumer<DATA, IN>(transportChannel, index));
        }

        return inputChannel;
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable output channel.
     */
    @Nonnull
    public static <OUT> OutputChannel<? extends ParcelableSelectable<OUT>> toSelectable(
            @Nullable final OutputChannel<? extends OUT> channel, final int index) {

        final TransportChannel<ParcelableSelectable<OUT>> transportChannel =
                JRoutine.transport().buildChannel();

        if (channel != null) {

            channel.passTo(new SelectableOutputConsumer<OUT>(transportChannel, index));
        }

        return transportChannel;
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

                    public ParcelableSelectable createFromParcel(@Nonnull final Parcel source) {

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
        protected ParcelableSelectable(@Nonnull final Parcel source) {

            super((DATA) source.readValue(ParcelableSelectable.class.getClassLoader()),
                  source.readInt());
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

            dest.writeValue(data);
            dest.writeInt(index);
        }
    }

    /**
     * Output consumer transforming input data into selectable ones.
     *
     * @param <DATA> the channel data type.
     * @param <IN>   the input data type.
     */
    private static class SelectableInputConsumer<DATA, IN extends DATA>
            implements OutputConsumer<IN> {

        private final TransportChannel<? super ParcelableSelectable<DATA>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the selectable channel.
         * @param index   the selectable index.
         */
        private SelectableInputConsumer(
                @Nonnull final TransportChannel<? super ParcelableSelectable<DATA>> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final IN input) {

            mChannel.pass(new ParcelableSelectable<DATA>(input, mIndex));
        }
    }

    /**
     * Output consumer transforming output data into selectable ones.
     *
     * @param <OUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUT> implements OutputConsumer<OUT> {

        private final TransportChannel<ParcelableSelectable<OUT>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the transport input channel.
         * @param index   the selectable index.
         */
        private SelectableOutputConsumer(
                @Nonnull final TransportChannel<ParcelableSelectable<OUT>> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final OUT output) {

            mChannel.pass(new ParcelableSelectable<OUT>(output, mIndex));
        }
    }
}
