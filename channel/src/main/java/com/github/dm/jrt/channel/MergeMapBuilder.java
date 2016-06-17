package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder implementation returning a channel merging data from a map of output channels.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeMapBuilder<OUT> extends AbstractBuilder<OutputChannel<? extends Selectable<OUT>>> {

    private final HashMap<Integer, OutputChannel<? extends OUT>> mChannelMap;

    /**
     * Constructor.
     *
     * @param channels the map of channels to merge.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     */
    MergeMapBuilder(@NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channels) {
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, OutputChannel<? extends OUT>> channelMap =
                new HashMap<Integer, OutputChannel<? extends OUT>>(channels);
        if (channelMap.containsValue(null)) {
            throw new NullPointerException("the map of channels must not contain null objects");
        }

        mChannelMap = channelMap;
    }

    @NotNull
    @Override
    protected OutputChannel<? extends Selectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {
        final IOChannel<Selectable<OUT>> ioChannel =
                JRoutineCore.io().channelConfiguration().with(configuration).apply().buildChannel();
        for (final Entry<Integer, ? extends OutputChannel<? extends OUT>> entry : mChannelMap
                .entrySet()) {
            ioChannel.pass(new SelectableOutputBuilder<OUT>(entry.getValue(),
                    entry.getKey()).channelConfiguration()
                                   .with(configuration)
                                   .apply()
                                   .buildChannels());
        }

        return ioChannel.close();
    }
}
