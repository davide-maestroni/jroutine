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

import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Split by count output consumer.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class SplitCountOutputConsumer<OUT> implements OutputConsumer<OUT> {

    private final ArrayList<IOChannel<OUT>> mChannels;

    private final int[] mIndexes;

    private final Random mRandom = new Random();

    /**
     * Constructor.
     *
     * @param channels the list of I/O channels.
     */
    SplitCountOutputConsumer(@NotNull final List<IOChannel<OUT>> channels) {

        mChannels = new ArrayList<IOChannel<OUT>>(channels);
        mIndexes = new int[channels.size()];
    }

    public void onComplete() {

        for (final IOChannel<OUT> channel : mChannels) {
            channel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) {

        for (final IOChannel<OUT> channel : mChannels) {
            channel.abort(error);
        }
    }

    public void onOutput(final OUT output) {

        int count = 0;
        int minSize = Integer.MAX_VALUE;
        final int[] indexes = mIndexes;
        final ArrayList<IOChannel<OUT>> channels = mChannels;
        final int size = channels.size();
        for (int i = 0; i < size; ++i) {
            final int channelSize = channels.get(i).size();
            if (channelSize < minSize) {
                count = 1;
                indexes[0] = i;
                minSize = channelSize;

            } else if (channelSize == minSize) {
                indexes[count++] = i;
            }
        }

        final int i = (count == 1) ? 0 : Math.round((count - 1) * mRandom.nextFloat());
        channels.get(indexes[i]).pass(output);
    }
}
