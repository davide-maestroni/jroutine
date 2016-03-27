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

import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

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
    RepeatedChannelBuilder(@NotNull final OutputChannel<OUT> channel) {

        mChannel = ConstantConditions.notNull("output channel", channel);
    }

    @NotNull
    @Override
    protected OutputChannel<OUT> build(@NotNull final ChannelConfiguration configuration) {

        return new RepeatedChannel<OUT>(configuration, mChannel);
    }
}
