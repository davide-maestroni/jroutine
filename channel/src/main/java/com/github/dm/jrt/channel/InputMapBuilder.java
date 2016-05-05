package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builder implementation returning a map of input channels accepting selectable data.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class InputMapBuilder<DATA, IN extends DATA> extends AbstractBuilder<Map<Integer, IOChannel<IN>>> {

    private final InputChannel<? super Selectable<DATA>> mChannel;

    private final HashSet<Integer> mIndexes;

    /**
     * Constructor.
     *
     * @param channel the selectable channel.
     * @param indexes the set of indexes.
     * @throws java.lang.NullPointerException if the specified set of indexes is null or contains a
     *                                        null object.
     */
    InputMapBuilder(@NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Set<Integer> indexes) {

        mChannel = ConstantConditions.notNull("input channel", channel);
        final HashSet<Integer> indexSet =
                new HashSet<Integer>(ConstantConditions.notNull("set of indexes", indexes));
        if (indexSet.contains(null)) {
            throw new NullPointerException("the set of indexes must not contain null objects");
        }

        mIndexes = indexSet;
    }

    @NotNull
    @Override
    protected Map<Integer, IOChannel<IN>> build(@NotNull final ChannelConfiguration configuration) {

        final HashSet<Integer> indexes = mIndexes;
        final InputChannel<? super Selectable<DATA>> channel = mChannel;
        final HashMap<Integer, IOChannel<IN>> channelMap =
                new HashMap<Integer, IOChannel<IN>>(indexes.size());
        for (final Integer index : indexes) {
            final IOChannel<IN> ioChannel =
                    new InputSelectBuilder<DATA, IN>(channel, index).channelConfiguration()
                                                                    .with(configuration)
                                                                    .apply()
                                                                    .buildChannels();
            channelMap.put(index, ioChannel);
        }

        return channelMap;
    }
}
