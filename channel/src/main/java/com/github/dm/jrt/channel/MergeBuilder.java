package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
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
class MergeBuilder<OUT> extends AbstractBuilder<Channel<?, ? extends Selectable<OUT>>> {

    private final ArrayList<Channel<?, ? extends OUT>> mChannels;

    private final int mStartIndex;

    /**
     * Constructor.
     *
     * @param startIndex the selectable start index.
     * @param channels   the channels to merge.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    MergeBuilder(final int startIndex,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        final ArrayList<Channel<?, ? extends OUT>> channelList =
                new ArrayList<Channel<?, ? extends OUT>>();
        for (final Channel<?, ? extends OUT> channel : channels) {
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
    protected Channel<?, ? extends Selectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {
        final Channel<Selectable<OUT>, Selectable<OUT>> outputChannel = JRoutineCore.io()
                                                                                    .channelConfiguration()
                                                                                    .with(configuration)
                                                                                    .applied()
                                                                                    .buildChannel();
        int i = mStartIndex;
        for (final Channel<?, ? extends OUT> channel : mChannels) {
            outputChannel.pass(new SelectableOutputBuilder<OUT>(channel, i++).channelConfiguration()
                                                                             .with(configuration)
                                                                             .applied()
                                                                             .buildChannels());
        }

        return outputChannel.close();
    }
}
