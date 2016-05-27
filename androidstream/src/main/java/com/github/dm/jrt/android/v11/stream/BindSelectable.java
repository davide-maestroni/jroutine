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

package com.github.dm.jrt.android.v11.stream;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Selectable binding function.
 * <p>
 * Created by davide-maestroni on 05/08/2016.
 *
 * @param <OUT> the output data type.
 */
class BindSelectable<OUT> extends DeepEqualObject
        implements Function<OutputChannel<OUT>, OutputChannel<ParcelableSelectable<OUT>>> {

    private final ChannelConfiguration mConfiguration;

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param index         the selectable index.
     */
    BindSelectable(@NotNull final ChannelConfiguration configuration, final int index) {

        super(asArgs(ConstantConditions.notNull("channel configuration", configuration), index));
        mConfiguration = configuration;
        mIndex = index;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OutputChannel<ParcelableSelectable<OUT>> apply(final OutputChannel<OUT> channel) {

        final OutputChannel<? extends ParcelableSelectable<OUT>> outputChannel =
                SparseChannels.toSelectable(channel, mIndex)
                              .channelConfiguration()
                              .with(mConfiguration)
                              .apply()
                              .buildChannels();
        return (OutputChannel<ParcelableSelectable<OUT>>) outputChannel;
    }
}
