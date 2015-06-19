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
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.TemplateOutputConsumer;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportInput;
import com.gh.bmd.jrt.channel.TransportChannel.TransportOutput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 3/15/15.
 */
public class JRoutineChannels {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineChannels() {

    }

    /**
     * Returns a new channel transforming the input data into selectable ones.
     *
     * @param index   the channel index.
     * @param channel the selectable channel.
     * @param <INPUT> the input data type.
     * @return the input channel.
     */
    @Nonnull
    public static <INPUT> InputChannel<INPUT> asInputChannel(final int index,
            @Nullable final InputChannel<? extends Selectable<? super INPUT>> channel) {

        final TransportChannel<INPUT> transport = JRoutine.transport().buildChannel();

        if (channel != null) {

            transport.output().passTo(new InputOutputConsumer<INPUT>(index, channel));
        }

        return transport.input();
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the selectable output channel.
     * @param indexes  the list of indexes.
     * @param <OUTPUT> the output data type.
     * @return the channel map.
     */
    @Nonnull
    public static <OUTPUT> Map<Integer, OutputChannel<OUTPUT>> asOutputChannels(
            @Nonnull final OutputChannel<? extends Selectable<? extends OUTPUT>> channel,
            @Nonnull final Collection<Integer> indexes) {

        final int size = indexes.size();
        final HashMap<Integer, TransportInput<OUTPUT>> inputMap =
                new HashMap<Integer, TransportInput<OUTPUT>>(size);
        final HashMap<Integer, OutputChannel<OUTPUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUTPUT>>(size);

        for (final Integer index : indexes) {

            final TransportChannel<OUTPUT> transportChannel = JRoutine.transport().buildChannel();
            inputMap.put(index, transportChannel.input());
            outputMap.put(index, transportChannel.output());
        }

        channel.passTo(new SplitOutputConsumer<OUTPUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the outputs filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the selectable output channel.
     * @param indexes  the list of indexes.
     * @param <OUTPUT> the output data type.
     * @return the channel map.
     */
    @Nonnull
    public static <OUTPUT> Map<Integer, OutputChannel<OUTPUT>> asOutputChannels(
            @Nonnull final OutputChannel<? extends Selectable<? extends OUTPUT>> channel,
            @Nonnull final int... indexes) {

        final ArrayList<Integer> list = new ArrayList<Integer>(indexes.length);

        for (final int index : indexes) {

            list.add(index);
        }

        return asOutputChannels(channel, list);
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param index    the channel index.
     * @param channel  the channel to make selectable.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<Selectable<OUTPUT>> asSelectable(final int index,
            @Nullable final OutputChannel<? extends OUTPUT> channel) {

        final SelectableOutputConsumer<OUTPUT> consumer =
                new SelectableOutputConsumer<OUTPUT>(index);

        if (channel != null) {

            channel.passTo(consumer);
        }

        return consumer.getOutput();
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
    public static <OUTPUT> OutputChannel<Selectable<OUTPUT>> asSelectable(
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels cannot be empty");
        }

        final TransportChannel<Selectable<OUTPUT>> transport = JRoutine.transport().buildChannel();
        final TransportInput<Selectable<OUTPUT>> input = transport.input();
        int i = 0;

        for (final OutputChannel<? extends OUTPUT> channel : channels) {

            input.pass(asSelectable(i++, channel));
        }

        input.close();
        return transport.output();
    }

    /**
     * Data class storing information about the origin of the data.
     *
     * @param <DATA> the data type.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "this is an immutable data class")
    public static class Selectable<DATA> {

        /**
         * The data object.
         */
        public final DATA data;

        /**
         * The origin channel index.
         */
        public final int index;

        /**
         * Constructor.
         *
         * @param data  the data object.
         * @param index the channel index.
         */
        public Selectable(final DATA data, final int index) {

            this.data = data;
            this.index = index;
        }

        /**
         * Returns the data object casted to the specific type.
         *
         * @param <TYPE> the data type.
         * @return the data object.
         */
        @SuppressWarnings("unchecked")
        public <TYPE extends DATA> TYPE data() {

            return (TYPE) data;
        }
    }

    /**
     * Output consumer feeding a selectable input channel.
     *
     * @param <INPUT> the input data type.
     */
    private static class InputOutputConsumer<INPUT> extends TemplateOutputConsumer<INPUT> {

        private final InputChannel<Selectable<? super INPUT>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param index   the channel index.
         * @param channel the selectable input channel.
         */
        @SuppressWarnings("unchecked")
        private InputOutputConsumer(final int index,
                @Nonnull final InputChannel<? extends Selectable<? super INPUT>> channel) {

            mIndex = index;
            mChannel = (InputChannel<Selectable<? super INPUT>>) channel;
        }

        public void onError(@Nullable final Throwable error) {

            mChannel.abort(error);
        }

        public void onOutput(final INPUT input) {

            mChannel.pass(new Selectable<INPUT>(input, mIndex));
        }
    }

    /**
     * Output consumer making an output channel selectable.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUTPUT> implements OutputConsumer<OUTPUT> {

        private final int mIndex;

        private final TransportInput<Selectable<OUTPUT>> mInputChannel;

        private final TransportOutput<Selectable<OUTPUT>> mOutputChannel;

        /**
         * Constructor.
         *
         * @param index the channel index.
         */
        private SelectableOutputConsumer(final int index) {

            mIndex = index;
            final TransportChannel<Selectable<OUTPUT>> transportChannel =
                    JRoutine.transport().buildChannel();
            mInputChannel = transportChannel.input();
            mOutputChannel = transportChannel.output();
        }

        public void onComplete() {

            mInputChannel.close();
        }

        private OutputChannel<Selectable<OUTPUT>> getOutput() {

            return mOutputChannel;
        }

        public void onError(@Nullable final Throwable error) {

            mInputChannel.abort(error);
        }

        public void onOutput(final OUTPUT output) {

            mInputChannel.pass(new Selectable<OUTPUT>(output, mIndex));
        }
    }

    /**
     * Output consumer sorting the output data among a map of output channels.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class SplitOutputConsumer<OUTPUT>
            implements OutputConsumer<Selectable<? extends OUTPUT>> {

        private final HashMap<Integer, TransportInput<OUTPUT>> mChannelMap;

        /**
         * Constructor.
         *
         * @param channelMap the map of indexes and transport input channels.
         */
        private SplitOutputConsumer(
                @Nonnull final HashMap<Integer, TransportInput<OUTPUT>> channelMap) {

            mChannelMap = channelMap;
        }

        public void onComplete() {

            for (final TransportInput<OUTPUT> inputChannel : mChannelMap.values()) {

                inputChannel.close();
            }
        }

        public void onError(@Nullable final Throwable error) {

            for (final TransportInput<OUTPUT> inputChannel : mChannelMap.values()) {

                inputChannel.abort(error);
            }
        }

        public void onOutput(final Selectable<? extends OUTPUT> selectable) {

            final TransportInput<OUTPUT> inputChannel = mChannelMap.get(selectable.index);

            if (inputChannel != null) {

                inputChannel.pass(selectable.data);
            }
        }
    }
}
