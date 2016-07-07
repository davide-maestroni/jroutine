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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Action;

import org.jetbrains.annotations.NotNull;

/**
 * Channel consumer performing a specific action on completion.
 * <p>
 * Created by davide-maestroni on 06/15/2016.
 *
 * @param <OUT> the output data type.
 */
class CompleteChannelConsumer<OUT> implements ChannelConsumer<OUT> {

    private final Action mCompleteAction;

    private final Channel<?, Void> mOutputChannel;

    /**
     * Constructor.
     *
     * @param action        the action instance.
     * @param outputChannel the output channel.
     */
    CompleteChannelConsumer(@NotNull final Action action,
            @NotNull final Channel<?, Void> outputChannel) {
        mCompleteAction = ConstantConditions.notNull("action instance", action);
        mOutputChannel = ConstantConditions.notNull("channel instance", outputChannel);
    }

    public void onComplete() throws Exception {
        try {
            mCompleteAction.perform();

        } finally {
            mOutputChannel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) {
        mOutputChannel.abort(error);
    }

    public void onOutput(final OUT output) {
    }
}
