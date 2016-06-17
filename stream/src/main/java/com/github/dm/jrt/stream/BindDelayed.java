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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Delayed binding function.
 * <p>
 * Created by davide-maestroni on 05/29/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindDelayed<IN, OUT> implements Function<Channel<?, IN>, Channel<?, OUT>> {

    private final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

    private final long mDelay;

    private final Runner mRunner;

    private final TimeUnit mTimeUnit;

    /**
     * Constructor.
     *
     * @param runner          the runner instance.
     * @param delay           the delay value.
     * @param timeUnit        the delay time unit.
     * @param bindingFunction the binding function.
     */
    BindDelayed(@Nullable final Runner runner, final long delay, @NotNull final TimeUnit timeUnit,
            @NotNull final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
        mRunner = runner;
        mDelay = ConstantConditions.notNegative("delay value", delay);
        mTimeUnit = ConstantConditions.notNull("delay time unit", timeUnit);
        mBindingFunction = ConstantConditions.notNull("binding function", bindingFunction);
    }

    public Channel<?, OUT> apply(final Channel<?, IN> channel) throws Exception {
        final Channel<IN, IN> inputChannel =
                JRoutineCore.io().channelConfiguration().withRunner(mRunner).apply().buildChannel();
        final Channel<?, OUT> outOutputChannel = mBindingFunction.apply(inputChannel);
        inputChannel.after(mDelay, mTimeUnit).pass(channel).close();
        return outOutputChannel;
    }
}
