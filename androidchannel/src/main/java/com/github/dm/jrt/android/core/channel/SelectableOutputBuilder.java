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

package com.github.dm.jrt.android.core.channel;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbstractBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel making an output one selectable.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class SelectableOutputBuilder<OUT>
        extends AbstractBuilder<OutputChannel<? extends ParcelableSelectable<OUT>>> {

    private final OutputChannel<? extends OUT> mChannel;

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param channel the output channel.
     * @param index   the selectable index.
     */
    @SuppressWarnings("ConstantConditions")
    SelectableOutputBuilder(@NotNull final OutputChannel<? extends OUT> channel, final int index) {

        if (channel == null) {
            throw new NullPointerException("the output channel must not be null");
        }

        mChannel = channel;
        mIndex = index;
    }

    @NotNull
    @Override
    protected OutputChannel<? extends ParcelableSelectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final IOChannel<ParcelableSelectable<OUT>> ioChannel =
                JRoutineCore.io().withChannels().with(configuration).getConfigured().buildChannel();
        mChannel.bindTo(new SelectableOutputConsumer<OUT, OUT>(ioChannel, mIndex));
        return ioChannel;
    }
}
