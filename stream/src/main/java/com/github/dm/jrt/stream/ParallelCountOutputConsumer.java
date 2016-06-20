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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Parallel by count output consumer.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class ParallelCountOutputConsumer<OUT> implements OutputConsumer<OUT> {

    private final HashMap<Channel<OUT, ?>, Channel<?, ?>> mChannels;

    private final Channel<OUT, ?>[] mInputs;

    private final Random mRandom = new Random();

    /**
     * Constructor.
     *
     * @param channels the map of input and invocation channels.
     */
    @SuppressWarnings("unchecked")
    ParallelCountOutputConsumer(
            @NotNull final Map<? extends Channel<OUT, ?>, ? extends Channel<?, ?>> channels) {
        mChannels = new HashMap<Channel<OUT, ?>, Channel<?, ?>>(channels);
        mInputs = new Channel[channels.size()];
    }

    public void onComplete() {
        for (final Channel<OUT, ?> channel : mChannels.keySet()) {
            channel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) {
        for (final Channel<OUT, ?> channel : mChannels.keySet()) {
            channel.abort(error);
        }
    }

    public void onOutput(final OUT output) {
        int count = 0;
        int minSize = Integer.MAX_VALUE;
        final Channel<OUT, ?>[] inputs = mInputs;
        final HashMap<Channel<OUT, ?>, Channel<?, ?>> channels = mChannels;
        for (final Entry<Channel<OUT, ?>, Channel<?, ?>> entry : channels.entrySet()) {
            final int channelSize = entry.getValue().inputCount();
            if (channelSize < minSize) {
                count = 1;
                inputs[0] = entry.getKey();
                minSize = channelSize;

            } else if (channelSize == minSize) {
                inputs[count++] = entry.getKey();
            }
        }

        final int i = (count == 1) ? 0 : Math.round((count - 1) * mRandom.nextFloat());
        inputs[i].pass(output);
    }
}
