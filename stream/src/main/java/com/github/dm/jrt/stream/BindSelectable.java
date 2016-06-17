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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Selectable binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class BindSelectable<OUT> implements Function<Channel<?, OUT>, Channel<?, Selectable<OUT>>> {

    private final ChannelConfiguration mConfiguration;

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param index         the selectable index.
     */
    BindSelectable(@NotNull final ChannelConfiguration configuration, final int index) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mIndex = index;
    }

    @SuppressWarnings("unchecked")
    public Channel<?, Selectable<OUT>> apply(final Channel<?, OUT> channel) {
        final Channel<?, ? extends Selectable<OUT>> outputChannel =
                Channels.selectableOutput(channel, mIndex)
                        .channelConfiguration()
                        .with(mConfiguration)
                        .apply()
                        .buildChannels();
        return (Channel<?, Selectable<OUT>>) outputChannel;
    }
}
