package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation merging data from a set of output channels into selectable objects.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeBuilder<OUT> extends AbstractBuilder<OutputChannel<? extends Selectable<OUT>>> {

    private final ArrayList<OutputChannel<? extends OUT>> mChannels;

    private final int mStartIndex;

    /**
     * Constructor.
     *
     * @param startIndex the selectable start index.
     * @param channels   the input channels to merge.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    MergeBuilder(final int startIndex,
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        final ArrayList<OutputChannel<? extends OUT>> channelList =
                new ArrayList<OutputChannel<? extends OUT>>();
        for (final OutputChannel<? extends OUT> channel : channels) {
            if (channel == null) {
                throw new NullPointerException(
                        "the collection of channels must not contain null objects");
            }

            channelList.add(channel);
        }

        if (channelList.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        mStartIndex = startIndex;
        mChannels = channelList;
    }

    @NotNull
    @Override
    protected OutputChannel<? extends Selectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final IOChannel<Selectable<OUT>> ioChannel = JRoutineCore.io()
                                                                 .channelConfiguration()
                                                                 .with(configuration)
                                                                 .applyConfiguration()
                                                                 .buildChannel();
        int i = mStartIndex;
        for (final OutputChannel<? extends OUT> channel : mChannels) {
            ioChannel.pass(new SelectableOutputBuilder<OUT>(channel, i++).channelConfiguration()
                                                                         .with(configuration)
                                                                         .applyConfiguration()
                                                                         .buildChannels());
        }

        return ioChannel.close();
    }
}
