package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builder implementation returning a map of channels accepting selectable data.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class InputMapBuilder<DATA, IN extends DATA> extends AbstractBuilder<Map<Integer, Channel<IN, ?>>> {

    private final Channel<? super Selectable<DATA>, ?> mChannel;

    private final HashSet<Integer> mIndexes;

    /**
     * Constructor.
     *
     * @param channel the selectable channel.
     * @param indexes the set of indexes.
     * @throws java.lang.NullPointerException if the specified set of indexes is null or contains a
     *                                        null object.
     */
    InputMapBuilder(@NotNull final Channel<? super Selectable<DATA>, ?> channel,
            @NotNull final Set<Integer> indexes) {
        mChannel = ConstantConditions.notNull("channel instance", channel);
        final HashSet<Integer> indexSet =
                new HashSet<Integer>(ConstantConditions.notNull("set of indexes", indexes));
        if (indexSet.contains(null)) {
            throw new NullPointerException("the set of indexes must not contain null objects");
        }

        mIndexes = indexSet;
    }

    @NotNull
    @Override
    protected Map<Integer, Channel<IN, ?>> build(
            @NotNull final ChannelConfiguration configuration) {
        final HashSet<Integer> indexes = mIndexes;
        final Channel<? super Selectable<DATA>, ?> channel = mChannel;
        final HashMap<Integer, Channel<IN, ?>> channelMap =
                new HashMap<Integer, Channel<IN, ?>>(indexes.size());
        for (final Integer index : indexes) {
            final Channel<IN, ?> inputChannel =
                    new InputSelectBuilder<DATA, IN>(channel, index).apply(configuration)
                                                                    .buildChannels();
            channelMap.put(index, inputChannel);
        }

        return channelMap;
    }
}
