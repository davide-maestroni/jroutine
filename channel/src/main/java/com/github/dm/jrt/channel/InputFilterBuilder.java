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
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
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
class InputFilterBuilder<IN> extends AbstractBuilder<IOChannel<Selectable<IN>>> {

    private final InputChannel<? super IN> mChannel;

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param channel the input channel.
     * @param index   the selectable index.
     */
    InputFilterBuilder(@NotNull final InputChannel<? super IN> channel, final int index) {

        mChannel = ConstantConditions.notNull("input channel", channel);
        mIndex = index;
    }

    @NotNull
    @Override
    protected IOChannel<Selectable<IN>> build(@NotNull final ChannelConfiguration configuration) {

        final IOChannel<Selectable<IN>> inputChannel = JRoutineCore.io()
                                                                   .getChannelConfiguration()
                                                                   .with(configuration)
                                                                   .setConfiguration()
                                                                   .buildChannel();
        final IOChannel<IN> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.bind(mChannel);
        return inputChannel.bind(new FilterOutputConsumer<IN>(ioChannel, mIndex));
    }

    /**
     * Output consumer filtering selectable data.
     *
     * @param <IN> the input data type.
     */
    private static class FilterOutputConsumer<IN> implements OutputConsumer<Selectable<IN>> {

        private final IOChannel<? super IN> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the input channel to feed.
         * @param index   the index to filter.
         */
        private FilterOutputConsumer(@NotNull final IOChannel<? super IN> channel,
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
