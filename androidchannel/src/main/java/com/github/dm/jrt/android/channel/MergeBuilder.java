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
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder implementation merging data from a set of output channels into selectable objects.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeBuilder<OUT>
        extends AbstractBuilder<OutputChannel<? extends ParcelableSelectable<OUT>>> {

    private final ArrayList<OutputChannel<? extends OUT>> mChannels;

    private final int mStartIndex;

    /**
     * Constructor.
     *
     * @param startIndex the selectable start index.
     * @param channels   the input channels to merge.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    MergeBuilder(final int startIndex,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        final ArrayList<OutputChannel<? extends OUT>> channelList =
                new ArrayList<OutputChannel<? extends OUT>>(channels);
        if (channelList.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        mStartIndex = startIndex;
        mChannels = channelList;
    }

    @NotNull
    @Override
    protected OutputChannel<? extends ParcelableSelectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final IOChannel<ParcelableSelectable<OUT>> ioChannel = JRoutineCore.io()
                                                                           .withChannels()
                                                                           .with(configuration)
                                                                           .setConfiguration()
                                                                           .buildChannel();
        int i = mStartIndex;
        for (final OutputChannel<? extends OUT> channel : mChannels) {
            ioChannel.pass(AndroidChannels.toSelectable(channel, i++).buildChannels());
        }

        return ioChannel.close();
    }
}
