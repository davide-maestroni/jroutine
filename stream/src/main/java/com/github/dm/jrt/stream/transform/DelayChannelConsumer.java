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
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Delay channel consumer.
 * <p>
 * Created by davide-maestroni on 06/29/2016.
 *
 * @param <OUT> the output data type.
 */
class DelayChannelConsumer<OUT> implements ChannelConsumer<OUT> {

    private final long mDelay;

    private final TimeUnit mDelayUnit;

    private final Channel<OUT, ?> mOutputChannel;

    private final Runner mRunner;

    private boolean mHasOutput;

    /**
     * Constructor.
     *
     * @param delay         the delay value.
     * @param timeUnit      the delay time unit.
     * @param runner        the runner instance.
     * @param outputChannel the output channel.
     */
    DelayChannelConsumer(final long delay, @NotNull final TimeUnit timeUnit,
            @NotNull final Runner runner, @NotNull final Channel<OUT, ?> outputChannel) {
        mDelay = ConstantConditions.notNegative("delay value", delay);
        mDelayUnit = ConstantConditions.notNull("delay unit", timeUnit);
        mRunner = ConstantConditions.notNull("runner instance", runner);
        mOutputChannel = ConstantConditions.notNull("output channel", outputChannel);
    }

    public void onComplete() throws Exception {
        if (!mHasOutput) {
            mRunner.run(new Execution() {

                public void run() {
                    mOutputChannel.close();
                }
            }, mDelay, mDelayUnit);

        } else {
            mOutputChannel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) {
        mOutputChannel.after(mDelay, mDelayUnit).abort(error);
    }

    public void onOutput(final OUT output) {
        mHasOutput = true;
        mOutputChannel.after(mDelay, mDelayUnit).pass(output);
    }
}
