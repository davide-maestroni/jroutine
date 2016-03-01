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

package com.github.dm.jrt.android.v4.ext.channel;

import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.ext.channel.Selectable;

import org.jetbrains.annotations.NotNull;

/**
 * Output consumer sorting the output data among a map of channels.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class SortingMapOutputConsumer<OUT> implements OutputConsumer<Selectable<? extends OUT>> {

    private final SparseArrayCompat<IOChannel<OUT>> mChannels;

    /**
     * Constructor.
     *
     * @param channels the map of indexes and I/O channels.
     */
    SortingMapOutputConsumer(@NotNull final SparseArrayCompat<IOChannel<OUT>> channels) {

        final SparseArrayCompat<IOChannel<OUT>> channelMap = channels.clone();
        if (channelMap.indexOfValue(null) >= 0) {
            throw new NullPointerException("the map of I/O channels must not contain null objects");
        }

        mChannels = channelMap;
    }

    public void onComplete() {

        final SparseArrayCompat<IOChannel<OUT>> channels = mChannels;
        final int size = channels.size();
        for (int i = 0; i < size; ++i) {
            channels.valueAt(i).close();
        }
    }

    public void onError(@NotNull final RoutineException error) {

        final SparseArrayCompat<IOChannel<OUT>> channels = mChannels;
        final int size = channels.size();
        for (int i = 0; i < size; ++i) {
            channels.valueAt(i).abort(error);
        }
    }

    public void onOutput(final Selectable<? extends OUT> selectable) {

        final IOChannel<OUT> channel = mChannels.get(selectable.index);
        if (channel != null) {
            channel.pass(selectable.data);
        }
    }
}
