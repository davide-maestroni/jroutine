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

package com.github.dm.jrt.ext.channel;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.channel.Channel.OutputChannel;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a repeating channel.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class RepeatedChannelBuilder<OUT> extends AbstractBuilder<OutputChannel<OUT>> {

    private final OutputChannel<OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the output channel.
     */
    @SuppressWarnings("ConstantConditions")
    RepeatedChannelBuilder(@NotNull final OutputChannel<OUT> channel) {

        if (channel == null) {
            throw new NullPointerException("the output channel must not be null");
        }

        mChannel = channel;
    }

    @NotNull
    @Override
    protected OutputChannel<OUT> build(@NotNull final ChannelConfiguration configuration) {

        return new RepeatedChannel<OUT>(configuration, mChannel);
    }
}
