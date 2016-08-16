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

package com.github.dm.jrt.method;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelDecorator;

import org.jetbrains.annotations.NotNull;

/**
 * Channel implementation acting as input of a routine method.
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 *
 * @see com.github.dm.jrt.method.RoutineMethod RoutineMethod
 */
public final class InputChannel<IN> extends ChannelDecorator<IN, IN> {

    /**
     * Constructor.
     *
     * @param channel the wrapped channel.
     */
    InputChannel(@NotNull final Channel<IN, IN> channel) {
        super(channel);
    }
}
