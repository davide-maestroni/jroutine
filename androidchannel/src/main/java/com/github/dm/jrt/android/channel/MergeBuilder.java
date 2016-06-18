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

package com.github.dm.jrt.android.channel;

import com.github.dm.jrt.channel.AbstractBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation merging data from a set of channels into selectable objects.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeBuilder<OUT> extends AbstractBuilder<Channel<?, ? extends ParcelableSelectable<OUT>>> {

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
    protected Channel<?, ? extends ParcelableSelectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {
        final Channel<ParcelableSelectable<OUT>, ParcelableSelectable<OUT>> outputChannel =
                JRoutineCore.io().channelConfiguration().with(configuration).apply().buildChannel();
        int i = mStartIndex;
        for (final Channel<?, ? extends OUT> channel : mChannels) {
            outputChannel.pass(AndroidChannels.selectableOutput(channel, i++).buildChannels());
        }

        return outputChannel.close();
    }
}
