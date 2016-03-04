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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.core.JRoutineCore;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel passing selectable data to an input channel.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class InputSelectBuilder<DATA, IN extends DATA> extends AbstractBuilder<IOChannel<IN>> {

    private final InputChannel<? super Selectable<DATA>> mChannel;

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param channel the input channel.
     * @param index   the selectable index.
     */
    @SuppressWarnings("ConstantConditions")
    InputSelectBuilder(@NotNull final InputChannel<? super Selectable<DATA>> channel,
            final int index) {

        if (channel == null) {
            throw new NullPointerException("the input channel must not be null");
        }

        mChannel = channel;
        mIndex = index;
    }

    @NotNull
    @Override
    protected IOChannel<IN> build(@NotNull final ChannelConfiguration configuration) {

        final IOChannel<IN> inputChannel =
                JRoutineCore.io().withChannels().with(configuration).getConfigured().buildChannel();
        final IOChannel<Selectable<DATA>> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.passTo(mChannel);
        return inputChannel.passTo(new SelectableOutputConsumer<DATA, IN>(ioChannel, mIndex));
    }
}
