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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel making an input one selectable.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class InputFilterBuilder<IN> extends AbstractBuilder<Channel<Selectable<IN>, ?>> {

    private final Channel<? super IN, ?> mChannel;

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param channel the channel.
     * @param index   the selectable index.
     */
    InputFilterBuilder(@NotNull final Channel<? super IN, ?> channel, final int index) {
        mChannel = ConstantConditions.notNull("channel instance", channel);
        mIndex = index;
    }

    @NotNull
    @Override
    protected Channel<Selectable<IN>, ?> build(@NotNull final ChannelConfiguration configuration) {
        final Channel<Selectable<IN>, Selectable<IN>> inputChannel = JRoutineCore.io()
                                                                                 .channelConfiguration()
                                                                                 .with(configuration)
                                                                                 .configured()
                                                                                 .buildChannel();
        final Channel<IN, IN> outputChannel = JRoutineCore.io().buildChannel();
        outputChannel.bind(mChannel);
        return inputChannel.bind(new FilterChannelConsumer<IN>(outputChannel, mIndex));
    }

    /**
     * Channel consumer filtering selectable data.
     *
     * @param <IN> the input data type.
     */
    private static class FilterChannelConsumer<IN> implements ChannelConsumer<Selectable<IN>> {

        private final Channel<? super IN, ?> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the channel to feed.
         * @param index   the index to filter.
         */
        private FilterChannelConsumer(@NotNull final Channel<? super IN, ?> channel,
                final int index) {
            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {
            mChannel.close();
        }

        public void onError(@NotNull final RoutineException error) {
            mChannel.abort(error);
        }

        public void onOutput(final Selectable<IN> selectable) {
            if (selectable.index == mIndex) {
                mChannel.pass(selectable.data);
            }
        }
    }
}
