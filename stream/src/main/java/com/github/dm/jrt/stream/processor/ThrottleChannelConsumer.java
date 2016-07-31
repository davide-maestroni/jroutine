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

package com.github.dm.jrt.stream.processor;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Invocation throttle channel consumer.
 * <p>
 * Created by davide-maestroni on 07/29/2016.
 *
 * @param <OUT> the output data type.
 */
class ThrottleChannelConsumer<OUT> implements ChannelConsumer<OUT> {

    private final CompletionHandler mHandler;

    private final Channel<OUT, ?> mOutputChannel;

    /**
     * Constructor.
     *
     * @param handler       the completion handler.
     * @param outputChannel the output channel.
     */
    ThrottleChannelConsumer(@NotNull final CompletionHandler handler,
            @NotNull final Channel<OUT, ?> outputChannel) {
        mHandler = ConstantConditions.notNull("completion handler", handler);
        mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
    }

    public void onComplete() {
        mOutputChannel.close();
        mHandler.onComplete();
    }

    public void onError(@NotNull final RoutineException error) {
        mOutputChannel.abort(error);
        mHandler.onComplete();
    }

    public void onOutput(final OUT output) {
        mOutputChannel.pass(output);
    }

    /**
     * Interface defining an invocation completion handler.
     */
    interface CompletionHandler {

        /**
         * Notifies the handler that the invocation has completed.
         */
        void onComplete();
    }
}
