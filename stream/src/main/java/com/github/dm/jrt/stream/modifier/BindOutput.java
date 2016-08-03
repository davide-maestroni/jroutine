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

package com.github.dm.jrt.stream.modifier;

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.Nullable;

/**
 * Binding function generating the output returned by a channel.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 *
 * @param <OUT> the output data type.
 */
class BindOutput<OUT> implements Function<Channel<?, ?>, Channel<?, OUT>> {

    private final Channel<?, ? extends OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    BindOutput(@Nullable final Channel<?, ? extends OUT> channel) {
        mChannel = Channels.replay((channel != null) ? channel : JRoutineCore.io().<OUT>of())
                           .buildChannels();
    }

    @SuppressWarnings("unchecked")
    public Channel<?, OUT> apply(final Channel<?, ?> channel) {
        return (Channel<?, OUT>) mChannel;
    }
}
