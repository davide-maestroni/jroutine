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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Action;

import org.jetbrains.annotations.NotNull;

/**
 * Try/finally channel consumer implementation.
 * <p>
 * Created by davide-maestroni on 04/21/2016.
 *
 * @param <OUT> the output data type.
 */
class TryFinallyChannelConsumer<OUT> implements ChannelConsumer<OUT> {

    private final Action mFinallyAction;

    private final Channel<OUT, ?> mOutputChannel;

    /**
     * Constructor.
     *
     * @param finallyAction the action instance.
     * @param outputChannel the output channel.
     */
    TryFinallyChannelConsumer(@NotNull final Action finallyAction,
            @NotNull final Channel<OUT, ?> outputChannel) {
        mFinallyAction = ConstantConditions.notNull("action instance", finallyAction);
        mOutputChannel = ConstantConditions.notNull("channel instance", outputChannel);
    }

    public void onComplete() throws Exception {
        try {
            mFinallyAction.perform();

        } finally {
            mOutputChannel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) throws Exception {
        try {
            mFinallyAction.perform();

        } finally {
            mOutputChannel.abort(error);
        }
    }

    public void onOutput(final OUT output) {
        mOutputChannel.pass(output);
    }
}
