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
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Output consumer performing a specific action on completion.
 * <p>
 * Created by davide-maestroni on 06/15/2016.
 *
 * @param <OUT> the output data type.
 */
public class CompleteOutputConsumer<OUT> implements OutputConsumer<OUT> {

    private final Runnable mCompleteAction;

    private final Channel<?, Void> mOutputChannel;

    /**
     * Constructor.
     *
     * @param action        the runnable instance.
     * @param outputChannel the output channel.
     */
    public CompleteOutputConsumer(@NotNull final Runnable action,
            @NotNull final Channel<?, Void> outputChannel) {
        mCompleteAction = ConstantConditions.notNull("runnable instance", action);
        mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
    }

    public void onComplete() throws Exception {
        try {
            mCompleteAction.run();

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
